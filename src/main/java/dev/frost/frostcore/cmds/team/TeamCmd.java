package dev.frost.frostcore.cmds.team;

import dev.frost.frostcore.utils.FrostLogger;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.exceptions.TeamException;
import dev.frost.frostcore.invites.Invite;
import dev.frost.frostcore.invites.InviteManager;
import dev.frost.frostcore.invites.InviteType;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.manager.TeamEchestManager;
import dev.frost.frostcore.manager.TeamManager;
import dev.frost.frostcore.teams.Team;
import dev.frost.frostcore.teams.TeamError;
import dev.frost.frostcore.utils.TeleportUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class TeamCmd implements CommandExecutor, TabCompleter {

    private final TeamManager manager = TeamManager.getInstance();
    private final MessageManager mm = MessageManager.get();
    private final TeleportUtil teleportUtil = Main.getTeleportUtil();
    private final InviteManager inviteManager = Main.getInviteManager();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private static final Map<String, String> STATE = Map.of(
            "true", "<#7ECFA0>enabled</#7ECFA0>",
            "false", "<#D4727A>disabled</#D4727A>"
    );

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.getComponent("teams.prefix").append(Component.text("Only players can use this command.")));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        if (!Main.getConfigManager().getBoolean("teams.enabled", true)) {
            mm.sendRaw(player, "<red>The teams system is currently disabled.</red>");
            return true;
        }

        try {
            switch (args[0].toLowerCase()) {

                case "help" -> sendHelp(player);

                case "create" -> {
                    if (args.length < 3) {
                        sendHelp(player);
                        return true;
                    }
                    String name = args[1];
                    String tag = args[2];
                    Team team = manager.createTeam(name, tag, player.getUniqueId());

                    Map<String, String> ph = Map.of("team", team.getDisplayName());
                    mm.send(player, "teams.create", ph);
                }

                case "disband" -> {
                    Team team = manager.getTeam(player.getUniqueId());
                    requireOwner(player, team);

                    String displayName = team.getDisplayName();
                    dev.frost.frostcore.gui.impls.TeamConfirmGui.openDisband(player, displayName, ctx -> {
                        ctx.close();
                        try {
                            manager.disbandTeam(team.getName());
                            mm.broadcast("teams.disband", Map.of("team", displayName));
                        } catch (Exception ex) {
                            FrostLogger.error("Error disbanding team", ex);
                        }
                    });
                }

                case "invite" -> handleInvite(player, args);

                case "join" -> handleJoin(player, args);

                case "accept" -> handleJoin(player, args);

                case "decline" -> handleDecline(player, args);

                case "leave" -> {
                    Team team = manager.getTeam(player.getUniqueId());

                    if (team.isOwner(player.getUniqueId()) && team.getOwners().size() == 1) {
                        throw new dev.frost.frostcore.exceptions.TeamException(
                                dev.frost.frostcore.teams.TeamError.CANNOT_LEAVE_AS_OWNER,
                                "Cannot leave as last owner");
                    }

                    String displayName = team.getDisplayName();
                    dev.frost.frostcore.gui.impls.TeamConfirmGui.openLeave(player, displayName, ctx -> {
                        ctx.close();
                        try {
                            Main.getEchestManager().forceCloseForPlayer(player);
                            manager.removeMember(player.getUniqueId());
                            mm.send(player, "teams.leave", Map.of("team", displayName));
                        } catch (dev.frost.frostcore.exceptions.TeamException ex) {
                            mm.send(player, getMessagePathForError(ex.getError()));
                        }
                    });
                }

                case "kick" -> handleKick(player, args);

                case "promote" -> handlePromote(player, args);

                case "demote" -> handleDemote(player, args);

                case "info" -> handleInfo(player, args);

                case "list" -> handleList(player, args);

                case "sethome" -> {
                    Team team = manager.getTeam(player.getUniqueId());
                    requireAdmin(player, team);
                    manager.setHome(team, player.getLocation());
                    mm.send(player, "teams.home-set");
                }

                case "delhome" -> {
                    Team team = manager.getTeam(player.getUniqueId());
                    requireAdmin(player, team);
                    manager.deleteHome(team);
                    mm.send(player, "teams.home-del");
                }

                case "home" -> handleHome(player);

                case "setwarp" -> {
                    if (args.length < 2) {
                        sendHelp(player);
                        return true;
                    }
                    Team team = manager.getTeam(player.getUniqueId());
                    requireAdmin(player, team);

                    String warpName = args[1];
                    manager.setWarp(team, warpName, player.getLocation());

                    mm.send(player, "teams.warp-set", Map.of("warp", warpName));
                }

                case "warp" -> handleWarp(player, args);

                case "delwarp" -> {
                    if (args.length < 2) {
                        sendHelp(player);
                        return true;
                    }
                    Team team = manager.getTeam(player.getUniqueId());
                    requireAdmin(player, team);

                    String warpName = args[1];
                    manager.deleteWarp(team, warpName);

                    mm.send(player, "teams.warp-del", Map.of("warp", warpName));
                }

                case "ally" -> handleAlly(player, args);

                case "unally" -> {
                    if (args.length < 2) {
                        sendHelp(player);
                        return true;
                    }
                    Team team = manager.getTeam(player.getUniqueId());
                    requireAdmin(player, team);

                    String targetTeam = args[1];
                    manager.removeAlly(team, targetTeam);
                    mm.send(player, "teams.unally", Map.of("team", targetTeam));
                }

                case "enemy" -> {
                    if (args.length < 2) {
                        sendHelp(player);
                        return true;
                    }
                    Team team = manager.getTeam(player.getUniqueId());
                    requireAdmin(player, team);

                    String targetTeam = args[1];
                    manager.addEnemy(team, targetTeam);
                    mm.send(player, "teams.enemy", Map.of("team", targetTeam));
                }

                case "unenemy" -> {
                    if (args.length < 2) {
                        sendHelp(player);
                        return true;
                    }
                    Team team = manager.getTeam(player.getUniqueId());
                    requireAdmin(player, team);

                    String targetTeam = args[1];
                    manager.removeEnemy(team, targetTeam);
                    mm.send(player, "teams.unenemy", Map.of("team", targetTeam));
                }

                case "settag" -> {
                    if (args.length < 2) {
                        sendHelp(player);
                        return true;
                    }
                    Team team = manager.getTeam(player.getUniqueId());
                    requireOwner(player, team);

                    String newTag = args[1];
                    int tagMin = Main.getConfigManager().getInt("teams.tag-min-length", 1);
                    int tagMax = Main.getConfigManager().getInt("teams.tag-max-length", 6);
                    if (newTag.length() < tagMin || newTag.length() > tagMax) {
                        mm.send(player, "teams.tag-invalid-length",
                                Map.of("min", String.valueOf(tagMin), "max", String.valueOf(tagMax)));
                        return true;
                    }

                    team.setTag(newTag);
                    manager.saveTag(team);
                    mm.send(player, "teams.settag-success");
                }

                case "pvp" -> {
                    Team team = manager.getTeam(player.getUniqueId());
                    requireAdmin(player, team);

                    boolean newState = !team.isPvpToggle();
                    team.setPvpToggle(newState);
                    manager.savePvpToggle(team);

                    Map<String, String> ph = Map.of("state", STATE.getOrDefault(String.valueOf(newState), "unknown"));
                    mm.send(player, "teams.pvp-toggle", ph);
                }

                case "chat" -> {
                    if (!Main.getConfigManager().getBoolean("teams.team-chat", true)) {
                        mm.sendRaw(player, "<red>Team chat is currently disabled.</red>");
                        return true;
                    }
                    Team team = manager.getTeam(player.getUniqueId());
                    team.toggleTeamChat(player.getUniqueId());
                    boolean enabled = team.isTeamChatEnabled(player.getUniqueId());
                    manager.saveChatToggle(team);
                    mm.send(player, enabled ? "teams.chat-enabled" : "teams.chat-disabled");
                }

                case "rename" -> {
                    if (args.length < 2) {
                        sendHelp(player);
                        return true;
                    }
                    Team team = manager.getTeam(player.getUniqueId());
                    requireOwner(player, team);

                    String oldName = team.getName();
                    String newName = args[1];
                    manager.renameTeam(team, newName);
                    mm.send(player, "teams.renamed", Map.of("old", oldName, "new", newName));
                }

                case "echest" -> {
                    if (!Main.getConfigManager().getBoolean("teams.echest.enabled", true)) {
                        mm.send(player, "teams.echest-disabled");
                        return true;
                    }
                    Team team = manager.getTeam(player.getUniqueId());
                    Main.getEchestManager().openEchest(player, team);
                }

                default -> sendHelp(player);
            }
        } catch (TeamException e) {
            String path = getMessagePathForError(e.getError());
            mm.send(player, path);
        } catch (Exception e) {
            mm.sendRaw(player, "<red>An unexpected error occurred.</red>");
            FrostLogger.error("An error occurred", e);
        }

        return true;
    }

    private void handleKick(Player player, String[] args) throws TeamException {
        if (args.length < 2) {
            sendHelp(player);
            return;
        }
        Team team = manager.getTeam(player.getUniqueId());
        requireAdmin(player, team);

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            mm.send(player, "teams.player-not-found");
            return;
        }
        if (!team.isMember(target.getUniqueId())) {
            mm.send(player, "teams.not-in-team");
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            mm.sendRaw(player, "<red>You cannot kick yourself. Use /team leave.</red>");
            return;
        }

        boolean targetIsOwner = team.isOwner(target.getUniqueId());
        boolean targetIsAdmin = team.isAdmin(target.getUniqueId());
        boolean kickerIsOwner = team.isOwner(player.getUniqueId());

        if (targetIsOwner) {

            mm.send(player, "teams.cannot-kick-higher");
            return;
        }
        if (targetIsAdmin && !kickerIsOwner) {

            mm.send(player, "teams.cannot-kick-higher");
            return;
        }

        Main.getEchestManager().forceCloseForPlayer(target);

        manager.removeMember(target.getUniqueId());

        mm.send(player, "teams.kick", Map.of("player", target.getName()));
        mm.send(target, "teams.kicked", Map.of("team", team.getName()));
    }

    private void handlePromote(Player player, String[] args) throws TeamException {
        if (args.length < 2) {
            sendHelp(player);
            return;
        }

        Team team = manager.getTeam(player.getUniqueId());
        requireAdmin(player, team);

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            mm.send(player, "teams.player-not-found");
            return;
        }
        if (!team.isMember(target.getUniqueId())) {
            mm.send(player, "teams.not-in-team");
            return;
        }

        String currentRole = manager.getRole(team, target.getUniqueId());

        switch (currentRole) {
            case "MEMBER" -> {
                manager.promoteToAdmin(team, target.getUniqueId());
                mm.send(player, "teams.promoted", Map.of("player", target.getName(), "role", "Admin"));
                mm.send(target, "teams.promoted-target", Map.of("role", "Admin"));
            }
            case "ADMIN" -> {
                requireOwner(player, team);
                manager.promoteToOwner(team, target.getUniqueId());
                mm.send(player, "teams.promoted", Map.of("player", target.getName(), "role", "Owner"));
                mm.send(target, "teams.promoted-target", Map.of("role", "Owner"));
            }
            case "OWNER" -> {
                throw new TeamException(TeamError.ALREADY_HIGHEST_RANK, "Already highest rank");
            }
        }
    }

    private void handleDemote(Player player, String[] args) throws TeamException {
        if (args.length < 2) {
            sendHelp(player);
            return;
        }

        Team team = manager.getTeam(player.getUniqueId());
        requireOwner(player, team);

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            mm.send(player, "teams.player-not-found");
            return;
        }
        if (!team.isMember(target.getUniqueId())) {
            mm.send(player, "teams.not-in-team");
            return;
        }

        String currentRole = manager.getRole(team, target.getUniqueId());

        switch (currentRole) {
            case "OWNER" -> {
                manager.demoteOwnerToAdmin(team, target.getUniqueId());
                mm.send(player, "teams.demoted", Map.of("player", target.getName(), "role", "Admin"));
                mm.send(target, "teams.demoted-target", Map.of("role", "Admin"));
            }
            case "ADMIN" -> {
                manager.demoteAdminToMember(team, target.getUniqueId());
                mm.send(player, "teams.demoted", Map.of("player", target.getName(), "role", "Member"));
                mm.send(target, "teams.demoted-target", Map.of("role", "Member"));
            }
            case "MEMBER" -> {
                throw new TeamException(TeamError.ALREADY_LOWEST_RANK, "Already lowest rank");
            }
        }
    }

    private void handleInfo(Player player, String[] args) {
        try {
            Team team;
            if (args.length >= 2) {
                team = manager.getTeam(args[1]);
            } else {
                team = manager.getTeam(player.getUniqueId());
            }

            String divider = "<gradient:#C8A87C:#A68B5B><strikethrough>                                          </strikethrough>";

            mm.sendRaw(player, "");
            mm.sendRaw(player, divider);
            mm.sendRaw(player, "  <gradient:#C8A87C:#A68B5B><bold>" + team.getDisplayName()
                    + "</bold> <dark_gray>[<white>" + team.getTag() + "<dark_gray>]");
            mm.sendRaw(player, "");

            mm.sendRaw(player, "  <#D4C4A8>⬥ Owners   <dark_gray>» <white>" + formatPlayerList(team.getOwners()));
            if (!team.getAdmins().isEmpty()) {
                mm.sendRaw(player, "  <#D4C4A8>⬥ Admins   <dark_gray>» <white>" + formatPlayerList(team.getAdmins()));
            }
            if (!team.getMembers().isEmpty()) {
                mm.sendRaw(player, "  <#D4C4A8>⬥ Members  <dark_gray>» <white>" + formatPlayerList(team.getMembers()));
            }
            mm.sendRaw(player, "");

            String pvpState = team.isPvpToggle() ? "<#7ECFA0>ON" : "<#D4727A>OFF";
            String homeState = team.getHome() != null ? "<#7ECFA0>Set" : "<#D4727A>Not set";
            mm.sendRaw(player, "  <#D4C4A8>Players <dark_gray>» <white>" + team.getTotalMembers()
                    + "    <#D4C4A8>PvP <dark_gray>» " + pvpState
                    + "    <#D4C4A8>Home <dark_gray>» " + homeState);

            if (!team.getAllies().isEmpty()) {
                mm.sendRaw(player, "  <#D4C4A8>Allies  <dark_gray>» <#7ECFA0>" + String.join("<dark_gray>, <#7ECFA0>", team.getAllies()));
            }
            if (!team.getEnemies().isEmpty()) {
                mm.sendRaw(player, "  <#D4C4A8>Enemies <dark_gray>» <#D4727A>" + String.join("<dark_gray>, <#D4727A>", team.getEnemies()));
            }

            if (!team.getWarps().isEmpty()) {
                mm.sendRaw(player, "  <#D4C4A8>Warps   <dark_gray>» <white>" + String.join("<dark_gray>, <white>", team.getWarps().keySet())
                        + " <dark_gray>(" + team.getWarps().size() + ")");
            }

            mm.sendRaw(player, divider);
            mm.sendRaw(player, "");

        } catch (TeamException e) {
            mm.send(player, getMessagePathForError(e.getError()));
        }
    }

    private void handleList(Player player, String[] args) {
        List<Team> allTeams = new ArrayList<>(manager.getAllTeams());

        if (allTeams.isEmpty()) {
            mm.sendRaw(player, "<!italic><#D4C4A8>There are no teams on this server.");
            return;
        }

        dev.frost.frostcore.gui.impls.TeamListGui gui =
                new dev.frost.frostcore.gui.impls.TeamListGui(player);
        gui.open(player);
    }

    /**
     * Format a set of UUIDs into a comma-separated player name list with online/offline coloring.
     */
    private String formatPlayerList(Set<UUID> uuids) {
        return uuids.stream()
                .map(uuid -> {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                    String name = op.getName() != null ? op.getName() : uuid.toString().substring(0, 8);
                    return op.isOnline() ? "<#7ECFA0>" + name : "<gray>" + name;
                })
                .collect(Collectors.joining("<dark_gray>, "));
    }

    private void handleJoin(Player player, String[] args) {
        UUID targetUUID = player.getUniqueId();
        Invite invite   = null;

        if (args.length >= 2) {
            String input = args[1];

            Player from = Bukkit.getPlayerExact(input);
            if (from != null) {
                List<Invite> byPlayer = inviteManager.getInvites(targetUUID, InviteType.TEAM_JOIN);
                invite = byPlayer.stream()
                        .filter(inv -> inv.getSender().equals(from.getUniqueId()))
                        .findFirst().orElse(null);
            }

            if (invite == null) {
                invite = findInviteByTeamName(targetUUID, InviteType.TEAM_JOIN, "team", input);
            }
        } else {

            List<Invite> pending = inviteManager.getInvites(targetUUID, InviteType.TEAM_JOIN);
            if (!pending.isEmpty()) invite = pending.get(0);
        }

        if (invite == null) {
            mm.send(player, "teams.no-pending-invite");
            return;
        }

        String teamName = capitalize(invite.getMeta("team", "unknown"));
        String senderName = Bukkit.getOfflinePlayer(invite.getSender()).getName();
        if (senderName == null) senderName = "Unknown";

        final Invite finalInvite = invite;
        final String finalSender = senderName;

        dev.frost.frostcore.gui.impls.TeamConfirmGui.openJoin(player, teamName, finalSender, ctx -> {
            ctx.close();
            boolean accepted = inviteManager.acceptInvite(targetUUID, InviteType.TEAM_JOIN,
                    finalInvite.getSender());
            if (!accepted) {
                mm.send(player, "teams.no-pending-invite");
            }
        });
    }

    private void handleInvite(Player player, String[] args) throws TeamException {
        if (args.length < 2) {
            sendHelp(player);
            return;
        }

        Team team = manager.getTeam(player.getUniqueId());
        requireAdmin(player, team);

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            mm.send(player, "teams.player-not-found");
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            mm.sendRaw(player, "<red>You cannot invite yourself.</red>");
            return;
        }

        if (manager.hasTeam(target.getUniqueId())) {
            mm.send(player, "teams.already-in-team");
            return;
        }

        List<Invite> existing = inviteManager.getInvites(target.getUniqueId(), InviteType.TEAM_JOIN);
        boolean alreadyInvited = existing.stream()
                .anyMatch(inv -> inv.getMeta("team", "").equalsIgnoreCase(team.getName()));
        if (alreadyInvited) {
            mm.sendRaw(player, "<red>That player already has a pending invite from your team.</red>");
            return;
        }

        int expiry = Main.getConfigManager().getInt("teams.invites.team-join-expiry", 60);

        inviteManager.sendInvite(
                InviteType.TEAM_JOIN,
                player.getUniqueId(),
                target.getUniqueId(),
                Map.of("team", team.getName()),
                expiry
        );

        mm.send(player, "teams.invite", Map.of("player", target.getName(), "team", team.getName()));

        mm.send(target, "teams.invite-received", Map.of("player", player.getName(), "team", team.getName()));
        sendClickableInviteActions(target, player.getName());
    }

    private void handleAccept(Player player, String[] args) {
        UUID targetUUID = player.getUniqueId();

        if (args.length >= 2) {
            String input = args[1];

            Player fromPlayer = Bukkit.getPlayerExact(input);
            if (fromPlayer != null) {
                if (inviteManager.acceptInvite(targetUUID, InviteType.TEAM_JOIN, fromPlayer.getUniqueId())) return;
                if (inviteManager.acceptInvite(targetUUID, InviteType.TEAM_ALLY, fromPlayer.getUniqueId())) return;
            }

            Invite teamJoinMatch = findInviteByTeamName(targetUUID, InviteType.TEAM_JOIN, "team", input);
            if (teamJoinMatch != null) {
                if (inviteManager.acceptInvite(targetUUID, InviteType.TEAM_JOIN, teamJoinMatch.getSender())) return;
            }

            Invite allyMatch = findInviteByTeamName(targetUUID, InviteType.TEAM_ALLY, "senderTeam", input);
            if (allyMatch != null) {
                if (inviteManager.acceptInvite(targetUUID, InviteType.TEAM_ALLY, allyMatch.getSender())) return;
            }

            mm.send(player, "teams.no-pending-invite");
            return;
        }

        if (inviteManager.acceptInvite(targetUUID, InviteType.TEAM_JOIN, null)) return;
        if (inviteManager.acceptInvite(targetUUID, InviteType.TEAM_ALLY, null)) return;

        mm.send(player, "teams.no-pending-invite");
    }

    private void handleDecline(Player player, String[] args) {
        UUID targetUUID = player.getUniqueId();

        if (args.length >= 2) {
            String input = args[1];

            Player fromPlayer = Bukkit.getPlayerExact(input);
            if (fromPlayer != null) {
                if (inviteManager.declineInvite(targetUUID, InviteType.TEAM_JOIN, fromPlayer.getUniqueId())) return;
                if (inviteManager.declineInvite(targetUUID, InviteType.TEAM_ALLY, fromPlayer.getUniqueId())) return;
            }

            Invite teamJoinMatch = findInviteByTeamName(targetUUID, InviteType.TEAM_JOIN, "team", input);
            if (teamJoinMatch != null) {
                if (inviteManager.declineInvite(targetUUID, InviteType.TEAM_JOIN, teamJoinMatch.getSender())) return;
            }

            Invite allyMatch = findInviteByTeamName(targetUUID, InviteType.TEAM_ALLY, "senderTeam", input);
            if (allyMatch != null) {
                if (inviteManager.declineInvite(targetUUID, InviteType.TEAM_ALLY, allyMatch.getSender())) return;
            }

            mm.send(player, "teams.no-pending-invite");
            return;
        }

        if (inviteManager.declineInvite(targetUUID, InviteType.TEAM_JOIN, null)) return;
        if (inviteManager.declineInvite(targetUUID, InviteType.TEAM_ALLY, null)) return;

        mm.send(player, "teams.no-pending-invite");
    }

    private Invite findInviteByTeamName(UUID target, InviteType type, String metaKey, String teamName) {
        List<Invite> invites = inviteManager.getInvites(target, type);
        for (Invite inv : invites) {
            String meta = inv.getMeta(metaKey);
            if (meta != null && meta.equalsIgnoreCase(teamName)) return inv;
        }
        return null;
    }

    private void handleAlly(Player player, String[] args) throws TeamException {
        if (args.length < 2) {
            sendHelp(player);
            return;
        }

        Team team = manager.getTeam(player.getUniqueId());
        requireAdmin(player, team);

        String targetTeamName = args[1].toLowerCase();

        Team targetTeam = manager.getTeam(targetTeamName);

        if (team.getName().equalsIgnoreCase(targetTeamName)) {
            throw new TeamException(TeamError.CANNOT_TARGET_SELF, "Cannot ally yourself");
        }

        if (team.isAlly(targetTeamName)) {
            throw new TeamException(TeamError.ALREADY_ALLY, "Already allied");
        }

        if (!Main.getConfigManager().getBoolean("teams.relations.allies-enabled")) {
            throw new TeamException(TeamError.ALLIES_DISABLED, "Allies are disabled");
        }

        int maxAllies = Main.getConfigManager().getInt("teams.relations.max-allies");
        if (team.getAllies().size() >= maxAllies) {
            throw new TeamException(TeamError.MAX_ALLIES_REACHED, "Max allies reached");
        }

        int expiry = Main.getConfigManager().getInt("teams.invites.team-ally-expiry", 120);

        boolean sentToAny = false;
        Set<UUID> recipients = new HashSet<>();
        recipients.addAll(targetTeam.getOwners());
        recipients.addAll(targetTeam.getAdmins());

        for (UUID uuid : recipients) {
            Player targetPlayer = Bukkit.getPlayer(uuid);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                inviteManager.sendInvite(
                        InviteType.TEAM_ALLY,
                        player.getUniqueId(),
                        uuid,
                        Map.of("senderTeam", team.getName(), "targetTeam", targetTeam.getName()),
                        expiry
                );

                mm.send(targetPlayer, "teams.ally-request-received", Map.of("team", team.getName()));
                sendClickableAllyActions(targetPlayer, player.getName());
                sentToAny = true;
            }
        }

        if (!sentToAny) {
            mm.sendRaw(player, "<red>No owners or admins of that team are online to receive the request.</red>");
            return;
        }

        mm.send(player, "teams.ally-request-sent", Map.of("team", targetTeam.getName()));
    }

    private void sendClickableInviteActions(Player target, String senderName) {
        Component accept = miniMessage.deserialize("<#7ECFA0><bold>[ACCEPT]</bold></#7ECFA0>")
                .clickEvent(ClickEvent.runCommand("/team accept " + senderName))
                .hoverEvent(HoverEvent.showText(miniMessage.deserialize("<#7ECFA0>Click to accept the invite")));

        Component decline = miniMessage.deserialize("<#D4727A><bold>[DECLINE]</bold></#D4727A>")
                .clickEvent(ClickEvent.runCommand("/team decline " + senderName))
                .hoverEvent(HoverEvent.showText(miniMessage.deserialize("<#D4727A>Click to decline the invite")));

        target.sendMessage(Component.text("   ").append(accept).append(Component.text("  ")).append(decline));
    }

    private void sendClickableAllyActions(Player target, String senderName) {
        Component accept = miniMessage.deserialize("<#7ECFA0><bold>[ACCEPT]</bold></#7ECFA0>")
                .clickEvent(ClickEvent.runCommand("/team accept " + senderName))
                .hoverEvent(HoverEvent.showText(miniMessage.deserialize("<#7ECFA0>Click to accept the alliance")));

        Component decline = miniMessage.deserialize("<#D4727A><bold>[DECLINE]</bold></#D4727A>")
                .clickEvent(ClickEvent.runCommand("/team decline " + senderName))
                .hoverEvent(HoverEvent.showText(miniMessage.deserialize("<#D4727A>Click to decline the alliance")));

        target.sendMessage(Component.text("   ").append(accept).append(Component.text("  ")).append(decline));
    }

    private void handleHome(Player player) {
        try {
            Team team = manager.getTeam(player.getUniqueId());
            Location home = team.getHome();

            if (home == null) {
                mm.send(player, "teams.home-not-set");
                return;
            }

            teleportUtil.teleportWithCooldownAndDelay(
                    player, home,
                    "team.home",
                    "teams.home.cooldown",
                    "teams.home-cooldown",
                    Main.getConfigManager().getInt("teams.home.delay"),
                    "teams.home-wait",
                    "teams.home-teleport",
                    "teams.home-teleport-cancelled"
            );
        } catch (TeamException e) {
            mm.send(player, getMessagePathForError(e.getError()));
        }
    }

    private void handleWarp(Player player, String[] args) {
        if (args.length < 2) {
            sendHelp(player);
            return;
        }

        try {
            Team team = manager.getTeam(player.getUniqueId());
            String warpName = args[1].toLowerCase();
            Location loc = team.getWarp(warpName);

            if (loc == null) {
                mm.send(player, "teams.warp-not-found");
                return;
            }

            mm.send(player, "teams.warp", Map.of("warp", warpName));

            teleportUtil.teleportWithCooldownAndDelay(
                    player, loc,
                    "team.warp",
                    "teams.warps.cooldown",
                    "teams.warp-cooldown",
                    Main.getConfigManager().getInt("teams.warps.delay"),
                    "teams.warp-wait",
                    "teams.warp-teleport",
                    "teams.warp-teleport-cancelled",
                    Map.of("warp", warpName)
            );
        } catch (TeamException e) {
            mm.send(player, getMessagePathForError(e.getError()));
        }
    }

    private String getMessagePathForError(TeamError error) {
        return switch (error) {
            case TEAM_ALREADY_EXISTS -> "teams.team-already-exists";
            case TEAM_NOT_FOUND -> "teams.team-not-found";
            case PLAYER_ALREADY_IN_TEAM -> "teams.already-in-team";
            case PLAYER_NOT_IN_TEAM -> "teams.no-team";
            case TEAM_NAME_TOO_SHORT -> "teams.name-too-short";
            case TEAM_NAME_TOO_LONG -> "teams.name-too-long";
            case TEAM_NAME_BANNED -> "teams.name-banned";
            case TEAM_FULL -> "teams.team-full";
            case MAX_ADMINS_REACHED -> "teams.max-admins-reached";
            case MAX_OWNERS_REACHED -> "teams.max-owners-reached";
            case MAX_WARPS_REACHED -> "teams.max-warps-reached";
            case WARPS_DISABLED -> "teams.warps-disabled";
            case WARP_ALREADY_EXISTS -> "teams.warp-already-exists";
            case WARP_DOES_NOT_EXIST -> "teams.warp-not-found";
            case ALLIES_DISABLED -> "teams.allies-disabled";
            case ENEMIES_DISABLED -> "teams.enemies-disabled";
            case MAX_ALLIES_REACHED -> "teams.max-allies-reached";
            case MAX_ENEMIES_REACHED -> "teams.max-enemies-reached";
            case ALREADY_ALLY -> "teams.already-ally";
            case ALREADY_ENEMY -> "teams.already-enemy";
            case NOT_ALLY -> "teams.not-ally";
            case NOT_ENEMY -> "teams.not-enemy";
            case CANNOT_TARGET_SELF -> "teams.cannot-self";
            case HOME_DISABLED -> "teams.home-disabled";
            case ONLY_OWNER -> "teams.only-owner";
            case ONLY_ADMIN -> "teams.only-admin";
            case CANNOT_LEAVE_AS_OWNER -> "teams.cannot-leave-as-owner";
            case CANNOT_KICK_HIGHER_ROLE -> "teams.cannot-kick-higher";
            case ALREADY_HIGHEST_RANK -> "teams.already-highest-rank";
            case ALREADY_LOWEST_RANK -> "teams.already-lowest-rank";
            case CANNOT_DEMOTE_LAST_OWNER -> "teams.cannot-demote-last-owner";
            default -> "teams.no-team";
        };
    }

    private void requireOwner(Player player, Team team) throws TeamException {
        if (!team.isOwner(player.getUniqueId())) {
            throw new TeamException(TeamError.ONLY_OWNER, "Only the owner can do this");
        }
    }

    private void requireAdmin(Player player, Team team) throws TeamException {
        if (!team.isOwner(player.getUniqueId()) && !team.isAdmin(player.getUniqueId())) {
            throw new TeamException(TeamError.ONLY_ADMIN, "Only the owner or admin can do this");
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private void sendHelp(Player player) {
        String div = "<gradient:#C8A87C:#A68B5B><strikethrough>                                                            </strikethrough>";
        mm.sendRaw(player, "");
        mm.sendRaw(player, div);
        mm.sendRaw(player, "  <gradient:#C8A87C:#A68B5B><bold>Team Commands</bold>");
        mm.sendRaw(player, "");
        mm.sendRaw(player, "  <#D4C4A8>/team create <dark_gray><name> <tag>   <#D4C4A8>/team disband");
        mm.sendRaw(player, "  <#D4C4A8>/team info <dark_gray>[team]          <#D4C4A8>/team list <dark_gray>[page]");
        mm.sendRaw(player, "  <#D4C4A8>/team invite <dark_gray><player>      <#D4C4A8>/team kick <dark_gray><player>");
        mm.sendRaw(player, "  <#D4C4A8>/team join <dark_gray>/ <#D4C4A8>decline");
        mm.sendRaw(player, "  <#D4C4A8>/team promote <dark_gray><player>     <#D4C4A8>/team demote <dark_gray><player>");
        mm.sendRaw(player, "  <#D4C4A8>/team home <dark_gray>/ <#D4C4A8>sethome <dark_gray>/ <#D4C4A8>delhome");
        mm.sendRaw(player, "  <#D4C4A8>/team warp <dark_gray>/ <#D4C4A8>setwarp <dark_gray>/ <#D4C4A8>delwarp <dark_gray><name>");
        mm.sendRaw(player, "  <#D4C4A8>/team ally <dark_gray><team>          <#D4C4A8>/team unally <dark_gray><team>");
        mm.sendRaw(player, "  <#D4C4A8>/team enemy <dark_gray><team>         <#D4C4A8>/team unenemy <dark_gray><team>");
        mm.sendRaw(player, "  <#D4C4A8>/team rename <dark_gray><name>        <#D4C4A8>/team settag <dark_gray><tag>");
        mm.sendRaw(player, "  <#D4C4A8>/team pvp <dark_gray>/ <#D4C4A8>chat <dark_gray>/ <#D4C4A8>echest <dark_gray>/ <#D4C4A8>leave");
        mm.sendRaw(player, div);
        mm.sendRaw(player, "");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("help", "create", "info", "list", "invite", "join", "accept", "decline",
                    "promote", "demote", "kick", "leave", "disband",
                    "home", "sethome", "delhome", "warp", "setwarp", "delwarp",
                    "ally", "unally", "enemy", "unenemy",
                    "pvp", "settag", "chat", "echest", "rename");
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();

            if (sub.equals("invite") || sub.equals("kick") || sub.equals("promote") || sub.equals("demote")) {
                if (sender instanceof Player p) {
                    try {
                        Team team = manager.getTeam(p.getUniqueId());
                        Set<UUID> all = new HashSet<>();
                        all.addAll(team.getOwners());
                        all.addAll(team.getAdmins());
                        all.addAll(team.getMembers());
                        return all.stream()
                                .map(Bukkit::getPlayer)
                                .filter(Objects::nonNull)
                                .map(Player::getName)
                                .filter(name -> !name.equals(p.getName()))
                                .toList();
                    } catch (Exception e) {
                        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                    }
                }
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            }

            if (sub.equals("join") || sub.equals("accept") || sub.equals("decline")) {
                if (sender instanceof Player p) {
                    List<Invite> pending = inviteManager.getAllInvites(p.getUniqueId());
                    Set<String> suggestions = new LinkedHashSet<>();
                    for (Invite inv : pending) {
                        Player s = Bukkit.getPlayer(inv.getSender());
                        if (s != null) suggestions.add(s.getName());
                        String teamMeta = inv.getMeta("team");
                        if (teamMeta != null) suggestions.add(teamMeta);
                        String senderTeamMeta = inv.getMeta("senderTeam");
                        if (senderTeamMeta != null) suggestions.add(senderTeamMeta);
                    }
                    return new ArrayList<>(suggestions);
                }
            }

            if (sub.equals("warp") || sub.equals("delwarp")) {
                if (sender instanceof Player p) {
                    try {
                        Team team = manager.getTeam(p.getUniqueId());
                        return new ArrayList<>(team.getWarps().keySet());
                    } catch (Exception ignored) {}
                }
            }

            if (sub.equals("ally") || sub.equals("enemy")) {
                return manager.getAllTeams().stream().map(Team::getName).toList();
            }

            if (sub.equals("unally")) {
                if (sender instanceof Player p) {
                    try {
                        Team team = manager.getTeam(p.getUniqueId());
                        return new ArrayList<>(team.getAllies());
                    } catch (Exception ignored) {}
                }
            }

            if (sub.equals("unenemy")) {
                if (sender instanceof Player p) {
                    try {
                        Team team = manager.getTeam(p.getUniqueId());
                        return new ArrayList<>(team.getEnemies());
                    } catch (Exception ignored) {}
                }
            }

            if (sub.equals("info")) {
                return manager.getAllTeams().stream().map(Team::getName).toList();
            }

            if (sub.equals("chat")) {
                return List.of("on", "off");
            }
        }
        return Collections.emptyList();
    }
}

