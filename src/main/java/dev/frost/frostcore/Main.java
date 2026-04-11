package dev.frost.frostcore;

import dev.frost.frostcore.cmds.*;
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
import dev.frost.frostcore.manager.WarpManager;
import dev.frost.frostcore.placeholderapi.TeamExpansion;
import dev.frost.frostcore.teams.Team;
import dev.frost.frostcore.utils.CmdUtil;
import dev.frost.frostcore.utils.FrostLogger;
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
    @Getter private static TeleportUtil teleportUtil;
    @Getter private static TeamEchestManager echestManager;
    @Getter private static WarpManager warpManager;
    private static CmdUtil cmdUtil;
    private TeamExpansion teamExpansion;

    @Override
    public void onEnable() {
        instance = this;
        FrostLogger.init(this);
        FrostLogger.printBanner();
        setupClasses();
        setupInviteHandlers();
        setupListeners();
        setupCmds();
        if (!this.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            FrostLogger.warn("PlaceholderAPI is not enabled! PlaceholderAPI is required for BlissSMP to function correctly.");
        } else {
            teamExpansion = new TeamExpansion();
            teamExpansion.register();
        }
    }

    private void setupClasses() {
        configManager = ConfigManager.getInstance(this);
        messageManager = new MessageManager(this);
        teleportUtil = new TeleportUtil(this);

        databaseManager = new DatabaseManager(this);
        databaseManager.init();

        teamManager = TeamManager.getInstance();
        teamManager.setDatabaseManager(databaseManager);
        teamManager.loadAll();

        inviteManager = new InviteManager(this);
        echestManager = new TeamEchestManager(databaseManager);
        warpManager = new WarpManager(databaseManager);
        cmdUtil = new CmdUtil();
        ConfigurationSerialization.registerClass(Team.class);
    }

    private void setupInviteHandlers() {
        MessageManager mm = messageManager;
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

        inviteManager.registerHandler(InviteType.TEAM_ALLY, new InviteHandler() {
            @Override
            public void onAccept(Invite invite) {
                String senderTeam = invite.getMeta("senderTeam", "unknown");
                String targetTeam = invite.getMeta("targetTeam", "unknown");

                try {
                    Team sTeam = teamManager.getTeam(senderTeam);
                    Team tTeam = teamManager.getTeam(targetTeam);

                    sTeam.addAlly(targetTeam);
                    tTeam.addAlly(senderTeam);

                    databaseManager.saveRelationsAsync(sTeam);
                    databaseManager.saveRelationsAsync(tTeam);

                    notifyTeam(sTeam, "teams.ally-accepted", Map.of("team", targetTeam));
                    notifyTeam(tTeam, "teams.ally-accepted", Map.of("team", senderTeam));

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

        inviteManager.registerHandler(InviteType.TPA, new InviteHandler() {
            @Override
            public void onAccept(Invite invite) {
                Player sender = Bukkit.getPlayer(invite.getSender()); // Sender of request (wanting to tp)
                Player target = Bukkit.getPlayer(invite.getTarget()); // Acceptor (the one being tp'd to)

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
                if (sender != null && target != null) {
                    mm.send(sender, "teleport.tpa-expired-sender", Map.of("player", target.getName()));
                    mm.send(target, "teleport.tpa-expired-target", Map.of("player", sender.getName()));
                }
            }
        });

        inviteManager.registerHandler(InviteType.TPA_HERE, new InviteHandler() {
            @Override
            public void onAccept(Invite invite) {
                Player sender = Bukkit.getPlayer(invite.getSender()); // Sender of request (wanting target to tp)
                Player target = Bukkit.getPlayer(invite.getTarget()); // Acceptor (the one teleporting)

                if (sender == null || target == null) return;
                
                mm.send(sender, "teleport.tpa-accepted-sender", Map.of("player", target.getName()));
                mm.send(target, "teleport.tpa-accepted-target", Map.of("player", sender.getName()));

                teleportUtil.teleportWithCooldownAndDelay(
                        target, sender.getLocation(),
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
                if (sender != null && target != null) {
                    mm.send(sender, "teleport.tpa-expired-sender", Map.of("player", target.getName()));
                    mm.send(target, "teleport.tpa-expired-target", Map.of("player", sender.getName()));
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
        
        TpaCmd tpaCmd = new TpaCmd();
        cmdUtil.registerCommand("tpa", tpaCmd, tpaCmd);
        
        TpaHereCmd tpahereCmd = new TpaHereCmd();
        cmdUtil.registerCommand("tpahere", tpahereCmd, tpahereCmd);
        
        TpAcceptCmd tpacceptCmd = new TpAcceptCmd();
        cmdUtil.registerCommand("tpaccept", tpacceptCmd, tpacceptCmd);
        
        TpDeclineCmd tpdeclineCmd = new TpDeclineCmd();
        cmdUtil.registerCommand("tpdecline", tpdeclineCmd, tpdeclineCmd);
        
        WarpCmd warpCmd = new WarpCmd();
        cmdUtil.registerCommand("warp", warpCmd, warpCmd);
        
        SpawnCmd spawnCmd = new SpawnCmd();
        cmdUtil.registerCommand("spawn", spawnCmd, null);
        
        TpCmd tpCmd = new TpCmd();
        cmdUtil.registerCommand("tp", tpCmd, tpCmd);
        cmdUtil.registerCommand("tp2p", tpCmd, tpCmd);
        cmdUtil.registerCommand("tphere", tpCmd, tpCmd);
        
        OfflineTpCmd otpCmd = new OfflineTpCmd();
        cmdUtil.registerCommand("otp", otpCmd, otpCmd);
        cmdUtil.registerCommand("offlinetp", otpCmd, otpCmd);
        
        SetWarpCmd setwarpCmd = new SetWarpCmd();
        cmdUtil.registerCommand("setwarp", setwarpCmd, setwarpCmd);
        cmdUtil.registerCommand("delwarp", setwarpCmd, setwarpCmd);
        
        SetSpawnCmd setspawnCmd = new SetSpawnCmd();
        cmdUtil.registerCommand("setspawn", setspawnCmd, null);
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
