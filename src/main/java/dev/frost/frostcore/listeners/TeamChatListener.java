package dev.frost.frostcore.listeners;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.manager.TeamManager;
import dev.frost.frostcore.teams.Team;
import dev.frost.frostcore.utils.FrostLogger;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class TeamChatListener implements Listener {

    private final TeamManager teamManager = TeamManager.getInstance();
    private final MessageManager mm = MessageManager.get();

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (!Main.getConfigManager().getBoolean("teams.team-chat", true)) return;

        if (!teamManager.hasTeam(player.getUniqueId())) return;

        try {
            Team team = teamManager.getTeam(player.getUniqueId());

            if (!team.isTeamChatEnabled(player.getUniqueId())) return;

            event.setCancelled(true);

            String plainMessage = PlainTextComponentSerializer.plainText().serialize(event.message());

            Map<String, String> placeholders = Map.of(
                    "player", player.getName(),
                    "chat", plainMessage
            );
            Component formatted = mm.getComponent("teams.chat-format", placeholders);

            Set<UUID> allMembers = new java.util.HashSet<>();
            allMembers.addAll(team.getOwners());
            allMembers.addAll(team.getAdmins());
            allMembers.addAll(team.getMembers());

            for (UUID uuid : allMembers) {
                Player teammate = Bukkit.getPlayer(uuid);
                if (teammate != null && teammate.isOnline()) {
                    teammate.sendMessage(formatted);
                }
            }

            Bukkit.getConsoleSender().sendMessage(
                    Component.text("[TeamChat:" + team.getName() + "] ").append(formatted)
            );

        } catch (Exception e) {

            FrostLogger.warn("TeamChatListener error for " + player.getName() + ": " + e.getMessage());
        }
    }
}

