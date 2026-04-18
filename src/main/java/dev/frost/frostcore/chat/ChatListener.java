package dev.frost.frostcore.chat;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.PrivateMessageManager;
import dev.frost.frostcore.utils.ChatColorUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatListener implements Listener {

    private static final Pattern MENTION_PATTERN_AT = Pattern.compile("@([A-Za-z0-9_]{3,16})\\b");
    private static final Pattern MENTION_PATTERN_ALL = Pattern.compile("(?<![A-Za-z0-9_])@?([A-Za-z0-9_]{3,16})\\b");
    
    private final ChatManager chatManager;

    public ChatListener(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!chatManager.isEnabled()) return;

        Player player = event.getPlayer();
        String rawMessage = PlainTextComponentSerializer.plainText().serialize(event.message());
        
        ChatContext ctx = chatManager.getPipeline().process(player, rawMessage);
        
        if (ctx.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        if (dev.frost.frostcore.moderation.ModerationManager.getInstance().isShadowMuted(player.getUniqueId())) {
            event.viewers().clear();
            event.viewers().add(player);
        }
        
        rawMessage = ctx.getMessage();
        
        FileConfiguration config = Main.getConfigManager().getConfig();
        boolean mentionsEnabled = config.getBoolean("chat.mentions.enabled", false);
        boolean requireAt = config.getBoolean("chat.mentions.require-at-symbol", true);

        Set<Player> mentionedPlayers = new HashSet<>();
        if (mentionsEnabled) {
            Pattern pattern = requireAt ? MENTION_PATTERN_AT : MENTION_PATTERN_ALL;
            Matcher m = pattern.matcher(rawMessage);
            while (m.find()) {
                String name = m.group(1);
                Player p = Bukkit.getPlayerExact(name);
                if (p != null && player.canSee(p)) {
                    mentionedPlayers.add(p);
                }
            }

            if (config.getBoolean("chat.mentions.everyone-tag", true) && player.hasPermission("frostcore.mention.everyone")) {
                if (rawMessage.toLowerCase().contains("@everyone")) {
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (online != player && player.canSee(online)) mentionedPlayers.add(online);
                    }
                }
            }

            if (!mentionedPlayers.isEmpty()) {
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    boolean playSound = config.getBoolean("chat.mentions.sound", true);
                    Sound sound = null;
                    if (playSound) {
                        try {
                            sound = Sound.valueOf(config.getString("chat.mentions.sound-type", "ENTITY_EXPERIENCE_ORB_PICKUP").toUpperCase());
                        } catch (Exception e) {
                            dev.frost.frostcore.utils.FrostLogger.warn("Failed to parse mention sound: " + e.getMessage());
                        }
                    }
                    float vol = (float) config.getDouble("chat.mentions.sound-volume", 1.0);
                    float pitch = (float) config.getDouble("chat.mentions.sound-pitch", 1.0);
                    
                    boolean sendActionbar = config.getBoolean("chat.mentions.actionbar", true);
                    String actionbarMsg = config.getString("chat.mentions.actionbar-message", "<yellow>You were mentioned by {player}!");
                    Component abComp = ChatColorUtil.toComponent(actionbarMsg.replace("{player}", player.getName()));

                    for (Player target : mentionedPlayers) {
                        if (playSound && sound != null) {
                            target.playSound(target.getLocation(), sound, vol, pitch);
                        }
                        if (sendActionbar) {
                            target.sendActionBar(abComp);
                        }
                    }
                });
            }
        }

        // ── /chattoggle: remove viewers who have hidden global chat ──
        PrivateMessageManager pmm = PrivateMessageManager.getInstance();
        if (pmm != null) {
            event.viewers().removeIf(audience ->
                    audience instanceof Player viewer
                    && !viewer.equals(player)
                    && pmm.isChatHidden(viewer.getUniqueId())
            );
        }

        String format = chatManager.buildFormat(player);
        String processedMessage = chatManager.processMessage(player, rawMessage);
        String finalString = format.replace("{message}", processedMessage);
        
        final String effectiveMessage = rawMessage;
        
        final boolean isMentionsEnabled = mentionsEnabled;
        final String formatSelf = config.getString("chat.mentions.format-self", "<yellow><bold>@{player}</bold></yellow>");
        final String formatOthers = config.getString("chat.mentions.format-others", "<yellow>@{player}</yellow>");
        final boolean tagsEveryone = config.getBoolean("chat.mentions.everyone-tag", true) && effectiveMessage.toLowerCase().contains("@everyone");

        event.renderer((source, sourceDisplayName, message, audience) -> {
            String renderString = finalString;
            
            if (isMentionsEnabled) {
                renderString = applyMentions(renderString, mentionedPlayers, player, audience, requireAt, formatSelf, formatOthers, tagsEveryone);
            }
            return ChatColorUtil.toComponent(renderString);
        });
    }
            
    private String applyMentions(String input, Set<Player> mentionedPlayers, Player source, net.kyori.adventure.audience.Audience audience, boolean requireAt, String formatSelf, String formatOthers, boolean tagsEveryone) {
        if (mentionedPlayers.isEmpty()) return input;

        String result = input;
        for (Player p : mentionedPlayers) {
            String regex = requireAt
                ? "(?i)@" + Pattern.quote(p.getName()) + "\\b"
                : "(?i)(?<![A-Za-z0-9_])@?" + Pattern.quote(p.getName()) + "\\b";

            boolean isTarget = audience instanceof Player target && target.equals(p);
            String replacement = Matcher.quoteReplacement(
                isTarget
                    ? formatSelf.replace("{player}", p.getName())
                    : formatOthers.replace("{player}", p.getName())
            );

            Matcher m = Pattern.compile(regex).matcher(result);
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                m.appendReplacement(sb, replacement);
            }
            m.appendTail(sb);
            result = sb.toString();
        }

        if (tagsEveryone && source.hasPermission("frostcore.mention.everyone")) {
            String regex = "(?i)@everyone\\b";
            
            boolean isTarget = audience instanceof Player target && mentionedPlayers.contains(target);
            String replacement = Matcher.quoteReplacement(
                isTarget
                    ? formatSelf.replace("{player}", "everyone")
                    : formatOthers.replace("{player}", "everyone")
            );

            Matcher m = Pattern.compile(regex).matcher(result);
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                m.appendReplacement(sb, replacement);
            }
            m.appendTail(sb);
            result = sb.toString();
        }

        return result;
    }
}