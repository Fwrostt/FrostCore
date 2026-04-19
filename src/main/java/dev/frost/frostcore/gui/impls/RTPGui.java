package dev.frost.frostcore.gui.impls;

import dev.frost.frostcore.rtp.RTPConfig;
import dev.frost.frostcore.rtp.RTPService;

import dev.frost.frostcore.gui.Button;
import dev.frost.frostcore.gui.Gui;
import dev.frost.frostcore.gui.GuiItem;
import dev.frost.frostcore.manager.CooldownManager;
import dev.frost.frostcore.utils.EconomyUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Premium world-selection GUI for RTP.
 * <p>
 * 3-row chest with NO border. Configurable filler panes for background.
 * World items are smartly centered in the middle row with maximum visual balance.
 * Each item shows rich dynamic lore: cost, cooldown, radius, and cached pool info.
 * All display properties are configurable via rtp.yml.
 */
public class RTPGui extends Gui {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Player viewer;
    private final RTPService rtpService;
    private final RTPConfig config;

    /**
     * Pre-computed slot positions for centering N items in the middle row.
     * Row 1 = slots 9–17 (9 slots). Positions provide maximum visual balance.
     */
    private static final int[][] CENTERED_SLOTS = {
        {},                                // 0 worlds
        {13},                              // 1 world  — dead center
        {11, 15},                          // 2 worlds — symmetric
        {10, 13, 16},                      // 3 worlds — evenly spaced
        {10, 12, 14, 16},                  // 4 worlds
        {10, 11, 13, 15, 16},              // 5 worlds
        {10, 11, 12, 14, 15, 16},          // 6 worlds
        {10, 11, 12, 13, 14, 15, 16},      // 7 worlds
    };

    public RTPGui(Player viewer, RTPService rtpService) {
        super(MM.deserialize(rtpService.getConfig().getGuiTitle()), 3);
        this.viewer = viewer;
        this.rtpService = rtpService;
        this.config = rtpService.getConfig();
    }

    @Override
    public void populate() {
        // ── Fill ALL slots with background filler pane ────────────
        GuiItem filler = Button.of(config.getFillerMaterial())
            .name(config.getFillerName()).build();
        fill(filler);

        // ── Place world items with smart centering ───────────────
        List<String> worlds = new ArrayList<>(config.getEnabledWorlds());
        int count = Math.min(worlds.size(), 7);

        if (count == 0) return;

        int[] slots = count < CENTERED_SLOTS.length
            ? CENTERED_SLOTS[count]
            : CENTERED_SLOTS[7];

        for (int i = 0; i < slots.length && i < worlds.size(); i++) {
            String worldName = worlds.get(i);
            RTPConfig.WorldSettings settings = config.getWorldSettings(worldName);
            if (settings == null) continue;

            setItem(slots[i], buildWorldItem(worldName, settings));
        }
    }

    /**
     * Builds a rich GUI item for a world with static description + dynamic info.
     */
    private GuiItem buildWorldItem(String worldName, RTPConfig.WorldSettings settings) {
        List<String> lore = new ArrayList<>();

        // ── Static description from rtp.yml ──────────────────────
        List<String> descLore = settings.getGuiLore();
        if (descLore != null && !descLore.isEmpty()) {
            lore.addAll(descLore);
        } else {
            lore.add("");
        }

        // ── Dynamic info separator ──────────────────────────────
        if (!config.getLoreSeparator().trim().isEmpty()) {
            lore.add(config.getLoreSeparator());
            lore.add("");
        }

        // ── Cost line ────────────────────────────────────────────
        if (settings.getCost() > 0 && EconomyUtil.isEnabled()) {
            boolean bypass = viewer.hasPermission("frostcore.rtp.bypass.cost");
            if (bypass) {
                addLoreIfPresent(lore, config.getLoreCostBypass());
            } else {
                addLoreIfPresent(lore, config.getLoreCost().replace("{cost}", EconomyUtil.format(settings.getCost())));
            }
        } else {
            addLoreIfPresent(lore, config.getLoreCostFree());
        }

        // ── Cooldown line ────────────────────────────────────────
        boolean bypassCD = viewer.hasPermission("frostcore.rtp.bypass.cooldown");
        if (bypassCD) {
            addLoreIfPresent(lore, config.getLoreCooldownBypass());
        } else if (CooldownManager.isOnCooldown(viewer, "rtp")) {
            int remaining = CooldownManager.getRemainingTime(viewer, "rtp");
            addLoreIfPresent(lore, config.getLoreCooldown().replace("{time}", String.valueOf(remaining)));
        } else {
            addLoreIfPresent(lore, config.getLoreCooldownReady());
        }

        // ── Radius info ──────────────────────────────────────────
        addLoreIfPresent(lore, config.getLoreRadius()
            .replace("{min}", formatNumber(settings.getMinRadius()))
            .replace("{max}", formatNumber(settings.getMaxRadius())));

        // ── Pool info ────────────────────────────────────────────
        int pooled = rtpService.getLocationService().getPoolSize(worldName);
        addLoreIfPresent(lore, config.getLoreCached()
            .replace("{count}", String.valueOf(pooled))
            .replace("{s}", pooled == 1 ? "" : "s"));

        String footer = config.getLoreFooter();
        if (footer != null && !footer.trim().isEmpty()) {
            lore.add("");
            lore.add(footer);
        }

        // ── Build item ───────────────────────────────────────────
        Button.Builder builder = Button.of(settings.getGuiMaterial())
            .name(settings.getGuiName())
            .lore(lore);

        if (settings.isGuiGlow()) {
            builder.glow();
        }

        return builder
            .onClick(ctx -> {
                ctx.close();
                rtpService.requestRTP(ctx.getPlayer(), worldName);
            })
            .build();
    }

    private void addLoreIfPresent(List<String> lore, String line) {
        if (line != null && !line.trim().isEmpty()) {
            lore.add(line);
        }
    }

    /**
     * Formats a number with comma separators for readability.
     */
    private String formatNumber(int number) {
        return String.format("%,d", number);
    }
}
