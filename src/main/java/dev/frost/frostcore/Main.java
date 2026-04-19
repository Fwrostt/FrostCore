package dev.frost.frostcore;

import dev.frost.frostcore.chat.ChatListener;
import dev.frost.frostcore.chat.ChatManager;
import dev.frost.frostcore.bounty.BountyPlaceholderExpansion;
import dev.frost.frostcore.bounty.BountyRepository;
import dev.frost.frostcore.bounty.BountyService;
import dev.frost.frostcore.cmds.player.BountyCmd;
import dev.frost.frostcore.cmds.teleport.RTPCommand;
import dev.frost.frostcore.listeners.BountyListener;
import dev.frost.frostcore.listeners.RTPListener;
import dev.frost.frostcore.rtp.RTPConfig;
import dev.frost.frostcore.rtp.RTPLocationService;
import dev.frost.frostcore.rtp.RTPService;
import dev.frost.frostcore.rtp.RTPStateTracker;
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
import dev.frost.frostcore.utils.EconomyUtil;
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
    @Getter private static ChatManager chatManager;
    @lombok.Getter private static dev.frost.frostcore.manager.CommandManager commandManager;
    @Getter private static BountyManager bountyManager;
    @Getter private static RTPService rtpService;
    private BountyService bountyService;
    private TeamExpansion teamExpansion;
    private BountyPlaceholderExpansion bountyExpansion;

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
            bountyExpansion = new BountyPlaceholderExpansion(bountyManager);
            bountyExpansion.register();
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

        glowManager = new GlowManager(this);

        chatManager = new ChatManager(this);

        commandManager = new dev.frost.frostcore.manager.CommandManager(this);

        // ── Bounty system ──────────────────────────────────────────────────────
        EconomyUtil.init();
        BountyRepository bountyRepo = new BountyRepository(databaseManager);
        bountyManager = new BountyManager(bountyRepo);
        bountyService = new BountyService(bountyManager);
        bountyManager.loadAsync();

        // ── RTP system ────────────────────────────────────────────────────────
        RTPConfig rtpConfig = new RTPConfig(this);
        RTPLocationService rtpLocationService = new RTPLocationService(this, rtpConfig);
        RTPStateTracker rtpStateTracker = new RTPStateTracker();
        rtpService = new RTPService(this, rtpConfig, rtpLocationService, rtpStateTracker);
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
        getServer().getPluginManager().registerEvents(new ChatListener(chatManager), this);
        // Bounty listener (registered only if economy is eventually enabled — checked inside)
        getServer().getPluginManager().registerEvents(
                new BountyListener(bountyManager, bountyService), this);

        // RTP listener
        getServer().getPluginManager().registerEvents(new RTPListener(rtpService), this);
    }

    private void setupCmds() {
        commandManager.registerCommand("team", new TeamCmd(), new TeamCmd());

        TpaCmd tpaCmd = new TpaCmd();
        commandManager.registerCommand("tpa", tpaCmd, tpaCmd);

        TpaHereCmd tpahereCmd = new TpaHereCmd();
        commandManager.registerCommand("tpahere", tpahereCmd, tpahereCmd);

        TpaToggleCmd tpaToggleCmd = new TpaToggleCmd();
        commandManager.registerCommand("tpatoggle", tpaToggleCmd, tpaToggleCmd);
        commandManager.registerCommand("tpaoff", tpaToggleCmd, tpaToggleCmd);
        commandManager.registerCommand("tpaon", tpaToggleCmd, tpaToggleCmd);

        TpAcceptCmd tpacceptCmd = new TpAcceptCmd();
        commandManager.registerCommand("tpaccept", tpacceptCmd, tpacceptCmd);

        TpDeclineCmd tpdeclineCmd = new TpDeclineCmd();
        commandManager.registerCommand("tpdecline", tpdeclineCmd, tpdeclineCmd);

        WarpCmd warpCmd = new WarpCmd();
        commandManager.registerCommand("warp", warpCmd, warpCmd);

        WarpsCmd warpsCmd = new WarpsCmd();
        commandManager.registerCommand("warps", warpsCmd, warpsCmd);

        SpawnCmd spawnCmd = new SpawnCmd();
        commandManager.registerCommand("spawn", spawnCmd, spawnCmd);

        TpCmd tpCmd = new TpCmd();
        commandManager.registerCommand("tp", tpCmd, tpCmd);
        commandManager.registerCommand("tp2p", tpCmd, tpCmd);
        commandManager.registerCommand("tphere", tpCmd, tpCmd);

        OfflineTpCmd otpCmd = new OfflineTpCmd();
        commandManager.registerCommand("otp", otpCmd, otpCmd);
        commandManager.registerCommand("offlinetp", otpCmd, otpCmd);

        SetWarpCmd setwarpCmd = new SetWarpCmd();
        commandManager.registerCommand("setwarp", setwarpCmd, setwarpCmd);
        commandManager.registerCommand("delwarp", setwarpCmd, setwarpCmd);

        SetHomeCmd sethomeCmd = new SetHomeCmd();
        commandManager.registerCommand("sethome", sethomeCmd, sethomeCmd);

        HomeCmd homeCmd = new HomeCmd();
        commandManager.registerCommand("home", homeCmd, homeCmd);

        DelHomeCmd delhomeCmd = new DelHomeCmd();
        commandManager.registerCommand("delhome", delhomeCmd, delhomeCmd);

        RenameHomeCmd renamehomeCmd = new RenameHomeCmd();
        commandManager.registerCommand("renamehome", renamehomeCmd, renamehomeCmd);

        HomesCmd homesCmd = new HomesCmd();
        commandManager.registerCommand("homes", homesCmd, homesCmd);

        SetSpawnCmd setspawnCmd = new SetSpawnCmd();
        commandManager.registerCommand("setspawn", setspawnCmd, setspawnCmd);

        FrostCoreCmd frostCoreCmd = new FrostCoreCmd();
        commandManager.registerCommand("frostcore", frostCoreCmd, frostCoreCmd);

        GamemodeCmd gmCmd = new GamemodeCmd();
        commandManager.registerCommand("gm", gmCmd, gmCmd);
        commandManager.registerCommand("gms", gmCmd, gmCmd);
        commandManager.registerCommand("gmc", gmCmd, gmCmd);
        commandManager.registerCommand("gma", gmCmd, gmCmd);
        commandManager.registerCommand("gmsp", gmCmd, gmCmd);

        FlyCmd flyCmd = new FlyCmd();
        commandManager.registerCommand("fly", flyCmd, flyCmd);
        HealCmd healCmd = new HealCmd();
        commandManager.registerCommand("heal", healCmd, healCmd);
        FeedCmd feedCmd = new FeedCmd();
        commandManager.registerCommand("feed", feedCmd, feedCmd);
        GodCmd godCmd = new GodCmd();
        commandManager.registerCommand("god", godCmd, godCmd);
        ClearCmd clearCmd = new ClearCmd();
        commandManager.registerCommand("clear", clearCmd, clearCmd);
        SpeedCmd speedCmd = new SpeedCmd();
        commandManager.registerCommand("speed", speedCmd, speedCmd);

        NickCmd nickCmd = new NickCmd();
        commandManager.registerCommand("nick", nickCmd, nickCmd);
        commandManager.registerCommand("unnick", nickCmd, nickCmd);

        ItemRenameCmd itemRenameCmd = new ItemRenameCmd();
        commandManager.registerCommand("itemrename", itemRenameCmd, itemRenameCmd);
        LoreCmd loreCmd = new LoreCmd();
        commandManager.registerCommand("lore", loreCmd, loreCmd);
        RepairCmd repairCmd = new RepairCmd();
        commandManager.registerCommand("repair", repairCmd, repairCmd);

        
        MuteCmd muteCmd = new MuteCmd();
        commandManager.registerCommand("mute", muteCmd, muteCmd);
        TempMuteCmd tempMuteCmd = new TempMuteCmd();
        commandManager.registerCommand("tempmute", tempMuteCmd, tempMuteCmd);
        UnmuteCmd unmuteCmd = new UnmuteCmd();
        commandManager.registerCommand("unmute", unmuteCmd, unmuteCmd);
        LockchatCmd lockchatCmd = new LockchatCmd();
        commandManager.registerCommand("lockchat", lockchatCmd, lockchatCmd);
        UnlockchatCmd unlockchatCmd = new UnlockchatCmd();
        commandManager.registerCommand("unlockchat", unlockchatCmd, unlockchatCmd);
        FreezeCmd freezeCmd = new FreezeCmd();
        commandManager.registerCommand("freeze", freezeCmd, freezeCmd);
        ScreenshareCmd screenshareCmd = new ScreenshareCmd();
        commandManager.registerCommand("screenshare", screenshareCmd, screenshareCmd);
        commandManager.registerCommand("ss", screenshareCmd, screenshareCmd);

        KickCmd kickCmd = new KickCmd();
        commandManager.registerCommand("kick", kickCmd, kickCmd);
        BanCmd banCmd = new BanCmd();
        commandManager.registerCommand("ban", banCmd, banCmd);
        TempBanCmd tempBanCmd = new TempBanCmd();
        commandManager.registerCommand("tempban", tempBanCmd, tempBanCmd);
        UnbanCmd unbanCmd = new UnbanCmd();
        commandManager.registerCommand("unban", unbanCmd, unbanCmd);
        WarnCmd warnCmd = new WarnCmd();
        commandManager.registerCommand("warn", warnCmd, warnCmd);
        UnwarnCmd unwarnCmd = new UnwarnCmd();
        commandManager.registerCommand("unwarn", unwarnCmd, unwarnCmd);

        IpBanCmd ipBanCmd = new IpBanCmd();
        commandManager.registerCommand("ipban", ipBanCmd, ipBanCmd);
        commandManager.registerCommand("banip", ipBanCmd, ipBanCmd);
        IpMuteCmd ipMuteCmd = new IpMuteCmd();
        commandManager.registerCommand("ipmute", ipMuteCmd, ipMuteCmd);
        commandManager.registerCommand("muteip", ipMuteCmd, ipMuteCmd);

        
        JailCmd jailCmd = new JailCmd();
        commandManager.registerCommand("jail", jailCmd, jailCmd);
        UnjailCmd unjailCmd = new UnjailCmd();
        commandManager.registerCommand("unjail", unjailCmd, unjailCmd);
        SetJailCmd setJailCmd = new SetJailCmd();
        commandManager.registerCommand("setjail", setJailCmd, setJailCmd);
        DelJailCmd delJailCmd = new DelJailCmd();
        commandManager.registerCommand("deljail", delJailCmd, delJailCmd);

        
        ReportCmd reportCmd = new ReportCmd();
        commandManager.registerCommand("report", reportCmd, reportCmd);

        
        CheckBanCmd checkBanCmd = new CheckBanCmd();
        commandManager.registerCommand("checkban", checkBanCmd, checkBanCmd);
        CheckPunishmentCmd checkMuteCmd = new CheckPunishmentCmd("MUTE");
        commandManager.registerCommand("checkmute", checkMuteCmd, checkMuteCmd);
        CheckPunishmentCmd checkWarnCmd = new CheckPunishmentCmd("WARN");
        commandManager.registerCommand("checkwarn", checkWarnCmd, checkWarnCmd);
        HistoryCmd historyCmd = new HistoryCmd();
        commandManager.registerCommand("history", historyCmd, historyCmd);
        StaffHistoryCmd staffHistoryCmd = new StaffHistoryCmd();
        commandManager.registerCommand("staffhistory", staffHistoryCmd, staffHistoryCmd);
        WarningsCmd warningsCmd = new WarningsCmd();
        commandManager.registerCommand("warnings", warningsCmd, warningsCmd);

        
        PunishmentListCmd banListCmd = new PunishmentListCmd("BAN", "Active Bans");
        commandManager.registerCommand("banlist", banListCmd, banListCmd);
        PunishmentListCmd muteListCmd = new PunishmentListCmd("MUTE", "Active Mutes");
        commandManager.registerCommand("mutelist", muteListCmd, muteListCmd);
        PunishmentListCmd warnListCmd = new PunishmentListCmd("WARN", "Active Warnings");
        commandManager.registerCommand("warnlist", warnListCmd, warnListCmd);

        
        AltsCmd altsCmd = new AltsCmd();
        commandManager.registerCommand("alts", altsCmd, altsCmd);
        
        NameHistoryCmd nameHistoryCmd = new NameHistoryCmd();
        commandManager.registerCommand("namehistory", nameHistoryCmd, null);
        IpHistoryCmd ipHistoryCmd = new IpHistoryCmd();
        commandManager.registerCommand("iphistory", ipHistoryCmd, ipHistoryCmd);

        
        ReportsCmd reportsCmd = new ReportsCmd();
        commandManager.registerCommand("reports", reportsCmd, reportsCmd);

        
        LockdownCmd lockdownCmd = new LockdownCmd();
        commandManager.registerCommand("lockdown", lockdownCmd, lockdownCmd);
        PruneHistoryCmd pruneHistoryCmd = new PruneHistoryCmd();
        commandManager.registerCommand("prunehistory", pruneHistoryCmd, pruneHistoryCmd);
        StaffRollbackCmd staffRollbackCmd = new StaffRollbackCmd();
        commandManager.registerCommand("staffrollback", staffRollbackCmd, staffRollbackCmd);
        ModerationCmd moderationCmd = new ModerationCmd();
        commandManager.registerCommand("moderation", moderationCmd, moderationCmd);
        StaffChatCmd staffChatCmd = new StaffChatCmd();
        commandManager.registerCommand("staffchat", staffChatCmd, staffChatCmd);
        commandManager.registerCommand("sc", staffChatCmd, staffChatCmd);

        SudoCmd sudoCmd = new SudoCmd();
        commandManager.registerCommand("sudo", sudoCmd, sudoCmd);
        BroadcastCmd broadcastCmd = new BroadcastCmd();
        commandManager.registerCommand("broadcast", broadcastCmd, broadcastCmd);
        ChatCmd chatCmd = new ChatCmd();
        commandManager.registerCommand("chat", chatCmd, chatCmd);
        TimeCmd timeCmd = new TimeCmd();
        commandManager.registerCommand("day", timeCmd, timeCmd);
        commandManager.registerCommand("night", timeCmd, timeCmd);
        commandManager.registerCommand("time", timeCmd, timeCmd);
        WeatherCmd weatherCmd = new WeatherCmd();
        commandManager.registerCommand("weather", weatherCmd, weatherCmd);

        BackCmd backCmd = new BackCmd();
        commandManager.registerCommand("back", backCmd, backCmd);

        InvseeCmd invseeCmd = new InvseeCmd();
        commandManager.registerCommand("invsee", invseeCmd, invseeCmd);
        EnderchestCmd ecCmd = new EnderchestCmd();
        commandManager.registerCommand("enderchest", ecCmd, ecCmd);
        commandManager.registerCommand("ec", ecCmd, ecCmd);
        HatCmd hatCmd = new HatCmd();
        commandManager.registerCommand("hat", hatCmd, hatCmd);
        WhoisCmd whoisCmd = new WhoisCmd();
        commandManager.registerCommand("whois", whoisCmd, whoisCmd);
        SeenCmd seenCmd = new SeenCmd();
        commandManager.registerCommand("seen", seenCmd, seenCmd);
        SmiteCmd smiteCmd = new SmiteCmd();
        commandManager.registerCommand("smite", smiteCmd, smiteCmd);
        VanishCmd vanishCmd = new VanishCmd();
        commandManager.registerCommand("vanish", vanishCmd, vanishCmd);
        commandManager.registerCommand("v", vanishCmd, vanishCmd);
        TpallCmd tpallCmd = new TpallCmd();
        commandManager.registerCommand("tpall", tpallCmd, tpallCmd);
        PingCmd pingCmd = new PingCmd();
        commandManager.registerCommand("ping", pingCmd, pingCmd);
        SkullCmd skullCmd = new SkullCmd();
        commandManager.registerCommand("skull", skullCmd, skullCmd);
        SocialSpyCmd ssCmd = new SocialSpyCmd();
        commandManager.registerCommand("socialspy", ssCmd, ssCmd);
        RamCmd ramCmd = new RamCmd();
        commandManager.registerCommand("ram", ramCmd, ramCmd);
        TopCmd topCmd = new TopCmd();
        commandManager.registerCommand("top", topCmd, topCmd);
        BottomCmd bottomCmd = new BottomCmd();
        commandManager.registerCommand("bottom", bottomCmd, bottomCmd);
        NearCmd nearCmd = new NearCmd();
        commandManager.registerCommand("near", nearCmd, nearCmd);
        CoordsCmd coordsCmd = new CoordsCmd();
        commandManager.registerCommand("coords", coordsCmd, coordsCmd);

        MaceCmd maceCmd = new MaceCmd();
        commandManager.registerCommand("mace", maceCmd, maceCmd);

        MsgCmd msgCmd = new MsgCmd();
        commandManager.registerCommand("msg", msgCmd, msgCmd);
        commandManager.registerCommand("tell", msgCmd, msgCmd);
        commandManager.registerCommand("w", msgCmd, msgCmd);
        commandManager.registerCommand("whisper", msgCmd, msgCmd);
        commandManager.registerCommand("pm", msgCmd, msgCmd);
        ReplyCmd replyCmd = new ReplyCmd();
        commandManager.registerCommand("r", replyCmd, replyCmd);
        commandManager.registerCommand("reply", replyCmd, replyCmd);
        IgnoreCmd ignoreCmd = new IgnoreCmd();
        commandManager.registerCommand("ignore", ignoreCmd, ignoreCmd);

        MsgToggleCmd msgToggleCmd = new MsgToggleCmd();
        commandManager.registerCommand("msgtoggle", msgToggleCmd, msgToggleCmd);
        commandManager.registerCommand("msgoff", msgToggleCmd, msgToggleCmd);
        commandManager.registerCommand("msgon", msgToggleCmd, msgToggleCmd);

        ChatToggleCmd chatToggleCmd = new ChatToggleCmd();
        commandManager.registerCommand("chattoggle", chatToggleCmd, chatToggleCmd);
        commandManager.registerCommand("chatoff", chatToggleCmd, chatToggleCmd);
        commandManager.registerCommand("chaton", chatToggleCmd, chatToggleCmd);

        GlowCmd glowCmd = new GlowCmd();
        commandManager.registerCommand("glow", glowCmd, glowCmd);

        BountyCmd bountyCmd = new BountyCmd(bountyManager, bountyService);
        commandManager.registerCommand("bounty", bountyCmd, bountyCmd);

        RTPCommand rtpCmd = new RTPCommand(rtpService);
        commandManager.registerCommand("rtp", rtpCmd, rtpCmd);
        commandManager.registerCommand("randomtp", rtpCmd, rtpCmd);
        commandManager.registerCommand("wild", rtpCmd, rtpCmd);

        commandManager.reload();
    }

    public void onDisable() {
        dev.frost.frostcore.utils.SignPrompt.cleanupAll();
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
        if (bountyManager != null) {
            bountyManager.shutdown();
        }
        if (bountyExpansion != null) {
            bountyExpansion.unregister();
        }
        if (rtpService != null) {
            rtpService.shutdown();
        }
    }
}

