package dev.frost.frostcore.invites.handlers;

import dev.frost.frostcore.database.DatabaseManager;
import dev.frost.frostcore.exceptions.TeamException;
import dev.frost.frostcore.invites.Invite;
import dev.frost.frostcore.invites.InviteHandler;
import dev.frost.frostcore.invites.InviteManager;
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
 * Handles all lifecycle events for {@link InviteType#TEAM_ALLY} invites.
 */
public class TeamAllyInviteHandler implements InviteHandler {

    private final MessageManager mm;
    private final TeamManager teamManager;
    private final DatabaseManager db;
    private final InviteManager inviteManager;

    public TeamAllyInviteHandler(MessageManager mm, TeamManager teamManager,
                                  DatabaseManager db, InviteManager inviteManager) {
        this.mm = mm;
        this.teamManager = teamManager;
        this.db = db;
        this.inviteManager = inviteManager;
    }

    @Override
    public void onAccept(Invite invite) {
        String senderTeam = invite.getMeta("senderTeam", "unknown");
        String targetTeam = invite.getMeta("targetTeam", "unknown");

        try {
            Team sTeam = teamManager.getTeam(senderTeam);
            Team tTeam = teamManager.getTeam(targetTeam);

            sTeam.addAlly(targetTeam);
            tTeam.addAlly(senderTeam);

            db.saveRelationsAsync(sTeam);
            db.saveRelationsAsync(tTeam);

            notifyTeam(sTeam, "teams.ally-accepted", Map.of("team", targetTeam));
            notifyTeam(tTeam, "teams.ally-accepted", Map.of("team", senderTeam));

            // Silently cancel any duplicate outstanding ally invites between these two teams
            inviteManager.cancelInvites(InviteType.TEAM_ALLY, inv ->
                    inv.getMeta("senderTeam", "").equals(senderTeam)
                            && inv.getMeta("targetTeam", "").equals(targetTeam));

        } catch (TeamException e) {
            Player acceptor = Bukkit.getPlayer(invite.getTarget());
            if (acceptor != null) {
                mm.sendRaw(acceptor, "<red>Could not create alliance: " + e.getMessage() + "</red>");
            }
        }
    }

    @Override
    public void onDecline(Invite invite) {
        Player sender = Bukkit.getPlayer(invite.getSender());
        Player target = Bukkit.getPlayer(invite.getTarget());
        String senderTeamName = invite.getMeta("senderTeam", "unknown");
        String targetTeamName = invite.getMeta("targetTeam", "unknown");

        if (target != null) {
            mm.send(target, "teams.ally-declined-target", Map.of("team", senderTeamName));
        }
        if (sender != null) {
            mm.send(sender, "teams.ally-declined-sender", Map.of("team", targetTeamName));
        }

        inviteManager.cancelInvites(InviteType.TEAM_ALLY, inv ->
                inv.getMeta("senderTeam", "").equals(senderTeamName)
                        && inv.getMeta("targetTeam", "").equals(targetTeamName));
    }

    @Override
    public void onExpire(Invite invite) {
        Player sender = Bukkit.getPlayer(invite.getSender());
        Player target = Bukkit.getPlayer(invite.getTarget());
        String targetTeamName = invite.getMeta("targetTeam", "unknown");
        String senderTeamName = invite.getMeta("senderTeam", "unknown");

        if (sender != null) {
            mm.send(sender, "teams.ally-expired", Map.of("team", targetTeamName));
        }
        if (target != null) {
            mm.send(target, "teams.ally-expired", Map.of("team", senderTeamName));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

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
