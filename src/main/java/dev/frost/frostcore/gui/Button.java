package dev.frost.frostcore.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Fluent builder for {@link GuiItem}s.
 *
 * <pre>{@code
 * GuiItem btn = Button.of(Material.DIAMOND)
 *     .name("<aqua><bold>Click me!")
 *     .lore("<gray>Line 1", "<gray>Line 2")
 *     .glow()
 *     .onClick(ctx -> ctx.getPlayer().sendMessage("Hi!"))
 *     .build();
 * }</pre>
 */
public final class Button {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Button() {}

    // ── Entry points ─────────────────────────────────────────────────────────

    public static Builder of(Material material) {
        return new Builder(new ItemStack(material));
    }

    public static Builder of(ItemStack base) {
        return new Builder(base.clone());
    }

    // =========================================================================
    // Builder
    // =========================================================================

    public static final class Builder {

        private final ItemStack item;
        private GuiAction<ClickContext> action;

        private Builder(ItemStack item) {
            this.item = item;
        }

        // ── Display ───────────────────────────────────────────────────────────

        /** Set the display name using MiniMessage formatting. */
        public Builder name(String miniMessage) {
            withMeta(meta -> meta.displayName(MM.deserialize(miniMessage)));
            return this;
        }

        /** Set the display name directly from an Adventure {@link Component}. */
        public Builder name(Component component) {
            withMeta(meta -> meta.displayName(component));
            return this;
        }

        /**
         * Set the lore lines using MiniMessage formatting.
         * Each string becomes one lore line.
         */
        public Builder lore(String... lines) {
            withMeta(meta -> {
                List<Component> lore = new ArrayList<>();
                for (String line : lines) {
                    lore.add(MM.deserialize(line));
                }
                meta.lore(lore);
            });
            return this;
        }

        /** Set the lore from a list of MiniMessage strings. */
        public Builder lore(List<String> lines) {
            return lore(lines.toArray(new String[0]));
        }

        /** Append lore lines to any existing lore. */
        public Builder appendLore(String... lines) {
            withMeta(meta -> {
                List<Component> existing = meta.lore() != null
                        ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                for (String line : lines) existing.add(MM.deserialize(line));
                meta.lore(existing);
            });
            return this;
        }

        /** Set the stack size (1–64). */
        public Builder amount(int amount) {
            item.setAmount(Math.max(1, Math.min(64, amount)));
            return this;
        }

        /**
         * Add an invisible enchant + {@code HIDE_ENCHANTS} flag to make the item
         * appear to glow without showing any enchant tooltip.
         */
        public Builder glow() {
            withMeta(meta -> {
                meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            });
            return this;
        }

        /** Set a custom model data value (for resource-pack custom items). */
        public Builder model(int modelData) {
            withMeta(meta -> meta.setCustomModelData(modelData));
            return this;
        }

        /** Hide all item flags (enchants, attributes, etc.) */
        public Builder hideAll() {
            withMeta(meta -> meta.addItemFlags(ItemFlag.values()));
            return this;
        }

        /** Hide specific item flags. */
        public Builder hide(ItemFlag... flags) {
            withMeta(meta -> meta.addItemFlags(flags));
            return this;
        }

        /**
         * Apply a player head texture by UUID.
         * Only works when {@link Material#PLAYER_HEAD} is the item material.
         */
        public Builder skull(UUID playerUUID) {
            if (item.getType() == Material.PLAYER_HEAD && item.getItemMeta() instanceof SkullMeta skull) {
                skull.setOwningPlayer(org.bukkit.Bukkit.getOfflinePlayer(playerUUID));
                item.setItemMeta(skull);
            }
            return this;
        }

        /**
         * Apply a player head texture by player name.
         * Only works when {@link Material#PLAYER_HEAD} is the item material.
         */
        public Builder skull(String playerName) {
            if (item.getType() == Material.PLAYER_HEAD && item.getItemMeta() instanceof SkullMeta skull) {
                skull.setOwningPlayer(org.bukkit.Bukkit.getOfflinePlayer(playerName));
                item.setItemMeta(skull);
            }
            return this;
        }

        // ── Behaviour ─────────────────────────────────────────────────────────

        /** Assign a click action to this button. */
        public Builder onClick(GuiAction<ClickContext> action) {
            this.action = action;
            return this;
        }

        // ── Build ─────────────────────────────────────────────────────────────

        /** Build a {@link GuiItem} from this builder. */
        public GuiItem build() {
            return new GuiItem(item, action);
        }

        // ── Helpers ───────────────────────────────────────────────────────────

        @FunctionalInterface
        private interface MetaConsumer {
            void apply(ItemMeta meta);
        }

        private void withMeta(MetaConsumer consumer) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                consumer.apply(meta);
                item.setItemMeta(meta);
            }
        }
    }
}
