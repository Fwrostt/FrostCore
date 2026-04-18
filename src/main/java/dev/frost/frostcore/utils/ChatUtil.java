package dev.frost.frostcore.utils;

public class ChatUtil {

    /**
     * Normalizes a message string by performing aggressive checks against obfuscation:
     * 1. Lowercases the string.
     * 2. Replaces common leetspeak characters efficiently.
     * 3. Collapses sequentially repeated characters down to singles (e.g. "heyyy" -> "hey").
     * 4. Strips all non-alphanumeric characters.
     */
    public static String normalize(String message) {
        if (message == null || message.isEmpty()) return "";

        // 1. Lowercase
        String n = message.toLowerCase();

        // 2. Map leetspeak
        n = n.replace('0', 'o')
             .replace('1', 'l')
             .replace('3', 'e')
             .replace('4', 'a')
             .replace('5', 's')
             .replace('7', 't')
             .replace('8', 'b')
             .replace('@', 'a')
             .replace('$', 's')
             .replace('!', 'i');

        // 3. Collapse repeating patterns
        n = n.replaceAll("(.)\\1{2,}", "$1");

        // 4. Strip non-alphanumeric characters globally
        n = n.replaceAll("[^a-z0-9]", "");

        return n;
    }
}
