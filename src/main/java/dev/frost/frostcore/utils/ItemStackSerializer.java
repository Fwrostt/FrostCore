package dev.frost.frostcore.utils;

import dev.frost.frostcore.utils.FrostLogger;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * Utility for serializing ItemStack arrays to/from Base64 strings.
 * <p>
 * Uses Bukkit's built-in object serialization which handles all item metadata,
 * enchantments, NBT data, custom model data, etc. The binary output is then
 * Base64-encoded for safe storage in any database TEXT/BLOB column.
 * <p>
 * This is the industry-standard approach used by EssentialsX, CMI, and other
 * major Minecraft plugins. It is far more compact and reliable than YAML-based
 * inventory serialization.
 */
public final class ItemStackSerializer {

    private ItemStackSerializer() {}

    /**
     * Serialize an ItemStack array to a Base64-encoded string.
     *
     * @param items the items to serialize (nulls are preserved as empty slots)
     * @return Base64 string, or null if serialization fails
     */
    public static String toBase64(ItemStack[] items) {
        if (items == null || items.length == 0) return null;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BukkitObjectOutputStream oos = new BukkitObjectOutputStream(baos)) {

            oos.writeInt(items.length);

            for (ItemStack item : items) {
                oos.writeObject(item);
            }

            oos.flush();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            FrostLogger.error("An error occurred", e);
            return null;
        }
    }

    /**
     * Deserialize a Base64-encoded string back to an ItemStack array.
     *
     * @param base64 the Base64 string
     * @return the deserialized ItemStack array, or null if deserialization fails
     */
    public static ItemStack[] fromBase64(String base64) {
        if (base64 == null || base64.isEmpty()) return null;

        try {
            byte[] data = Base64.getDecoder().decode(base64);

            try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                 BukkitObjectInputStream ois = new BukkitObjectInputStream(bais)) {

                int length = ois.readInt();
                ItemStack[] items = new ItemStack[length];

                for (int i = 0; i < length; i++) {
                    items[i] = (ItemStack) ois.readObject();
                }

                return items;
            }
        } catch (Exception e) {
            FrostLogger.error("An error occurred", e);
            return null;
        }
    }
}


