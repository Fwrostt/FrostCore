package dev.frost.frostcore.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class PluginUtil {

    // Send an action bar
    public static void sendBar(Player player, String message) {
        Component component = MiniMessage.miniMessage().deserialize(message);
        player.sendActionBar(component);
    }

    // Send a chat message
    public static void sendMessage(Player player, String message) {
        Component component = MiniMessage.miniMessage().deserialize(message);
        player.sendMessage(component);
    }

    // Broadcast a message to all players
    public static void broadcast(String message) {
        Component component = MiniMessage.miniMessage().deserialize(message);
        Bukkit.broadcast(component);
    }

    // Play a sound for a player
    public static void playSound(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    // Give an item to a player
    public static void giveItem(Player player, ItemStack item) {
        player.getInventory().addItem(item);
    }

    // Send a title and subtitle
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Component titleComponent = MiniMessage.miniMessage().deserialize(title);
        Component subtitleComponent = MiniMessage.miniMessage().deserialize(subtitle);
        player.showTitle(
                net.kyori.adventure.title.Title.title(
                        titleComponent,
                        subtitleComponent,
                        net.kyori.adventure.title.Title.Times.times(
                                java.time.Duration.ofMillis(fadeIn * 50L),
                                java.time.Duration.ofMillis(stay * 50L),
                                java.time.Duration.ofMillis(fadeOut * 50L)
                        )
                )
        );
    }

    // Get a player by UUID (null-safe)
    public static Player getPlayer(UUID uuid) {
        return Bukkit.getPlayer(uuid);
    }

    // Kick a player with a message
    public static void kickPlayer(Player player, String reason) {
        Component component = MiniMessage.miniMessage().deserialize(reason);
        player.kick(component);
    }

    // Send multiple lines of messages to a player
    public static void sendMessages(Player player, List<String> messages) {
        for (String message : messages) {
            sendMessage(player, message);
        }
    }

}
