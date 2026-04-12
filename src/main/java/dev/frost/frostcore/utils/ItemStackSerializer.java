package dev.frost.frostcore.utils;

import dev.frost.frostcore.utils.FrostLogger;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;


public final class ItemStackSerializer {

    private ItemStackSerializer() {}

    
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


