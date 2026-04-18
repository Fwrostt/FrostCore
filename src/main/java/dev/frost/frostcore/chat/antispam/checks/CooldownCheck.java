package dev.frost.frostcore.chat.antispam.checks;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.chat.ChatContext;
import dev.frost.frostcore.chat.ViolationType;
import dev.frost.frostcore.chat.antispam.SpamCheck;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownCheck implements SpamCheck {

    private boolean enabled;
    private boolean punish;
    private int weight;
    private long cooldownMs;
    private double scale;
    private long maxCooldownMs;
    private final Map<UUID, Long> lastMessageMap = new ConcurrentHashMap<>();

    @Override
    public boolean check(ChatContext context) {
        if (!enabled) return true;
        UUID uuid = context.getPlayer().getUniqueId();
        
        long now = System.currentTimeMillis();
        long last = lastMessageMap.getOrDefault(uuid, 0L);
        
        int currentViolations = 0;
        if (Main.getChatManager() != null && Main.getChatManager().getPipeline() != null) {
            currentViolations = Main.getChatManager().getPipeline().getPunishmentHandler().getViolationScore(uuid);
        }

        long effectiveCooldown = cooldownMs;
        if (currentViolations > 0 && scale > 0) {
            effectiveCooldown = (long) (cooldownMs * (1.0 + (currentViolations * scale)));
            if (effectiveCooldown > maxCooldownMs) {
                effectiveCooldown = maxCooldownMs;
            }
        }

        if (now - last < effectiveCooldown) {
            long remaining = effectiveCooldown - (now - last);
            String timeStr = String.format("%.1fs", remaining / 1000.0);
            context.flagViolation(ViolationType.SPAM_COOLDOWN, "chat-filter.cooldown-active", punish, weight, java.util.Map.of("time", timeStr));
            return false;
        }

        lastMessageMap.put(uuid, now);
        return true;
    }

    @Override
    public void reload() {
        FileConfiguration config = Main.getConfigManager().getConfig();
        enabled = config.getBoolean("chat.anti-spam.cooldown.enabled", true);
        punish = config.getBoolean("chat.anti-spam.cooldown.punish", false);
        weight = config.getInt("chat.punishments.weights.COOLDOWN", 1);
        cooldownMs = config.getLong("chat.anti-spam.cooldown.time-ms", 1500L);
        scale = config.getDouble("chat.anti-spam.cooldown.scale", 0.35);
        maxCooldownMs = config.getLong("chat.anti-spam.cooldown.max-cooldown-ms", 8000L);
    }

    /**
     * Evicts last-message timestamps that are old enough that no cooldown could still
     * be active. Called periodically by SpamManager to prevent memory leaks for
     * players who have left the server.
     */
    public void cleanup() {
        // Any timestamp older than 2× maxCooldownMs is definitively expired.
        long cutoff = System.currentTimeMillis() - Math.max(maxCooldownMs * 2, 60_000L);
        lastMessageMap.entrySet().removeIf(e -> e.getValue() < cutoff);
    }
}
