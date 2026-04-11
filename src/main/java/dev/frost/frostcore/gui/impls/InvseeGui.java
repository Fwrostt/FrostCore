package dev.frost.frostcore.gui.impls;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.listeners.InvseeListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * InvSee++ — A premium interactive inventory viewer/editor.
 * <p>
 * Layout (6 rows, 54 slots):
 * <pre>
 * Row 0: [Helmet] [Chest] [Legs] [Boots] [filler] [Offhand] [filler] [Info] [Close]
 * Row 1: [Main Inventory 9-17]
 * Row 2: [Main Inventory 18-26]
 * Row 3: [Main Inventory 27-35]
 * Row 4: [--- separator ---]
 * Row 5: [Hotbar 0-8]
 * </pre>
 * All item slots are fully editable and sync back to the target player in real-time.
 */
public class InvseeGui {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    // GUI slot constants
    public static final int SLOT_HELMET     = 0;
    public static final int SLOT_CHESTPLATE = 1;
    public static final int SLOT_LEGGINGS   = 2;
    public static final int SLOT_BOOTS      = 3;
    public static final int SLOT_OFFHAND    = 5;
    public static final int SLOT_INFO       = 7;
    public static final int SLOT_CLOSE      = 8;

    /** Slots that are filler / decoration and should NOT be interactable. */
    public static final Set<Integer> LOCKED_SLOTS = Set.of(4, 6, 7, 8, 36, 37, 38, 39, 40, 41, 42, 43, 44);

    /** Maps a GUI slot → the corresponding player inventory slot index. */
    private static final Map<Integer, Integer> GUI_TO_PLAYER = new HashMap<>();

    static {
        // Armor
        GUI_TO_PLAYER.put(SLOT_HELMET, -1);     // Special: helmet
        GUI_TO_PLAYER.put(SLOT_CHESTPLATE, -2);  // Special: chestplate
        GUI_TO_PLAYER.put(SLOT_LEGGINGS, -3);    // Special: leggings
        GUI_TO_PLAYER.put(SLOT_BOOTS, -4);       // Special: boots
        GUI_TO_PLAYER.put(SLOT_OFFHAND, -5);     // Special: offhand

        // Main inventory (GUI slots 9-35 → player slots 9-35)
        for (int i = 9; i <= 35; i++) {
            GUI_TO_PLAYER.put(i, i);
        }

        // Hotbar (GUI slots 45-53 → player slots 0-8)
        for (int i = 0; i < 9; i++) {
            GUI_TO_PLAYER.put(45 + i, i);
        }
    }

    private final UUID targetUUID;
    private final String targetName;
    private final Inventory gui;
    private final boolean isOffline;

    /**
     * Create an InvSee GUI for an ONLINE player.
     */
    public InvseeGui(Player target) {
        this.targetUUID = target.getUniqueId();
        this.targetName = target.getName();
        this.isOffline = false;
        this.gui = Bukkit.createInventory(null, 54,
                MM.deserialize("<gradient:#FF5555:#FF55FF>InvSee++</gradient> <dark_gray>» <white>" + target.getName()));
        loadFromPlayer(target);
    }

    /**
     * Load the target player's inventory into the GUI.
     */
    private void loadFromPlayer(Player target) {
        PlayerInventory inv = target.getInventory();

        // Armor row
        gui.setItem(SLOT_HELMET, inv.getHelmet() != null ? inv.getHelmet().clone() : labeledAir("Helmet", Material.GLASS));
        gui.setItem(SLOT_CHESTPLATE, inv.getChestplate() != null ? inv.getChestplate().clone() : labeledAir("Chestplate", Material.GLASS));
        gui.setItem(SLOT_LEGGINGS, inv.getLeggings() != null ? inv.getLeggings().clone() : labeledAir("Leggings", Material.GLASS));
        gui.setItem(SLOT_BOOTS, inv.getBoots() != null ? inv.getBoots().clone() : labeledAir("Boots", Material.GLASS));
        gui.setItem(SLOT_OFFHAND, inv.getItemInOffHand().getType() != Material.AIR ? inv.getItemInOffHand().clone() : labeledAir("Offhand", Material.GLASS));

        // Main inventory (player slots 9-35 → GUI slots 9-35)
        for (int i = 9; i <= 35; i++) {
            ItemStack item = inv.getItem(i);
            gui.setItem(i, item != null ? item.clone() : null);
        }

        // Hotbar (player slots 0-8 → GUI slots 45-53)
        for (int i = 0; i < 9; i++) {
            ItemStack item = inv.getItem(i);
            gui.setItem(45 + i, item != null ? item.clone() : null);
        }

        // Filler / decoration
        ItemStack filler = createFiller();
        gui.setItem(4, filler);
        gui.setItem(6, filler);

        // Separator row (row 4: slots 36-44)
        ItemStack separator = createSeparator();
        for (int i = 36; i <= 44; i++) {
            gui.setItem(i, separator);
        }

        // Player info head
        gui.setItem(SLOT_INFO, createInfoHead(target));

        // Close button
        gui.setItem(SLOT_CLOSE, createCloseButton());
    }

