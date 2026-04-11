package dev.frost.frostcore.listeners;

import dev.frost.frostcore.manager.UtilityManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerUtilityListener implements Listener {

    private final UtilityManager utilityManager = UtilityManager.getInstance();
    private final MiniMessage mini = MiniMessage.miniMessage();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        if (utilityManager.isGodMode(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        utilityManager.applyNick(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String nick = utilityManager.getNickname(player.getUniqueId());
        
        if (nick != null) {
            Component displayName = mini.deserialize(nick);
            event.renderer((source, sourceDisplayName, message, audience) ->
                    Component.empty()
                            .append(displayName)
                            .append(Component.text(" "))
                            .append(mini.deserialize("<dark_gray>» <white>"))
                            .append(message)
            );
        }
    }
}
