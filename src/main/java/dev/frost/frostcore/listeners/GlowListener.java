package dev.frost.frostcore.listeners;

import dev.frost.frostcore.manager.GlowManager;
import dev.frost.frostcore.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GlowListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        GlowManager mgr = Main.getGlowManager();
        if (mgr != null) mgr.handleJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        GlowManager mgr = Main.getGlowManager();
        if (mgr != null) mgr.handleQuit(event.getPlayer());
    }
}
