package dev.frost.frostcore.chat.antispam.checks;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.chat.ChatContext;
import dev.frost.frostcore.chat.ViolationType;
import dev.frost.frostcore.chat.antispam.SpamCheck;
import org.bukkit.configuration.file.FileConfiguration;

public class CapsCheck implements SpamCheck {

    private boolean enabled;
    private boolean punish;
    private int weight;
    private int maxPercentage;
    private int minLength;

    @Override
    public boolean check(ChatContext context) {
        if (!enabled) return true;
        
        String msg = context.getMessage();
        if (msg.length() <= minLength) return true;

        int uppercaseCount = 0;
        int letterCount = 0;

        for (int i = 0; i < msg.length(); i++) {
            char c = msg.charAt(i);
            if (Character.isLetter(c)) {
                letterCount++;
                if (Character.isUpperCase(c)) {
                    uppercaseCount++;
                }
            }
        }

        if (letterCount > 0) {
            int percentage = (uppercaseCount * 100) / letterCount;
            if (percentage > maxPercentage) {
                context.flagViolation(ViolationType.SPAM_CAPS, "chat-filter.caps-detected", punish, weight);
                return false;
            }
        }
        
        return true;
    }

    @Override
    public void reload() {
        FileConfiguration config = Main.getConfigManager().getConfig();
        enabled = config.getBoolean("chat.anti-spam.caps.enabled", true);
        punish = config.getBoolean("chat.anti-spam.caps.punish", true);
        weight = config.getInt("chat.punishments.weights.CAPS", 1);
        maxPercentage = config.getInt("chat.anti-spam.caps.max-percentage", 70);
        minLength = config.getInt("chat.anti-spam.caps.min-length", 5);
    }
}
