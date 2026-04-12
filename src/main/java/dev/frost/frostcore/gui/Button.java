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


public final class Button {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Button() {}

    public static Builder of(Material material) {
        return new Builder(new ItemStack(material));
    }

    public static Builder of(ItemStack base) {
        return new Builder(base.clone());
    }

    public static final class Builder {

        private final ItemStack item;
        private GuiAction<ClickContext> action;

        private Builder(ItemStack item) {
            this.item = item;
        }

        
        public Builder name(String miniMessage) {
            withMeta(meta -> meta.displayName(MM.deserialize(miniMessage)));
            return this;
        }

        
        public Builder name(Component component) {
            withMeta(meta -> meta.displayName(component));
            return this;
        }

        
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

        
        public Builder lore(List<String> lines) {
            return lore(lines.toArray(new String[0]));
        }

        
        public Builder appendLore(String... lines) {
            withMeta(meta -> {
                List<Component> existing = meta.lore() != null
                        ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                for (String line : lines) existing.add(MM.deserialize(line));
                meta.lore(existing);
            });
            return this;
        }

        
        public Builder amount(int amount) {
            item.setAmount(Math.max(1, Math.min(64, amount)));
            return this;
        }

        
        public Builder glow() {
            withMeta(meta -> {
                meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            });
            return this;
        }

        
        public Builder model(int modelData) {
            withMeta(meta -> meta.setCustomModelData(modelData));
            return this;
        }

        
        public Builder hideAll() {
            withMeta(meta -> meta.addItemFlags(ItemFlag.values()));
            return this;
        }

        
        public Builder hide(ItemFlag... flags) {
            withMeta(meta -> meta.addItemFlags(flags));
            return this;
        }

        
        public Builder skull(UUID playerUUID) {
            if (item.getType() == Material.PLAYER_HEAD && item.getItemMeta() instanceof SkullMeta skull) {
                skull.setOwningPlayer(org.bukkit.Bukkit.getOfflinePlayer(playerUUID));
                item.setItemMeta(skull);
            }
            return this;
        }

        
        public Builder skull(String playerName) {
            if (item.getType() == Material.PLAYER_HEAD && item.getItemMeta() instanceof SkullMeta skull) {
                skull.setOwningPlayer(org.bukkit.Bukkit.getOfflinePlayer(playerName));
                item.setItemMeta(skull);
            }
            return this;
        }

        
        public Builder onClick(GuiAction<ClickContext> action) {
            this.action = action;
            return this;
        }

        
        public GuiItem build() {
            return new GuiItem(item, action);
        }

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

