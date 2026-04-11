package dev.frost.frostcore.manager;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.database.DatabaseManager;
import dev.frost.frostcore.teams.Team;
import dev.frost.frostcore.utils.ItemStackSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages shared team ender chests.
 * <p>
 * Echest contents are cached in memory while open, and saved to the database
 * as Base64-encoded binary when the inventory is closed. Loading happens
 * on-demand (lazy) when a player opens their team's echest.
 * <p>
 * Inventory size is configurable via config.yml and snaps to multiples of 9.
 * <p>
 * Anti-dupe: All viewers share the SAME Inventory instance. On close, a
 * deep-copy snapshot is taken before the async save to prevent race conditions.
 * On disband/kick, viewers are force-closed before cache invalidation.
 */
public class TeamEchestManager {

    /**
     * Cache of open echest inventories, keyed by team name (lowercase).
     * Multiple players viewing the same team's echest share the SAME Inventory instance,
     * so changes are visible in real-time to all viewers.
     */
    private final Map<String, Inventory> openEchests = new ConcurrentHashMap<>();

    private final DatabaseManager db;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public TeamEchestManager(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Get the echest slot count from config, snapped to a valid inventory size (9-54).
     */
    public int getSlots() {
        int raw = Main.getConfigManager().getInt("teams.echest.slots", 27);

        int snapped = Math.max(9, Math.min(54, (raw / 9) * 9));
        if (snapped == 0) snapped = 9;
        return snapped;
    }

    /**
     * Open a team's echest for a player.
     * If another teammate already has it open, they share the same live Inventory.
     */
    public void openEchest(Player player, Team team) {
        String key = team.getName().toLowerCase();
        int slots = getSlots();

        Inventory cached = openEchests.get(key);
        if (cached != null) {
            openInventory(player, cached, key);
            return;
        }

        player.sendMessage(miniMessage.deserialize("<gray>Opening team echest...</gray>"));

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            String base64 = db.loadEchest(team.getName());

            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                if (!player.isOnline()) return;

                Inventory inv = openEchests.get(key);
                if (inv == null) {
                    inv = Bukkit.createInventory(null, slots,
                            miniMessage.deserialize("<gold>" + team.getName() + "'s Echest</gold>"));

                    if (base64 != null) {
                        ItemStack[] items = ItemStackSerializer.fromBase64(base64);
                        if (items != null) {
                            for (int i = 0; i < Math.min(items.length, slots); i++) {
                                inv.setItem(i, items[i]);
                            }
                        }
                    }
                    openEchests.put(key, inv);
                }

                openInventory(player, inv, key);
            });
        });
    }

    private void openInventory(Player player, Inventory inv, String key) {
        player.openInventory(inv);
        player.setMetadata("viewing_team_echest",
                new org.bukkit.metadata.FixedMetadataValue(Main.getInstance(), key));
    }

    /**
     * Called when a player closes an echest inventory.
     * Takes a deep-copy snapshot of items before async save to prevent race conditions.
     */
    public void handleClose(Player player) {
        if (!player.hasMetadata("viewing_team_echest")) return;

        String key = player.getMetadata("viewing_team_echest").get(0).asString();
        player.removeMetadata("viewing_team_echest", Main.getInstance());

        Inventory inv = openEchests.get(key);
        if (inv == null) return;

        ItemStack[] snapshot = deepCopy(inv.getContents());

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            String base64 = ItemStackSerializer.toBase64(snapshot);
            db.saveEchest(key, base64);
        });

        if (inv.getViewers().size() <= 1) {
            openEchests.remove(key);
        }
    }

    /**
     * Force-save all currently open echests. Called during onDisable.
     */
    public void saveAll() {
        for (Map.Entry<String, Inventory> entry : openEchests.entrySet()) {
            String teamName = entry.getKey();
            Inventory inv = entry.getValue();

            String base64 = ItemStackSerializer.toBase64(inv.getContents());
            db.saveEchest(teamName, base64);

            for (HumanEntity viewer : new ArrayList<>(inv.getViewers())) {
                if (viewer instanceof Player p) {
                    dropCursorItem(p);
                    p.closeInventory();
                    p.removeMetadata("viewing_team_echest", Main.getInstance());
                }
            }
        }
        openEchests.clear();
    }

    /**
     * Force-close all viewers and remove echest from cache (e.g. on disband).
     * Drops any items players are holding on their cursor to prevent dupes.
     */
    public void invalidate(String teamName) {
        String key = teamName.toLowerCase();
        Inventory inv = openEchests.remove(key);
        if (inv != null) {
            for (HumanEntity viewer : new ArrayList<>(inv.getViewers())) {
                if (viewer instanceof Player p) {
                    dropCursorItem(p);
                    p.closeInventory();
                    p.removeMetadata("viewing_team_echest", Main.getInstance());
                }
            }
        }
    }

    /**
     * Force-close a specific player's echest view.
     * Used when a player is kicked/leaves a team while viewing echest.
     */
    public void forceCloseForPlayer(Player player) {
        if (!player.hasMetadata("viewing_team_echest")) return;

        String key = player.getMetadata("viewing_team_echest").get(0).asString();
        player.removeMetadata("viewing_team_echest", Main.getInstance());

        dropCursorItem(player);
        player.closeInventory();

        Inventory inv = openEchests.get(key);
        if (inv != null) {
            ItemStack[] snapshot = deepCopy(inv.getContents());
            Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
                String base64 = ItemStackSerializer.toBase64(snapshot);
                db.saveEchest(key, base64);
            });

            if (inv.getViewers().isEmpty()) {
                openEchests.remove(key);
            }
        }
    }

    /**
     * Check if a player is currently viewing a team echest.
     */
    public static boolean isViewingEchest(Player player) {
        return player.hasMetadata("viewing_team_echest");
    }

    /**
     * Check if a specific inventory is a team echest managed by this system.
     */
    public boolean isEchestInventory(Inventory inventory) {
        return openEchests.containsValue(inventory);
    }

    /**
     * Deep-copy an ItemStack array to create a snapshot that is safe for async serialization.
     * This prevents race conditions where another viewer modifies items while the async
     * save is in progress.
     */
    private ItemStack[] deepCopy(ItemStack[] source) {
        if (source == null) return new ItemStack[0];
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i] != null ? source[i].clone() : null;
        }
        return copy;
    }

    /**
     * Drop any item the player is holding on their cursor back into the echest or at their feet.
     * Prevents the classic "grab item from shared inventory, then get force-closed" dupe.
     */
    private void dropCursorItem(Player player) {
        ItemStack cursor = player.getOpenInventory().getCursor();
        if (cursor != null && !cursor.getType().isAir()) {

            player.getOpenInventory().setCursor(null);
            var remaining = player.getInventory().addItem(cursor);
            for (ItemStack leftover : remaining.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
    }
}

