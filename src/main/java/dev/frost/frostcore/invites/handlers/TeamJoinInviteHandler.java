package dev.frost.frostcore.invites.handlers;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.exceptions.TeamException;
import dev.frost.frostcore.invites.Invite;
import dev.frost.frostcore.invites.InviteHandler;
import dev.frost.frostcore.invites.InviteType;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.manager.TeamManager;
import dev.frost.frostcore.teams.Team;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles all lifecycle events for {@link InviteType#TEAM_JOIN} invites.
 */
public class TeamJoinInviteHandler implements InviteHandler {

    private final MessageManager mm;
    private final TeamManager teamManager;

    public TeamJoinInviteHandler(MessageManager mm, TeamManager teamManager) {
        this.mm = mm;
        this.teamManager = teamManager;
    }

    @Override
    public void onAccept(Invite invite) {
        Player target = Bukkit.getPlayer(invite.getTarget());
        String teamName = invite.getMeta("team", "unknown");

        try {
            Team team = teamManager.getTeam(teamName);
            teamManager.addMember(team, invite.getTarget());

            if (target != null) {
                mm.send(target, "teams.join", Map.of("team", team.getName()));
            }

            String playerDisplayName = target != null ? target.getName() : "Unknown";
            notifyTeam(team, "teams.invite-accepted", Map.of("player", playerDisplayName));

        } catch (TeamException e) {
            if (target != null) {
                mm.sendRaw(target, "<red>Could not join team: " + e.getMessage() + "</red>");
            }
        }
    }

    @Override
    public void onDecline(Invite invite) {
        Player sender = Bukkit.getPlayer(invite.getSender());
        Player target = Bukkit.getPlayer(invite.getTarget());
        String targetName = target != null ? target.getName() : "Unknown";

        if (target != null) {
            mm.send(target, "teams.invite-declined-target");
        }
        if (sender != null) {
            mm.send(sender, "teams.invite-declined-sender", Map.of("player", targetName));
        }
    }

    @Override
    public void onExpire(Invite invite) {
        Player sender = Bukkit.getPlayer(invite.getSender());
        Player target = Bukkit.getPlayer(invite.getTarget());
        String teamName = invite.getMeta("team", "unknown");

        if (target != null) {
            mm.send(target, "teams.invite-expired", Map.of("team", teamName));
        }
        if (sender != null) {
            String targetName = target != null ? target.getName() : "Unknown";
            mm.send(sender, "teams.invite-expired-sender", Map.of("player", targetName));
        }
    }

    private void notifyTeam(Team team, String path, Map<String, String> placeholders) {
        Set<UUID> all = new java.util.HashSet<>();
        all.addAll(team.getOwners());
        all.addAll(team.getAdmins());
        all.addAll(team.getMembers());
        for (UUID uuid : all) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                mm.send(p, path, placeholders);
            }
        }
    }
}

