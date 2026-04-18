package dev.frost.frostcore.chat.antispam.checks;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.chat.ChatContext;
import dev.frost.frostcore.chat.ViolationType;
import dev.frost.frostcore.chat.antispam.SpamCheck;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BurstCheck implements SpamCheck {

    private boolean enabled;
    private boolean punish;
    private int weight;
    private int maxMessages;
    private long intervalMs;

    private final Map<UUID, Queue<Long>> messageHistory = new ConcurrentHashMap<>();

    @Override
    public boolean check(ChatContext context) {
        if (!enabled) return true;

        UUID uuid = context.getPlayer().getUniqueId();
        long now = System.currentTimeMillis();

        Queue<Long> history = messageHistory.computeIfAbsent(uuid, k -> new LinkedList<>());

        synchronized (history) {
            // Prune timestamps that have fallen outside the burst window.
            while (!history.isEmpty() && now - history.peek() > intervalMs) {
                history.poll();
            }

            // Check BEFORE adding — ensures the configured maxMessages is the exact
            // threshold at which throttling kicks in (not maxMessages + 1).
            if (history.size() >= maxMessages) {
                context.flagViolation(ViolationType.SPAM_BURST, "chat-filter.spam-burst", punish, weight);
                return false;
            }

            // Only add to history when the message is allowed through.
            history.add(now);
        }

        return true;
    }

    @Override
    public void reload() {
        FileConfiguration config = Main.getConfigManager().getConfig();
        enabled = config.getBoolean("chat.anti-spam.burst.enabled", true);
        punish = config.getBoolean("chat.anti-spam.burst.punish", true);
        weight = config.getInt("chat.punishments.weights.BURST", 2);
        maxMessages = config.getInt("chat.anti-spam.burst.max-messages", 5);
        intervalMs = config.getLong("chat.anti-spam.burst.interval-ms", 3000L);
    }
    
    public void cleanup() {
        long now = System.currentTimeMillis();
        messageHistory.entrySet().removeIf(entry -> {
            Queue<Long> queue = entry.getValue();
            synchronized (queue) {
                return queue.isEmpty() || (now - queue.peek() > intervalMs);
            }
        });
    }
}
