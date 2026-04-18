package dev.frost.frostcore.bounty;

import dev.frost.frostcore.bounty.model.Bounty;
import dev.frost.frostcore.bounty.model.BountyContributor;
import dev.frost.frostcore.database.DatabaseManager;
import dev.frost.frostcore.database.DatabaseType;
import dev.frost.frostcore.utils.FrostLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles all SQL for the bounty system.
 *
 * <p>All public methods are <em>blocking</em> and must only be called from async
 * Bukkit scheduler tasks or background threads. Never call from the main thread.</p>
 */
public class BountyRepository {

    private final DatabaseManager db;

    public BountyRepository(DatabaseManager db) {
        this.db = db;
    }

    // ── Schema ─────────────────────────────────────────────────────────────────

    public void createTables() {
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS bounties (
                    target_uuid  VARCHAR(36)  NOT NULL PRIMARY KEY,
                    target_name  VARCHAR(64)  NOT NULL,
                    total_amount DOUBLE       NOT NULL DEFAULT 0,
                    status       VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
                    created_at   BIGINT       NOT NULL,
                    updated_at   BIGINT       NOT NULL,
                    expires_at   BIGINT       NOT NULL DEFAULT -1
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS bounty_contributors (
                    %s
                    target_uuid       VARCHAR(36)  NOT NULL,
                    contributor_uuid  VARCHAR(36)  NOT NULL,
                    contributor_name  VARCHAR(64)  NOT NULL,
                    amount            DOUBLE       NOT NULL,
                    created_at        BIGINT       NOT NULL
                )
            """.formatted(db.getType() == DatabaseType.MYSQL 
                    ? "id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT," 
                    : "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,"));

            if (db.getType() == DatabaseType.MYSQL) {
                try {
                    stmt.executeUpdate("""
                        CREATE INDEX IF NOT EXISTS idx_bc_target
                        ON bounty_contributors (target_uuid)
                    """);
                } catch (SQLException ignored) { /* already exists */ }
            } else {
                stmt.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_bc_target
                    ON bounty_contributors (target_uuid)
                """);
            }

