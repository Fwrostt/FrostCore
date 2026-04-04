package dev.frost.frostcore.placeholderapi;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.exceptions.TeamException;
import dev.frost.frostcore.manager.TeamManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

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
        try {
            switch (params) {
                case "name":
                    return tm.getTeam(uuid).getName();
                case "members":
                    return String.valueOf(tm.getTeam(uuid).getTotalMembers());
                case "warps":
                    return String.valueOf(tm.getTeam(uuid).getWarps().size());
                case "allies":
                    return String.valueOf(tm.getTeam(uuid).getAllies().size());
                case "enemies":
                    return String.valueOf(tm.getTeam(uuid).getEnemies().size());

            }
        } catch (TeamException e) {
            return null;
        }

        return null;
    }
}
