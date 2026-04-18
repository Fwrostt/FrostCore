package dev.frost.frostcore.utils;

import dev.frost.frostcore.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class SignPrompt implements Listener {

    private static final Map<UUID, SignPrompt> ACTIVE_PROMPTS = new HashMap<>();

    private final Player player;
    private final Consumer<String> callback;
    private final Location signLocation;
    private final BlockData previousData;
    private boolean active = true;
    private int timeoutTask = -1;

    private SignPrompt(Player player, String[] lines, Consumer<String> callback) {
        // Cleanup any existing prompt for this player to prevent exploits
        if (ACTIVE_PROMPTS.containsKey(player.getUniqueId())) {
            ACTIVE_PROMPTS.remove(player.getUniqueId()).cleanup();
        }

        this.player = player;
        this.callback = callback;
        
        Location loc = player.getLocation().clone().add(0, 1, 0);
        
        // Find safe air block
        for (int i = 0; i < 4; i++) {
            if (loc.getBlock().getType().isAir()) {
                break;
            }
            loc.add(0, 1, 0);
        }

        // Crucial: Snap coordinates strictly to integers! If we compare with decimal events, it fails!
        this.signLocation = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        Block block = signLocation.getBlock();
        this.previousData = block.getBlockData();

        // Place physical sign state safely
        block.setType(Material.OAK_WALL_SIGN, false);
        Sign sign = (Sign) block.getState();

        if (lines != null) {
            for (int i = 0; i < Math.min(4, lines.length); i++) {
                sign.setLine(i, lines[i]);
            }
        } else {
            sign.setLine(1, "^^^");
            sign.setLine(2, "Type to search");
        }
        // Force the block entity to update immediately
        sign.update(true, false);

        ACTIVE_PROMPTS.put(player.getUniqueId(), this);
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());

        // Introduce a slight delay to let the server's chunk propagate, then feed exact packets.
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            if (!active) return;
            player.sendSignChange(signLocation, sign.getLines());
            player.openSign(sign);
        }, 3L);

        // Fail-safe cleanup: Force closure after 60 seconds if they afk or GUI desyncs
        this.timeoutTask = Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getInstance(), () -> {
            if (active) cleanAndUnregister();
        }, 20L * 60);
    }

    public static void prompt(Player player, String[] lines, Consumer<String> callback) {
        new SignPrompt(player, lines, callback);
    }

    public static void cleanupAll() {
        for (SignPrompt prompt : ACTIVE_PROMPTS.values()) {
            prompt.cleanup();
        }
        ACTIVE_PROMPTS.clear();
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (!active || !event.getPlayer().equals(player)) return;

        Location evLoc = event.getBlock().getLocation();
        // Exact block alignment matching
        if (evLoc.getBlockX() == signLocation.getBlockX() &&
            evLoc.getBlockY() == signLocation.getBlockY() &&
            evLoc.getBlockZ() == signLocation.getBlockZ()) {
            
            cleanAndUnregister();
            event.setCancelled(true);

            String input = event.getLine(0);
            if (input == null) input = "";
            
            final String finalInput = input;
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> callback.accept(finalInput.trim()));
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!active || !event.getPlayer().equals(player)) return;
        // If they walk/fall away, purge it (distance squared > 0.01 ignores head rotations)
        if (event.getFrom().distanceSquared(event.getTo()) > 0.05) {
            cleanAndUnregister();
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!active || !event.getPlayer().equals(player)) return;
        cleanAndUnregister();
    }
    
    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!active || !event.getPlayer().equals(player)) return;
        cleanAndUnregister();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!active || !event.getPlayer().equals(player)) return;
        cleanAndUnregister();
    }

    // ── Block Protection ───────────────────────────────────────────────

    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        if (active && event.getBlock().getLocation().equals(signLocation)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPhysics(org.bukkit.event.block.BlockPhysicsEvent event) {
        if (active && event.getBlock().getLocation().equals(signLocation)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        if (active) {
            event.blockList().removeIf(b -> b.getLocation().equals(signLocation));
        }
    }

    @EventHandler
    public void onBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        if (active) {
            event.blockList().removeIf(b -> b.getLocation().equals(signLocation));
        }
    }

    private void cleanAndUnregister() {
        active = false;
        if (timeoutTask != -1) {
            Bukkit.getScheduler().cancelTask(timeoutTask);
        }
        ACTIVE_PROMPTS.remove(player.getUniqueId());
        cleanup();
    }

    private void cleanup() {
        HandlerList.unregisterAll(this);
        // revert block precisely to what it was
        Block block = signLocation.getBlock();
        if (block.getType() == Material.OAK_WALL_SIGN) {
            block.setBlockData(previousData, false);
        }
    }
}