            FrostLogger.info("[Bounty] Database tables verified.");
        } catch (SQLException e) {
            FrostLogger.error("[Bounty] Failed to create tables!", e);
        }
    }

    // ── Load ───────────────────────────────────────────────────────────────────

    /** Loads all ACTIVE bounties and populates their contributor lists. */
    public List<Bounty> loadActiveBounties() {
        List<Bounty> bounties = new ArrayList<>();

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM bounties WHERE status = 'ACTIVE'");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Bounty b = mapBounty(rs);
                // Inline contributor load to avoid N+1 with a second query per bounty
                bounties.add(b);
            }

        } catch (SQLException e) {
            FrostLogger.error("[Bounty] Failed to load active bounties!", e);
        }

        // Bulk-load all contributors, then fan out to each bounty object
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM bounty_contributors ORDER BY target_uuid, created_at ASC");
             ResultSet rs = ps.executeQuery()) {

            // Map: target_uuid → bounty for quick lookup
            java.util.Map<UUID, Bounty> idx = new java.util.HashMap<>();
            for (Bounty b : bounties) idx.put(b.getTargetUuid(), b);

            while (rs.next()) {
                UUID targetUuid = UUID.fromString(rs.getString("target_uuid"));
                Bounty b = idx.get(targetUuid);
                if (b != null) {
                    b.addContributor(mapContributor(rs));
                }
            }

        } catch (SQLException e) {
            FrostLogger.error("[Bounty] Failed to load contributors!", e);
        }

        return bounties;
    }

    // ── Upsert bounty header ───────────────────────────────────────────────────

    /** Inserts a new ACTIVE bounty row. Returns false on failure. */
    public boolean insertBounty(Bounty bounty) {
        String sql = db.getType() == DatabaseType.MYSQL
                ? """
                  INSERT INTO bounties
                    (target_uuid, target_name, total_amount, status, created_at, updated_at, expires_at)
                  VALUES (?, ?, ?, 'ACTIVE', ?, ?, ?)
                  ON DUPLICATE KEY UPDATE
                    total_amount = total_amount + VALUES(total_amount),
                    updated_at   = VALUES(updated_at),
                    expires_at   = VALUES(expires_at)
                  """
                : """
                  INSERT INTO bounties
                    (target_uuid, target_name, total_amount, status, created_at, updated_at, expires_at)
                  VALUES (?, ?, ?, 'ACTIVE', ?, ?, ?)
                  ON CONFLICT(target_uuid) DO UPDATE SET
                    total_amount = total_amount + excluded.total_amount,
                    updated_at   = excluded.updated_at,
                    expires_at   = excluded.expires_at
                  """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bounty.getTargetUuid().toString());
            ps.setString(2, bounty.getTargetName());
            ps.setDouble(3, bounty.getTotalAmount());
            ps.setLong(4, bounty.getCreatedAt());
            ps.setLong(5, bounty.getUpdatedAt());
            ps.setLong(6, bounty.getExpiresAt());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            FrostLogger.error("[Bounty] Failed to insert bounty for " + bounty.getTargetName(), e);
            return false;
        }
    }

    /** Atomically adds deltaAmount to the live bounty. */
    public void updateBountyAmount(UUID targetUuid, double deltaAmount) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE bounties SET total_amount = total_amount + ?, updated_at = ? WHERE target_uuid = ?")) {
            ps.setDouble(1, deltaAmount);
            ps.setLong(2, System.currentTimeMillis());
            ps.setString(3, targetUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("[Bounty] Failed to update amount for " + targetUuid, e);
        }
    }

    // ── Status transitions ─────────────────────────────────────────────────────

    public void markClaimed(UUID targetUuid) {
        updateStatus(targetUuid, BountyStatus.CLAIMED);
    }

    public void markExpired(UUID targetUuid) {
        updateStatus(targetUuid, BountyStatus.EXPIRED);
    }

    private void updateStatus(UUID targetUuid, BountyStatus status) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE bounties SET status = ?, updated_at = ? WHERE target_uuid = ?")) {
            ps.setString(1, status.name());
            ps.setLong(2, System.currentTimeMillis());
            ps.setString(3, targetUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("[Bounty] Failed to mark " + status + " for " + targetUuid, e);
        }
    }

    /** Deletes all rows for a target (both header + contributors). */
    public void deleteBounty(UUID targetUuid) {
        String uuidStr = targetUuid.toString();
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM bounty_contributors WHERE target_uuid = ?")) {
                    ps.setString(1, uuidStr);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM bounties WHERE target_uuid = ?")) {
                    ps.setString(1, uuidStr);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                FrostLogger.error("[Bounty] Failed to delete bounty for " + targetUuid, e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            FrostLogger.error("[Bounty] Failed to acquire connection for delete " + targetUuid, e);
        }
    }

    // ── Contributors ───────────────────────────────────────────────────────────

    /**
     * Inserts a new contributor row and returns the generated ID, or -1 on failure.
     */
    public long insertContributor(BountyContributor c) {
        String sql = "INSERT INTO bounty_contributors "
                + "(target_uuid, contributor_uuid, contributor_name, amount, created_at) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getTargetUuid().toString());
            ps.setString(2, c.getContributorUuid().toString());
            ps.setString(3, c.getContributorName());
            ps.setDouble(4, c.getAmount());
            ps.setLong(5, c.getCreatedAt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        } catch (SQLException e) {
            FrostLogger.error("[Bounty] Failed to insert contributor for " + c.getTargetUuid(), e);
        }
        return -1;
    }

    /** Atomically adds deltaAmount to an existing contributor row. */
    public void updateContributorAmount(long contributorId, double deltaAmount) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE bounty_contributors SET amount = amount + ? WHERE id = ?")) {
            ps.setDouble(1, deltaAmount);
            ps.setLong(2, contributorId);
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("[Bounty] Failed to update contributor " + contributorId, e);
        }
    }

    // ── Mappers ─────────────────────────────────────────────────────────────────

    private Bounty mapBounty(ResultSet rs) throws SQLException {
        return new Bounty(
                UUID.fromString(rs.getString("target_uuid")),
                rs.getString("target_name"),
                rs.getDouble("total_amount"),
                BountyStatus.valueOf(rs.getString("status")),
                rs.getLong("created_at"),
                rs.getLong("updated_at"),
                rs.getLong("expires_at")
        );
    }

    private BountyContributor mapContributor(ResultSet rs) throws SQLException {
        return new BountyContributor(
                rs.getLong("id"),
                UUID.fromString(rs.getString("target_uuid")),
                UUID.fromString(rs.getString("contributor_uuid")),
                rs.getString("contributor_name"),
                rs.getDouble("amount"),
                rs.getLong("created_at")
        );
    }
}
