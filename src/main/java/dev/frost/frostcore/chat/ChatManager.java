package dev.frost.frostcore.chat;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.utils.ChatColorUtil;
import dev.frost.frostcore.utils.FrostLogger;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

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
    private Chat vaultChat;
    private boolean vaultAvailable;
    private boolean papiAvailable;

    private ChatPipeline chatPipeline;

    public ChatManager(Main plugin) {
        this.plugin = plugin;
        this.chatFile = new File(plugin.getDataFolder(), "chat-format.yml");

        if (!chatFile.exists()) {
            plugin.getDataFolder().mkdirs();
            plugin.saveResource("chat-format.yml", false);
        }

        reload();
        hookLuckPerms();
        hookVault();
        papiAvailable = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        
        chatPipeline = new ChatPipeline();
    }

    public void reload() {
        chatConfig = YamlConfiguration.loadConfiguration(chatFile);

        InputStream defStream = plugin.getResource("chat-format.yml");
        if (defStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream));
            chatConfig.setDefaults(defaults);
            chatConfig.options().copyDefaults(true);
        }
        
        if (chatPipeline != null) {
            chatPipeline.reload();
        }
    }

    public ChatPipeline getPipeline() {
        return chatPipeline;
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

    private void hookVault() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            vaultAvailable = false;
            FrostLogger.warn("Vault not found! Chat prefixes/suffixes fallback will not be available if LuckPerms is missing.");
            return;
        }

        try {
            RegisteredServiceProvider<Chat> rsp = Bukkit.getServer().getServicesManager().getRegistration(Chat.class);
            if (rsp != null) {
                vaultChat = rsp.getProvider();
                vaultAvailable = true;
                FrostLogger.info("Hooked into Vault for chat formatting.");
            } else {
                vaultAvailable = false;
                FrostLogger.warn("Vault found but Chat provider could not be loaded.");
            }
        } catch (Exception e) {
            vaultAvailable = false;
            FrostLogger.warn("Failed to hook into Vault: " + e.getMessage());
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
        String prefix = "";
        String suffix = "";
        String prefixesCombined = "";
        String suffixesCombined = "";
        String usernameColor = "";
        String messageColor = "";

        if (luckPermsAvailable) {
            CachedMetaData metaData = luckPerms.getPlayerAdapter(Player.class).getMetaData(player);
            group = metaData.getPrimaryGroup();
            prefix = metaData.getPrefix();
            suffix = metaData.getSuffix();
            usernameColor = metaData.getMetaValue("username-color");
            messageColor = metaData.getMetaValue("message-color");
            prefixesCombined = metaData.getPrefixes().keySet().stream()
                    .map(key -> metaData.getPrefixes().get(key))
                    .collect(Collectors.joining());
            suffixesCombined = metaData.getSuffixes().keySet().stream()
                    .map(key -> metaData.getSuffixes().get(key))
                    .collect(Collectors.joining());
        } else if (vaultAvailable && vaultChat != null) {
            group = vaultChat.getPrimaryGroup(player);
            prefix = vaultChat.getPlayerPrefix(player);
            suffix = vaultChat.getPlayerSuffix(player);
            usernameColor = vaultChat.getPlayerInfoString(player, "username-color", "");
            messageColor = vaultChat.getPlayerInfoString(player, "message-color", "");
            prefixesCombined = prefix != null ? prefix : "";
            suffixesCombined = suffix != null ? suffix : "";
        }

        if (group != null) {
            String groupFormatKey = "group-formats." + group;
            if (chatConfig.getString(groupFormatKey) != null) {
                format = chatConfig.getString(groupFormatKey);
            } else {
                format = chatConfig.getString("chat-format", "{prefix}{name}&r: {message}");
            }
        } else {
            format = chatConfig.getString("chat-format", "{name}&r: {message}");
        }

        String displayName = PlainTextComponentSerializer.plainText().serialize(player.displayName());

        format = format
                .replace("{world}", player.getWorld().getName())
                .replace("{name}", player.getName())
                .replace("{displayname}", displayName)
                .replace("{prefix}", prefix != null ? prefix : "")
                .replace("{suffix}", suffix != null ? suffix : "")
                .replace("{prefixes}", prefixesCombined != null ? prefixesCombined : "")
                .replace("{suffixes}", suffixesCombined != null ? suffixesCombined : "")
                .replace("{username-color}", usernameColor != null ? usernameColor : "")
                .replace("{message-color}", messageColor != null ? messageColor : "");

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

    public boolean isVaultAvailable() {
        return vaultAvailable;
    }
}