package dev.frost.frostcore.chat;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.chat.antispam.SpamManager;
import dev.frost.frostcore.chat.filter.FilterManager;
import dev.frost.frostcore.chat.punishment.ChatPunishmentHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ChatPipeline {
    
    public static boolean DEBUG = false;

    private final SpamManager spamManager;
    private final FilterManager filterManager;
    private final ChatPunishmentHandler punishmentHandler;

    public ChatPipeline() {
        this.spamManager = new SpamManager();
        this.filterManager = new FilterManager();
        this.punishmentHandler = new ChatPunishmentHandler();

        Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(), () -> {
            spamManager.cleanup();
        }, 20 * 60L * 10L, 20 * 60L * 10L); // every 10 minutes
    }

    public void reload() {
        spamManager.reload();
        filterManager.reload();
        punishmentHandler.reload();
    }

    public ChatContext process(Player player, String rawMessage) {
        ChatContext context = new ChatContext(player, rawMessage);

        if (player.isOp() || 
            player.hasPermission("frostcore.chat.bypass") || 
            player.hasPermission("frostcore.moderation.bypass")) {
            return context;
        }

        // 1. Spam checks (Cooldown, Caps, Burst)
        if (!spamManager.process(context)) {
            handleViolationPost(context);
            return context;
        }

        // 2. Filter checks (Regex, Domain, IP, Blacklist)
        filterManager.process(context);

        // If a violation was flagged (by either spam or filter checks), dispatch post-processing.
        // Note: filter violations with cancelMessage=false set violationType but leave
        // cancelled=false so the cleaned message passes through — handleViolationPost
        // still runs to record the score / send the replace-mode notice.
        if (context.getViolationType() != null) {
            handleViolationPost(context);
        }

        return context;
    }

    public ChatPunishmentHandler getPunishmentHandler() {
        return punishmentHandler;
    }

    private void handleViolationPost(ChatContext context) {
        if (DEBUG && context.getViolationType() != null) {
            String shortMsg = context.getMessage().length() > 20
                    ? context.getMessage().substring(0, 20) + "..."
                    : context.getMessage();
            dev.frost.frostcore.utils.FrostLogger.info(String.format(
                    "[FrostCore-Debug] %s -> %s (weight=%d, ref=%s, punish=%s) msg=\"%s\"",
                    context.getPlayer().getName(),
                    context.getViolationType().name(),
                    context.getWeight(),
                    context.getViolationMessageRef() != null ? context.getViolationMessageRef() : "none",
                    context.shouldPunish(),
                    shortMsg));
        }

        punishmentHandler.handleViolation(context);

        // Send the player-facing violation notice (e.g. cooldown timer, caps warning)
        // if one is configured and the admin wants warnings sent.
        if (context.getViolationMessageRef() != null) {
            String warningConfigVal = Main.getConfigManager().getConfig().getString("chat.anti-spam.send-warning", "true");
            boolean wantsWarning = Boolean.parseBoolean(warningConfigVal);
            if (wantsWarning) {
                Main.getMessageManager().send(context.getPlayer(), context.getViolationMessageRef(), context.getPlaceholders());
            }
        }
    }
}
