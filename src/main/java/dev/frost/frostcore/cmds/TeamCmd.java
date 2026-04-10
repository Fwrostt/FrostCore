package dev.frost.frostcore.cmds;

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
            "true", "<green>enabled</green>",
            "false", "<red>disabled</red>"
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

        // Global teams system toggle
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

                    Map<String, String> ph = Map.of("team", team.getName());
                    mm.send(player, "teams.create", ph);
                }

                case "disband" -> {
                    Team team = manager.getTeam(player.getUniqueId());
                    requireOwner(player, team);

                    String teamName = team.getName();
                    manager.disbandTeam(teamName);

                    Map<String, String> ph = Map.of("team", teamName);
                    mm.broadcast("teams.disband", ph);
                }

                case "invite" -> handleInvite(player, args);

                case "accept" -> handleAccept(player, args);

                case "decline" -> handleDecline(player, args);

                case "leave" -> {
                    Team team = manager.getTeam(player.getUniqueId());

                    // Force-close echest if open (anti-dupe)
                    Main.getEchestManager().forceCloseForPlayer(player);

                    // removeMember throws CANNOT_LEAVE_AS_OWNER if last owner
                    String teamName = team.getName();
                    manager.removeMember(player.getUniqueId());

                    mm.send(player, "teams.leave", Map.of("team", teamName));
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
            e.printStackTrace();
        }

        return true;
    }

    // ==================== KICK ====================

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

        // Role hierarchy: owners can kick admins+members, admins can only kick members
        boolean targetIsOwner = team.isOwner(target.getUniqueId());
        boolean targetIsAdmin = team.isAdmin(target.getUniqueId());
        boolean kickerIsOwner = team.isOwner(player.getUniqueId());

        if (targetIsOwner) {
            // Nobody can kick an owner
            mm.send(player, "teams.cannot-kick-higher");
            return;
        }
        if (targetIsAdmin && !kickerIsOwner) {
            // Only owners can kick admins
            mm.send(player, "teams.cannot-kick-higher");
            return;
        }

        // Force-close echest if target is viewing it (anti-dupe)
        Main.getEchestManager().forceCloseForPlayer(target);

        manager.removeMember(target.getUniqueId());

        mm.send(player, "teams.kick", Map.of("player", target.getName()));
        mm.send(target, "teams.kicked", Map.of("team", team.getName()));
    }

    // ==================== PROMOTE / DEMOTE ====================

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
                // Member → Admin (admin or owner can do this)
                manager.promoteToAdmin(team, target.getUniqueId());
                mm.send(player, "teams.promoted", Map.of("player", target.getName(), "role", "Admin"));
                mm.send(target, "teams.promoted-target", Map.of("role", "Admin"));
            }
            case "ADMIN" -> {
                // Admin → Owner (only owner can do this)
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
        requireOwner(player, team); // Only owners can demote

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
                // Owner → Admin (can't demote self if last owner)
                manager.demoteOwnerToAdmin(team, target.getUniqueId());
                mm.send(player, "teams.demoted", Map.of("player", target.getName(), "role", "Admin"));
                mm.send(target, "teams.demoted-target", Map.of("role", "Admin"));
            }
            case "ADMIN" -> {
                // Admin → Member
                manager.demoteAdminToMember(team, target.getUniqueId());
                mm.send(player, "teams.demoted", Map.of("player", target.getName(), "role", "Member"));
                mm.send(target, "teams.demoted-target", Map.of("role", "Member"));
            }
            case "MEMBER" -> {
                throw new TeamException(TeamError.ALREADY_LOWEST_RANK, "Already lowest rank");
            }
        }
    }

    // ==================== INFO / LIST ====================

    private void handleInfo(Player player, String[] args) {
        try {
            Team team;
            if (args.length >= 2) {
                team = manager.getTeam(args[1]);
            } else {
                team = manager.getTeam(player.getUniqueId());
            }

            mm.sendRaw(player, "");
            mm.sendRaw(player, "<gradient:#FFD700:#FFA500><bold>━━━ " + team.getName()
                    + " <dark_gray>[<white>" + team.getTag() + "<dark_gray>] <gradient:#FFD700:#FFA500>━━━</bold></gradient>");

            // Owners
            String owners = formatPlayerList(team.getOwners());
            mm.sendRaw(player, "<#FFD27F>Owners: <white>" + owners);

            // Admins
            if (!team.getAdmins().isEmpty()) {
                String admins = formatPlayerList(team.getAdmins());
                mm.sendRaw(player, "<#FFD27F>Admins: <white>" + admins);
            }

            // Members
            if (!team.getMembers().isEmpty()) {
                String members = formatPlayerList(team.getMembers());
                mm.sendRaw(player, "<#FFD27F>Members: <white>" + members);
            }

            mm.sendRaw(player, "<#FFD27F>Total: <white>" + team.getTotalMembers() + " players");

            // Relations
            if (!team.getAllies().isEmpty()) {
                mm.sendRaw(player, "<#FFD27F>Allies: <#55FF55>" + String.join("<dark_gray>, <#55FF55>", team.getAllies()));
            }
            if (!team.getEnemies().isEmpty()) {
                mm.sendRaw(player, "<#FFD27F>Enemies: <#FF5555>" + String.join("<dark_gray>, <#FF5555>", team.getEnemies()));
            }

            // Status
            String pvpState = team.isPvpToggle() ? "<green>enabled" : "<red>disabled";
            mm.sendRaw(player, "<#FFD27F>PvP: " + pvpState);
            mm.sendRaw(player, "<#FFD27F>Home: " + (team.getHome() != null ? "<green>set" : "<red>not set"));

            // Warps
            if (!team.getWarps().isEmpty()) {
                String warpList = String.join("<dark_gray>, <white>", team.getWarps().keySet());
                mm.sendRaw(player, "<#FFD27F>Warps: <white>" + warpList + " <dark_gray>(" + team.getWarps().size() + ")");
            }

            mm.sendRaw(player, "");

        } catch (TeamException e) {
            mm.send(player, getMessagePathForError(e.getError()));
        }
    }

    private void handleList(Player player, String[] args) {
        List<Team> allTeams = new ArrayList<>(manager.getAllTeams());
        int teamsPerPage = Main.getConfigManager().getInt("teams.list.teams-per-page", 10);

        if (allTeams.isEmpty()) {
            mm.sendRaw(player, "<#FFD27F>There are no teams on this server.");
            return;
        }

        int totalPages = (int) Math.ceil((double) allTeams.size() / teamsPerPage);
        int page = 1;

        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
                page = 1;
            }
        }

        page = Math.max(1, Math.min(page, totalPages));

        int startIndex = (page - 1) * teamsPerPage;
        int endIndex = Math.min(startIndex + teamsPerPage, allTeams.size());

        mm.sendRaw(player, "");
        mm.sendRaw(player, "<gradient:#FFD700:#FFA500><bold>━━━ Team List ━━━</bold></gradient>"
                + " <dark_gray>(" + page + "/" + totalPages + ")");

        for (int i = startIndex; i < endIndex; i++) {
            Team team = allTeams.get(i);
            String line = "<#FFD27F>" + team.getName()
                    + " <dark_gray>[<white>" + team.getTag() + "<dark_gray>]"
                    + " <dark_gray>- <white>" + team.getTotalMembers() + " members";
            mm.sendRaw(player, line);
        }

        // Build clickable navigation bar
        Component nav = Component.empty();

        if (page > 1) {
            nav = nav.append(miniMessage.deserialize("<#FFD27F><bold>[← PREV]</bold></#FFD27F>")
                    .clickEvent(ClickEvent.runCommand("/team list " + (page - 1)))
                    .hoverEvent(HoverEvent.showText(miniMessage.deserialize("<#FFD27F>Go to page " + (page - 1) + "</"))));
        } else {
            nav = nav.append(miniMessage.deserialize("<dark_gray><bold>[← PREV]</bold></dark_gray>"));
        }

        nav = nav.append(miniMessage.deserialize(" <dark_gray>" + page + "/" + totalPages + " "));

        if (page < totalPages) {
            nav = nav.append(miniMessage.deserialize("<#FFD27F><bold>[NEXT →]</bold></#FFD27F>")
                    .clickEvent(ClickEvent.runCommand("/team list " + (page + 1)))
                    .hoverEvent(HoverEvent.showText(miniMessage.deserialize("<#FFD27F>Go to page " + (page + 1) + "</"))));
        } else {
            nav = nav.append(miniMessage.deserialize("<dark_gray><bold>[NEXT →]</bold></dark_gray>"));
        }

        player.sendMessage(nav);
        mm.sendRaw(player, "<dark_gray>Total: <white>" + allTeams.size() + " teams");
        mm.sendRaw(player, "");
    }

    /**
     * Format a set of UUIDs into a comma-separated player name list with online/offline coloring.
     */
    private String formatPlayerList(Set<UUID> uuids) {
        return uuids.stream()
                .map(uuid -> {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                    String name = op.getName() != null ? op.getName() : uuid.toString().substring(0, 8);
                    return op.isOnline() ? "<green>" + name : "<gray>" + name;
                })
                .collect(Collectors.joining("<dark_gray>, "));
    }

    // ==================== INVITE HANDLERS ====================

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

        // Check if there's already a pending invite from this team to this player
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

        // Notify sender
        mm.send(player, "teams.invite", Map.of("player", target.getName(), "team", team.getName()));

        // Notify target with clickable accept/decline
        mm.send(target, "teams.invite-received", Map.of("player", player.getName(), "team", team.getName()));
        sendClickableInviteActions(target, player.getName());
    }

    private void handleAccept(Player player, String[] args) {
        UUID targetUUID = player.getUniqueId();

        if (args.length >= 2) {
            String input = args[1];

            // 1. Try matching an online player name first
            Player fromPlayer = Bukkit.getPlayerExact(input);
            if (fromPlayer != null) {
                if (inviteManager.acceptInvite(targetUUID, InviteType.TEAM_JOIN, fromPlayer.getUniqueId())) return;
                if (inviteManager.acceptInvite(targetUUID, InviteType.TEAM_ALLY, fromPlayer.getUniqueId())) return;
            }

            // 2. Try matching a team name in pending invites
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

        // No args: accept the most recent invite of any type
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

    // ==================== ALLY ====================

    private void handleAlly(Player player, String[] args) throws TeamException {
        if (args.length < 2) {
            sendHelp(player);
            return;
        }

        Team team = manager.getTeam(player.getUniqueId());
        requireAdmin(player, team);

        String targetTeamName = args[1].toLowerCase();

        // Validate the target team exists
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

        // Send invite to all online owners & admins of target team
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

    // ==================== CLICKABLE MESSAGES ====================

    private void sendClickableInviteActions(Player target, String senderName) {
        Component accept = miniMessage.deserialize("<green><bold>[ACCEPT]</bold></green>")
                .clickEvent(ClickEvent.runCommand("/team accept " + senderName))
                .hoverEvent(HoverEvent.showText(miniMessage.deserialize("<green>Click to accept the invite</green>")));

        Component decline = miniMessage.deserialize("<red><bold>[DECLINE]</bold></red>")
                .clickEvent(ClickEvent.runCommand("/team decline " + senderName))
                .hoverEvent(HoverEvent.showText(miniMessage.deserialize("<red>Click to decline the invite</red>")));

        target.sendMessage(Component.text("   ").append(accept).append(Component.text("  ")).append(decline));
    }

    private void sendClickableAllyActions(Player target, String senderName) {
        Component accept = miniMessage.deserialize("<green><bold>[ACCEPT]</bold></green>")
                .clickEvent(ClickEvent.runCommand("/team accept " + senderName))
                .hoverEvent(HoverEvent.showText(miniMessage.deserialize("<green>Click to accept the alliance</green>")));

        Component decline = miniMessage.deserialize("<red><bold>[DECLINE]</bold></red>")
                .clickEvent(ClickEvent.runCommand("/team decline " + senderName))
                .hoverEvent(HoverEvent.showText(miniMessage.deserialize("<red>Click to decline the alliance</red>")));

        target.sendMessage(Component.text("   ").append(accept).append(Component.text("  ")).append(decline));
    }

    // ==================== TELEPORT HANDLERS ====================

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
                    "teams.warp-teleport-cancelled"
            );
        } catch (TeamException e) {
            mm.send(player, getMessagePathForError(e.getError()));
        }
    }

    // ==================== HELPER METHODS ====================

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

    private void sendHelp(Player player) {
        mm.sendRaw(player, "<gradient:#FFD700:#FFA500><bold>Team Commands:</bold></gradient>");
        mm.sendRaw(player, "<#FFD27F>/team create <name> <tag>");
        mm.sendRaw(player, "<#FFD27F>/team info <dark_gray>[team]");
        mm.sendRaw(player, "<#FFD27F>/team list");
        mm.sendRaw(player, "<#FFD27F>/team invite <player>");
        mm.sendRaw(player, "<#FFD27F>/team accept <dark_gray>[team/player]");
        mm.sendRaw(player, "<#FFD27F>/team decline <dark_gray>[team/player]");
        mm.sendRaw(player, "<#FFD27F>/team promote <player> <dark_gray>| <#FFD27F>demote <player>");
        mm.sendRaw(player, "<#FFD27F>/team kick <player>");
        mm.sendRaw(player, "<#FFD27F>/team leave <dark_gray>| <#FFD27F>disband");
        mm.sendRaw(player, "<#FFD27F>/team home <dark_gray>| <#FFD27F>sethome <dark_gray>| <#FFD27F>delhome");
        mm.sendRaw(player, "<#FFD27F>/team warp <dark_gray>| <#FFD27F>setwarp <dark_gray>| <#FFD27F>delwarp <name>");
        mm.sendRaw(player, "<#FFD27F>/team ally <team> <dark_gray>| <#FFD27F>unally <team>");
        mm.sendRaw(player, "<#FFD27F>/team enemy <team> <dark_gray>| <#FFD27F>unenemy <team>");
        mm.sendRaw(player, "<#FFD27F>/team pvp <dark_gray>| <#FFD27F>settag <tag>");
        mm.sendRaw(player, "<#FFD27F>/team rename <name>");
        mm.sendRaw(player, "<#FFD27F>/team chat <dark_gray>(toggle)");
        mm.sendRaw(player, "<#FFD27F>/team echest");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("help", "create", "info", "list", "invite", "accept", "decline",
                    "promote", "demote", "kick", "leave", "disband",
                    "home", "sethome", "delhome", "warp", "setwarp", "delwarp",
                    "ally", "unally", "enemy", "unenemy",
                    "pvp", "settag", "chat", "echest", "rename");
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();

            if (sub.equals("invite") || sub.equals("kick") || sub.equals("promote") || sub.equals("demote")) {
                // For promote/demote/kick: show team members if in a team
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
                                .filter(name -> !name.equals(p.getName())) // don't suggest self
                                .toList();
                    } catch (Exception e) {
                        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                    }
                }
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            }

            if (sub.equals("accept") || sub.equals("decline")) {
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