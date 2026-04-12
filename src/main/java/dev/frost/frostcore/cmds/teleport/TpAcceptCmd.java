package dev.frost.frostcore.cmds.teleport;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.invites.Invite;
import dev.frost.frostcore.invites.InviteManager;
import dev.frost.frostcore.invites.InviteType;
import dev.frost.frostcore.manager.ConfigManager;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.utils.TeleportUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class TpAcceptCmd implements CommandExecutor, TabCompleter {

    private final MessageManager mm = MessageManager.get();
    private final InviteManager inviteManager = Main.getInviteManager();
    private final TeleportUtil teleportUtil = Main.getTeleportUtil();
    private final ConfigManager config = Main.getConfigManager();

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

        Invite tpaInvite = findInvite(targetUUID, InviteType.TPA, fromSender);
        Invite tpaHereInvite = findInvite(targetUUID, InviteType.TPA_HERE, fromSender);

        Invite invite = tpaInvite != null ? tpaInvite : tpaHereInvite;

        if (invite == null) {
            mm.send(player, "teleport.tpa-no-pending");
            return true;
        }

        boolean accepted;
        if (invite.getType() == InviteType.TPA) {
            accepted = inviteManager.acceptInvite(targetUUID, InviteType.TPA, invite.getSender());
        } else {
            accepted = inviteManager.acceptInvite(targetUUID, InviteType.TPA_HERE, invite.getSender());
        }

        if (!accepted) {
            mm.send(player, "teleport.tpa-no-pending");
        }

        return true;
    }

    private Invite findInvite(UUID target, InviteType type, UUID fromSender) {
        List<Invite> invites = inviteManager.getInvites(target, type);
        if (invites.isEmpty()) return null;

        if (fromSender != null) {
            return invites.stream()
                    .filter(inv -> inv.getSender().equals(fromSender))
                    .findFirst()
                    .orElse(null);
        }

        return invites.get(invites.size() - 1);
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

