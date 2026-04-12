package dev.frost.frostcore.moderation;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.ConfigManager;
import dev.frost.frostcore.utils.DW;
import dev.frost.frostcore.utils.FrostLogger;
import org.bukkit.Bukkit;

import java.awt.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Manages Discord webhook integrations for the moderation system.
 * Validates webhook URLs on startup and sends punishment/report/staff embeds.
 */
public class WebhookManager {

    private String punishmentUrl;
    private String reportUrl;
    private String staffActivityUrl;

    private boolean punishmentEnabled;
    private boolean reportEnabled;
    private boolean staffEnabled;

    // Reddish moderation theme colors
    private static final Color COLOR_BAN     = new Color(0xD4, 0x72, 0x7A);
    private static final Color COLOR_MUTE    = new Color(0xD4, 0xA7, 0x6A);
    private static final Color COLOR_WARN    = new Color(0xC8, 0xA8, 0x7C);
    private static final Color COLOR_KICK    = new Color(0xA3, 0x55, 0x60);
    private static final Color COLOR_UNBAN   = new Color(0x7E, 0xCF, 0xA0);
    private static final Color COLOR_REPORT  = new Color(0x6B, 0x8D, 0xAE);
    private static final Color COLOR_STAFF   = new Color(0xA6, 0x8F, 0xCF);

    public WebhookManager() {
        reload();
    }

    /**
     * Load/reload webhook URLs from config and validate each one.
     */
    public void reload() {
        ConfigManager config = Main.getConfigManager();
        punishmentUrl = config.getString("moderation.webhooks.punishments", "");
        reportUrl = config.getString("moderation.webhooks.reports", "");
        staffActivityUrl = config.getString("moderation.webhooks.staff-activity", "");

        // Validate each webhook asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            punishmentEnabled = validateWebhook(punishmentUrl, "Punishments");
            reportEnabled = validateWebhook(reportUrl, "Reports");
            staffEnabled = validateWebhook(staffActivityUrl, "Staff Activity");

            int count = (punishmentEnabled ? 1 : 0) + (reportEnabled ? 1 : 0) + (staffEnabled ? 1 : 0);
            if (count > 0) {
                FrostLogger.info("Discord webhooks: " + count + "/3 channels active.");
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

    // ━━━━━━━━━━━━━━━━━━━━━━━ PUNISHMENT WEBHOOK ━━━━━━━━━━━━━━━━━━━━━━━

    public void sendPunishmentWebhookAsync(Punishment p) {
        if (!punishmentEnabled) return;
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> sendPunishmentWebhook(p));
    }

    private void sendPunishmentWebhook(Punishment p) {
        try {
            DW webhook = new DW(punishmentUrl);
            webhook.setUsername("FrostCore Moderation");

            Color color = switch (p.type().getCategory()) {
                case "BAN"  -> COLOR_BAN;
                case "MUTE" -> COLOR_MUTE;
                case "WARN" -> COLOR_WARN;
                case "KICK" -> COLOR_KICK;
                default     -> COLOR_BAN;
            };

            String silentTag = p.silent() ? " [Silent]" : "";
            String durationStr = p.isPermanent() ? "Permanent" : p.getFormattedDuration();

            DW.EmbedObject embed = new DW.EmbedObject()
                    .setTitle(p.type().getDisplayName() + silentTag)
                    .setColor(color)
                    .addField("Player", p.getTargetDisplayName(), true)
                    .addField("Staff", p.getStaffDisplayName(), true)
                    .addField("Duration", durationStr, true)
                    .addField("Reason", p.reason(), false)
                    .setFooter("ID: #" + p.randomId() + " | " + p.server(), null);

            webhook.addEmbed(embed);
            webhook.execute();
        } catch (IOException e) {
            FrostLogger.warn("Failed to send punishment webhook: " + e.getMessage());
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━ UNPUNISH WEBHOOK ━━━━━━━━━━━━━━━━━━━━━━━

    public void sendUnpunishWebhookAsync(Punishment p, String removedBy) {
        if (!punishmentEnabled) return;
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> sendUnpunishWebhook(p, removedBy));
    }

    private void sendUnpunishWebhook(Punishment p, String removedBy) {
        try {
            DW webhook = new DW(punishmentUrl);
            webhook.setUsername("FrostCore Moderation");

            String action = switch (p.type().getCategory()) {
                case "BAN"  -> "Unbanned";
                case "MUTE" -> "Unmuted";
                case "WARN" -> "Unwarned";
                default     -> "Removed";
            };

            DW.EmbedObject embed = new DW.EmbedObject()
                    .setTitle(action)
                    .setColor(COLOR_UNBAN)
                    .addField("Player", p.getTargetDisplayName(), true)
                    .addField("Removed By", removedBy, true)
                    .setFooter("Original ID: #" + p.randomId(), null);

            webhook.addEmbed(embed);
            webhook.execute();
        } catch (IOException e) {
            FrostLogger.warn("Failed to send unpunish webhook: " + e.getMessage());
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━ REPORT WEBHOOK ━━━━━━━━━━━━━━━━━━━━━━━

    public void sendReportWebhookAsync(Report r) {
        if (!reportEnabled) return;
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> sendReportWebhook(r));
    }

    private void sendReportWebhook(Report r) {
        try {
            DW webhook = new DW(reportUrl);
            webhook.setUsername("FrostCore Reports");

            DW.EmbedObject embed = new DW.EmbedObject()
                    .setTitle("Player Report #" + r.id())
                    .setColor(COLOR_REPORT)
                    .addField("Reporter", r.getReporterDisplayName(), true)
                    .addField("Target", r.getTargetDisplayName(), true)
                    .addField("Reason", r.reason(), false)
                    .setFooter("Report ID: #" + r.id(), null);

            webhook.addEmbed(embed);
            webhook.execute();
        } catch (IOException e) {
            FrostLogger.warn("Failed to send report webhook: " + e.getMessage());
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━ STAFF ACTIVITY ━━━━━━━━━━━━━━━━━━━━━━━

    public void sendStaffActivityAsync(String action, String staffName, String details) {
        if (!staffEnabled) return;
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                DW webhook = new DW(staffActivityUrl);
                webhook.setUsername("FrostCore Staff");

                DW.EmbedObject embed = new DW.EmbedObject()
                        .setTitle(action)
                        .setColor(COLOR_STAFF)
                        .addField("Staff", staffName, true)
                        .addField("Details", details, false);

                webhook.addEmbed(embed);
                webhook.execute();
            } catch (IOException e) {
                FrostLogger.warn("Failed to send staff activity webhook: " + e.getMessage());
            }
        });
    }
}
