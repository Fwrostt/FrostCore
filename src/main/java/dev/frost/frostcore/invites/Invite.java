package dev.frost.frostcore.invites;

import lombok.Getter;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Getter
public class Invite {

    private final InviteType type;
    private final UUID sender;
    private final UUID target;
    private final Map<String, String> metadata;
    private final long createdAt;
    private final long expiresAt;

    public Invite(InviteType type, UUID sender, UUID target,
                  Map<String, String> metadata, int expirySeconds) {
        this.type = type;
        this.sender = sender;
        this.target = target;
        this.metadata = metadata != null
                ? Collections.unmodifiableMap(metadata)
                : Collections.emptyMap();
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = createdAt + (expirySeconds * 1000L);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }

    public int getRemainingSeconds() {
        long remaining = expiresAt - System.currentTimeMillis();
        return remaining > 0 ? (int) (remaining / 1000) : 0;
    }

    
    public String getMeta(String key, String def) {
        return metadata.getOrDefault(key, def);
    }

    public String getMeta(String key) {
        return metadata.get(key);
    }
}
