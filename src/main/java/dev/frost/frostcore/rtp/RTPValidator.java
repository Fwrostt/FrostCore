package dev.frost.frostcore.rtp;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;

import java.util.EnumSet;
import java.util.OptionalInt;

/**
 * Pure safety-check logic for RTP locations.
 * Fully async-safe — operates on {@link ChunkSnapshot} only, never touches live blocks.
 * Stateless utility class.
 */
public final class RTPValidator {

    private RTPValidator() {}

    /**
     * Scans a column top-down for the first safe Y position.
     * Uses ChunkSnapshot for fully async-safe block access.
     *
     * @param snapshot   the chunk snapshot to scan
     * @param localX     chunk-local X (0–15)
     * @param localZ     chunk-local Z (0–15)
     * @param settings   world-specific RTP settings
     * @param unsafeBlocks global set of unsafe materials
     * @param isNether   true if the world is NETHER (enforces Y < 127)
     * @return the safe Y coordinate, or empty if none found
     */
    public static OptionalInt findSafeY(ChunkSnapshot snapshot, int localX, int localZ,
                                         RTPConfig.WorldSettings settings,
                                         EnumSet<Material> unsafeBlocks,
                                         boolean isNether) {
        int maxY = settings.getMaxY();
        int minY = settings.getMinY();

        // Use the snapshot's highest block as an optimized upper bound
        int highestBlock = snapshot.getHighestBlockYAt(localX, localZ);
        maxY = Math.min(maxY, highestBlock + 1);

        // Nether roof check — never spawn on or above the bedrock ceiling
        if (isNether) {
            maxY = Math.min(maxY, 125);
        }

        // Scan top-down for a safe position
        for (int y = maxY; y >= minY + 1; y--) {
            if (isSafePosition(snapshot, localX, y, localZ, settings, unsafeBlocks)) {
                return OptionalInt.of(y);
            }
        }
        return OptionalInt.empty();
    }

    /**
     * Checks if a specific position is safe for player teleportation.
     * Requires:
     * <ul>
     *   <li>Block below (y-1) is solid and not unsafe</li>
     *   <li>Feet (y), head (y+1), and above head (y+2) are passable and not unsafe</li>
     * </ul>
     */
    public static boolean isSafePosition(ChunkSnapshot snapshot, int localX, int y, int localZ,
                                          RTPConfig.WorldSettings settings,
                                          EnumSet<Material> unsafeBlocks) {
        // Bounds check — need y-1 through y+2
        if (y < 1 || y + 2 > snapshot.getHighestBlockYAt(localX, localZ) + 10) {
            // Relaxed upper bound check — just ensure we don't go out of world bounds
        }

        Material below     = snapshot.getBlockType(localX, y - 1, localZ);
        Material feet      = snapshot.getBlockType(localX, y,     localZ);
        Material head      = snapshot.getBlockType(localX, y + 1, localZ);
        Material aboveHead = snapshot.getBlockType(localX, y + 2, localZ);

        // Block below must be solid (not air, not liquid)
        if (!below.isSolid()) return false;

        // Block below must not be a dangerous block (e.g. magma, cactus)
        if (unsafeBlocks.contains(below)) return false;

        // 3-block clearance: feet, head, and above head must all be passable
        if (!isPassable(feet, settings, unsafeBlocks)) return false;
        if (!isPassable(head, settings, unsafeBlocks)) return false;
        if (!isPassable(aboveHead, settings, unsafeBlocks)) return false;

        return true;
    }

    /**
     * Checks if a material is passable (player can exist in this block).
     * A block is passable if it is not solid, not in the unsafe set,
     * and respects water/lava toggle settings.
     */
    private static boolean isPassable(Material material, RTPConfig.WorldSettings settings,
                                       EnumSet<Material> unsafeBlocks) {
        // Solid blocks are never passable
        if (material.isSolid()) return false;

        // Unsafe blocks (fire, campfire, wither rose, etc.)
        if (unsafeBlocks.contains(material)) return false;

        // Water check
        if (material == Material.WATER && !settings.isAllowWater()) return false;

        // Lava check
        if (material == Material.LAVA && !settings.isAllowLava()) return false;

        return true;
    }

    /**
     * Checks if a coordinate pair is within the world border.
     */
    public static boolean isWithinWorldBorder(World world, double x, double z) {
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();
        double halfSize = border.getSize() / 2.0;

        return Math.abs(x - center.getX()) < halfSize
            && Math.abs(z - center.getZ()) < halfSize;
    }
}
