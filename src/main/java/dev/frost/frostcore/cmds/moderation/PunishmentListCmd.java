package dev.frost.frostcore.cmds.moderation;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.gui.impls.PunishmentListGui;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.moderation.*;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/** /banlist, /mutelist, /warnlist — paginated active punishment lists */
public class PunishmentListCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();
    private final String category;
    private final String title;

    public PunishmentListCmd(String category, String title) { this.category = category; this.title = title; }

    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        int page = 0;
        if (args.length >= 1) { try { page = Math.max(0, Integer.parseInt(args[0]) - 1); } catch (NumberFormatException ignored) {} }
        final int pNum = page;

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            ModerationDatabase modDb = ModerationManager.getInstance().getDatabase();
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                if (sender instanceof org.bukkit.entity.Player p) {
                    List<Punishment> guiList = modDb.getActivePunishmentsPaginated(category, 0, 1000);
                    PunishmentListGui.open(p, category, title, guiList);
                } else {
                    List<Punishment> list = modDb.getActivePunishmentsPaginated(category, pNum, 10);
                    int total = modDb.countActivePunishments(category);
                    int totalPages = Math.max(1, (int) Math.ceil(total / 10.0));
                    mm.sendRaw(sender, "");
                    mm.sendRaw(sender, "<gradient:#D4727A:#A35560>MODERATION</gradient> <dark_gray>» <#8FA3BF>" + title + " <dark_gray>(Page " + (pNum + 1) + "/" + totalPages + ", " + total + " total)");
                    mm.sendRaw(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                    if (list.isEmpty()) {
                        mm.sendRaw(sender, "  <#707880>No active " + category.toLowerCase() + "s found.");
                    } else {
                        for (Punishment pun : list) {
                            mm.sendRaw(sender, "  <#D4727A>#" + pun.randomId() + " <dark_gray>| <white>" + pun.getTargetDisplayName()
                                    + " <dark_gray>| <#8FA3BF>" + pun.reason() + " <dark_gray>| <#707880>" + pun.getFormattedRemaining());
                        }
                    }
                    mm.sendRaw(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                }
            });
        });
        return true;
    }
    @Override public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) { return Collections.emptyList(); }
}
