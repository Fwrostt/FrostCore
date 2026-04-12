package dev.frost.frostcore.cmds.teleport;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.invites.Invite;
import dev.frost.frostcore.invites.InviteManager;
import dev.frost.frostcore.invites.InviteType;
import dev.frost.frostcore.manager.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class TpDeclineCmd implements CommandExecutor, TabCompleter {

    private final MessageManager mm = MessageManager.get();
    private final InviteManager inviteManager = Main.getInviteManager();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        UUID targetUUID = player.getUniqueId();
        UUID fromSender = null;

        if (args.length >= 1) {
            Player fromPlayer = Bukkit.getPlayerExact(args[0]);
            if (fromPlayer != null) {
                fromSender = fromPlayer.getUniqueId();
            } else {
                mm.send(player, "teleport.player-not-found");
                return true;
            }
        }

        if (inviteManager.declineInvite(targetUUID, InviteType.TPA, fromSender)) return true;
        if (inviteManager.declineInvite(targetUUID, InviteType.TPA_HERE, fromSender)) return true;

        mm.send(player, "teleport.tpa-no-pending");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender instanceof Player p) {
            Set<String> names = new LinkedHashSet<>();
            for (Invite inv : inviteManager.getAllInvites(p.getUniqueId())) {
                if (inv.getType() == InviteType.TPA || inv.getType() == InviteType.TPA_HERE) {
                    Player s = Bukkit.getPlayer(inv.getSender());
                    if (s != null) names.add(s.getName());
                }
            }
            return new ArrayList<>(names);
        }
        return Collections.emptyList();
    }
}

