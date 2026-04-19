package dev.frost.frostcore.manager;

import dev.frost.frostcore.glow.GlowColor;
import dev.frost.frostcore.Main;
import dev.frost.frostcore.utils.FrostLogger;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Glow system using per-viewer private scoreboards with per-player glow teams.
 *
 * <h3>Problem</h3>
 * Minecraft ties glow outline colour to the scoreboard Team colour of the glowing
 * entity.  LuckPerms assigns players to rank-based teams for prefix colouring.
 * If we simply add a player to a shared glow team, they lose their LP prefix/suffix.
 * If LP overwrites our scoreboard, glow colours revert to the rank colour.
 *
 * <h3>Solution</h3>
 * <ol>
 *   <li>Wait for LuckPerms to finish setting up its scoreboard (delayed init).</li>
 *   <li>Create a private Scoreboard per viewer by <b>copying all LP teams</b>
 *       so nametag prefixes, suffixes, and sort order are preserved.</li>
 *   <li>For each glowing player, create a <b>per-player glow team</b> ({@code fg_<name>})
 *       that carries the LP prefix/suffix but uses the selected glow colour.</li>
 *   <li>A periodic sync task updates prefix/suffix if LP changes a player's rank.</li>
 * </ol>
 */
public class GlowManager {

    private static GlowManager instance;

    private final Main plugin;

    /** UUID of the glowing player → their chosen GlowColor. */
    private final Map<UUID, GlowColor> activeGlows = new ConcurrentHashMap<>();

    /** UUID of a viewer → their private Scoreboard. */
    private final Map<UUID, Scoreboard> viewerBoards = new ConcurrentHashMap<>();

    /** UUID of glowing player → their original LP team name (for restoration on glow removal). */
    private final Map<UUID, String> originalTeamNames = new ConcurrentHashMap<>();

    /** Periodic task handle for LP prefix/suffix sync. */
    private BukkitTask syncTask;

    public GlowManager(Main plugin) {
        this.plugin = plugin;
        instance = this;

        // Give every already-online player a board (reload-safe).
        for (Player p : Bukkit.getOnlinePlayers()) {
            scheduleInitBoard(p);
        }

        // Periodic sync: every 5 seconds, refresh LP prefix/suffix on glow teams.
        syncTask = Bukkit.getScheduler().runTaskTimer(plugin, this::syncGlowPrefixes, 200L, 100L);

        FrostLogger.info("Glow system initialised (per-viewer boards) with "
                + GlowColor.values().length + " colours.");
    }

    public static GlowManager getInstance() { return instance; }

    // ── Board lifecycle ────────────────────────────────────────────────────────

    /**
     * Called when a player joins: schedules private board creation after LP finishes.
     */
    public void handleJoin(Player viewer) {
        scheduleInitBoard(viewer);
    }

    /**
     * Called when a player quits: remove their glow from all viewers' boards,
     * then discard their own board.
     */
    public void handleQuit(Player player) {
        GlowColor color = activeGlows.remove(player.getUniqueId());
        if (color != null) {
            String glowTeamName = glowTeamName(player);
            for (Map.Entry<UUID, Scoreboard> entry : viewerBoards.entrySet()) {
                if (entry.getKey().equals(player.getUniqueId())) continue;
                removeGlowTeamFromBoard(entry.getValue(), player, glowTeamName);
            }
            originalTeamNames.remove(player.getUniqueId());
        }
        player.setGlowing(false);
        viewerBoards.remove(player.getUniqueId());
    }

    // ── Glow API ───────────────────────────────────────────────────────────────

    public void setGlow(Player player, GlowColor color) {
        removeGlowEntry(player); // clear old color first

        activeGlows.put(player.getUniqueId(), color);
        player.setGlowing(true);

        // Apply per-player glow team on every viewer's board.
        for (Map.Entry<UUID, Scoreboard> entry : viewerBoards.entrySet()) {
            applyGlowOnBoard(entry.getValue(), player, color);
        }
    }

    public void removeGlow(Player player) {
        removeGlowEntry(player);
        player.setGlowing(false);
    }

    public boolean hasGlow(Player player) {
        return activeGlows.containsKey(player.getUniqueId());
    }

    public GlowColor getGlow(Player player) {
        return activeGlows.get(player.getUniqueId());
    }

    // ── Permission helpers ─────────────────────────────────────────────────────

    public boolean hasPermission(Player player, GlowColor color) {
        return player.hasPermission("frostcore.glow.*")
                || player.hasPermission("frostcore.glow." + color.name().toLowerCase());
    }

    public List<GlowColor> getAvailableColors(Player player) {
        List<GlowColor> available = new ArrayList<>();
        for (GlowColor color : GlowColor.values()) {
            if (hasPermission(player, color)) available.add(color);
        }
        return available;
    }

    // ── Cleanup ────────────────────────────────────────────────────────────────

