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
    @Getter private static UtilityManager utilityManager;
    @Getter private static PunishmentManager punishmentManager;
    @Getter private static BackManager backManager;
    @Getter private static VanishManager vanishManager;
    @Getter private static PrivateMessageManager privateMessageManager;
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
        utilityManager = new UtilityManager(databaseManager);
        punishmentManager = new PunishmentManager(databaseManager);
        backManager = new BackManager();
        vanishManager = new VanishManager();
        privateMessageManager = new PrivateMessageManager();
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
        getServer().getPluginManager().registerEvents(new PlayerUtilityListener(), this);
        getServer().getPluginManager().registerEvents(new ModerationListener(), this);
        getServer().getPluginManager().registerEvents(new BackListener(), this);
        getServer().getPluginManager().registerEvents(new InvseeListener(), this);
        getServer().getPluginManager().registerEvents(new VanishListener(), this);
        getServer().getPluginManager().registerEvents(homeManager, this);
    }

    private void setupCmds() {
        cmdUtil.registerCommand("team", new TeamCmd(), new TeamCmd());

        TpaCmd tpaCmd = new TpaCmd();
        cmdUtil.registerCommand("tpa", tpaCmd, tpaCmd);

        TpaHereCmd tpahereCmd = new TpaHereCmd();
        cmdUtil.registerCommand("tpahere", tpahereCmd, tpahereCmd);

        TpaToggleCmd tpaToggleCmd = new TpaToggleCmd();
        cmdUtil.registerCommand("tpatoggle", tpaToggleCmd, tpaToggleCmd);
        cmdUtil.registerCommand("tpaoff", tpaToggleCmd, tpaToggleCmd);
        cmdUtil.registerCommand("tpaon", tpaToggleCmd, tpaToggleCmd);

        TpAcceptCmd tpacceptCmd = new TpAcceptCmd();
        cmdUtil.registerCommand("tpaccept", tpacceptCmd, tpacceptCmd);

        TpDeclineCmd tpdeclineCmd = new TpDeclineCmd();
        cmdUtil.registerCommand("tpdecline", tpdeclineCmd, tpdeclineCmd);

        WarpCmd warpCmd = new WarpCmd();
        cmdUtil.registerCommand("warp", warpCmd, warpCmd);

        WarpsCmd warpsCmd = new WarpsCmd();
        cmdUtil.registerCommand("warps", warpsCmd, warpsCmd);

        SpawnCmd spawnCmd = new SpawnCmd();
        cmdUtil.registerCommand("spawn", spawnCmd, spawnCmd);

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
        cmdUtil.registerCommand("sethome", sethomeCmd, sethomeCmd);

        HomeCmd homeCmd = new HomeCmd();
        cmdUtil.registerCommand("home", homeCmd, homeCmd);

        DelHomeCmd delhomeCmd = new DelHomeCmd();
        cmdUtil.registerCommand("delhome", delhomeCmd, delhomeCmd);

        RenameHomeCmd renamehomeCmd = new RenameHomeCmd();
        cmdUtil.registerCommand("renamehome", renamehomeCmd, renamehomeCmd);

        HomesCmd homesCmd = new HomesCmd();
        cmdUtil.registerCommand("homes", homesCmd, homesCmd);

        SetSpawnCmd setspawnCmd = new SetSpawnCmd();
        cmdUtil.registerCommand("setspawn", setspawnCmd, setspawnCmd);

        FrostCoreCmd frostCoreCmd = new FrostCoreCmd();
        cmdUtil.registerCommand("frostcore", frostCoreCmd, frostCoreCmd);

        GamemodeCmd gmCmd = new GamemodeCmd();
        cmdUtil.registerCommand("gm", gmCmd, gmCmd);
        cmdUtil.registerCommand("gms", gmCmd, gmCmd);
        cmdUtil.registerCommand("gmc", gmCmd, gmCmd);
        cmdUtil.registerCommand("gma", gmCmd, gmCmd);
        cmdUtil.registerCommand("gmsp", gmCmd, gmCmd);

        PlayerAttributeCmds attrCmds = new PlayerAttributeCmds();
        cmdUtil.registerCommand("fly", attrCmds, attrCmds);
        cmdUtil.registerCommand("heal", attrCmds, attrCmds);
        cmdUtil.registerCommand("feed", attrCmds, attrCmds);
        cmdUtil.registerCommand("god", attrCmds, attrCmds);
        cmdUtil.registerCommand("clear", attrCmds, attrCmds);
        cmdUtil.registerCommand("speed", attrCmds, attrCmds);

        NickCmd nickCmd = new NickCmd();
        cmdUtil.registerCommand("nick", nickCmd, nickCmd);
        cmdUtil.registerCommand("unnick", nickCmd, nickCmd);

        ItemEditCmds itemCmds = new ItemEditCmds();
        cmdUtil.registerCommand("itemrename", itemCmds, itemCmds);
        cmdUtil.registerCommand("lore", itemCmds, itemCmds);
        cmdUtil.registerCommand("repair", itemCmds, itemCmds);

        ModerationCmds modCmds = new ModerationCmds();
        cmdUtil.registerCommand("mute", modCmds, modCmds);
        cmdUtil.registerCommand("unmute", modCmds, modCmds);
        cmdUtil.registerCommand("lockchat", modCmds, modCmds);
        cmdUtil.registerCommand("unlockchat", modCmds, modCmds);
        cmdUtil.registerCommand("freeze", modCmds, modCmds);

        AdminMiscCmds adminMiscCmds = new AdminMiscCmds();
        cmdUtil.registerCommand("sudo", adminMiscCmds, adminMiscCmds);
        cmdUtil.registerCommand("broadcast", adminMiscCmds, adminMiscCmds);
        cmdUtil.registerCommand("chat", adminMiscCmds, adminMiscCmds);
        cmdUtil.registerCommand("day", adminMiscCmds, adminMiscCmds);
        cmdUtil.registerCommand("night", adminMiscCmds, adminMiscCmds);
        cmdUtil.registerCommand("time", adminMiscCmds, adminMiscCmds);
        cmdUtil.registerCommand("weather", adminMiscCmds, adminMiscCmds);

        BackCmd backCmd = new BackCmd();
        cmdUtil.registerCommand("back", backCmd, backCmd);

        AdminExtraCmds adminExtraCmds = new AdminExtraCmds();
        cmdUtil.registerCommand("invsee", adminExtraCmds, adminExtraCmds);
        cmdUtil.registerCommand("enderchest", adminExtraCmds, adminExtraCmds);
        cmdUtil.registerCommand("ec", adminExtraCmds, adminExtraCmds);
        cmdUtil.registerCommand("hat", adminExtraCmds, adminExtraCmds);
        cmdUtil.registerCommand("whois", adminExtraCmds, adminExtraCmds);
        cmdUtil.registerCommand("seen", adminExtraCmds, adminExtraCmds);
        cmdUtil.registerCommand("smite", adminExtraCmds, adminExtraCmds);
        cmdUtil.registerCommand("vanish", adminExtraCmds, adminExtraCmds);
        cmdUtil.registerCommand("v", adminExtraCmds, adminExtraCmds);
        cmdUtil.registerCommand("kick", adminExtraCmds, adminExtraCmds);
        cmdUtil.registerCommand("ban", adminExtraCmds, adminExtraCmds);
        cmdUtil.registerCommand("unban", adminExtraCmds, adminExtraCmds);
        cmdUtil.registerCommand("warn", adminExtraCmds, adminExtraCmds);
        cmdUtil.registerCommand("tpall", adminExtraCmds, adminExtraCmds);
        cmdUtil.registerCommand("ping", adminExtraCmds, adminExtraCmds);
        cmdUtil.registerCommand("skull", adminExtraCmds, adminExtraCmds);
        cmdUtil.registerCommand("socialspy", adminExtraCmds, adminExtraCmds);
        cmdUtil.registerCommand("ram", adminExtraCmds, adminExtraCmds);
        cmdUtil.registerCommand("top", adminExtraCmds, adminExtraCmds);
        cmdUtil.registerCommand("bottom", adminExtraCmds, adminExtraCmds);
        cmdUtil.registerCommand("near", adminExtraCmds, adminExtraCmds);
        cmdUtil.registerCommand("coords", adminExtraCmds, adminExtraCmds);

        MessageCmds msgCmds = new MessageCmds();
        cmdUtil.registerCommand("msg", msgCmds, msgCmds);
        cmdUtil.registerCommand("tell", msgCmds, msgCmds);
        cmdUtil.registerCommand("w", msgCmds, msgCmds);
        cmdUtil.registerCommand("whisper", msgCmds, msgCmds);
        cmdUtil.registerCommand("pm", msgCmds, msgCmds);
        cmdUtil.registerCommand("r", msgCmds, msgCmds);
        cmdUtil.registerCommand("reply", msgCmds, msgCmds);
        cmdUtil.registerCommand("ignore", msgCmds, msgCmds);
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

