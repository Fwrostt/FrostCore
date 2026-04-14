package dev.frost.frostcore.manager;

import dev.frost.frostcore.glow.GlowColor;
import dev.frost.frostcore.Main;
import dev.frost.frostcore.utils.FrostLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GlowManager {

    private static GlowManager instance;

    private final Map<UUID, GlowColor> activeGlows = new ConcurrentHashMap<>();
    private final Map<String, Team> glowTeams = new HashMap<>();

    public GlowManager() {
        instance = this;
        setupTeams();
        FrostLogger.info("Glow system initialized with " + GlowColor.values().length + " colors.");
    }

    public static GlowManager getInstance() { return instance; }

    private void setupTeams() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();

        for (GlowColor color : GlowColor.values()) {
            String teamName = "glow_" + color.name().toLowerCase();
            Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
            }
            team.color(color.getNamedColor());
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            glowTeams.put(color.name(), team);
        }
    }

    public void setGlow(Player player, GlowColor color) {
        removeGlow(player);

        Team team = glowTeams.get(color.name());
        if (team == null) return;

        team.addEntry(player.getName());
        player.setGlowing(true);
        activeGlows.put(player.getUniqueId(), color);
    }

    public void removeGlow(Player player) {
        GlowColor current = activeGlows.remove(player.getUniqueId());
        if (current != null) {
            Team team = glowTeams.get(current.name());
            if (team != null) {
                team.removeEntry(player.getName());
            }
        }
        player.setGlowing(false);
    }

    public boolean hasGlow(Player player) {
        return activeGlows.containsKey(player.getUniqueId());
    }

    public GlowColor getGlow(Player player) {
        return activeGlows.get(player.getUniqueId());
    }

    public boolean hasPermission(Player player, GlowColor color) {
        return player.hasPermission("frostcore.glow.*")
                || player.hasPermission("frostcore.glow." + color.name().toLowerCase());
    }

    public List<GlowColor> getAvailableColors(Player player) {
        List<GlowColor> available = new ArrayList<>();
        for (GlowColor color : GlowColor.values()) {
            if (hasPermission(player, color)) {
                available.add(color);
            }
        }
        return available;
    }

    public void handleQuit(Player player) {
        removeGlow(player);
    }

    public void cleanup() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        for (GlowColor color : GlowColor.values()) {
            String teamName = "glow_" + color.name().toLowerCase();
            Team team = board.getTeam(teamName);
            if (team != null) {
                team.unregister();
            }
        }
        activeGlows.clear();
        glowTeams.clear();
    }
}
