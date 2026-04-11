package dev.frost.frostcore.cmds;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.invites.InviteManager;
import dev.frost.frostcore.invites.InviteType;
import dev.frost.frostcore.manager.ConfigManager;
import dev.frost.frostcore.manager.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TpaCmd implements CommandExecutor, TabCompleter {

    private final MessageManager mm = MessageManager.get();
    private final InviteManager inviteManager = Main.getInviteManager();
    private final ConfigManager config = Main.getConfigManager();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!config.getBoolean("tpa.enabled", true)) {
            mm.send(player, "teleport.tpa-disabled");
            return true;
        }

        if (args.length < 1) {
            mm.sendRaw(player, "<#B0C4FF>Usage: <white>/tpa <player>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            mm.send(player, "teleport.player-not-found");
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            mm.send(player, "teleport.tpa-self");
            return true;
        }

        if (dev.frost.frostcore.manager.CooldownManager.isOnCooldown(player, "tpa")) {
            int remaining = dev.frost.frostcore.manager.CooldownManager.getRemainingTime(player, "tpa");
            mm.send(player, "teleport.tpa-cooldown", Map.of("time", String.valueOf(remaining)));
            return true;
        }

        if (target.getPersistentDataContainer().has(dev.frost.frostcore.cmds.TpaToggleCmd.TPA_DISABLED_KEY, org.bukkit.persistence.PersistentDataType.BYTE)) {
            mm.sendRaw(player, "<red>" + target.getName() + " has teleport requests disabled.");
            return true;
        }

        if (inviteManager.hasInviteFrom(target.getUniqueId(), InviteType.TPA, player.getUniqueId())) {
            mm.send(player, "teleport.tpa-already-sent");
            return true;
        }

        int expiry = config.getInt("tpa.expiry", 60);

        inviteManager.sendInvite(
                InviteType.TPA,
                player.getUniqueId(),
                target.getUniqueId(),
                Map.of(),
                expiry
        );

        mm.send(player, "teleport.tpa-sent", Map.of("player", target.getName()));
        TpaUIHelper.sendTpaRequest(target, player.getName(), "wants to teleport to you", expiry);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> !name.equals(sender.getName()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
