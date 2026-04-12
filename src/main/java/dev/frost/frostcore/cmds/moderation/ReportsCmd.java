package dev.frost.frostcore.cmds.moderation;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.gui.impls.ReportsGui;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.moderation.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/** /reports [page] — view open reports */
public class ReportsCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();
    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        int page = 0;
        if (args.length >= 1) try { page = Math.max(0, Integer.parseInt(args[0]) - 1); } catch (NumberFormatException ignored) {}
        final int pNum = page;

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            ModerationDatabase modDb = ModerationManager.getInstance().getDatabase();
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                if (sender instanceof org.bukkit.entity.Player p) {
                    List<Report> guiReports = modDb.getOpenReports(0, 1000); // load max 1000
                    ReportsGui.open(p, guiReports);
                } else {
                    List<Report> reports = modDb.getOpenReports(pNum, 10);
                    int total = modDb.countOpenReports();
                    int totalPages = Math.max(1, (int) Math.ceil(total / 10.0));
                    mm.sendRaw(sender, "");
                    mm.sendRaw(sender, "<gradient:#D4727A:#A35560>REPORTS</gradient> <dark_gray>» <#8FA3BF>Open Reports <dark_gray>(Page " + (pNum + 1) + "/" + totalPages + ", " + total + " total)");
                    mm.sendRaw(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    if (reports.isEmpty()) mm.sendRaw(sender, "  <#707880>No open reports.");
                    else for (Report r : reports) {
                        mm.sendRaw(sender, "  <#6B8DAE>#" + r.id() + " <dark_gray>| <white>" + r.getTargetDisplayName()
                                + " <dark_gray>by <#8FA3BF>" + r.getReporterDisplayName()
                                + " <dark_gray>| <white>" + r.reason()
                                + " <dark_gray>| <#707880>" + r.getAge());
                    }
                    mm.sendRaw(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                }
            });
        });
        return true;
    }
    @Override public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) { return Collections.emptyList(); }
}
