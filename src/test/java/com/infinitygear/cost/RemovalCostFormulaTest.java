package com.infinitygear.cost;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemovalCostFormulaTest {
    @Test void appliesWeightsProfileAndOvercapSurcharge() {
        WeightTable levels = new WeightTable(new TreeMap<>(Map.of(1, 1.0, 5, 2.0)), 0.5);
        assertEquals(720, RemovalCostFormula.calculate(100, "eco:test", 7, 5,
                1.0, Map.of("eco:test", 1.5), levels, 2.0, 0.1));
    }

    @Test void usesFloorAndFinalWeightForLevelsBeyondTable() {
        WeightTable levels = new WeightTable(new TreeMap<>(Map.of(1, 1.0, 5, 3.0)), 0.5);
        assertEquals(3.0, levels.weight(999));
        assertEquals(0.5, levels.weight(0));
    }
}
