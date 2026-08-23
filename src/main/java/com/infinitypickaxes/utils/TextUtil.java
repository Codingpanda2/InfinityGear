package com.infinitypickaxes.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public final class TextUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    private static final TreeMap<Integer, String> ROMAN_NUMERALS = new TreeMap<>();

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
     * Parses a string containing MiniMessage formatting or legacy '&' codes into a rich Adventure Component.
     */
    public static Component parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        // If string uses legacy & color codes, convert to MiniMessage friendly or parse with legacy
        if (text.contains("&")) {
            text = text.replace("&0", "<black>")
                       .replace("&1", "<dark_blue>")
                       .replace("&2", "<dark_green>")
                       .replace("&3", "<dark_aqua>")
                       .replace("&4", "<dark_red>")
                       .replace("&5", "<dark_purple>")
                       .replace("&6", "<gold>")
                       .replace("&7", "<gray>")
                       .replace("&8", "<dark_gray>")
                       .replace("&9", "<blue>")
                       .replace("&a", "<green>")
                       .replace("&b", "<aqua>")
                       .replace("&c", "<red>")
                       .replace("&d", "<light_purple>")
                       .replace("&e", "<yellow>")
                       .replace("&f", "<white>")
                       .replace("&l", "<b>")
                       .replace("&o", "<i>")
                       .replace("&n", "<u>")
                       .replace("&m", "<st>")
                       .replace("&k", "<obf>")
                       .replace("&r", "<reset>");
        }
        try {
            return MINI_MESSAGE.deserialize(text);
        } catch (Exception e) {
            return LEGACY_SERIALIZER.deserialize(text);
        }
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
}
