package dev.frost.frostcore.moderation;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.utils.FrostLogger;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;


public class GroupLimitManager {

    private final List<StaffGroup> groups = new ArrayList<>();

    public GroupLimitManager() {
        reload();
    }

    public void reload() {
        groups.clear();
        File file = new File(Main.getInstance().getDataFolder(), "punishments.yml");
        if (!file.exists()) return;

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection groupsSection = yml.getConfigurationSection("groups");
        if (groupsSection == null) return;

        for (String key : groupsSection.getKeys(false)) {
            ConfigurationSection gs = groupsSection.getConfigurationSection(key);
            if (gs == null) continue;

            String permission = gs.getString("permission", "none");
            int weight = gs.getInt("weight", 0);
            String maxBan = gs.getString("max-ban-duration", "permanent");
            String maxMute = gs.getString("max-mute-duration", "permanent");
            boolean requireTemplate = gs.getBoolean("require-template", false);

            Map<String, Long> cooldowns = new LinkedHashMap<>();
            ConfigurationSection cdSection = gs.getConfigurationSection("cooldowns");
            if (cdSection != null) {
                for (String action : cdSection.getKeys(false)) {
                    String cdStr = cdSection.getString(action, "5s");
                    long cdMs = Punishment.parseTime(cdStr);
                    if (cdMs > 0) cooldowns.put(action, cdMs);
                }
            }

            groups.add(new StaffGroup(key, permission, weight, maxBan, maxMute, requireTemplate, cooldowns));
        }

        
        groups.sort((a, b) -> Integer.compare(b.weight(), a.weight()));

        FrostLogger.info("Loaded " + groups.size() + " staff groups.");
    }

    
    public StaffGroup getGroup(CommandSender sender) {
        if (!(sender instanceof Player player)) return null;
        if (player.hasPermission("frostcore.moderation.group.unlimited")) return null; 

        for (StaffGroup group : groups) {
            if (group.permission().equals("none") || player.hasPermission(group.permission())) {
                return group;
            }
        }
        return null;
    }

    
    public int getWeight(CommandSender sender) {
        if (!(sender instanceof Player player)) return Integer.MAX_VALUE; 
        if (player.hasPermission("frostcore.moderation.group.unlimited")) return Integer.MAX_VALUE;

        for (StaffGroup group : groups) {
            if (group.permission().equals("none") || player.hasPermission(group.permission())) {
                return group.weight();
            }
        }
        return 0;
    }

    
    public boolean canPunish(CommandSender staff, Player target) {
        if (target == null) return true;
        if (!target.hasPermission("frostcore.moderation.exempt")) return true;

        boolean useWeights = Main.getConfigManager().getBoolean("moderation.use-group-weights", true);
        if (!useWeights) return true;

        int staffWeight = getWeight(staff);
        int targetWeight = getWeight(target);
        return staffWeight > targetWeight;
    }

    
    public long getMaxDuration(CommandSender sender, String type) {
        if (!(sender instanceof Player player)) return -1;
        if (player.hasPermission("frostcore.moderation.group.unlimited")) return -1;

        StaffGroup group = getGroup(sender);
        if (group == null) return -1;

        String maxStr = switch (type.toUpperCase()) {
            case "BAN", "TEMPBAN", "IPBAN" -> group.maxBanDuration();
            case "MUTE", "TEMPMUTE", "IPMUTE" -> group.maxMuteDuration();
            default -> "permanent";
        };

        if (maxStr.equalsIgnoreCase("permanent") || maxStr.equalsIgnoreCase("perm")) return -1;
        long parsed = Punishment.parseTime(maxStr);
        return parsed == -2 ? -1 : parsed;
    }

    
    public boolean exceedsMaxDuration(CommandSender sender, String type, long durationMs) {
        long maxMs = getMaxDuration(sender, type);
        if (maxMs == -1) return false; 
        if (durationMs == -1) return true; 
        return durationMs > maxMs;
    }

    
    public long getCooldown(CommandSender sender, String action) {
        if (!(sender instanceof Player player)) return 0;
        if (player.hasPermission("frostcore.moderation.cooldown.bypass")) return 0;

        StaffGroup group = getGroup(sender);
        if (group == null) return 0;
        return group.cooldowns().getOrDefault(action.toLowerCase(), 0L);
    }

    
    public boolean requiresTemplate(CommandSender sender) {
        if (!(sender instanceof Player player)) return false;
        if (player.hasPermission("frostcore.moderation.group.unlimited")) return false;

        StaffGroup group = getGroup(sender);
        return group != null && group.requireTemplate();
    }

    public record StaffGroup(
            String name,
            String permission,
            int weight,
            String maxBanDuration,
            String maxMuteDuration,
            boolean requireTemplate,
            Map<String, Long> cooldowns
    ) {}
}
