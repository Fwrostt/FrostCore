package dev.frost.frostcore.invites.handlers;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.invites.Invite;
import dev.frost.frostcore.invites.InviteHandler;
import dev.frost.frostcore.invites.InviteType;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.utils.TeleportUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Handles all lifecycle events for {@link InviteType#TPA} invites.
 * The invite sender is the one who wants to teleport TO the target.
 */
public class TpaInviteHandler implements InviteHandler {

    private final MessageManager mm;
    private final TeleportUtil teleportUtil;

    public TpaInviteHandler(MessageManager mm, TeleportUtil teleportUtil) {
        this.mm = mm;
        this.teleportUtil = teleportUtil;
    }

    @Override
    public void onAccept(Invite invite) {
        Player sender = Bukkit.getPlayer(invite.getSender());
        Player target = Bukkit.getPlayer(invite.getTarget());

        if (sender == null || target == null) return;

        mm.send(sender, "teleport.tpa-accepted-sender", Map.of("player", target.getName()));
        mm.send(target, "teleport.tpa-accepted-target", Map.of("player", sender.getName()));

        teleportUtil.teleportWithCooldownAndDelay(
                sender, target.getLocation(),
                "tpa",
                "tpa.cooldown",
                "teleport.tpa-cooldown",
                Main.getConfigManager().getInt("tpa.delay", 3),
                "teleport.tpa-wait",
                "teleport.tpa-teleport",
                "teleport.tpa-teleport-cancelled"
        );
    }

    @Override
    public void onDecline(Invite invite) {
        Player sender = Bukkit.getPlayer(invite.getSender());
        Player target = Bukkit.getPlayer(invite.getTarget());

        if (sender != null && target != null) {
            mm.send(sender, "teleport.tpa-declined-sender", Map.of("player", target.getName()));
        }
        if (target != null) {
            mm.send(target, "teleport.tpa-declined-target");
        }
    }

    @Override
    public void onExpire(Invite invite) {
        Player sender = Bukkit.getPlayer(invite.getSender());
        Player target = Bukkit.getPlayer(invite.getTarget());

        if (sender != null) {
            String targetName = target != null ? target.getName() : "Unknown";
            mm.send(sender, "teleport.tpa-expired-sender", Map.of("player", targetName));
        }
        if (target != null) {
            String senderName = sender != null ? sender.getName() : "Unknown";
            mm.send(target, "teleport.tpa-expired-target", Map.of("player", senderName));
        }
    }
}