    /**
     * Open the GUI for the viewing admin.
     */
    public void open(Player viewer) {
        InvseeListener.register(viewer.getUniqueId(), this);
        viewer.openInventory(gui);
    }

    /**
     * Sync all GUI contents back to the target online player.
     */
    public void syncToPlayer() {
        Player target = Bukkit.getPlayer(targetUUID);
        if (target == null || !target.isOnline()) return;

        PlayerInventory inv = target.getInventory();

        // Armor
        inv.setHelmet(cleanSlot(gui.getItem(SLOT_HELMET)));
        inv.setChestplate(cleanSlot(gui.getItem(SLOT_CHESTPLATE)));
        inv.setLeggings(cleanSlot(gui.getItem(SLOT_LEGGINGS)));
        inv.setBoots(cleanSlot(gui.getItem(SLOT_BOOTS)));
        inv.setItemInOffHand(cleanSlot(gui.getItem(SLOT_OFFHAND)) != null ? cleanSlot(gui.getItem(SLOT_OFFHAND)) : new ItemStack(Material.AIR));

        // Main inventory (GUI slots 9-35 → player slots 9-35)
        for (int i = 9; i <= 35; i++) {
            inv.setItem(i, gui.getItem(i));
        }

        // Hotbar (GUI slots 45-53 → player slots 0-8)
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, gui.getItem(45 + i));
        }
    }

    /**
     * Remove placeholder items (labeled glass) when syncing - they represent empty slots.
     */
    private ItemStack cleanSlot(ItemStack item) {
        if (item == null) return null;
        if (item.getType() == Material.GLASS && item.hasItemMeta()) {
            // This is our placeholder, treat as empty
            return null;
        }
        return item;
    }

    /**
     * Check if a slot is locked (decoration/filler).
     */
    public boolean isLockedSlot(int slot) {
        return LOCKED_SLOTS.contains(slot);
    }

    /**
     * Check if a slot is an interactive inventory slot.
     */
    public boolean isInteractiveSlot(int slot) {
        return !isLockedSlot(slot) && slot >= 0 && slot < 54;
    }

    public UUID getTargetUUID() { return targetUUID; }
    public String getTargetName() { return targetName; }
    public Inventory getInventory() { return gui; }
    public boolean isOffline() { return isOffline; }

    // ━━━ Item Builders ━━━

    private ItemStack labeledAir(String label, Material type) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize("<dark_gray><italic>" + label + " <gray>(empty)"));
        meta.lore(List.of(MM.deserialize("<dark_gray>Place an item here")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFiller() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSeparator() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize("<dark_gray>▼ Hotbar ▼"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInfoHead(Player target) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(target);
        meta.displayName(MM.deserialize("<gradient:#FF5555:#FF55FF>" + target.getName()));
        meta.lore(List.of(
                MM.deserialize("<dark_gray>━━━━━━━━━━━━━━━━"),
                MM.deserialize("<#B0C4FF>Health: <white>" + String.format("%.0f", target.getHealth()) + "/" + String.format("%.0f", target.getMaxHealth())),
                MM.deserialize("<#B0C4FF>Food: <white>" + target.getFoodLevel() + "/20"),
                MM.deserialize("<#B0C4FF>Gamemode: <white>" + target.getGameMode().name()),
                MM.deserialize("<#B0C4FF>World: <white>" + target.getWorld().getName()),
                MM.deserialize("<#B0C4FF>XP Level: <white>" + target.getLevel()),
                MM.deserialize("<dark_gray>━━━━━━━━━━━━━━━━")
        ));
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize("<#FF5555><bold>✘ Close"));
        item.setItemMeta(meta);
        return item;
    }
}
