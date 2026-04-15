package dev.frost.frostcore.chat;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.utils.ChatColorUtil;
import dev.frost.frostcore.utils.FrostLogger;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class ChatManager {

    private final Main plugin;
    private FileConfiguration chatConfig;
    private final File chatFile;

    private LuckPerms luckPerms;
    private boolean luckPermsAvailable;
    private boolean papiAvailable;

    public ChatManager(Main plugin) {
        this.plugin = plugin;
        this.chatFile = new File(plugin.getDataFolder(), "chat-format.yml");

        if (!chatFile.exists()) {
            plugin.getDataFolder().mkdirs();
            plugin.saveResource("chat-format.yml", false);
        }

        reload();
        hookLuckPerms();
        papiAvailable = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public void reload() {
        chatConfig = YamlConfiguration.loadConfiguration(chatFile);

        InputStream defStream = plugin.getResource("chat-format.yml");
        if (defStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream));
            chatConfig.setDefaults(defaults);
            chatConfig.options().copyDefaults(true);
        }
    }

    private void hookLuckPerms() {
        if (!Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            luckPermsAvailable = false;
            FrostLogger.warn("LuckPerms not found! Chat prefixes/suffixes will not be available.");
            return;
        }

        try {
            luckPerms = Bukkit.getServicesManager().load(LuckPerms.class);
            if (luckPerms != null) {
                luckPermsAvailable = true;
                FrostLogger.info("Hooked into LuckPerms for chat formatting.");
            } else {
                luckPermsAvailable = false;
                FrostLogger.warn("LuckPerms found but API could not be loaded.");
            }
        } catch (Exception e) {
            luckPermsAvailable = false;
            FrostLogger.warn("Failed to hook into LuckPerms: " + e.getMessage());
        }
    }

    public boolean isEnabled() {
        return chatConfig.getBoolean("enabled", true);
    }

    public FileConfiguration getChatConfig() {
        return chatConfig;
    }

    public String buildFormat(Player player) {
        String group = null;
        String format;

        if (luckPermsAvailable) {
            CachedMetaData metaData = luckPerms.getPlayerAdapter(Player.class).getMetaData(player);
            group = metaData.getPrimaryGroup();

            String groupFormatKey = "group-formats." + group;
            if (chatConfig.getString(groupFormatKey) != null) {
                format = chatConfig.getString(groupFormatKey);
            } else {
                format = chatConfig.getString("chat-format", "{prefix}{name}&r: {message}");
            }

            String prefix = metaData.getPrefix();
            String suffix = metaData.getSuffix();
            String usernameColor = metaData.getMetaValue("username-color");
            String messageColor = metaData.getMetaValue("message-color");

            format = format
                    .replace("{prefix}", prefix != null ? prefix : "")
                    .replace("{suffix}", suffix != null ? suffix : "")
                    .replace("{prefixes}", metaData.getPrefixes().keySet().stream()
                            .map(key -> metaData.getPrefixes().get(key))
                            .collect(Collectors.joining()))
                    .replace("{suffixes}", metaData.getSuffixes().keySet().stream()
                            .map(key -> metaData.getSuffixes().get(key))
                            .collect(Collectors.joining()))
                    .replace("{username-color}", usernameColor != null ? usernameColor : "")
                    .replace("{message-color}", messageColor != null ? messageColor : "");
        } else {
            format = chatConfig.getString("chat-format", "{name}&r: {message}");
            format = format
                    .replace("{prefix}", "")
                    .replace("{suffix}", "")
                    .replace("{prefixes}", "")
                    .replace("{suffixes}", "")
                    .replace("{username-color}", "")
                    .replace("{message-color}", "");
        }

        String displayName = PlainTextComponentSerializer.plainText().serialize(player.displayName());

        format = format
                .replace("{world}", player.getWorld().getName())
                .replace("{name}", player.getName())
                .replace("{displayname}", displayName);

        if (papiAvailable) {
            try {
                format = PlaceholderAPI.setPlaceholders(player, format);
            } catch (Exception ignored) {
            }
        }

        return format;
    }

    public String processMessage(Player player, String message) {
        boolean allowLegacy = player.hasPermission("frostcore.chat.colorcodes");
        boolean allowHex = player.hasPermission("frostcore.chat.hexcodes");

        return ChatColorUtil.processPlayerMessage(message, allowLegacy, allowHex);
    }

    public Component renderChat(Player player, String rawMessage) {
        String format = buildFormat(player);
        String processedMessage = processMessage(player, rawMessage);
        String finalString = format.replace("{message}", processedMessage);
        return ChatColorUtil.toComponent(finalString);
    }

    public boolean isLuckPermsAvailable() {
        return luckPermsAvailable;
    }
}
