package dev.frost.frostcore.listeners;

import dev.frost.frostcore.cmds.moderation.StaffChatCmd;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class StaffChatListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (!StaffChatCmd.isToggled(player.getUniqueId())) return;

        event.setCancelled(true);

        String plainMessage = PlainTextComponentSerializer.plainText().serialize(event.message());
        StaffChatCmd.broadcastStaffMessage(player, plainMessage);
    }
}
