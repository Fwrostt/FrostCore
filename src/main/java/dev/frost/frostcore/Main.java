package dev.frost.frostcore;

import dev.frost.frostcore.cmds.TeamCmd;
import dev.frost.frostcore.database.DatabaseManager;
import dev.frost.frostcore.exceptions.TeamException;
import dev.frost.frostcore.invites.Invite;
import dev.frost.frostcore.invites.InviteHandler;
import dev.frost.frostcore.invites.InviteManager;
import dev.frost.frostcore.invites.InviteType;
import dev.frost.frostcore.listeners.InventoryCloseListener;
import dev.frost.frostcore.listeners.PlayerQuitListener;
import dev.frost.frostcore.listeners.TeamChatListener;
import dev.frost.frostcore.listeners.TeamPvPListener;
import dev.frost.frostcore.manager.ConfigManager;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.manager.TeamEchestManager;
import dev.frost.frostcore.manager.TeamManager;
import dev.frost.frostcore.placeholderapi.TeamExpansion;
import dev.frost.frostcore.teams.Team;
import dev.frost.frostcore.utils.CmdUtil;
import dev.frost.frostcore.utils.TeleportUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;

public final class Main extends JavaPlugin {

    @Getter private static Main instance;
    @Getter private static ConfigManager configManager;
    @Getter private static TeamManager teamManager;
    @Getter private static MessageManager messageManager;
    @Getter private static InviteManager inviteManager;
    @Getter private static DatabaseManager databaseManager;
    private static CmdUtil cmdUtil;
    @Getter private static TeleportUtil teleportUtil;
    @Getter private static TeamEchestManager echestManager;
    private TeamExpansion teamExpansion;

    @Override
    public void onEnable() {
        instance = this;
        setupClasses();
        setupInviteHandlers();
        setupListeners();
        setupCmds();
    }

    private void setupClasses() {
        configManager = ConfigManager.getInstance(this);
        messageManager = new MessageManager(this);
        teleportUtil = new TeleportUtil(this);

        // Database — init before TeamManager so data can be loaded
        databaseManager = new DatabaseManager(this);
        databaseManager.init();

        teamManager = TeamManager.getInstance();
        teamManager.setDatabaseManager(databaseManager);
        teamManager.loadAll();

        inviteManager = new InviteManager(this);
        echestManager = new TeamEchestManager(databaseManager);
        cmdUtil = new CmdUtil();
        ConfigurationSerialization.registerClass(Team.class);

        if (!this.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this.getLogger().warning("PlaceholderAPI is not enabled! PlaceholderAPI is required for BlissSMP to function correctly.");
        } else {
            teamExpansion = new TeamExpansion();
            teamExpansion.register();
        }
    }

    private void setupInviteHandlers() {
        MessageManager mm = messageManager;

        // --- Team Join Handler ---
        inviteManager.registerHandler(InviteType.TEAM_JOIN, new InviteHandler() {
            @Override
            public void onAccept(Invite invite) {
                Player target = Bukkit.getPlayer(invite.getTarget());
                Player sender = Bukkit.getPlayer(invite.getSender());
                String teamName = invite.getMeta("team", "unknown");

                try {
                    Team team = teamManager.getTeam(teamName);
                    teamManager.addMember(team, invite.getTarget());

                    if (target != null) {
                        mm.send(target, "teams.join", Map.of("team", team.getName()));
                    }

                    // Notify all team members
                    Map<String, String> ph = Map.of("player", target != null ? target.getName() : "Unknown");
                    notifyTeam(team, "teams.invite-accepted", ph);
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
        });

        // --- Team Ally Handler ---
        inviteManager.registerHandler(InviteType.TEAM_ALLY, new InviteHandler() {
            @Override
            public void onAccept(Invite invite) {
                String senderTeam = invite.getMeta("senderTeam", "unknown");
                String targetTeam = invite.getMeta("targetTeam", "unknown");

                try {
                    Team sTeam = teamManager.getTeam(senderTeam);
                    Team tTeam = teamManager.getTeam(targetTeam);

                    // Make mutual allies
                    sTeam.addAlly(targetTeam);
                    tTeam.addAlly(senderTeam);

                    // Save both teams' relations
                    databaseManager.saveRelationsAsync(sTeam);
                    databaseManager.saveRelationsAsync(tTeam);

                    // Notify both teams
                    notifyTeam(sTeam, "teams.ally-accepted", Map.of("team", targetTeam));
                    notifyTeam(tTeam, "teams.ally-accepted", Map.of("team", senderTeam));

                    // Cancel remaining ally invites for this pair
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

                // Cancel remaining ally invites for this pair
                inviteManager.cancelInvites(InviteType.TEAM_ALLY, inv ->
                        inv.getMeta("senderTeam", "").equals(senderTeamName)
                                && inv.getMeta("targetTeam", "").equals(targetTeamName));
            }

            @Override
            public void onExpire(Invite invite) {
                Player sender = Bukkit.getPlayer(invite.getSender());
                String targetTeamName = invite.getMeta("targetTeam", "unknown");
                String senderTeamName = invite.getMeta("senderTeam", "unknown");

                if (sender != null) {
                    mm.send(sender, "teams.ally-expired", Map.of("team", targetTeamName));
                }

                Player target = Bukkit.getPlayer(invite.getTarget());
                if (target != null) {
                    mm.send(target, "teams.ally-expired", Map.of("team", senderTeamName));
                }
            }
        });
    }

    private void notifyTeam(Team team, String path, Map<String, String> placeholders) {
        for (UUID uuid : team.getOwners()) notifyPlayer(uuid, path, placeholders);
        for (UUID uuid : team.getAdmins()) notifyPlayer(uuid, path, placeholders);
        for (UUID uuid : team.getMembers()) notifyPlayer(uuid, path, placeholders);
    }

    private void notifyPlayer(UUID uuid, String path, Map<String, String> placeholders) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null && p.isOnline()) {
            messageManager.send(p, path, placeholders);
        }
    }

    private void setupListeners() {
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryCloseListener(), this);
        getServer().getPluginManager().registerEvents(new TeamChatListener(), this);
        getServer().getPluginManager().registerEvents(new TeamPvPListener(), this);
    }

    private void setupCmds() {
        cmdUtil.registerCommand("team", new TeamCmd(), new TeamCmd());
    }

    @Override
    public void onDisable() {
        if (inviteManager != null) {
            inviteManager.shutdown();
        }
        if (echestManager != null) {
            echestManager.saveAll();
        }
        if (teamManager != null) {
            teamManager.saveAll();
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        if (teamExpansion != null) {
            teamExpansion.unregister();
        }
    }
}
