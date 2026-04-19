package dev.frost.frostcore.rtp;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.utils.FrostLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.*;

/**
 * Async location search engine with pre-generation pool.
 * <p>
 * Uses circular random distribution for uniform density,
 * Paper's async chunk API for non-blocking chunk loads, and
 * {@link org.bukkit.ChunkSnapshot} for fully async-safe block scanning.
 * <p>
 * A background task continuously refills per-world location pools
 * so that most RTP requests can be served instantly from cache.
 */
public class RTPLocationService {

    private final Main plugin;
    private final RTPConfig config;
    private final Map<String, ConcurrentLinkedQueue<Location>> locationPools = new ConcurrentHashMap<>();
    private BukkitTask refillTask;

    public RTPLocationService(Main plugin, RTPConfig config) {
        this.plugin = plugin;
        this.config = config;
        startPoolRefill();
    }

    // ── Pool management ──────────────────────────────────────────

    /**
     * Starts the background pool refill task.
     * Runs on the main thread (cheap check), kicks off async generation
     * for any world whose pool is below target size.
     */
    private void startPoolRefill() {
        refillTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (String worldName : config.getEnabledWorlds()) {
                ConcurrentLinkedQueue<Location> pool =
                    locationPools.computeIfAbsent(worldName, k -> new ConcurrentLinkedQueue<>());

                if (pool.size() >= config.getPoolSize()) continue;

                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;

                RTPConfig.WorldSettings settings = config.getWorldSettings(worldName);
                if (settings == null) continue;

                // Generate one location per cycle per world to avoid burst loading
                generateLocationAsync(world, settings).thenAccept(loc -> {
                    if (loc != null) {
                        pool.offer(loc);
                    }
                });
            }
        }, 100L, config.getPoolRefillInterval());
    }

    // ── Public API ───────────────────────────────────────────────

    /**
     * Retrieves a safe location, trying the pre-generated pool first.
     * Falls back to on-demand async generation if the pool is empty.
     *
     * @param world    the target world
     * @param settings per-world RTP settings
     * @return a future that completes with a safe Location, or null if all attempts exhausted
     */
    public CompletableFuture<Location> pollLocation(World world, RTPConfig.WorldSettings settings) {
        ConcurrentLinkedQueue<Location> pool = locationPools.get(settings.getWorldName());
        if (pool != null) {
            Location cached = pool.poll();
            if (cached != null) {
                return CompletableFuture.completedFuture(cached);
            }
        }
        // Pool empty — on-demand generation
        return generateLocationAsync(world, settings);
    }

    /**
     * Generates a safe location asynchronously.
     * Pre-generates a batch of random coordinates and tries each one
     * via async chunk loading + ChunkSnapshot scanning.
     *
     * @param world    the target world
     * @param settings per-world RTP settings
     * @return a future that completes with a safe Location, or null
     */
    public CompletableFuture<Location> generateLocationAsync(World world, RTPConfig.WorldSettings settings) {
        CompletableFuture<Location> future = new CompletableFuture<>();

        int maxAttempts = config.getMaxAttempts();
        int[][] coords = new int[maxAttempts][2];
        for (int i = 0; i < maxAttempts; i++) {
            coords[i] = generateCircularCoords(settings);
        }

        boolean isNether = world.getEnvironment() == World.Environment.NETHER;
        tryNextCoordinate(world, settings, coords, 0, isNether, future);
        return future;
    }

    // ── Recursive async coordinate search ────────────────────────

    private void tryNextCoordinate(World world, RTPConfig.WorldSettings settings,
                                    int[][] coords, int attempt, boolean isNether,
                                    CompletableFuture<Location> future) {
        if (attempt >= coords.length) {
            future.complete(null);
            return;
        }

        int x = coords[attempt][0];
        int z = coords[attempt][1];

        // World border check (fast, no IO)
        if (!RTPValidator.isWithinWorldBorder(world, x, z)) {
            tryNextCoordinate(world, settings, coords, attempt + 1, isNether, future);
            return;
        }

        world.getChunkAtAsync(x >> 4, z >> 4)
            // getChunkSnapshot runs on main thread (where getChunkAtAsync completes) — very fast
            .thenApply(chunk -> chunk.getChunkSnapshot(true, true, false))
            // Scanning runs on ForkJoinPool — fully async, zero main-thread cost
            .thenApplyAsync(snapshot -> {
                int localX = x & 15;
                int localZ = z & 15;
                return RTPValidator.findSafeY(snapshot, localX, localZ, settings,
                    config.getUnsafeBlocks(), isNether);
            })
            .thenAccept(safeY -> {
                if (safeY.isPresent()) {
                    Location location = new Location(world, x + 0.5, safeY.getAsInt(), z + 0.5);
                    // Random yaw for natural look
                    location.setYaw(ThreadLocalRandom.current().nextFloat() * 360f);
                    future.complete(location);
                } else {
                    // Schedule retry on main thread (safe for next getChunkAtAsync call)
                    Bukkit.getScheduler().runTask(plugin, () ->
                        tryNextCoordinate(world, settings, coords, attempt + 1, isNether, future));
                }
            })
            .exceptionally(ex -> {
                FrostLogger.warn("[RTP] Chunk load failed at " + x + ", " + z + ": " + ex.getMessage());
                Bukkit.getScheduler().runTask(plugin, () ->
                    tryNextCoordinate(world, settings, coords, attempt + 1, isNether, future));
                return null;
            });
    }

    // ── Circular random distribution ─────────────────────────────

    /**
     * Generates uniformly distributed random coordinates within a circular area.
     * Uses sqrt distribution to prevent center clustering.
     * Supports a minimum radius for donut-shaped distribution.
     */
    private int[] generateCircularCoords(RTPConfig.WorldSettings settings) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        int minRadius = settings.getMinRadius();
        int maxRadius = settings.getMaxRadius();

        // Uniform circular distribution:
        // r = sqrt(random * (maxR² - minR²) + minR²)
        double minR2 = (double) minRadius * minRadius;
        double maxR2 = (double) maxRadius * maxRadius;
        double r = Math.sqrt(random.nextDouble() * (maxR2 - minR2) + minR2);
        double theta = random.nextDouble() * 2.0 * Math.PI;

        int x = settings.getCenterX() + (int) (r * Math.cos(theta));
        int z = settings.getCenterZ() + (int) (r * Math.sin(theta));

        return new int[]{x, z};
    }

    // ── Utility ──────────────────────────────────────────────────

    public int getPoolSize(String worldName) {
        ConcurrentLinkedQueue<Location> pool = locationPools.get(worldName);
        return pool != null ? pool.size() : 0;
    }

    public void clearPools() {
        locationPools.values().forEach(ConcurrentLinkedQueue::clear);
    }

    public void shutdown() {
        if (refillTask != null && !refillTask.isCancelled()) {
            refillTask.cancel();
        }
        locationPools.clear();
    }
}
