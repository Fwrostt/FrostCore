package dev.frost.frostcore.listeners;

import dev.frost.frostcore.manager.GlowManager;
import dev.frost.frostcore.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class GlowListener implements Listener {

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        GlowManager mgr = Main.getGlowManager();
        if (mgr != null) {
            mgr.handleQuit(event.getPlayer());
        }
    }
}
