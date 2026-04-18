package dev.frost.frostcore.chat.antispam;

import dev.frost.frostcore.chat.ChatContext;

public interface SpamCheck {
    
    /**
     * Executes the spam check against the context.
     * @param context the context to modify if a violation occurs
     * @return true if passed, false if violation triggered
     */
    boolean check(ChatContext context);

    /**
     * Reloads configuration variables.
     */
    void reload();
}
