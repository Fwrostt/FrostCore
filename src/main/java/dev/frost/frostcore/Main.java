package dev.frost.frostcore;

import dev.frost.frostcore.cmds.admin.*;
import dev.frost.frostcore.cmds.item.*;
import dev.frost.frostcore.cmds.messaging.*;
import dev.frost.frostcore.cmds.moderation.*;
import dev.frost.frostcore.cmds.player.*;
import dev.frost.frostcore.cmds.team.*;
import dev.frost.frostcore.cmds.teleport.*;
import dev.frost.frostcore.database.DatabaseManager;
import dev.frost.frostcore.gui.GuiManager;
import dev.frost.frostcore.invites.InviteManager;
import dev.frost.frostcore.invites.InviteType;
import dev.frost.frostcore.invites.handlers.TeamAllyInviteHandler;
import dev.frost.frostcore.invites.handlers.TeamJoinInviteHandler;
import dev.frost.frostcore.invites.handlers.TpaHereInviteHandler;
import dev.frost.frostcore.invites.handlers.TpaInviteHandler;
import dev.frost.frostcore.listeners.*;
import dev.frost.frostcore.mace.MaceDatabase;
import dev.frost.frostcore.manager.*;
import dev.frost.frostcore.moderation.*;
import dev.frost.frostcore.placeholderapi.TeamExpansion;
import dev.frost.frostcore.utils.CmdUtil;
import dev.frost.frostcore.utils.FrostLogger;
import dev.frost.frostcore.utils.TeleportUtil;
import lombok.Getter;
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
    @Getter private static ModerationManager moderationManager;
    @Getter private static TemplateManager templateManager;
    @Getter private static GroupLimitManager groupLimitManager;
    @Getter private static WebhookManager webhookManager;
    @Getter private static BackManager backManager;
    @Getter private static VanishManager vanishManager;
    @Getter private static PrivateMessageManager privateMessageManager;
    @Getter private static MaceManager maceManager;
    @Getter private static GlowManager glowManager;
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

        
        webhookManager = new WebhookManager();
        ModerationDatabase modDb = new ModerationDatabase(databaseManager, this);
        modDb.createTables();
        moderationManager = new ModerationManager(modDb, webhookManager);
        templateManager = new TemplateManager();
        groupLimitManager = new GroupLimitManager();

        backManager = new BackManager();
        vanishManager = new VanishManager();
        privateMessageManager = new PrivateMessageManager();

        MaceDatabase maceDb = new MaceDatabase(databaseManager);
        maceDb.createTable();
        maceManager = new MaceManager(maceDb);

        glowManager = new GlowManager();

        cmdUtil = new CmdUtil();
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
        getServer().getPluginManager().registerEvents(new IPTrackingListener(), this);
        getServer().getPluginManager().registerEvents(new BackListener(), this);
        getServer().getPluginManager().registerEvents(new InvseeListener(), this);
        getServer().getPluginManager().registerEvents(new VanishListener(), this);
        getServer().getPluginManager().registerEvents(new StaffChatListener(), this);
        getServer().getPluginManager().registerEvents(homeManager, this);
        getServer().getPluginManager().registerEvents(new MaceListener(), this);
        getServer().getPluginManager().registerEvents(new GlowListener(), this);
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

        FlyCmd flyCmd = new FlyCmd();
        cmdUtil.registerCommand("fly", flyCmd, flyCmd);
        HealCmd healCmd = new HealCmd();
        cmdUtil.registerCommand("heal", healCmd, healCmd);
        FeedCmd feedCmd = new FeedCmd();
        cmdUtil.registerCommand("feed", feedCmd, feedCmd);
        GodCmd godCmd = new GodCmd();
        cmdUtil.registerCommand("god", godCmd, godCmd);
        ClearCmd clearCmd = new ClearCmd();
        cmdUtil.registerCommand("clear", clearCmd, clearCmd);
        SpeedCmd speedCmd = new SpeedCmd();
        cmdUtil.registerCommand("speed", speedCmd, speedCmd);

        NickCmd nickCmd = new NickCmd();
        cmdUtil.registerCommand("nick", nickCmd, nickCmd);
        cmdUtil.registerCommand("unnick", nickCmd, nickCmd);

        ItemRenameCmd itemRenameCmd = new ItemRenameCmd();
        cmdUtil.registerCommand("itemrename", itemRenameCmd, itemRenameCmd);
        LoreCmd loreCmd = new LoreCmd();
        cmdUtil.registerCommand("lore", loreCmd, loreCmd);
        RepairCmd repairCmd = new RepairCmd();
        cmdUtil.registerCommand("repair", repairCmd, repairCmd);

        
        MuteCmd muteCmd = new MuteCmd();
        cmdUtil.registerCommand("mute", muteCmd, muteCmd);
        TempMuteCmd tempMuteCmd = new TempMuteCmd();
        cmdUtil.registerCommand("tempmute", tempMuteCmd, tempMuteCmd);
        UnmuteCmd unmuteCmd = new UnmuteCmd();
        cmdUtil.registerCommand("unmute", unmuteCmd, unmuteCmd);
        LockchatCmd lockchatCmd = new LockchatCmd();
        cmdUtil.registerCommand("lockchat", lockchatCmd, lockchatCmd);
        UnlockchatCmd unlockchatCmd = new UnlockchatCmd();
        cmdUtil.registerCommand("unlockchat", unlockchatCmd, unlockchatCmd);
        FreezeCmd freezeCmd = new FreezeCmd();
        cmdUtil.registerCommand("freeze", freezeCmd, freezeCmd);
        ScreenshareCmd screenshareCmd = new ScreenshareCmd();
        cmdUtil.registerCommand("screenshare", screenshareCmd, screenshareCmd);
        cmdUtil.registerCommand("ss", screenshareCmd, screenshareCmd);

        KickCmd kickCmd = new KickCmd();
        cmdUtil.registerCommand("kick", kickCmd, kickCmd);
        BanCmd banCmd = new BanCmd();
        cmdUtil.registerCommand("ban", banCmd, banCmd);
        TempBanCmd tempBanCmd = new TempBanCmd();
        cmdUtil.registerCommand("tempban", tempBanCmd, tempBanCmd);
        UnbanCmd unbanCmd = new UnbanCmd();
        cmdUtil.registerCommand("unban", unbanCmd, unbanCmd);
        WarnCmd warnCmd = new WarnCmd();
        cmdUtil.registerCommand("warn", warnCmd, warnCmd);
        UnwarnCmd unwarnCmd = new UnwarnCmd();
        cmdUtil.registerCommand("unwarn", unwarnCmd, unwarnCmd);

        IpBanCmd ipBanCmd = new IpBanCmd();
        cmdUtil.registerCommand("ipban", ipBanCmd, ipBanCmd);
        cmdUtil.registerCommand("banip", ipBanCmd, ipBanCmd);
        IpMuteCmd ipMuteCmd = new IpMuteCmd();
        cmdUtil.registerCommand("ipmute", ipMuteCmd, ipMuteCmd);
        cmdUtil.registerCommand("muteip", ipMuteCmd, ipMuteCmd);

        
        JailCmd jailCmd = new JailCmd();
        cmdUtil.registerCommand("jail", jailCmd, jailCmd);
        UnjailCmd unjailCmd = new UnjailCmd();
        cmdUtil.registerCommand("unjail", unjailCmd, unjailCmd);
        SetJailCmd setJailCmd = new SetJailCmd();
        cmdUtil.registerCommand("setjail", setJailCmd, setJailCmd);
        DelJailCmd delJailCmd = new DelJailCmd();
        cmdUtil.registerCommand("deljail", delJailCmd, delJailCmd);

        
        ReportCmd reportCmd = new ReportCmd();
        cmdUtil.registerCommand("report", reportCmd, reportCmd);

        
        CheckBanCmd checkBanCmd = new CheckBanCmd();
        cmdUtil.registerCommand("checkban", checkBanCmd, checkBanCmd);
        CheckPunishmentCmd checkMuteCmd = new CheckPunishmentCmd("MUTE");
        cmdUtil.registerCommand("checkmute", checkMuteCmd, checkMuteCmd);
        CheckPunishmentCmd checkWarnCmd = new CheckPunishmentCmd("WARN");
        cmdUtil.registerCommand("checkwarn", checkWarnCmd, checkWarnCmd);
        HistoryCmd historyCmd = new HistoryCmd();
        cmdUtil.registerCommand("history", historyCmd, historyCmd);
        StaffHistoryCmd staffHistoryCmd = new StaffHistoryCmd();
        cmdUtil.registerCommand("staffhistory", staffHistoryCmd, staffHistoryCmd);
        WarningsCmd warningsCmd = new WarningsCmd();
        cmdUtil.registerCommand("warnings", warningsCmd, warningsCmd);

        
        PunishmentListCmd banListCmd = new PunishmentListCmd("BAN", "Active Bans");
        cmdUtil.registerCommand("banlist", banListCmd, banListCmd);
        PunishmentListCmd muteListCmd = new PunishmentListCmd("MUTE", "Active Mutes");
        cmdUtil.registerCommand("mutelist", muteListCmd, muteListCmd);
        PunishmentListCmd warnListCmd = new PunishmentListCmd("WARN", "Active Warnings");
        cmdUtil.registerCommand("warnlist", warnListCmd, warnListCmd);

        
        AltsCmd altsCmd = new AltsCmd();
        cmdUtil.registerCommand("alts", altsCmd, altsCmd);
        
        NameHistoryCmd nameHistoryCmd = new NameHistoryCmd();
        cmdUtil.registerCommand("namehistory", nameHistoryCmd, null);
        IpHistoryCmd ipHistoryCmd = new IpHistoryCmd();
        cmdUtil.registerCommand("iphistory", ipHistoryCmd, ipHistoryCmd);

        
        ReportsCmd reportsCmd = new ReportsCmd();
        cmdUtil.registerCommand("reports", reportsCmd, reportsCmd);

        
        LockdownCmd lockdownCmd = new LockdownCmd();
        cmdUtil.registerCommand("lockdown", lockdownCmd, lockdownCmd);
        PruneHistoryCmd pruneHistoryCmd = new PruneHistoryCmd();
        cmdUtil.registerCommand("prunehistory", pruneHistoryCmd, pruneHistoryCmd);
        StaffRollbackCmd staffRollbackCmd = new StaffRollbackCmd();
        cmdUtil.registerCommand("staffrollback", staffRollbackCmd, staffRollbackCmd);
        ModerationCmd moderationCmd = new ModerationCmd();
        cmdUtil.registerCommand("moderation", moderationCmd, moderationCmd);
        StaffChatCmd staffChatCmd = new StaffChatCmd();
        cmdUtil.registerCommand("staffchat", staffChatCmd, staffChatCmd);
        cmdUtil.registerCommand("sc", staffChatCmd, staffChatCmd);

        SudoCmd sudoCmd = new SudoCmd();
        cmdUtil.registerCommand("sudo", sudoCmd, sudoCmd);
        BroadcastCmd broadcastCmd = new BroadcastCmd();
        cmdUtil.registerCommand("broadcast", broadcastCmd, broadcastCmd);
        ChatCmd chatCmd = new ChatCmd();
        cmdUtil.registerCommand("chat", chatCmd, chatCmd);
        TimeCmd timeCmd = new TimeCmd();
        cmdUtil.registerCommand("day", timeCmd, timeCmd);
        cmdUtil.registerCommand("night", timeCmd, timeCmd);
        cmdUtil.registerCommand("time", timeCmd, timeCmd);
        WeatherCmd weatherCmd = new WeatherCmd();
        cmdUtil.registerCommand("weather", weatherCmd, weatherCmd);

        BackCmd backCmd = new BackCmd();
        cmdUtil.registerCommand("back", backCmd, backCmd);

        InvseeCmd invseeCmd = new InvseeCmd();
        cmdUtil.registerCommand("invsee", invseeCmd, invseeCmd);
        EnderchestCmd ecCmd = new EnderchestCmd();
        cmdUtil.registerCommand("enderchest", ecCmd, ecCmd);
        cmdUtil.registerCommand("ec", ecCmd, ecCmd);
        HatCmd hatCmd = new HatCmd();
        cmdUtil.registerCommand("hat", hatCmd, hatCmd);
        WhoisCmd whoisCmd = new WhoisCmd();
        cmdUtil.registerCommand("whois", whoisCmd, whoisCmd);
        SeenCmd seenCmd = new SeenCmd();
        cmdUtil.registerCommand("seen", seenCmd, seenCmd);
        SmiteCmd smiteCmd = new SmiteCmd();
        cmdUtil.registerCommand("smite", smiteCmd, smiteCmd);
        VanishCmd vanishCmd = new VanishCmd();
        cmdUtil.registerCommand("vanish", vanishCmd, vanishCmd);
        cmdUtil.registerCommand("v", vanishCmd, vanishCmd);
        TpallCmd tpallCmd = new TpallCmd();
        cmdUtil.registerCommand("tpall", tpallCmd, tpallCmd);
        PingCmd pingCmd = new PingCmd();
        cmdUtil.registerCommand("ping", pingCmd, pingCmd);
        SkullCmd skullCmd = new SkullCmd();
        cmdUtil.registerCommand("skull", skullCmd, skullCmd);
        SocialSpyCmd ssCmd = new SocialSpyCmd();
        cmdUtil.registerCommand("socialspy", ssCmd, ssCmd);
        RamCmd ramCmd = new RamCmd();
        cmdUtil.registerCommand("ram", ramCmd, ramCmd);
        TopCmd topCmd = new TopCmd();
        cmdUtil.registerCommand("top", topCmd, topCmd);
        BottomCmd bottomCmd = new BottomCmd();
        cmdUtil.registerCommand("bottom", bottomCmd, bottomCmd);
        NearCmd nearCmd = new NearCmd();
        cmdUtil.registerCommand("near", nearCmd, nearCmd);
        CoordsCmd coordsCmd = new CoordsCmd();
        cmdUtil.registerCommand("coords", coordsCmd, coordsCmd);

        MaceCmd maceCmd = new MaceCmd();
        cmdUtil.registerCommand("mace", maceCmd, maceCmd);

        MsgCmd msgCmd = new MsgCmd();
        cmdUtil.registerCommand("msg", msgCmd, msgCmd);
        cmdUtil.registerCommand("tell", msgCmd, msgCmd);
        cmdUtil.registerCommand("w", msgCmd, msgCmd);
        cmdUtil.registerCommand("whisper", msgCmd, msgCmd);
        cmdUtil.registerCommand("pm", msgCmd, msgCmd);
        ReplyCmd replyCmd = new ReplyCmd();
        cmdUtil.registerCommand("r", replyCmd, replyCmd);
        cmdUtil.registerCommand("reply", replyCmd, replyCmd);
        IgnoreCmd ignoreCmd = new IgnoreCmd();
        cmdUtil.registerCommand("ignore", ignoreCmd, ignoreCmd);

        GlowCmd glowCmd = new GlowCmd();
        cmdUtil.registerCommand("glow", glowCmd, glowCmd);

        cmdUtil.printSummary();
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
        if (glowManager != null) {
            glowManager.cleanup();
        }
    }
}

