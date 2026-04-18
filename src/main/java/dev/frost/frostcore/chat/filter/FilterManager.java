package dev.frost.frostcore.chat.filter;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.chat.ChatContext;
import dev.frost.frostcore.chat.ViolationType;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilterManager {

    private boolean enabled;
    private boolean sendWarning;
    private boolean cancelMessage;
    private String replaceString;
    private boolean blockIps;
    private boolean blockDomains;
    
    private int weightBlacklist;
    private int weightRegex;
    private int weightDomain;
    private int weightIp;

    private final List<Pattern> blacklistPatterns = new ArrayList<>();
    private final List<Pattern> regexPatterns = new ArrayList<>();
    private final List<String> whitelistedDomains = new ArrayList<>();

    private static final Pattern IP_PATTERN = Pattern.compile(
            "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b"
    );

    private static final Pattern DOMAIN_PATTERN = Pattern.compile(
            "\\b([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}\\b"
    );

    public FilterManager() {
        reload();
    }

    public void reload() {
        FileConfiguration config = Main.getConfigManager().getConfig();
        enabled = config.getBoolean("chat.filter.enabled", true);
        sendWarning = config.getBoolean("chat.filter.send-warning", true);
        blockIps = config.getBoolean("chat.filter.block-ips", true);
        blockDomains = config.getBoolean("chat.filter.block-domains", true);
        
        weightBlacklist = config.getInt("chat.punishments.weights.BLACKLIST", 5);
        weightRegex = config.getInt("chat.punishments.weights.REGEX", 3);
        weightDomain = config.getInt("chat.punishments.weights.DOMAIN", 3);
        weightIp = config.getInt("chat.punishments.weights.IP", 3);
        
        cancelMessage = config.getBoolean("chat.filter.actions.cancel-message", true);
        replaceString = config.getString("chat.filter.actions.replace", "***");

        boolean inWord = config.getBoolean("chat.filter.in-word-detection", true);
        blacklistPatterns.clear();
        for (String word : config.getStringList("chat.filter.blacklist")) {
            String patternString;
            if (inWord) {
                patternString = "(?i)" + word.replace("*", ".*");
            } else {
                patternString = "(?i)\\b" + word.replace("*", ".*") + "\\b";
            }
            try {
                blacklistPatterns.add(Pattern.compile(patternString));
            } catch (Exception ignored) {}
        }

        regexPatterns.clear();
        for (String reg : config.getStringList("chat.filter.regex")) {
            try {
                regexPatterns.add(Pattern.compile(reg));
            } catch (Exception ignored) {}
        }

        whitelistedDomains.clear();
        for (String domain : config.getStringList("chat.filter.whitelisted-domains")) {
            whitelistedDomains.add(domain.toLowerCase());
        }
    }

    public boolean process(ChatContext context) {
        if (!enabled) return true;

        String msg = context.getMessage();
        String unObfuscatedMsg = dev.frost.frostcore.utils.ChatUtil.normalize(msg);

        boolean flagged = false;
        ViolationType type = null;
        String reasonRef = null;
        int currentWeight = 1;
        java.util.Map<String, String> placeholders = java.util.Collections.emptyMap();

        // ── 1. IP check ────────────────────────────────────────────────────────────────
        if (blockIps) {
            Matcher ipMatcher = IP_PATTERN.matcher(msg);
            if (ipMatcher.find()) {
                String matchedIp = ipMatcher.group(0);
                flagged = true;
                type = ViolationType.FILTER_IP;
                reasonRef = "chat-filter.blocked-ip";
                currentWeight = weightIp;
                placeholders = java.util.Map.of("ip", matchedIp);
            }
        }

        // ── 2. Domain check ────────────────────────────────────────────────────────────
        // Bug fix: use group(0) for the full matched string instead of group(1)
        // (group(1) only captures the last repeating subdomain component, e.g. "server."
        // instead of the full "play.server.net").
        // Also performs parent-aware whitelist matching so that whitelisting "server.net"
        // also allows "play.server.net".
        if (!flagged && blockDomains) {
            Matcher m = DOMAIN_PATTERN.matcher(msg);
            while (m.find()) {
                String domain = m.group(0).toLowerCase();
                boolean isWhitelisted = whitelistedDomains.stream()
                        .anyMatch(wd -> domain.equals(wd) || domain.endsWith("." + wd));
                if (!isWhitelisted) {
                    flagged = true;
                    type = ViolationType.FILTER_DOMAIN;
                    reasonRef = "chat-filter.blocked-domain";
                    currentWeight = weightDomain;
                    placeholders = java.util.Map.of("domain", domain);
                    break;
                }
            }
        }

        // ── 3. Regex check ─────────────────────────────────────────────────────────────
        if (!flagged) {
            for (Pattern p : regexPatterns) {
                if (p.matcher(msg).find()) {
                    flagged = true;
                    type = ViolationType.FILTER_REGEX;
                    reasonRef = "chat-filter.blocked-message";
                    currentWeight = weightRegex;
                    break;
                }
            }
        }

        // ── 4. Blacklist check ─────────────────────────────────────────────────────────
        if (!flagged) {
            for (Pattern p : blacklistPatterns) {
                if (p.matcher(unObfuscatedMsg).find() || p.matcher(msg).find()) {
                    flagged = true;
                    type = ViolationType.FILTER_BLACKLIST;
                    reasonRef = "chat-filter.blocked-message";
                    currentWeight = weightBlacklist;

                    if (!cancelMessage) {
                        msg = p.matcher(msg).replaceAll(replaceString);
                    } else {
                        break;
                    }
                }
            }
        }

        if (flagged) {
            if (cancelMessage) {
                // Hard block: cancel the message and punish the player normally.
                context.flagViolation(type, reasonRef, true, currentWeight, placeholders);
                return false;
            } else {
                // Replace mode: the cleaned message passes through.
                // Only add to the violation score if explicitly configured, since the player
                // already had their message sanitised (punishing again is surprising).
                boolean punishOnReplace = Main.getConfigManager().getConfig()
                        .getBoolean("chat.filter.actions.punish-on-replace", false);
                int effectiveWeight = punishOnReplace ? currentWeight : 0;
                context.flagViolation(type, reasonRef, punishOnReplace, effectiveWeight, placeholders);
                context.setCancelled(false); // message is allowed through
                context.setMessage(msg);     // with bad content replaced
            }
        }

        return true;
    }


}
