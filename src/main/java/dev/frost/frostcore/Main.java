package dev.frost.frostcore;

import dev.frost.frostcore.cmds.TeamCmd;
import dev.frost.frostcore.manager.ConfigManager;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.manager.TeamManager;
import dev.frost.frostcore.placeholderapi.TeamExpansion;
import dev.frost.frostcore.teams.Team;
import dev.frost.frostcore.utils.CmdUtil;
import lombok.Getter;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    @Getter private static Main instance;
    @Getter private static ConfigManager configManager;
    @Getter private static TeamManager teamManager;
    @Getter private static MessageManager messageManager;
    private static CmdUtil cmdUtil;

    @Override
    public void onEnable() {
        instance = this;
        setupClasses();
        setupCmds();
    }

    private void setupClasses() {
        configManager = ConfigManager.getInstance(this);
        messageManager = new MessageManager(this);
        teamManager = TeamManager.getInstance();
        cmdUtil = new CmdUtil();
        ConfigurationSerialization.registerClass(Team.class);
        if (!this.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this.getLogger().warning("PlaceholderAPI is not enabled! PlaceholderAPI is required for BlissSMP to function correctly.");
        } else {
            new TeamExpansion().register();
        }
    }

    private void setupCmds() {
        cmdUtil.registerCommand("team", new TeamCmd(), new TeamCmd());
    }

    @Override
    public void onDisable() {
        new TeamExpansion().unregister();
    }
}
