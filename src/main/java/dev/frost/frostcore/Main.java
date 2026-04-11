package dev.frost.frostcore;

import dev.frost.frostcore.cmds.*;
import dev.frost.frostcore.database.DatabaseManager;
import dev.frost.frostcore.gui.GuiManager;
import dev.frost.frostcore.invites.InviteManager;
import dev.frost.frostcore.invites.InviteType;
import dev.frost.frostcore.invites.handlers.TeamAllyInviteHandler;
import dev.frost.frostcore.invites.handlers.TeamJoinInviteHandler;
import dev.frost.frostcore.invites.handlers.TpaHereInviteHandler;
import dev.frost.frostcore.invites.handlers.TpaInviteHandler;
import dev.frost.frostcore.listeners.*;
import dev.frost.frostcore.manager.*;
import dev.frost.frostcore.placeholderapi.TeamExpansion;
import dev.frost.frostcore.teams.Team;
import dev.frost.frostcore.utils.CmdUtil;
import dev.frost.frostcore.utils.FrostLogger;
import dev.frost.frostcore.utils.TeleportUtil;
import lombok.Getter;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

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
    @Getter private static HomeManager homeManager;
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
            FrostLogger.warn("PlaceholderAPI is not enabled! PlaceholderAPI is required for FrostCore to function correctly.");
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

        GuiManager.init(this);

        CooldownManager.init(databaseManager);

        teamManager = TeamManager.getInstance();
        teamManager.setDatabaseManager(databaseManager);
        teamManager.loadAll();

        inviteManager = new InviteManager(this);
        echestManager = new TeamEchestManager(databaseManager);
        warpManager = new WarpManager(this, databaseManager);
        homeManager = new HomeManager(this, configManager);
        cmdUtil = new CmdUtil();
        ConfigurationSerialization.registerClass(Team.class);
    }

    private void setupInviteHandlers() {
        inviteManager.registerHandler(InviteType.TEAM_JOIN,
                new TeamJoinInviteHandler(messageManager, teamManager));

        inviteManager.registerHandler(InviteType.TEAM_ALLY,
                new TeamAllyInviteHandler(messageManager, teamManager, databaseManager, inviteManager));

        inviteManager.registerHandler(InviteType.TPA,
                new TpaInviteHandler(messageManager, teleportUtil));

        inviteManager.registerHandler(InviteType.TPA_HERE,
                new TpaHereInviteHandler(messageManager, teleportUtil));
    }

    private void setupListeners() {
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryCloseListener(), this);
        getServer().getPluginManager().registerEvents(new TeamChatListener(), this);
        getServer().getPluginManager().registerEvents(new TeamPvPListener(), this);
        getServer().getPluginManager().registerEvents(
                new SpawnListener(configManager, warpManager, teleportUtil), this);
        getServer().getPluginManager().registerEvents(homeManager, this);
    }

    private void setupCmds() {
        cmdUtil.registerCommand("team", new TeamCmd(), new TeamCmd());

        TpaCmd tpaCmd = new TpaCmd();
        cmdUtil.registerCommand("tpa", tpaCmd, tpaCmd);

        TpaHereCmd tpahereCmd = new TpaHereCmd();
        cmdUtil.registerCommand("tpahere", tpahereCmd, tpahereCmd);

        TpaToggleCmd tpaToggleCmd = new TpaToggleCmd();
        cmdUtil.registerCommand("tpatoggle", tpaToggleCmd, null);
        cmdUtil.registerCommand("tpaoff", tpaToggleCmd, null);
        cmdUtil.registerCommand("tpaon", tpaToggleCmd, null);

        TpAcceptCmd tpacceptCmd = new TpAcceptCmd();
        cmdUtil.registerCommand("tpaccept", tpacceptCmd, tpacceptCmd);

        TpDeclineCmd tpdeclineCmd = new TpDeclineCmd();
        cmdUtil.registerCommand("tpdecline", tpdeclineCmd, tpdeclineCmd);

        WarpCmd warpCmd = new WarpCmd();
        cmdUtil.registerCommand("warp", warpCmd, warpCmd);

        WarpsCmd warpsCmd = new WarpsCmd();
        cmdUtil.registerCommand("warps", warpsCmd, null);

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

        SetHomeCmd sethomeCmd = new SetHomeCmd();
        cmdUtil.registerCommand("sethome", sethomeCmd, null);

        HomeCmd homeCmd = new HomeCmd();
        cmdUtil.registerCommand("home", homeCmd, homeCmd);

        DelHomeCmd delhomeCmd = new DelHomeCmd();
        cmdUtil.registerCommand("delhome", delhomeCmd, delhomeCmd);

        RenameHomeCmd renamehomeCmd = new RenameHomeCmd();
        cmdUtil.registerCommand("renamehome", renamehomeCmd, renamehomeCmd);

        HomesCmd homesCmd = new HomesCmd();
        cmdUtil.registerCommand("homes", homesCmd, null);

        SetSpawnCmd setspawnCmd = new SetSpawnCmd();
        cmdUtil.registerCommand("setspawn", setspawnCmd, null);

        FrostCoreCmd frostCoreCmd = new FrostCoreCmd();
        cmdUtil.registerCommand("frostcore", frostCoreCmd, frostCoreCmd);
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

