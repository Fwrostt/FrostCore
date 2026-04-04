package dev.frost.frostcore.cmds;

import dev.frost.frostcore.exceptions.TeamException;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.manager.TeamManager;
import dev.frost.frostcore.teams.Team;
import dev.frost.frostcore.teams.TeamError;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class TeamCmd implements CommandExecutor, TabCompleter {

    private final TeamManager manager = TeamManager.getInstance();
    private final MessageManager mm = MessageManager.get();

    private static final Map<String, String> STATE = Map.of(
            "true", "<green>enabled</green>",
            "false", "<red>disabled</red>"
    );

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.getComponent("teams.prefix") + "Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
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

                case "invite" -> {
                    if (args.length < 2) {
                        sendHelp(player);
                        return true;
                    }
                    Team team = manager.getTeam(player.getUniqueId());
                    requireAdmin(player, team);

                    Player target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) {
                        mm.send(player, "teams.player-not-found");
                        return true;
                    }
                    if (manager.hasTeam(target.getUniqueId())) {
                        mm.send(player, "teams.already-in-team");
                        return true;
                    }

                    // TODO: Add proper invite system later (for now just message)
                    Map<String, String> ph = Map.of("player", target.getName(), "team", team.getName());
                    mm.send(player, "teams.invite", ph);
                    mm.send(target, "teams.invite-received", ph);
                }

                case "join" -> {
                    // TODO: Proper invite acceptance logic (for now basic join if invited)
                    if (args.length < 2) {
                        sendHelp(player);
                        return true;
                    }
                    String teamName = args[1].toLowerCase();
                    Team team = manager.getTeam(teamName); // will throw if not found

                    manager.addMember(team, player.getUniqueId());

                    Map<String, String> ph = Map.of("team", team.getName());
                    mm.send(player, "teams.join", ph);

                    Map<String, String> joinPh = Map.of("player", player.getName());
                    sendToTeam(team, "teams.invite-accepted", joinPh);
                }

                case "leave" -> {
                    Team team = manager.getTeam(player.getUniqueId());
                    String teamName = team.getName();

                    manager.removeMember(player.getUniqueId());

                    Map<String, String> ph = Map.of("team", teamName);
                    mm.send(player, "teams.leave", ph);
                }

                case "kick" -> {
                    if (args.length < 2) {
                        sendHelp(player);
                        return true;
                    }
                    Team team = manager.getTeam(player.getUniqueId());
                    requireAdmin(player, team);

                    Player target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) {
                        mm.send(player, "teams.player-not-found");
                        return true;
                    }
                    if (!team.isMember(target.getUniqueId())) {
                        mm.send(player, "teams.no-team"); // or custom message
                        return true;
                    }
                    if (team.isOwner(target.getUniqueId())) {
                        mm.send(player, "teams.only-owner");
                        return true;
                    }

                    manager.removeMember(target.getUniqueId());

                    Map<String, String> ph = Map.of("player", target.getName());
                    mm.send(player, "teams.kick", ph);
                    mm.send(target, "teams.leave", Map.of("team", team.getName()));
                }

                case "sethome" -> {
                    Team team = manager.getTeam(player.getUniqueId());
                    requireAdmin(player, team);

                    manager.setHome(team, player.getLocation());
                    mm.send(player, "teams.home-set");
                }

                case "delhome" -> {
                    Team team = manager.getTeam(player.getUniqueId());
                    requireAdmin(player, team);

                    team.setHome(null);
                    mm.send(player, "teams.home-del");
                }

                case "home" -> {
                    Team team = manager.getTeam(player.getUniqueId());
                    if (team.getHome() == null) {
                        mm.send(player, "teams.home-cooldown"); // reuse or add new if needed
                        return true;
                    }
                    // TODO: Add teleport with cooldown later
                    mm.send(player, "teams.home");
                    player.teleport(team.getHome());
                    mm.send(player, "teams.home-teleport");
                }

                case "setwarp" -> {
                    if (args.length < 2) {
                        sendHelp(player);
                        return true;
                    }
                    Team team = manager.getTeam(player.getUniqueId());
                    requireAdmin(player, team);

                    String warpName = args[1];
                    manager.setWarp(team, warpName, player.getLocation());

                    Map<String, String> ph = Map.of("warp", warpName);
                    mm.send(player, "teams.warp-set", ph);
                }

                case "warp" -> {
                    if (args.length < 2) {
                        sendHelp(player);
                        return true;
                    }
                    Team team = manager.getTeam(player.getUniqueId());
                    String warpName = args[1];

                    Location loc = team.getWarp(warpName);

                    Map<String, String> ph = Map.of("warp", warpName);
                    mm.send(player, "teams.warp", ph);
                    player.teleport(loc);
                    mm.send(player, "teams.warp-teleport", ph);
                }

                case "delwarp" -> {
                    if (args.length < 2) {
                        sendHelp(player);
                        return true;
                    }
                    Team team = manager.getTeam(player.getUniqueId());
                    requireAdmin(player, team);

                    String warpName = args[1];
                    manager.deleteWarp(team, warpName);

                    Map<String, String> ph = Map.of("warp", warpName);
                    mm.send(player, "teams.warp-del", ph);
                }

                case "ally" -> {
                    if (args.length < 2) {
                        sendHelp(player);
                        return true;
                    }
                    Team team = manager.getTeam(player.getUniqueId());
                    requireAdmin(player, team);

                    String targetTeam = args[1];
                    manager.addAlly(team, targetTeam);

                    Map<String, String> ph = Map.of("team", targetTeam);
                    mm.send(player, "teams.ally", ph);
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

                    Map<String, String> ph = Map.of("team", targetTeam);
                    mm.send(player, "teams.enemy", ph);
                }

                case "rename" -> {
                    if (args.length < 2) {
                        sendHelp(player);
                        return true;
                    }
                    Team team = manager.getTeam(player.getUniqueId());
                    requireOwner(player, team);

                    // TODO: Add rename logic with checks
                    mm.send(player, "teams.only-owner");
                }

                case "settag" -> {
                    if (args.length < 2) {
                        sendHelp(player);
                        return true;
                    }
                    Team team = manager.getTeam(player.getUniqueId());
                    requireOwner(player, team);

                    team.setTag(args[1]);
                    mm.send(player, "teams.create"); // reuse or add new message if needed
                }

                case "pvp" -> {
                    Team team = manager.getTeam(player.getUniqueId());
                    requireAdmin(player, team);

                    boolean newState = !team.isPvpToggle();
                    team.setPvpToggle(newState);

                    Map<String, String> ph = Map.of("state", STATE.getOrDefault(String.valueOf(newState), "unknown"));
                    mm.send(player, "teams.pvp-toggle", ph);
                }

                case "chat" -> {
                    if (args.length < 2) {
                        sendHelp(player);
                        return true;
                    }
                    Team team = manager.getTeam(player.getUniqueId());
                    boolean enable = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true");

                    team.setTeamChat(player.getUniqueId(), enable);
                    mm.send(player, enable ? "teams.create" : "teams.leave"); // placeholder - add proper chat toggle msg
                }

                case "echest" -> {
                    mm.send(player, "teams.no-team");
                }

                default -> sendHelp(player);
            }
        } catch (TeamException e) {
            String path = switch (e.getError()) {
                case PLAYER_NOT_IN_TEAM -> "teams.no-team";
                case TEAM_NOT_FOUND -> "teams.team-not-found";
                case PLAYER_ALREADY_IN_TEAM -> "teams.already-in-team";
                case TEAM_FULL -> "teams.team-full";
                case CANNOT_TARGET_SELF -> "teams.cannot-self";
                case ALREADY_ALLY -> "teams.already-ally";
                case ALREADY_ENEMY -> "teams.already-enemy";
                case ONLY_OWNER -> "teams.only-owner";
                case ONLY_ADMIN -> "teams.only-admin";
                default -> "teams.no-team";
            };
            mm.send(player, path);
        } catch (Exception e) {
            mm.sendRaw(player, "<red>An unexpected error occurred.</red>");
            e.printStackTrace();
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6§lTeam Commands:");
        player.sendMessage("§e/team create <name> <tag>");
        player.sendMessage("§e/team invite <player>");
        player.sendMessage("§e/team join <team>");
        player.sendMessage("§e/team leave");
        player.sendMessage("§e/team disband");
        player.sendMessage("§e/team home | sethome | delhome");
        player.sendMessage("§e/team warp | setwarp | delwarp <name>");
        player.sendMessage("§e/team ally | enemy <team>");
        player.sendMessage("§e/team pvp");
        player.sendMessage("§e/team chat <on/off>");
    }

    private void requireOwner(Player player, Team team) throws TeamException {
        if (!team.isOwner(player.getUniqueId())) {
            throw new TeamException(TeamError.ONLY_OWNER, "Only owners can do this.");
        }
    }

    private void requireAdmin(Player player, Team team) throws TeamException {
        if (!team.isOwner(player.getUniqueId()) && !team.isAdmin(player.getUniqueId())) {
            throw new TeamException(TeamError.ONLY_ADMIN, "Only admins or owners can do this.");
        }
    }

    private void sendToTeam(Team team, String path, Map<String, String> placeholders) {
        for (UUID uuid : team.getOwners()) sendIfOnline(uuid, path, placeholders);
        for (UUID uuid : team.getAdmins()) sendIfOnline(uuid, path, placeholders);
        for (UUID uuid : team.getMembers()) sendIfOnline(uuid, path, placeholders);
    }

    private void sendIfOnline(UUID uuid, String path, Map<String, String> placeholders) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null && p.isOnline()) {
            mm.send(p, path, placeholders);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("help", "create", "invite", "join", "leave", "disband",
                    "home", "sethome", "delhome", "warp", "setwarp", "delwarp",
                    "ally", "enemy", "pvp", "chat", "echest");
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("invite")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .toList();
            }
            if (sub.equals("warp") || sub.equals("delwarp")) {
                if (sender instanceof Player p) {
                    try {
                        Team team = manager.getTeam(p.getUniqueId());
                        return new ArrayList<>(team.getWarps().keySet());
                    } catch (Exception ignored) {}
                }
            }
            if (sub.equals("ally") || sub.equals("enemy") || sub.equals("join")) {
                return manager.getAllTeams().stream()
                        .map(Team::getName)
                        .toList();
            }
        }

        return Collections.emptyList();
    }
}