package dev.frost.frostcore.cmds.admin;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class RamCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();
    private final MiniMessage mini = MiniMessage.miniMessage();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("frostcore.admin.ram")) {
            mm.send(sender, "general.no-permission");
            return true;
        }

        Runtime runtime = Runtime.getRuntime();
        long maxMem = runtime.maxMemory() / 1024 / 1024;
        long totalMem = runtime.totalMemory() / 1024 / 1024;
        long freeMem = runtime.freeMemory() / 1024 / 1024;
        long usedMem = totalMem - freeMem;
        double usagePercent = (double) usedMem / maxMem * 100;

        String bar = "<dark_gray><strikethrough>                                        </strikethrough>";
        String memBar = buildBar(usedMem, maxMem, usagePercent < 75 ? "<#55FF55>" : "<#FF5555>", "<dark_gray>");

        sender.sendMessage(mini.deserialize(bar));
        sender.sendMessage(mini.deserialize("  <gradient:#6BA3E3:#4979C7><bold>SERVER INFO</bold>"));
        sender.sendMessage(mini.deserialize(bar));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>💾 Memory:      " + memBar + " <white>" + usedMem + "/" + maxMem + " MB"));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>👥 Online:      <white>" + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers()));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>🌍 Worlds:      <white>" + Bukkit.getWorlds().size()));

        double[] tps = Bukkit.getTPS();
        String tpsColor = tps[0] >= 19.5 ? "<#55FF55>" : tps[0] >= 15 ? "<#FFAA00>" : "<#FF5555>";
        sender.sendMessage(mini.deserialize("  <#6BA3E3>⚡ TPS:          " + tpsColor + String.format("%.1f", tps[0]) + "<dark_gray>, " + String.format("%.1f", tps[1]) + ", " + String.format("%.1f", tps[2])));

        sender.sendMessage(mini.deserialize("  <#6BA3E3>☕ Java:         <white>" + System.getProperty("java.version")));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>📦 Version:     <white>" + Bukkit.getVersion()));
        sender.sendMessage(mini.deserialize(bar));
        return true;
    }

    private String buildBar(double current, double max, String filledColor, String emptyColor) {
        int barLength = 10;
        int filled = (int) Math.round((current / max) * barLength);
        int empty = barLength - filled;
        return filledColor + "█".repeat(Math.max(0, filled)) + emptyColor + "█".repeat(Math.max(0, empty));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
