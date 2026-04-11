package dev.frost.frostcore.utils;

import dev.frost.frostcore.Main;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FrostLogger {

    private static File auditLogFile;
    private static final MiniMessage MINIMESSAGE = MiniMessage.miniMessage();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void init(Main plugin) {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File logsFolder = new File(dataFolder, "logs");
        if (!logsFolder.exists()) {
            logsFolder.mkdirs();
        }

        auditLogFile = new File(logsFolder, "audit.log");
        try {
            if (!auditLogFile.exists()) {
                auditLogFile.createNewFile();
            }
        } catch (IOException e) {
            error("Failed to create audit.log file!", e);
        }
    }

    public static void printBanner() {
        String banner = """
                            <aqua>   ______               _    _____                  </aqua>
                            <aqua>  |  ____|             | |  / ____|                 </aqua>
                            <aqua>  | |__ _ __ ___  ___  | |_| |     ___  _ __ ___    </aqua>
                            <aqua>  |  __| '__/ _ \\/ __| | __| |    / _ \\| '__/ _ \\   </aqua>
                            <aqua>  | |  | | | (_) \\__ \\ | |_| |___| (_) | | |  __/   </aqua>
                            <aqua>  |_|  |_|  \\___/|___/  \\__|\\_____\\___/|_|  \\___|   </aqua>

                            <gray>             Running FrostCore %s               </gray>
                            """.formatted(Main.getInstance().getDescription().getVersion());

        String[] lines = banner.split("\n");
        for (String line : lines) {
            Bukkit.getConsoleSender().sendMessage(MINIMESSAGE.deserialize(line));
        }
    }

    public static void info(String message) {
        Bukkit.getConsoleSender().sendMessage(MINIMESSAGE.deserialize("<bold><aqua>[FrostCore]</aqua></bold> <white>" + message + "</white>"));
    }

    public static void warn(String message) {
        Bukkit.getConsoleSender().sendMessage(MINIMESSAGE.deserialize("<bold><yellow>[FrostCore]</yellow></bold> <gold>" + message + "</gold>"));
    }

    public static void error(String message) {
        Bukkit.getConsoleSender().sendMessage(MINIMESSAGE.deserialize("<bold><dark_red>[FrostCore]</dark_red></bold> <red>" + message + "</red>"));
    }

    public static void error(String message, Throwable throwable) {
        error(message);
        Bukkit.getConsoleSender().sendMessage(MINIMESSAGE.deserialize("<red>" + throwable.toString() + "</red>"));
        for (StackTraceElement element : throwable.getStackTrace()) {
            Bukkit.getConsoleSender().sendMessage(MINIMESSAGE.deserialize("<gray>   at " + element.toString() + "</gray>"));
        }
    }

    public static void audit(String message) {
        if (auditLogFile == null) return;
        if (!Main.getConfigManager().getBoolean("logging.audit-enabled", true)) return;

        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        String logEntry = String.format("[%s] [AUDIT] %s", timestamp, message);
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try (FileWriter fw = new FileWriter(auditLogFile, true);
                 PrintWriter pw = new PrintWriter(fw)) {
                pw.println(logEntry);
            } catch (IOException e) {
                error("Failed to write to audit log", e);
            }
        });
    }
}

