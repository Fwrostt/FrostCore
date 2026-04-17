package dev.frost.frostcore.manager;

import dev.frost.frostcore.glow.GlowColor;
import dev.frost.frostcore.Main;
import dev.frost.frostcore.utils.FrostLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Glow system using per-viewer private scoreboards.
 *
 * Why: A player can only belong to one scoreboard Team at a time.
 * LuckPerms assigns players to rank teams on the MAIN scoreboard for prefix
 * colouring — overwriting any glow team we set there, making the glow colour
 * follow the rank colour.
 *
 * Fix: every online player gets their own private Scoreboard.  We register
 * all glow_* teams on that board and manage glowing players' entries there.
 * LuckPerms never touches private boards, so glow colours are always correct.
 */
public class GlowManager {

    private static GlowManager instance;

    /** UUID of the glowing player → their chosen GlowColor. */
    private final Map<UUID, GlowColor> activeGlows = new ConcurrentHashMap<>();

    /** UUID of a viewer → their private Scoreboard (with glow teams on it). */
    private final Map<UUID, Scoreboard> viewerBoards = new ConcurrentHashMap<>();

    public GlowManager() {
        instance = this;
        // Give every already-online player a board (reload-safe).
        for (Player p : Bukkit.getOnlinePlayers()) {
            initBoard(p);
        }
        FrostLogger.info("Glow system initialised (per-viewer boards) with "
                + GlowColor.values().length + " colours.");
    }

    public static GlowManager getInstance() { return instance; }

    // ── Board lifecycle ────────────────────────────────────────────────────────

    /**
     * Called when a player joins: creates their private board, registers all
     * glow teams, and populates it with whoever is currently glowing.
     */
    public void handleJoin(Player viewer) {
        initBoard(viewer);
    }

    /**
     * Called when a player quits: remove their glow (if any) from all other
     * viewers' boards, then discard their own board.
     */
    public void handleQuit(Player player) {
        // Remove this player's entry from every other viewer's board.
        GlowColor color = activeGlows.remove(player.getUniqueId());
        if (color != null) {
            for (Map.Entry<UUID, Scoreboard> entry : viewerBoards.entrySet()) {
                if (entry.getKey().equals(player.getUniqueId())) continue;
                Team team = entry.getValue().getTeam("glow_" + color.name().toLowerCase());
                if (team != null) team.removeEntry(player.getName());
            }
        }
        player.setGlowing(false);

        // Discard this player's private board (set them back to the main board).
        viewerBoards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    // ── Glow API ───────────────────────────────────────────────────────────────

    public void setGlow(Player player, GlowColor color) {
        removeGlowEntry(player); // clear old color first

        activeGlows.put(player.getUniqueId(), color);
        player.setGlowing(true);

        // Add this player's name to the matching glow team on every viewer's board.
        String teamName = "glow_" + color.name().toLowerCase();
        for (Map.Entry<UUID, Scoreboard> entry : viewerBoards.entrySet()) {
            Team team = entry.getValue().getTeam(teamName);
            if (team != null) team.addEntry(player.getName());
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
        for (Map.Entry<UUID, Scoreboard> entry : viewerBoards.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null) p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        activeGlows.clear();
        viewerBoards.clear();
    }

    // ── Internals ──────────────────────────────────────────────────────────────

    /** Create a fresh private Scoreboard for this viewer, register glow teams,
     *  populate with whoever is currently glowing, and assign it to the player. */
    private void initBoard(Player viewer) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();

        // Register a team for every glow colour.
        for (GlowColor color : GlowColor.values()) {
            String teamName = "glow_" + color.name().toLowerCase();
            Team team = board.registerNewTeam(teamName);
            team.color(color.getNamedColor());
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        }

        // Add every currently-glowing player to the correct team.
        for (Map.Entry<UUID, GlowColor> glow : activeGlows.entrySet()) {
            Player glowingPlayer = Bukkit.getPlayer(glow.getKey());
            if (glowingPlayer == null) continue;
            Team team = board.getTeam("glow_" + glow.getValue().name().toLowerCase());
            if (team != null) team.addEntry(glowingPlayer.getName());
        }

        viewerBoards.put(viewer.getUniqueId(), board);
        viewer.setScoreboard(board);
    }

    /** Remove a player's name from all glow teams on every viewer's board,
     *  and clear them from the activeGlows map. */
    private void removeGlowEntry(Player player) {
        GlowColor old = activeGlows.remove(player.getUniqueId());
        if (old == null) return;
        String teamName = "glow_" + old.name().toLowerCase();
        for (Scoreboard board : viewerBoards.values()) {
            Team team = board.getTeam(teamName);
            if (team != null) team.removeEntry(player.getName());
        }
    }
}
