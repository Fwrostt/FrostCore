package dev.frost.frostcore.manager;

import dev.frost.frostcore.utils.FrostLogger;

import dev.frost.frostcore.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

public class MessageManager {

    private static MessageManager instance;

    private final Main plugin;
    private FileConfiguration config;
    private final File file;

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public MessageManager(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "messages.yml");

        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            plugin.saveResource("messages.yml", false);
        }

        reload();
        instance = this;
    }

    public static MessageManager get() {
        return instance;
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);

        InputStream defStream = plugin.getResource("messages.yml");
        if (defStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream));
            config.setDefaults(defaults);
            config.options().copyDefaults(true);
        }
    }

    public String getRaw(String path) {
        String msg = config.getString(path);
        return msg != null ? msg : "<red>Missing message: " + path + "</red>";
    }

    /**
     * Resolve nested message references like {teams.prefix} by looking them up in messages.yml.
     * ONLY resolves keys that exist in the config. Unknown keys like {player}, {team}, etc.
     * are left untouched for applyPlaceholders() to handle later.
     */
    private String resolveNested(String message) {
        if (message == null || !message.contains("{")) return message;

        int depth = 0;
        int searchFrom = 0;

        while (searchFrom < message.length() && depth < 10) {
            int start = message.indexOf("{", searchFrom);
            if (start == -1) break;

            int end = message.indexOf("}", start);
            if (end == -1) break;

            String key = message.substring(start + 1, end);

            if (key.contains(":") || !config.isString(key)) {
                searchFrom = end + 1;
                continue;
            }

            String replacement = config.getString(key);
            if (replacement == null) {

                searchFrom = end + 1;
                continue;
            }

            message = message.substring(0, start) + replacement + message.substring(end + 1);
            depth++;

        }
        return message;
    }

    private String applyPlaceholders(String message, Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) return message;

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    private Component parse(String text) {
        if (text == null || text.isEmpty()) return Component.empty();

        try {
            return miniMessage.deserialize(text);
        } catch (Exception e) {
            FrostLogger.warn("MiniMessage parse failed for: " + text);
            return Component.text(text);
        }
    }

    public void send(org.bukkit.command.CommandSender sender, String path) {
        sender.sendMessage(getComponent(path));
    }

    public void send(org.bukkit.command.CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(getComponent(path, placeholders));
    }

    public void sendRaw(org.bukkit.command.CommandSender sender, String message) {
        sender.sendMessage(parse(message));
    }

    public void send(Player player, String path) {
        player.sendMessage(getComponent(path));
    }

    public void send(Player player, String path, Map<String, String> placeholders) {
        player.sendMessage(getComponent(path, placeholders));
    }

    public void sendRaw(Player player, String message) {
        player.sendMessage(parse(message));
    }

    public Component getComponent(String path) {
        return getComponent(path, null);
    }

    public Component getComponent(String path, Map<String, String> placeholders) {
        String msg = getRaw(path);
        msg = resolveNested(msg);
        msg = applyPlaceholders(msg, placeholders);
        return parse(msg);
    }

    public void broadcast(String path) {
        broadcast(path, null);
    }

    public void broadcast(String path, Map<String, String> placeholders) {
        Component component = getComponent(path, placeholders);
        plugin.getServer().broadcast(component);
    }

    public List<String> getRawList(String path) {
        List<String> list = config.getStringList(path);
        return list.isEmpty() ? List.of("§cMissing message list: " + path) : list;
    }

    public List<Component> getComponentList(String path) {
        return getComponentList(path, null);
    }

    public List<Component> getComponentList(String path, Map<String, String> placeholders) {
        return getRawList(path).stream()
                .map(msg -> {
                    String processed = resolveNested(msg);
                    processed = applyPlaceholders(processed, placeholders);
                    return parse(processed);
                })
                .toList();
    }

    public void sendActionBar(Player player, String path) {
        sendActionBar(player, path, null);
    }

    public void sendActionBar(Player player, String path, Map<String, String> placeholders) {
        Component component = getComponent(path, placeholders);
        player.sendActionBar(component);
    }

    public void sendActionBarRaw(Player player, String message) {
        player.sendActionBar(parse(message));
    }

    public void sendTitle(Player player, String titlePath, String subtitlePath) {
        sendTitle(player, titlePath, subtitlePath, 10, 70, 20, null);
    }

    public void sendTitle(Player player, String titlePath, String subtitlePath, Map<String, String> placeholders) {
        sendTitle(player, titlePath, subtitlePath, 10, 70, 20, placeholders);
    }

    public void sendTitle(Player player, String titlePath, String subtitlePath,
                          int fadeIn, int stay, int fadeOut, Map<String, String> placeholders) {
        Component title = titlePath != null ? getComponent(titlePath, placeholders) : Component.empty();
        Component subtitle = subtitlePath != null ? getComponent(subtitlePath, placeholders) : Component.empty();

        player.showTitle(net.kyori.adventure.title.Title.title(
                title,
                subtitle,
                net.kyori.adventure.title.Title.Times.times(
                        net.kyori.adventure.util.Ticks.duration(fadeIn),
                        net.kyori.adventure.util.Ticks.duration(stay),
                        net.kyori.adventure.util.Ticks.duration(fadeOut)
                )
        ));
    }
}

