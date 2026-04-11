package dev.frost.frostcore.placeholderapi;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.exceptions.TeamException;
import dev.frost.frostcore.manager.TeamManager;
import dev.frost.frostcore.teams.Team;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

public class TeamExpansion extends PlaceholderExpansion {

    @Override
    public @NotNull String getAuthor() {
        return "frost";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "teamcore";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return null;
        UUID uuid = player.getUniqueId();
        TeamManager tm = Main.getTeamManager();

        switch (params) {
            case "has_team":
                return String.valueOf(tm.hasTeam(uuid));
        }

        if (!tm.hasTeam(uuid)) return "";

        try {
            Team team = tm.getTeam(uuid);

            return switch (params) {
                case "name" -> team.getName();
                case "tag" -> team.getTag();
                case "color" -> team.getColor();
                case "role" -> getRole(team, uuid);
                case "members" -> String.valueOf(team.getTotalMembers());
                case "members_online" -> String.valueOf(countOnline(team));
                case "warps" -> String.valueOf(team.getWarps().size());
                case "allies" -> String.valueOf(team.getAllies().size());
                case "enemies" -> String.valueOf(team.getEnemies().size());
                case "pvp" -> team.isPvpToggle() ? "enabled" : "disabled";
                case "home_set" -> String.valueOf(team.getHome() != null);
                case "owners" -> formatPlayerNames(team.getOwners());
                case "chat" -> String.valueOf(team.isTeamChatEnabled(uuid));
                default -> null;
            };
        } catch (TeamException e) {
            return null;
        }
    }

    /**
     * Get the player's role display name.
     */
    private String getRole(Team team, UUID uuid) {
        if (team.isOwner(uuid)) return "Owner";
        if (team.isAdmin(uuid)) return "Admin";
        return "Member";
    }

    /**
     * Count how many team members are currently online.
     */
    private int countOnline(Team team) {
        int count = 0;
        for (UUID uuid : team.getOwners()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) count++;
        }
        for (UUID uuid : team.getAdmins()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) count++;
        }
        for (UUID uuid : team.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) count++;
        }
        return count;
    }

    /**
     * Format a set of UUIDs into a comma-separated list of player names.
     */
    private String formatPlayerNames(Set<UUID> uuids) {
        StringBuilder sb = new StringBuilder();
        for (UUID uuid : uuids) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            if (sb.length() > 0) sb.append(", ");
            sb.append(op.getName() != null ? op.getName() : uuid.toString().substring(0, 8));
        }
        return sb.toString();
    }
}

