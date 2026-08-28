package com.infinitygear.cost;

import java.util.Locale;
import java.util.Map;

/** base × enchantment weight × level weight × profile multiplier, then overcap surcharge. */
public final class RemovalCostFormula {
    private RemovalCostFormula() {}

    public static double calculate(double base, String enchantmentKey, int level, int standardMaximum,
                                   double defaultEnchantWeight, Map<String, Double> overrides,
                                   WeightTable levelWeights, double profileMultiplier,
                                   double overcapMultiplierPerLevel) {
        requireNonNegative(base, "base");
        requireNonNegative(defaultEnchantWeight, "default enchantment weight");
        requireNonNegative(profileMultiplier, "profile multiplier");
        requireNonNegative(overcapMultiplierPerLevel, "overcap multiplier");
        double enchantWeight = overrides == null ? defaultEnchantWeight
                : overrides.getOrDefault(enchantmentKey.toLowerCase(Locale.ROOT), defaultEnchantWeight);
        requireNonNegative(enchantWeight, "enchantment weight");
        int overcap = Math.max(0, level - Math.max(1, standardMaximum));
        double surcharge = 1.0 + overcap * overcapMultiplierPerLevel;
        double result = base * enchantWeight * levelWeights.weight(level) * profileMultiplier * surcharge;
        if (!Double.isFinite(result)) throw new ArithmeticException("Removal cost overflow.");
        return result;
    }

    private static void requireNonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Invalid " + name + '.');
    }
}
