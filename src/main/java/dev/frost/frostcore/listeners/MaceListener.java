package dev.frost.frostcore.listeners;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.mace.*;
import dev.frost.frostcore.manager.MaceManager;
import dev.frost.frostcore.manager.MessageManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class MaceListener implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private MaceManager manager() {
        return Main.getMaceManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (manager() == null || !manager().isEnabled()) return;

        ItemStack result = event.getInventory().getResult();
        if (result == null || !manager().isTrackedMaterial(result.getType())) return;

        if (!(event.getView().getPlayer() instanceof Player player)) return;

        if (!manager().canCraftMace(player)) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (manager() == null || !manager().isEnabled()) return;

        ItemStack result = event.getInventory().getResult();
        if (result == null || !manager().isTrackedMaterial(result.getType())) return;

        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!manager().canCraftMace(player)) {
            event.setCancelled(true);
            MessageManager mm = Main.getMessageManager();
            if (manager().hasReachedGlobalLimit()) {
                mm.sendRaw(player, "<#6B8DAE>MACE <dark_gray>» <#D4727A>Server mace limit reached <dark_gray>(" +
                        manager().getActiveMaceCount() + "/" + manager().getMaxMacesOverall() + ")");
            } else {
                mm.sendRaw(player, "<#6B8DAE>MACE <dark_gray>» <#D4727A>You've reached your personal mace limit <dark_gray>(" +
                        manager().getMaxMacesPerPlayer() + ")");
            }
            return;
        }

        manager().registerMace(player, result);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        if (manager() == null || !manager().isEnabled()) return;

        ItemStack item = event.getItem();
        if (!manager().isTrackedMaterial(item.getType())) return;
        if (event.getEnchanter().hasPermission("frostcore.mace.bypass.enchant")) return;

        for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : event.getEnchantsToAdd().entrySet()) {
            String name = entry.getKey().getKey().getKey().toUpperCase();
            int level = entry.getValue();
            if (!manager().isEnchantAllowed(name, level)) {
                event.setCancelled(true);
                Main.getMessageManager().sendRaw(event.getEnchanter(),
                        "<#6B8DAE>MACE <dark_gray>» <#D4727A>" + name + " " + level
                                + " exceeds the limit of " + manager().getEnchantCap(name));
                return;
            }
        }

        String maceId = manager().getMaceId(item);
        if (maceId != null) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                manager().updateEnchantments(maceId, item);
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAnvil(PrepareAnvilEvent event) {
        if (manager() == null || !manager().isEnabled()) return;

        ItemStack result = event.getResult();
        if (result == null || !manager().isTrackedMaterial(result.getType())) return;

        if (event.getView().getPlayer() instanceof Player player) {
            if (player.hasPermission("frostcore.mace.bypass.enchant")) return;
        }

        for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : result.getEnchantments().entrySet()) {
            String name = entry.getKey().getKey().getKey().toUpperCase();
            int level = entry.getValue();
            if (!manager().isEnchantAllowed(name, level)) {
                event.setResult(null);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        if (manager() == null || !manager().isEnabled()) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (item == null) return;

        String maceId = manager().getMaceId(item);
        if (maceId != null) {
            manager().updateHolder(maceId, player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (manager() == null || !manager().isEnabled()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack current = event.getCurrentItem();
        if (current != null) {
            String maceId = manager().getMaceId(current);
            if (maceId != null) {
                manager().updateHolder(maceId, player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (manager() == null || !manager().isEnabled()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack item = event.getItem().getItemStack();
        String maceId = manager().getMaceId(item);
        if (maceId != null) {
            manager().updateHolder(maceId, player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (manager() == null || !manager().isEnabled()) return;

        ItemStack item = event.getItemDrop().getItemStack();
        String maceId = manager().getMaceId(item);
        if (maceId != null) {
            Location loc = event.getItemDrop().getLocation();
            manager().updateLocation(maceId, loc);
            manager().clearHolder(maceId);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        if (manager() == null || !manager().isEnabled()) return;

        Player player = event.getEntity();
        Location deathLoc = player.getLocation();

        for (int i = event.getDrops().size() - 1; i >= 0; i--) {
            ItemStack drop = event.getDrops().get(i);
            String maceId = manager().getMaceId(drop);
            if (maceId != null) {
                if (manager().isDisableOnDeath()) {
                    event.getDrops().remove(i);
                    manager().destroyMace(maceId);
                    Main.getMessageManager().sendRaw(player,
                            "<#6B8DAE>MACE <dark_gray>» <#D4727A>Your mace was destroyed on death.");
                } else {
                    manager().updateLocation(maceId, deathLoc);
                    manager().clearHolder(maceId);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDespawn(ItemDespawnEvent event) {
        if (manager() == null || !manager().isEnabled()) return;

        ItemStack item = event.getEntity().getItemStack();
        String maceId = manager().getMaceId(item);
        if (maceId != null) {
            manager().destroyMace(maceId);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (manager() == null || !manager().isEnabled()) return;

        Player player = event.getPlayer();

        manager().processPendingRemovals(player);

        String action = manager().getUntrackedAction();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !manager().isTrackedMaterial(item.getType())) continue;

            String existing = manager().getMaceId(item);
            if (existing != null) {
                manager().updateHolder(existing, player);
                continue;
            }

            switch (action.toUpperCase()) {
                case "TRACK" -> {
                    if (!manager().hasReachedGlobalLimit()) {
                        manager().registerUntrackedMace(player, item);
                    }
                }
                case "CONFISCATE" -> {
                    item.setAmount(0);
                    Main.getMessageManager().sendRaw(player,
                            "<#6B8DAE>MACE <dark_gray>» <#D4727A>Unregistered mace confiscated.");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (manager() == null || !manager().isEnabled()) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player)) return;

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (!manager().isTrackedMaterial(weapon.getType())) return;

        if (manager().isWorldRestricted(attacker.getWorld().getName())) {
            event.setCancelled(true);
            Main.getMessageManager().sendRaw(attacker,
                    "<#6B8DAE>MACE <dark_gray>» <#D4727A>Maces are restricted in this world.");
            return;
        }

        if (manager().isOnPvpCooldown(attacker.getUniqueId())) {
            event.setCancelled(true);
            long remaining = manager().getPvpCooldownRemaining(attacker.getUniqueId());
            Main.getMessageManager().sendRaw(attacker,
                    "<#6B8DAE>MACE <dark_gray>» <#D4727A>Mace on cooldown <dark_gray>(" +
                            String.format("%.1f", remaining / 1000.0) + "s)");
            return;
        }

        if (manager().getDamageCap() > 0 && event.getDamage() > manager().getDamageCap()) {
            event.setDamage(manager().getDamageCap());
        }

        if (manager().hasPvpCooldown()) {
            manager().setPvpCooldown(attacker.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (manager() == null || !manager().isEnabled()) return;
        if (!event.hasItem()) return;

        ItemStack item = event.getItem();
        if (item == null || !manager().isTrackedMaterial(item.getType())) return;

        Player player = event.getPlayer();
        if (manager().isWorldRestricted(player.getWorld().getName())) {
            if (!player.hasPermission("frostcore.mace.bypass")) {
                event.setCancelled(true);
                Main.getMessageManager().sendRaw(player,
                        "<#6B8DAE>MACE <dark_gray>» <#D4727A>Maces are restricted in this world.");
            }
        }
    }
}
