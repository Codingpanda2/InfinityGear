package com.infinitypickaxes.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    private static final TreeMap<Integer, String> ROMAN_NUMERALS = new TreeMap<>();
    private static final Pattern CENTER_PATTERN = Pattern.compile("<center>(.*?)</center>", Pattern.CASE_INSENSITIVE);
    private static final int CHAT_CENTER_PX = 154;

    static {
        ROMAN_NUMERALS.put(1000, "M");
        ROMAN_NUMERALS.put(900, "CM");
        ROMAN_NUMERALS.put(500, "D");
        ROMAN_NUMERALS.put(400, "CD");
        ROMAN_NUMERALS.put(100, "C");
        ROMAN_NUMERALS.put(90, "XC");
        ROMAN_NUMERALS.put(50, "L");
        ROMAN_NUMERALS.put(40, "XL");
        ROMAN_NUMERALS.put(10, "X");
        ROMAN_NUMERALS.put(9, "IX");
        ROMAN_NUMERALS.put(5, "V");
        ROMAN_NUMERALS.put(4, "IV");
        ROMAN_NUMERALS.put(1, "I");
    }

    private TextUtil() {}

    /**
     * Parses a string containing MiniMessage formatting, legacy '&' codes, and <center> tags into a rich Adventure Component.
     */
    public static Component parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        // Process <center> tags with exact Minecraft font width padding
        if (text.toLowerCase().contains("<center>")) {
            text = processCentering(text);
        }

        // Convert legacy color codes if present
        if (text.contains("&") || text.contains("§")) {
            text = convertLegacyCodes(text);
        }

        // Fix any stray or unmatched closing tags
        text = fixRogueTags(text);

        try {
            return MINI_MESSAGE.deserialize(text);
        } catch (Exception e) {
            try {
                String plain = stripFormatting(text);
                return MINI_MESSAGE.deserialize("<gray>" + plain + "</gray>");
            } catch (Exception ex) {
                return Component.text(stripFormatting(text));
            }
        }
    }

    private static String convertLegacyCodes(String text) {
        return text.replaceAll("(?i)[&§]0", "<black>")
                   .replaceAll("(?i)[&§]1", "<dark_blue>")
                   .replaceAll("(?i)[&§]2", "<dark_green>")
                   .replaceAll("(?i)[&§]3", "<dark_aqua>")
                   .replaceAll("(?i)[&§]4", "<dark_red>")
                   .replaceAll("(?i)[&§]5", "<dark_purple>")
                   .replaceAll("(?i)[&§]6", "<gold>")
                   .replaceAll("(?i)[&§]7", "<gray>")
                   .replaceAll("(?i)[&§]8", "<dark_gray>")
                   .replaceAll("(?i)[&§]9", "<blue>")
                   .replaceAll("(?i)[&§]a", "<green>")
                   .replaceAll("(?i)[&§]b", "<aqua>")
                   .replaceAll("(?i)[&§]c", "<red>")
                   .replaceAll("(?i)[&§]d", "<light_purple>")
                   .replaceAll("(?i)[&§]e", "<yellow>")
                   .replaceAll("(?i)[&§]f", "<white>")
                   .replaceAll("(?i)[&§]l", "<b>")
                   .replaceAll("(?i)[&§]o", "<i>")
                   .replaceAll("(?i)[&§]n", "<u>")
                   .replaceAll("(?i)[&§]m", "<st>")
                   .replaceAll("(?i)[&§]k", "<obf>")
                   .replaceAll("(?i)[&§]r", "<reset>");
    }

    private static String fixRogueTags(String text) {
        if (text == null) return "";
        // Clean double closing tags or trailing </gray> that don't match
        String s = text;
        if (s.endsWith("</gray></gray>")) {
            s = s.substring(0, s.length() - 7);
        }
        return s;
    }

    /**
     * Converts a list of formatted strings into a list of Components.
     */
    public static List<Component> parseList(List<String> lines) {
        if (lines == null) return new ArrayList<>();
        List<Component> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            result.add(parse(line));
        }
        return result;
    }

    /**
     * Replaces <center>content</center> with space-padded text matching the Minecraft chat window center.
     */
    public static String processCentering(String text) {
        Matcher matcher = CENTER_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String content = matcher.group(1);
            String padded = center(content);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(padded));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static String center(String text) {
        if (text == null || text.isEmpty()) return "";
        String stripped = text.replaceAll("<[^>]*>", "").replaceAll("&[0-9a-fk-orA-FK-OR]", "");

        int messagePxSize = 0;
        for (char c : stripped.toCharArray()) {
            messagePxSize += getCharWidth(c);
        }

        int halvedMessageSize = messagePxSize / 2;
        int toCompensate = CHAT_CENTER_PX - halvedMessageSize;
        int spaceLength = 4; // Standard space pixel length in Minecraft font
        int compensated = 0;
        StringBuilder padding = new StringBuilder();
        while (compensated < toCompensate) {
            padding.append(" ");
            compensated += spaceLength;
        }

        return padding.toString() + text;
    }

    private static int getCharWidth(char c) {
        return switch (c) {
            case 'i', '!', '|', ':', ';', '.', ',' -> 2;
            case 'l', '\'', '`' -> 3;
            case 't', 'I', '[', ']', ' ' -> 4;
            case 'k', 'f', '(', ')', '{', '}', '<', '>' -> 5;
            case '@', '~' -> 7;
            default -> 6;
        };
    }

    /**
     * Converts an integer to Roman Numeral format (e.g. 1 -> I, 5 -> V, 25 -> XXV).
     */
    public static String toRoman(int number) {
        if (number <= 0) return String.valueOf(number);
        int l = ROMAN_NUMERALS.floorKey(number);
        if (number == l) {
            return ROMAN_NUMERALS.get(number);
        }
        return ROMAN_NUMERALS.get(l) + toRoman(number - l);
    }

    /**
     * Converts a Roman Numeral string (or integer string) back to integer.
     */
    public static int fromRoman(String roman) {
        if (roman == null || roman.trim().isEmpty()) return 1;
        roman = roman.trim().toUpperCase();
        int result = 0;
        int lastValue = 0;
        for (int i = roman.length() - 1; i >= 0; i--) {
            int value = switch (roman.charAt(i)) {
                case 'I' -> 1;
                case 'V' -> 5;
                case 'X' -> 10;
                case 'L' -> 50;
                case 'C' -> 100;
                case 'D' -> 500;
                case 'M' -> 1000;
                default -> 0;
            };
            if (value == 0) {
                try {
                    return Integer.parseInt(roman);
                } catch (NumberFormatException e) {
                    return 1;
                }
            }
            if (value < lastValue) {
                result -= value;
            } else {
                result += value;
                lastValue = value;
            }
        }
        return result > 0 ? result : 1;
    }

    /**
     * Strips all MiniMessage and legacy color tags from a string.
     */
    public static String stripFormatting(String text) {
        if (text == null) return "";
        return text.replaceAll("<[^>]*>", "").replaceAll("&[0-9a-fk-orA-FK-OR]", "").replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
    }
}
