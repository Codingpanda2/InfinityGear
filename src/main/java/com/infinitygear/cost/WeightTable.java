package com.infinitygear.cost;

import java.util.NavigableMap;
import java.util.TreeMap;

/** Floor-entry weight table; levels above the final entry use that final weight. */
public final class WeightTable {
    private final NavigableMap<Integer, Double> weights;
    private final double fallback;

    public WeightTable(NavigableMap<Integer, Double> weights, double fallback) {
        if (!Double.isFinite(fallback) || fallback < 0) throw new IllegalArgumentException("Invalid fallback weight.");
        this.weights = new TreeMap<>();
        if (weights != null) weights.forEach((level, weight) -> {
            if (level == null || level < 0 || weight == null || !Double.isFinite(weight) || weight < 0) {
                throw new IllegalArgumentException("Invalid weight table entry.");
            }
            this.weights.put(level, weight);
        });
        this.fallback = fallback;
    }

    public double weight(int level) {
        var entry = weights.floorEntry(Math.max(0, level));
        return entry == null ? fallback : entry.getValue();
    }
}
