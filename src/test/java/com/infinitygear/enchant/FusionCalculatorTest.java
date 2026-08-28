package com.infinitygear.enchant;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FusionCalculatorTest {
    @Test void pairProducesNextLevel() {
        var plan = FusionCalculator.fusePair(3, 3, 5);
        assertEquals(List.of(4), plan.outputs());
        assertEquals(1, plan.fusionCount());
    }

    @Test void differentLevelsAndMaximumAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> FusionCalculator.fusePair(2, 3, 5));
        assertThrows(IllegalArgumentException.class, () -> FusionCalculator.fusePair(5, 5, 5));
    }

    @Test void fiveLevelThreeBooksCompressWithLeftover() {
        var plan = FusionCalculator.fuseAll(List.of(3, 3, 3, 3, 3), 5);
        assertEquals(List.of(5, 3), plan.outputs());
        assertEquals(3, plan.fusionCount());
        assertEquals(List.of(0, 1, 2, 3), plan.consumedInputIndices());
        assertEquals(List.of(5), plan.createdOutputs());
        assertEquals(List.of(
                new FusionCalculator.FusionStep(3, 4),
                new FusionCalculator.FusionStep(3, 4),
                new FusionCalculator.FusionStep(4, 5)), plan.steps());
    }

    @Test void costSumsEveryPairwiseResultWeight() {
        var plan = FusionCalculator.fuseAll(List.of(3, 3, 3, 3, 3), 5);
        assertEquals(25, plan.weightedCost(Map.of(4, 5L, 5, 15L), 99));
    }

    @Test void compressionStopsAtMaximum() {
        var plan = FusionCalculator.fuseAll(List.of(4, 4, 4, 4), 5);
        assertEquals(List.of(5, 5), plan.outputs());
        assertEquals(2, plan.fusionCount());
    }
}
