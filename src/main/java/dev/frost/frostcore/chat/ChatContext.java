package dev.frost.frostcore.chat;

import org.bukkit.entity.Player;

public class ChatContext {

    private final Player player;
    private final String originalMessage;
    private String message;
    
    private boolean cancelled = false;
    private ViolationType violationType = null;
    private String violationMessageRef = null;
    private boolean punish = true;
    private int weight = 1;
    private java.util.Map<String, String> placeholders = java.util.Collections.emptyMap();

    public ChatContext(Player player, String originalMessage) {
        this.player = player;
        this.originalMessage = originalMessage;
        this.message = originalMessage;
    }

    public Player getPlayer() {
        return player;
    }

    public String getOriginalMessage() {
        return originalMessage;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public ViolationType getViolationType() {
        return violationType;
    }

    public String getViolationMessageRef() {
        return violationMessageRef;
    }

    public boolean shouldPunish() {
        return punish;
    }

    public int getWeight() {
        return weight;
    }

    public java.util.Map<String, String> getPlaceholders() {
        return placeholders;
    }

    public void flagViolation(ViolationType type, String messageRef, boolean punish, int weight) {
        flagViolation(type, messageRef, punish, weight, java.util.Collections.emptyMap());
    }

    public void flagViolation(ViolationType type, String messageRef, boolean punish, int weight, java.util.Map<String, String> placeholders) {
        this.cancelled = true;
        this.violationType = type;
        this.violationMessageRef = messageRef;
        this.punish = punish;
        this.weight = weight;
        this.placeholders = placeholders != null ? placeholders : java.util.Collections.emptyMap();
    }
}
