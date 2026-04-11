package dev.frost.frostcore.gui;

/**
 * Functional interface for all GUI callbacks (click, open, close).
 *
 * @param <T> The context type passed to the callback.
 *            For clicks: {@link ClickContext}. For open/close: {@link org.bukkit.entity.Player}.
 */
@FunctionalInterface
public interface GuiAction<T> {
    void execute(T context);
}

