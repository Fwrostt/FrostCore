package dev.frost.frostcore.chat.antispam;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.chat.ChatContext;
import dev.frost.frostcore.chat.antispam.checks.*;

import java.util.ArrayList;
import java.util.List;

public class SpamManager {
    
    private final List<SpamCheck> checks = new ArrayList<>();
    
    public SpamManager() {
        checks.add(new CooldownCheck());
        checks.add(new SimilarityCheck());
        checks.add(new CapsCheck());
        checks.add(new RepeatCheck());
        checks.add(new BurstCheck());
        reload();
    }
    
    public void reload() {
        for (SpamCheck check : checks) {
            check.reload();
        }
    }
    
    public boolean process(ChatContext context) {
        if (!Main.getConfigManager().getConfig().getBoolean("chat.anti-spam.enabled", true)) {
            return true;
        }

        // Run checks
        for (SpamCheck check : checks) {
            if (!check.check(context)) {
                return false;
            }
        }
        return true;
    }
    
    public void cleanup() {
        for (SpamCheck check : checks) {
            if (check instanceof BurstCheck burst) {
                burst.cleanup();
            } else if (check instanceof CooldownCheck cooldown) {
                cooldown.cleanup();
            }
        }
    }
}
