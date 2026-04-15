package dev.frost.frostcore.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatColorUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.legacySection();
    private static final LegacyComponentSerializer AMPERSAND_SERIALIZER =
            LegacyComponentSerializer.legacyAmpersand();

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern BUKKIT_HEX_PATTERN = Pattern.compile("&x(&[A-Fa-f0-9]){6}");
    private static final Pattern SECTION_HEX_PATTERN = Pattern.compile("§x(§[A-Fa-f0-9]){6}");
    private static final Pattern LEGACY_CODE_PATTERN = Pattern.compile("&[0-9a-fA-Fk-oK-OrR]");
    private static final Pattern SECTION_CODE_PATTERN = Pattern.compile("§[0-9a-fA-Fk-oK-OrR]");

    private static final Map<Character, String> LEGACY_TO_MINIMESSAGE = Map.ofEntries(
            Map.entry('0', "<black>"),
            Map.entry('1', "<dark_blue>"),
            Map.entry('2', "<dark_green>"),
            Map.entry('3', "<dark_aqua>"),
            Map.entry('4', "<dark_red>"),
            Map.entry('5', "<dark_purple>"),
            Map.entry('6', "<gold>"),
            Map.entry('7', "<gray>"),
            Map.entry('8', "<dark_gray>"),
            Map.entry('9', "<blue>"),
            Map.entry('a', "<green>"),
            Map.entry('b', "<aqua>"),
            Map.entry('c', "<red>"),
            Map.entry('d', "<light_purple>"),
            Map.entry('e', "<yellow>"),
            Map.entry('f', "<white>"),
            Map.entry('k', "<obfuscated>"),
            Map.entry('l', "<bold>"),
            Map.entry('m', "<strikethrough>"),
            Map.entry('n', "<underlined>"),
            Map.entry('o', "<italic>"),
            Map.entry('r', "<reset>")
    );

    private ChatColorUtil() {
    }

    public static String translateHexToMiniMessage(String message) {
        if (message == null) return "";

        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, "<color:#" + matcher.group(1) + ">");
        }
        message = matcher.appendTail(buffer).toString();

        matcher = BUKKIT_HEX_PATTERN.matcher(message);
        buffer = new StringBuilder();
        while (matcher.find()) {
            String raw = matcher.group().replace("&x", "").replace("&", "");
            matcher.appendReplacement(buffer, "<color:#" + raw + ">");
        }
        message = matcher.appendTail(buffer).toString();

        matcher = SECTION_HEX_PATTERN.matcher(message);
        buffer = new StringBuilder();
        while (matcher.find()) {
            String raw = matcher.group().replace("§x", "").replace("§", "");
            matcher.appendReplacement(buffer, "<color:#" + raw + ">");
        }
        return matcher.appendTail(buffer).toString();
    }

    public static String translateLegacyToMiniMessage(String message) {
        if (message == null) return "";

        StringBuilder result = new StringBuilder(message.length());
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < message.length()) {
                char code = Character.toLowerCase(message.charAt(i + 1));
                String replacement = LEGACY_TO_MINIMESSAGE.get(code);
                if (replacement != null) {
                    result.append(replacement);
                    i++;
                    continue;
                }
            }
            result.append(c);
        }
        return result.toString();
    }

    public static Component toComponent(String message) {
        if (message == null || message.isEmpty()) return Component.empty();

        try {
            String processed = translateHexToMiniMessage(message);
            processed = translateLegacyToMiniMessage(processed);
            return MINI_MESSAGE.deserialize(processed);
        } catch (Exception e) {
            try {
                return AMPERSAND_SERIALIZER.deserialize(message);
            } catch (Exception e2) {
                return Component.text(message);
            }
        }
    }

    public static String toLegacyString(String message) {
        if (message == null) return "";
        Component component = toComponent(message);
        return LEGACY_SERIALIZER.serialize(component);
    }

    public static String stripLegacyCodes(String message) {
        if (message == null) return "";
        String result = LEGACY_CODE_PATTERN.matcher(message).replaceAll("");
        return SECTION_CODE_PATTERN.matcher(result).replaceAll("");
    }

    public static String stripHexCodes(String message) {
        if (message == null) return "";
        String result = HEX_PATTERN.matcher(message).replaceAll("");
        result = BUKKIT_HEX_PATTERN.matcher(result).replaceAll("");
        return SECTION_HEX_PATTERN.matcher(result).replaceAll("");
    }

    public static String stripAll(String message) {
        return stripLegacyCodes(stripHexCodes(message));
    }

    public static String processPlayerMessage(String message, boolean allowLegacy, boolean allowHex) {
        if (allowLegacy && allowHex) {
            return message;
        } else if (allowLegacy) {
            return stripHexCodes(message);
        } else if (allowHex) {
            return stripLegacyCodes(message);
        } else {
            return stripAll(message);
        }
    }
}