    public void cleanup() {
        if (syncTask != null) syncTask.cancel();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (activeGlows.containsKey(p.getUniqueId())) {
                p.setGlowing(false);
            }
        }
        activeGlows.clear();
        viewerBoards.clear();
        originalTeamNames.clear();
    }

    // ── Internals ──────────────────────────────────────────────────────────────

    /**
     * Schedules board creation with a 20-tick delay so LuckPerms has time
     * to finish setting up its scoreboard teams on the player.
     */
    private void scheduleInitBoard(Player viewer) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (viewer.isOnline()) initBoard(viewer);
        }, 20L);
    }

    /**
     * Creates a fresh private Scoreboard by cloning all teams from the viewer's
     * current (LP-managed) board, then injects per-player glow teams for every
     * actively glowing player.
     */
    private void initBoard(Player viewer) {
        Scoreboard source = viewer.getScoreboard();
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();

        // Clone every LP team (preserves nametag prefixes, suffixes, sort order).
        for (Team src : source.getTeams()) {
            Team copy = board.registerNewTeam(src.getName());
            copy.color(src.color() instanceof net.kyori.adventure.text.format.NamedTextColor named
                    ? named : net.kyori.adventure.text.format.NamedTextColor.WHITE);
            copy.prefix(src.prefix());
            copy.suffix(src.suffix());
            copy.setAllowFriendlyFire(src.allowFriendlyFire());
            copy.setCanSeeFriendlyInvisibles(src.canSeeFriendlyInvisibles());
            try {
                copy.setOption(Team.Option.NAME_TAG_VISIBILITY,
                        src.getOption(Team.Option.NAME_TAG_VISIBILITY));
                copy.setOption(Team.Option.COLLISION_RULE,
                        src.getOption(Team.Option.COLLISION_RULE));
                copy.setOption(Team.Option.DEATH_MESSAGE_VISIBILITY,
                        src.getOption(Team.Option.DEATH_MESSAGE_VISIBILITY));
            } catch (Exception ignored) { /* older API guard */ }
            for (String entry : src.getEntries()) {
                copy.addEntry(entry);
            }
        }

        // Inject per-player glow teams for all actively glowing players.
        for (Map.Entry<UUID, GlowColor> glow : activeGlows.entrySet()) {
            Player glowing = Bukkit.getPlayer(glow.getKey());
            if (glowing != null) applyGlowOnBoard(board, glowing, glow.getValue());
        }

        viewerBoards.put(viewer.getUniqueId(), board);
        viewer.setScoreboard(board);
    }

    /**
     * Creates (or updates) a per-player glow team on the given board.
     * Preserves the player's LP prefix/suffix while setting the glow colour.
     */
    private void applyGlowOnBoard(Scoreboard board, Player glowing, GlowColor color) {
        String teamName = glowTeamName(glowing);

        // Read the player's current LP team info before we move them.
        Team lpTeam = board.getEntryTeam(glowing.getName());
        Component prefix = Component.empty();
        Component suffix = Component.empty();

        if (lpTeam != null) {
            prefix = lpTeam.prefix();
            suffix = lpTeam.suffix();
            originalTeamNames.putIfAbsent(glowing.getUniqueId(), lpTeam.getName());
            lpTeam.removeEntry(glowing.getName());
        }

        // Create or update the per-player glow team.
        Team glowTeam = board.getTeam(teamName);
        if (glowTeam == null) glowTeam = board.registerNewTeam(teamName);

        glowTeam.color(color.getNamedColor());
        glowTeam.prefix(prefix);
        glowTeam.suffix(suffix);
        try {
            glowTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        } catch (Exception ignored) {}

        if (!glowTeam.hasEntry(glowing.getName())) {
            glowTeam.addEntry(glowing.getName());
        }
    }

    /**
     * Removes a player's per-player glow team from a board and restores
     * their original LP team membership.
     */
    private void removeGlowTeamFromBoard(Scoreboard board, Player player, String teamName) {
        Team glowTeam = board.getTeam(teamName);
        if (glowTeam == null) return;

        glowTeam.removeEntry(player.getName());
        glowTeam.unregister();

        // Restore to original LP team.
        String origName = originalTeamNames.get(player.getUniqueId());
        if (origName != null) {
            Team lpTeam = board.getTeam(origName);
            if (lpTeam != null) lpTeam.addEntry(player.getName());
        }
    }

    /**
     * Removes a player's glow from all viewer boards and restores LP teams.
     */
    private void removeGlowEntry(Player player) {
        GlowColor old = activeGlows.remove(player.getUniqueId());
        if (old == null) return;

        String teamName = glowTeamName(player);
        for (Scoreboard board : viewerBoards.values()) {
            removeGlowTeamFromBoard(board, player, teamName);
        }
        originalTeamNames.remove(player.getUniqueId());
    }

    /** Generates a unique per-player glow team name. */
    private String glowTeamName(Player player) {
        return "fg_" + player.getName();
    }

    /**
     * Periodic sync: refreshes LP prefix/suffix on all glow teams to catch
     * rank changes, prefix updates, etc.  Reads from the main scoreboard
     * (where LP maintains canonical team data).
     */
    private void syncGlowPrefixes() {
        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();

        for (Map.Entry<UUID, GlowColor> glow : activeGlows.entrySet()) {
            Player glowing = Bukkit.getPlayer(glow.getKey());
            if (glowing == null) continue;

            // Read current LP prefix/suffix from the main scoreboard.
            String origTeam = originalTeamNames.get(glowing.getUniqueId());
            if (origTeam == null) continue;

            Team lpTeam = mainBoard.getTeam(origTeam);
            if (lpTeam == null) continue;

            Component prefix = lpTeam.prefix();
            Component suffix = lpTeam.suffix();
            String teamName = glowTeamName(glowing);

            // Push updated prefix/suffix to every viewer's glow team.
            for (Scoreboard board : viewerBoards.values()) {
                Team glowTeam = board.getTeam(teamName);
                if (glowTeam != null) {
                    glowTeam.prefix(prefix);
                    glowTeam.suffix(suffix);
                }
            }
        }
    }
}
