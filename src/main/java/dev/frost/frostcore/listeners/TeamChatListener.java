package dev.frost.frostcore.listeners;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.manager.TeamManager;
import dev.frost.frostcore.teams.Team;
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

/**
 * Routes chat messages to team-only when a player has team chat enabled.
 * <p>
 * When team chat is ON for a player:
 * - The global chat message is cancelled
 * - The message is sent only to online teammates
 * - Uses the configurable teams.chat-format from messages.yml
 */
public class TeamChatListener implements Listener {

    private final TeamManager teamManager = TeamManager.getInstance();
    private final MessageManager mm = MessageManager.get();

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        // Check if team chat feature is enabled globally
        if (!Main.getConfigManager().getBoolean("teams.team-chat", true)) return;

        // Check if the player is in a team
        if (!teamManager.hasTeam(player.getUniqueId())) return;

        try {
            Team team = teamManager.getTeam(player.getUniqueId());

            // Check if this player has team chat toggled on
            if (!team.isTeamChatEnabled(player.getUniqueId())) return;

            // Cancel the global message
            event.setCancelled(true);

            // Extract plain text from the Component message
            String plainMessage = PlainTextComponentSerializer.plainText().serialize(event.message());

            // Build the team chat message using the format from messages.yml
            Map<String, String> placeholders = Map.of(
                    "player", player.getName(),
                    "chat", plainMessage
            );
            Component formatted = mm.getComponent("teams.chat-format", placeholders);

            // Send to all online team members
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

            // Also log to console so admins can see
            Bukkit.getConsoleSender().sendMessage(
                    Component.text("[TeamChat:" + team.getName() + "] ").append(formatted)
            );

        } catch (Exception ignored) {
            // If anything fails, let the message go through normally
        }
    }
}
