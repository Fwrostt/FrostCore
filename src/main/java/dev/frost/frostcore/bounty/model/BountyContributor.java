package dev.frost.frostcore.bounty.model;

import java.util.UUID;

/**
 * Represents a single player's contribution to a bounty.
 */
public class BountyContributor {

    private final long   id;
    private final UUID   targetUuid;
    private final UUID   contributorUuid;
    private final String contributorName;
    private volatile double amount;
    private final long   createdAt;

    public BountyContributor(long id, UUID targetUuid, UUID contributorUuid,
                             String contributorName, double amount, long createdAt) {
        this.id               = id;
        this.targetUuid       = targetUuid;
        this.contributorUuid  = contributorUuid;
        this.contributorName  = contributorName;
        this.amount           = amount;
        this.createdAt        = createdAt;
    }

    public long   getId()               { return id; }
    public UUID   getTargetUuid()       { return targetUuid; }
    public UUID   getContributorUuid()  { return contributorUuid; }
    public String getContributorName()  { return contributorName; }
    public double getAmount()           { return amount; }
    public long   getCreatedAt()        { return createdAt; }

    public synchronized void addAmount(double extra) { this.amount += extra; }
}
