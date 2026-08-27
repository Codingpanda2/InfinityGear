package com.infinitygear.enchant;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Pure removal/transfer transformations; Bukkit metadata mutation is an adapter concern. */
public final class EnchantmentTransforms {
    private EnchantmentTransforms() {}

    public enum Failure { NONE, ENCHANTMENT_MISSING, NON_REMOVABLE, BLANK_BOOK_REQUIRED, OVERCAP_TRANSFER }
    public record Result(Failure failure, Map<String, Integer> sourceEnchantments,
                         Map<String, Integer> outputBookEnchantments, boolean blankSourceBook) {
        public Result {
            sourceEnchantments = Map.copyOf(sourceEnchantments);
            outputBookEnchantments = Map.copyOf(outputBookEnchantments);
        }
        public boolean allowed() { return failure == Failure.NONE; }
    }

    public static Result remove(Map<String, Integer> source, String selected, boolean removable,
                                boolean sourceIsBook) {
        String key = normalize(selected);
        LinkedHashMap<String, Integer> copy = normalizedCopy(source);
        if (!copy.containsKey(key)) return failure(Failure.ENCHANTMENT_MISSING, copy);
        if (!removable) return failure(Failure.NON_REMOVABLE, copy);
        copy.remove(key);
        return new Result(Failure.NONE, copy, Map.of(), sourceIsBook && copy.isEmpty());
    }

    public static Result transfer(Map<String, Integer> source, String selected, int standardMaximum,
                                  boolean blankOrdinaryBookPresent) {
        String key = normalize(selected);
        LinkedHashMap<String, Integer> copy = normalizedCopy(source);
        Integer level = copy.get(key);
        if (level == null) return failure(Failure.ENCHANTMENT_MISSING, copy);
        if (!blankOrdinaryBookPresent) return failure(Failure.BLANK_BOOK_REQUIRED, copy);
        if (level > standardMaximum) return failure(Failure.OVERCAP_TRANSFER, copy);
        copy.remove(key);
        return new Result(Failure.NONE, copy, Map.of(key, level), false);
    }

    private static Result failure(Failure failure, Map<String, Integer> source) {
        return new Result(failure, source, Map.of(), false);
    }

    private static LinkedHashMap<String, Integer> normalizedCopy(Map<String, Integer> source) {
        LinkedHashMap<String, Integer> copy = new LinkedHashMap<>();
        if (source != null) source.forEach((key, value) -> {
            if (key != null && value != null && value > 0) copy.put(normalize(key), value);
        });
        return copy;
    }

    private static String normalize(String key) {
        return key == null ? "" : key.toLowerCase(Locale.ROOT);
    }
}
