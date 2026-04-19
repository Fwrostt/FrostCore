package dev.frost.frostcore.bounty;

import dev.frost.frostcore.manager.BountyManager;
import dev.frost.frostcore.bounty.model.Bounty;
import dev.frost.frostcore.utils.EconomyUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * PlaceholderAPI expansion for the bounty system.
 *
 * <p>All values served entirely from memory cache — zero DB calls.</p>
 *
 * <table>
 *   <tr><th>Placeholder</th><th>Returns</th></tr>
 *   <tr><td>%frostcore_bounty_has%</td><td>true/false — does this player have a bounty?</td></tr>
 *   <tr><td>%frostcore_bounty_amount%</td><td>total bounty amount (formatted compact)</td></tr>
 *   <tr><td>%frostcore_bounty_contributors%</td><td>number of contributors</td></tr>
 *   <tr><td>%frostcore_bounty_top_name%</td><td>name of #1 bounty target</td></tr>
 *   <tr><td>%frostcore_bounty_top_amount%</td><td>amount of #1 bounty (formatted)</td></tr>
 *   <tr><td>%frostcore_bounty_count%</td><td>total active bounty count</td></tr>
 * </table>
 */
public class BountyPlaceholderExpansion extends PlaceholderExpansion {

    private final BountyManager manager;

    public BountyPlaceholderExpansion(BountyManager manager) {
        this.manager = manager;
    }

    @Override
    public @NotNull String getAuthor() { return "frost"; }

    @Override
    public @NotNull String getIdentifier() { return "frostcore_bounty"; }

    @Override
    public @NotNull String getVersion() { return "1.0.0"; }

    @Override
    public boolean persist() { return true; }

    @Override
    public boolean canRegister() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (!manager.isEnabled()) return "disabled";

        return switch (params.toLowerCase()) {
            case "has"           -> player == null ? "false"
                    : String.valueOf(manager.hasBounty(player.getUniqueId()));

            case "amount"        -> {
                if (player == null) yield "0";
                Bounty b = manager.getBounty(player.getUniqueId());
                yield b != null ? EconomyUtil.formatCompact(b.getTotalAmount()) : "0";
            }

            case "contributors"  -> {
                if (player == null) yield "0";
                Bounty b = manager.getBounty(player.getUniqueId());
                yield b != null ? String.valueOf(b.getContributorCount()) : "0";
            }

            case "top_name"      -> {
                List<Bounty> lb = manager.getLeaderboard();
                yield lb.isEmpty() ? "None" : lb.get(0).getTargetName();
            }

            case "top_amount"    -> {
                List<Bounty> lb = manager.getLeaderboard();
                yield lb.isEmpty() ? "0" : EconomyUtil.formatCompact(lb.get(0).getTotalAmount());
            }

            case "count"         -> String.valueOf(manager.getActiveBountyCount());

            default              -> null;
        };
    }
}
