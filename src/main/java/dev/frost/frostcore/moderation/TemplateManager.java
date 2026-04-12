package dev.frost.frostcore.moderation;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.utils.FrostLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * Manages punishment templates loaded from punishments.yml.
 * Supports escalation (increasing severity per repeat offense).
 */
public class TemplateManager {

    private final Map<String, PunishmentTemplate> templates = new LinkedHashMap<>();

    public TemplateManager() {
        reload();
    }

    public void reload() {
        templates.clear();
        File file = new File(Main.getInstance().getDataFolder(), "punishments.yml");
        if (!file.exists()) {
            Main.getInstance().saveResource("punishments.yml", false);
        }

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection templatesSection = yml.getConfigurationSection("templates");
        if (templatesSection == null) return;

        for (String key : templatesSection.getKeys(false)) {
            ConfigurationSection ts = templatesSection.getConfigurationSection(key);
            if (ts == null) continue;

            String typeName = ts.getString("type", "BAN").toUpperCase();
            PunishmentType type;
            try {
                type = PunishmentType.valueOf(typeName);
            } catch (IllegalArgumentException e) {
                FrostLogger.warn("Invalid punishment type '" + typeName + "' in template: " + key);
                continue;
            }

            String reason = ts.getString("reason", "No reason");
            String defaultDuration = ts.getString("duration", "permanent");

            // Parse escalation tiers
            Map<Integer, String> escalation = new LinkedHashMap<>();
            ConfigurationSection escSection = ts.getConfigurationSection("escalation");
            if (escSection != null) {
                for (String tier : escSection.getKeys(false)) {
                    try {
                        int tierNum = Integer.parseInt(tier);
                        escalation.put(tierNum, escSection.getString(tier, defaultDuration));
                    } catch (NumberFormatException ignored) {}
                }
            }

            templates.put(key.toLowerCase(), new PunishmentTemplate(key, type, reason, defaultDuration, escalation));
        }

        FrostLogger.info("Loaded " + templates.size() + " punishment templates.");
    }

    /**
     * Get a template by name.
     */
    public PunishmentTemplate getTemplate(String name) {
        return templates.get(name.toLowerCase());
    }

    /**
     * Get all template names for tab completion.
     */
    public Set<String> getTemplateNames() {
        return Collections.unmodifiableSet(templates.keySet());
    }

    /**
     * Resolve the duration for a template given the player's offense count.
     *
     * @param template the template
     * @param offenseCount the number of times this player has been punished with this template type
     * @return duration in ms, or -1 for permanent
     */
    public long resolveDuration(PunishmentTemplate template, int offenseCount) {
        String durationStr;
        if (!template.escalation().isEmpty()) {
            // Find the highest tier that matches
            durationStr = template.defaultDuration();
            for (Map.Entry<Integer, String> entry : template.escalation().entrySet()) {
                if (offenseCount >= entry.getKey()) {
                    durationStr = entry.getValue();
                }
            }
        } else {
            durationStr = template.defaultDuration();
        }

        if (durationStr.equalsIgnoreCase("permanent") || durationStr.equalsIgnoreCase("perm")) {
            return -1;
        }
        long parsed = Punishment.parseTime(durationStr);
        return parsed == -2 ? -1 : parsed;
    }

    /**
     * Immutable template definition.
     */
    public record PunishmentTemplate(
            String name,
            PunishmentType type,
            String reason,
            String defaultDuration,
            Map<Integer, String> escalation
    ) {}
}
