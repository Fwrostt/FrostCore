package dev.frost.frostcore.chat.antispam.checks;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.chat.ChatContext;
import dev.frost.frostcore.chat.ViolationType;
import dev.frost.frostcore.chat.antispam.SpamCheck;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RepeatCheck implements SpamCheck {

    private boolean enabled;
    private boolean punish;
    private int weight;
    private int maxChars;
    private Pattern repeatPattern;

    @Override
    public boolean check(ChatContext context) {
        if (!enabled || repeatPattern == null) return true;

        Matcher m = repeatPattern.matcher(context.getMessage());
        if (m.find()) {
            context.flagViolation(ViolationType.SPAM_REPEAT, "chat-filter.repeat-chars", punish, weight);
            return false;
        }

        return true;
    }

    @Override
    public void reload() {
        FileConfiguration config = Main.getConfigManager().getConfig();
        enabled = config.getBoolean("chat.anti-spam.repeat.enabled", true);
        punish = config.getBoolean("chat.anti-spam.repeat.punish", true);
        weight = config.getInt("chat.punishments.weights.REPEAT", 2);
        maxChars = config.getInt("chat.anti-spam.repeat.max-chars", 5);
        if (maxChars > 0) {
            
            repeatPattern = Pattern.compile("(.)\\1{" + maxChars + ",}", Pattern.CASE_INSENSITIVE);
        } else {
            repeatPattern = null;
        }
    }
}
