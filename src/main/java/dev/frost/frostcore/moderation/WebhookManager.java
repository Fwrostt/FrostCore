package dev.frost.frostcore.moderation;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.ConfigManager;
import dev.frost.frostcore.utils.DW;
import dev.frost.frostcore.utils.FrostLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class WebhookManager {

    private String punishmentUrl;
    private String reportUrl;
    private String staffActivityUrl;
    private String staffChatUrl;

    private boolean punishmentEnabled;
    private boolean reportEnabled;
    private boolean staffEnabled;
    private boolean staffChatEnabled;

    private static final Color COLOR_BAN     = new Color(0xD4, 0x72, 0x7A);
    private static final Color COLOR_MUTE    = new Color(0xD4, 0xA7, 0x6A);
    private static final Color COLOR_WARN    = new Color(0xC8, 0xA8, 0x7C);
    private static final Color COLOR_KICK    = new Color(0xA3, 0x55, 0x60);
    private static final Color COLOR_JAIL    = new Color(0x6B, 0x8D, 0xAE);
    private static final Color COLOR_UNBAN   = new Color(0x7E, 0xCF, 0xA0);
    private static final Color COLOR_REPORT  = new Color(0x6B, 0x8D, 0xAE);
    private static final Color COLOR_STAFF   = new Color(0xA6, 0x8F, 0xCF);

    private static final String HEAD_URL = "https://mc-heads.net/avatar/%s/128";
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a").withZone(ZoneId.systemDefault());

    public WebhookManager() {
        reload();
    }

    public void reload() {
        ConfigManager config = Main.getConfigManager();
        punishmentUrl = config.getString("moderation.webhooks.punishments", "");
        reportUrl = config.getString("moderation.webhooks.reports", "");
        staffActivityUrl = config.getString("moderation.webhooks.staff-activity", "");
        staffChatUrl = config.getString("moderation.webhooks.staffchat", "");

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            punishmentEnabled = validateWebhook(punishmentUrl, "Punishments");
            reportEnabled = validateWebhook(reportUrl, "Reports");
            staffEnabled = validateWebhook(staffActivityUrl, "Staff Activity");
            staffChatEnabled = validateWebhook(staffChatUrl, "Staff Chat");

            int count = (punishmentEnabled ? 1 : 0) + (reportEnabled ? 1 : 0)
                    + (staffEnabled ? 1 : 0) + (staffChatEnabled ? 1 : 0);
            if (count > 0) {
                FrostLogger.info("Discord webhooks: " + count + "/4 channels active.");
            } else {
                FrostLogger.info("Discord webhooks: disabled (no valid URLs configured).");
            }
        });
    }

    private boolean validateWebhook(String url, String label) {
        if (url == null || url.isEmpty()) return false;
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int code = conn.getResponseCode();
            conn.disconnect();
            if (code == 200) {
                FrostLogger.info("Webhook [" + label + "] validated successfully.");
                return true;
            } else {
                FrostLogger.warn("Webhook [" + label + "] returned status " + code + ". Disabled.");
                return false;
            }
        } catch (Exception e) {
            FrostLogger.warn("Webhook [" + label + "] validation failed: " + e.getMessage() + ". Disabled.");
            return false;
        }
    }

    private String headUrl(UUID uuid) {
        return String.format(HEAD_URL, uuid != null ? uuid.toString().replace("-", "") : "Steve");
    }

    private String timestamp(long epochMs) {
        return TIMESTAMP_FMT.format(Instant.ofEpochMilli(epochMs));
    }

    

    public void sendPunishmentWebhookAsync(Punishment p, String playerStatus) {
        if (!punishmentEnabled) return;
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> sendPunishmentWebhook(p, playerStatus));
    }

    private void sendPunishmentWebhook(Punishment p, String playerStatus) {
        try {
            DW webhook = new DW(punishmentUrl);
            webhook.setUsername("FrostCore Moderation");
            webhook.setAvatarUrl(headUrl(p.staffUuid()));

            Color color = switch (p.type().getCategory()) {
                case "BAN"  -> COLOR_BAN;
                case "MUTE" -> COLOR_MUTE;
                case "WARN" -> COLOR_WARN;
                case "KICK" -> COLOR_KICK;
                case "JAIL" -> COLOR_JAIL;
                default     -> COLOR_BAN;
            };

            String emoji = switch (p.type().getCategory()) {
                case "BAN"  -> "\uD83D\uDD28";  
                case "MUTE" -> "\uD83D\uDD07";  
                case "WARN" -> "⚠\uFE0F";       
                case "KICK" -> "\uD83D\uDC62";  
                case "JAIL" -> "⛓\uFE0F";       
                default     -> "\uD83D\uDEA8";  
            };

            String silentTag = p.silent() ? "  `[SILENT]`" : "";
            String durationStr = p.isPermanent() ? "**Permanent**" : "`" + p.getFormattedDuration() + "`";
            String expiresStr = p.isPermanent() ? "Never" : timestamp(p.expiresAt());

            StringBuilder desc = new StringBuilder();
            desc.append("**").append(p.getStaffDisplayName()).append("** ")
                .append(p.type().getPastTense()).append(" **")
                .append(p.getTargetDisplayName()).append("**")
                .append(silentTag).append("\n\n");

            desc.append("> **Reason**\n");
            desc.append("> ```").append(p.reason()).append("```\n\n");

            desc.append("\uD83D\uDD52 **Duration:** ").append(durationStr).append("\n");
            if (!p.isPermanent()) {
                desc.append("\uD83D\uDCC5 **Expires:** `").append(expiresStr).append("`\n");
            }
            desc.append("\uD83C\uDFAE **Server:** `").append(p.server()).append("`\n");  

            if (playerStatus.equalsIgnoreCase("Offline")) {
                desc.append("\uD83D\uDCF6 **Status:** `Offline`");
            } else {
                String gamemode = playerStatus.substring(0, 1).toUpperCase() + playerStatus.substring(1).toLowerCase();
                desc.append("\uD83D\uDCAE **Gamemode:** `").append(gamemode).append("`");
            }

            DW.EmbedObject embed = new DW.EmbedObject()
                    .setTitle(emoji + "  " + p.type().getDisplayName() + silentTag)
                    .setDescription(desc.toString())
                    .setColor(color)
                    .setThumbnail(headUrl(p.targetUuid()))
                    .setAuthor(
                            p.getStaffDisplayName(),
                            null,
                            headUrl(p.staffUuid())
                    )
                    .setFooter(
                            "ID: #" + p.randomId() + "  •  " + timestamp(p.createdAt()),
                            null
                    );

            webhook.addEmbed(embed);
            webhook.execute();
        } catch (IOException e) {
            FrostLogger.warn("Failed to send punishment webhook: " + e.getMessage());
        }
    }

    

    public void sendUnpunishWebhookAsync(Punishment p, String removedBy) {
        if (!punishmentEnabled) return;
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> sendUnpunishWebhook(p, removedBy));
    }

    private void sendUnpunishWebhook(Punishment p, String removedBy) {
        try {
            DW webhook = new DW(punishmentUrl);
            webhook.setUsername("FrostCore Moderation");
            webhook.setAvatarUrl(headUrl(p.targetUuid()));

            String action = switch (p.type().getCategory()) {
                case "BAN"  -> "Unbanned";
                case "MUTE" -> "Unmuted";
                case "WARN" -> "Unwarned";
                case "JAIL" -> "Unjailed";
                default     -> "Removed";
            };

            String emoji = switch (p.type().getCategory()) {
                case "BAN"  -> "✅";
                case "MUTE" -> "\uD83D\uDD0A";  
                case "WARN" -> "\uD83D\uDDD1\uFE0F";  
                case "JAIL" -> "\uD83D\uDD13";  
                default     -> "↩\uFE0F";       
            };

            StringBuilder desc = new StringBuilder();
            desc.append("**").append(removedBy).append("** ")
                .append(action.toLowerCase()).append(" **")
                .append(p.getTargetDisplayName()).append("**\n\n");

            desc.append("> **Original Reason**\n");
            desc.append("> ```").append(p.reason()).append("```\n\n");

            desc.append("\uD83D\uDD52 **Original Duration:** `").append(p.getFormattedDuration()).append("`\n");  
            desc.append("\uD83D\uDCC5 **Issued:** `").append(timestamp(p.createdAt())).append("`");  

            DW.EmbedObject embed = new DW.EmbedObject()
                    .setTitle(emoji + "  " + action)
                    .setDescription(desc.toString())
                    .setColor(COLOR_UNBAN)
                    .setThumbnail(headUrl(p.targetUuid()))
                    .setFooter(
                            "Original ID: #" + p.randomId() + "  •  " + timestamp(System.currentTimeMillis()),
                            null
                    );

            webhook.addEmbed(embed);
            webhook.execute();
        } catch (IOException e) {
            FrostLogger.warn("Failed to send unpunish webhook: " + e.getMessage());
        }
    }

    

    public void sendReportWebhookAsync(Report r) {
        if (!reportEnabled) return;
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> sendReportWebhook(r));
    }

    private void sendReportWebhook(Report r) {
        try {
            DW webhook = new DW(reportUrl);
            webhook.setUsername("FrostCore Reports");
            webhook.setAvatarUrl(headUrl(r.reporterUuid()));

            StringBuilder desc = new StringBuilder();
            desc.append("**").append(r.getReporterDisplayName()).append("** reported **")
                .append(r.getTargetDisplayName()).append("**\n\n");

            desc.append("> **Reason**\n");
            desc.append("> ```").append(r.reason()).append("```\n\n");

            desc.append("\uD83D\uDD52 **Submitted:** `").append(timestamp(r.createdAt())).append("`");  

            DW.EmbedObject embed = new DW.EmbedObject()
                    .setTitle("\uD83D\uDCE8  Player Report #" + r.id())  
                    .setDescription(desc.toString())
                    .setColor(COLOR_REPORT)
                    .setThumbnail(headUrl(r.targetUuid()))
                    .setAuthor(
                            r.getReporterDisplayName() + " → " + r.getTargetDisplayName(),
                            null,
                            headUrl(r.reporterUuid())
                    )
                    .setFooter(
                            "Report #" + r.id() + "  •  " + timestamp(r.createdAt()),
                            null
                    );

            webhook.addEmbed(embed);
            webhook.execute();
        } catch (IOException e) {
            FrostLogger.warn("Failed to send report webhook: " + e.getMessage());
        }
    }

    

    public void sendStaffActivityAsync(String action, String staffName, String details) {
        if (!staffEnabled) return;
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                DW webhook = new DW(staffActivityUrl);
                webhook.setUsername("FrostCore Staff");

                StringBuilder desc = new StringBuilder();
                desc.append("**").append(staffName).append("** performed an action\n\n");
                desc.append("> **Details**\n");
                desc.append("> ```").append(details).append("```");

                DW.EmbedObject embed = new DW.EmbedObject()
                        .setTitle("\uD83D\uDC6E  " + action)  
                        .setDescription(desc.toString())
                        .setColor(COLOR_STAFF)
                        .setFooter(
                                "Staff: " + staffName + "  •  " + timestamp(System.currentTimeMillis()),
                                null
                        );

                webhook.addEmbed(embed);
                webhook.execute();
            } catch (IOException e) {
                FrostLogger.warn("Failed to send staff activity webhook: " + e.getMessage());
            }
        });
    }

    

    public void sendStaffChatWebhookAsync(String staffName, String message) {
        if (!staffChatEnabled) return;
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                DW webhook = new DW(staffChatUrl);
                webhook.setUsername(staffName);
                webhook.setAvatarUrl("https://mc-heads.net/avatar/" + staffName + "/128");
                webhook.setContent(message);
                webhook.execute();
            } catch (IOException e) {
                FrostLogger.warn("Failed to send staff chat webhook: " + e.getMessage());
            }
        });
    }
}
