package dev.frost.frostcore.chat.antispam.checks;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.chat.ChatContext;
import dev.frost.frostcore.chat.ViolationType;
import dev.frost.frostcore.chat.antispam.SpamCheck;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SimilarityCheck implements SpamCheck {

    private boolean enabled;
    private boolean punish;
    private int weight;
    private double threshold;
    private final Map<UUID, String> previousMessages = new ConcurrentHashMap<>();

    @Override
    public boolean check(ChatContext context) {
        if (!enabled) return true;

        UUID uuid = context.getPlayer().getUniqueId();
        String current = dev.frost.frostcore.utils.ChatUtil.normalize(context.getMessage());
        if (current.isEmpty()) return true;

        String previous = previousMessages.get(uuid);

        if (previous != null) {
            double similarity = calculateSimilarity(current, previous);
            if (similarity >= threshold) {
                context.flagViolation(ViolationType.SPAM_SIMILARITY, "chat-filter.similarity", punish, weight);
                // Do NOT update previousMessages here — the baseline should remain the
                // last *allowed* message so that a player can't shift the window by
                // repeatedly sending variations of blocked content.
                return false;
            }
        }

        // Only store when the message is allowed through.
        previousMessages.put(uuid, current);
        return true;
    }

    @Override
    public void reload() {
        FileConfiguration config = Main.getConfigManager().getConfig();
        enabled = config.getBoolean("chat.anti-spam.similarity.enabled", true);
        punish = config.getBoolean("chat.anti-spam.similarity.punish", true);
        weight = config.getInt("chat.punishments.weights.SIMILARITY", 2);
        threshold = config.getDouble("chat.anti-spam.similarity.threshold", 0.85);
    }
    
    private double calculateSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        int len1 = s1.length();
        int len2 = s2.length();
        if (len1 == 0 || len2 == 0) return 0.0;
        
        int[][] dp = new int[len1 + 1][len2 + 1];
        
        for (int i = 0; i <= len1; i++) dp[i][0] = i;
        for (int j = 0; j <= len2; j++) dp[0][j] = j;
        
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        
        int maxLength = Math.max(len1, len2);
        int distance = dp[len1][len2];
        return 1.0 - ((double) distance / maxLength);
    }
}
