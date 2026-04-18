package dev.frost.frostcore.chat.punishment;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.chat.ChatContext;
import dev.frost.frostcore.chat.ViolationType;
import dev.frost.frostcore.moderation.ModerationManager;
import dev.frost.frostcore.moderation.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatPunishmentHandler {

    private boolean enabled;
    private long decaySeconds;
    private final Map<Integer, LevelDef> levels = new ConcurrentHashMap<>();
    private int maxLevel = 0;

    private final Map<UUID, Deque<ViolationEntry>> violations = new ConcurrentHashMap<>();

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([smhd])");

    public ChatPunishmentHandler() {
        reload();

        // Decay Task
        Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(), () -> {
            if (!enabled || decaySeconds <= 0) return;
            
            long now = System.currentTimeMillis();
            long decayMs = decaySeconds * 1000L;
            
            for (Map.Entry<UUID, Deque<ViolationEntry>> entry : violations.entrySet()) {
                Deque<ViolationEntry> deque = entry.getValue();
                synchronized (deque) {
                    while (!deque.isEmpty()) {
                        ViolationEntry oldest = deque.peekFirst();
                        if (now - oldest.timestamp > decayMs) {
                            deque.pollFirst();
                        } else {
                            break;
                        }
                    }
                    if (deque.isEmpty()) {
                        violations.remove(entry.getKey());
                    }
                }
            }
        }, 20 * 60L, 20 * 60L); // check every minute
    }

    public void reload() {
        FileConfiguration config = Main.getConfigManager().getConfig();
        enabled = config.getBoolean("chat.punishments.enabled", true);
        decaySeconds = config.getLong("chat.punishments.decay-seconds", 900L);

        levels.clear();
        maxLevel = 0;
        ConfigurationSection levelsSec = config.getConfigurationSection("chat.punishments.levels");
        if (levelsSec != null) {
            for (String key : levelsSec.getKeys(false)) {
                try {
                    int vCount = Integer.parseInt(key);
                    String action = levelsSec.getString(key + ".action", "WARN").toUpperCase();
                    String durationStr = levelsSec.getString(key + ".duration", "0s");
                    levels.put(vCount, new LevelDef(action, parseDuration(durationStr)));
                    if (vCount > maxLevel) {
                        maxLevel = vCount;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    public int getViolationScore(UUID uuid) {
        Deque<ViolationEntry> deque = violations.get(uuid);
        if (deque == null) return 0;
        synchronized (deque) {
            return deque.stream().mapToInt(ViolationEntry::weight).sum();
        }
    }

    public void handleViolation(ChatContext context) {
        if (!enabled || !context.shouldPunish()) return;

        UUID uuid = context.getPlayer().getUniqueId();
        Deque<ViolationEntry> deque = violations.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        
        int currentScore = 0;
        synchronized (deque) {
            deque.addLast(new ViolationEntry(context.getViolationType(), context.getWeight(), System.currentTimeMillis()));
            currentScore = deque.stream().mapToInt(ViolationEntry::weight).sum();
        }

        LevelDef def = levels.get(currentScore);
        if (def != null) {
            applyPunishment(context, def);
        }
        
        if (maxLevel > 0 && currentScore >= maxLevel) {
            violations.remove(uuid); // Reset violations
        }
    }

    private void applyPunishment(ChatContext context, LevelDef def) {
        PunishmentType type;
        if (def.action.equals("WARN")) {
            type = PunishmentType.WARN;
        } else if (def.action.equals("MUTE")) {
            type = def.durationMs > 0 ? PunishmentType.TEMPMUTE : PunishmentType.MUTE;
        } else if (def.action.equals("KICK")) {
            type = PunishmentType.KICK;
        } else if (def.action.equals("SHADOWMUTE")) {
            type = PunishmentType.SHADOWMUTE;
        } else {
            type = PunishmentType.WARN;
        }

        String reason = "Chat Filter Violation";
        if (context.getViolationType() != null) {
            reason = "Chat Filter Violation (" + context.getViolationType().name() + ")";
        }
        final String finalReason = reason;
        final PunishmentType finalType = type;

        // For WARN punishments, suppress the player-facing WARN screen — the violation
        // message ref (e.g. "You are on cooldown") already informs the player directly.
        // Non-WARN punishments (MUTE, KICK, etc.) still notify the target normally.
        final boolean notifyTarget = finalType != PunishmentType.WARN;

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            ModerationManager.getInstance().punish(
                    finalType,
                    context.getPlayer().getUniqueId(),
                    context.getPlayer().getName(),
                    null,
                    finalReason,
                    null, // CONSOLE issuer
                    def.durationMs <= 0 ? -1 : def.durationMs,
                    false,       // not silent — staff should still see the broadcast
                    notifyTarget // suppress player WARN screen for auto-chat-punish WARNs
            );
        });
    }

    private long parseDuration(String str) {
        Matcher m = TIME_PATTERN.matcher(str.toLowerCase());
        long ms = 0;
        while (m.find()) {
            long val = Long.parseLong(m.group(1));
            switch (m.group(2)) {
                case "s": ms += val * 1000L; break;
                case "m": ms += val * 60000L; break;
                case "h": ms += val * 3600000L; break;
                case "d": ms += val * 86400000L; break;
            }
        }
        return ms;
    }

    private record LevelDef(String action, long durationMs) {}
    private record ViolationEntry(ViolationType type, int weight, long timestamp) {}
}
