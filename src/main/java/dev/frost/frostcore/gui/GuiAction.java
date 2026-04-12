package dev.frost.frostcore.gui;


@FunctionalInterface
public interface GuiAction<T> {
    void execute(T context);
}

