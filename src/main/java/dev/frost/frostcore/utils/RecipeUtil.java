package dev.frost.frostcore.utils;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.ConfigManager;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RecipeUtil {

    private static ConfigManager config() { return Main.getConfigManager(); }
    private static JavaPlugin plugin() { return Main.getInstance(); }

    private static final Map<String, ItemStack> premadeItems = new HashMap<>();

    public static Material getItemFromConfig(String path) {
        String materialName = config().getString(path);
        if (materialName == null) {
            return Material.BEDROCK;
        }
        Material material = Material.getMaterial(materialName.toUpperCase());
        return (material != null) ? material : Material.BEDROCK;
    }

    public static RecipeChoice getChoice(String path) {
        String itemKey = config().getString(path);
        if (itemKey == null) {
            return new RecipeChoice.MaterialChoice(Material.BEDROCK);
        }

        if (itemKey.toLowerCase().startsWith("frostcore_")) {
            return RecipeUtil.getExactChoice(itemKey);
        }

        Material mat = getItemFromConfig(path);
        return new RecipeChoice.MaterialChoice(Objects.requireNonNullElse(mat, Material.BEDROCK));
    }

    public static ItemStack getItemStackFromConfig(String path) {
        return new ItemStack(getItemFromConfig(path));
    }

    public static RecipeChoice.ExactChoice getExactChoice(String key) {
        ItemStack item = premadeItems.get(key);
        if (item != null) {
            return new RecipeChoice.ExactChoice(item);
        } else {
            return new RecipeChoice.ExactChoice(new ItemStack(Material.BEDROCK));
        }
    }

    public static void registerPremadeItem(String name, ItemStack item) {
        premadeItems.put(name, item);
    }
}

