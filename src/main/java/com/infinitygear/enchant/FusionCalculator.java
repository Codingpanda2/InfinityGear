package com.infinitygear.enchant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Deterministic, inventory-independent enchanted-book fusion planning. */
public final class FusionCalculator {
    private FusionCalculator() {}

    public record FusionStep(int inputLevel, int resultLevel) {}

    public record Plan(List<Integer> inputs, List<Integer> outputs, List<FusionStep> steps,
                       List<Integer> consumedInputIndices, List<Integer> createdOutputs) {
        public Plan {
            inputs = List.copyOf(inputs);
            outputs = List.copyOf(outputs);
            steps = List.copyOf(steps);
            consumedInputIndices = List.copyOf(consumedInputIndices);
            createdOutputs = List.copyOf(createdOutputs);
        }

        public Plan(List<Integer> inputs, List<Integer> outputs, List<FusionStep> steps) {
            this(inputs, outputs, steps, java.util.stream.IntStream.range(0, inputs.size()).boxed().toList(), outputs);
        }

        public int fusionCount() {
            return steps.size();
        }

        public long weightedCost(Map<Integer, Long> resultLevelWeights, long fallbackWeight) {
            long fallback = Math.max(0, fallbackWeight);
            long total = 0;
            for (FusionStep step : steps) {
                total = Math.addExact(total, Math.max(0,
                        resultLevelWeights.getOrDefault(step.resultLevel(), fallback)));
            }
            return total;
        }
    }

    public static Plan fusePair(int firstLevel, int secondLevel, int standardMaximum) {
        if (firstLevel < 1 || secondLevel < 1) throw new IllegalArgumentException("Book levels must be positive.");
        if (firstLevel != secondLevel) throw new IllegalArgumentException("Book levels must be identical.");
        if (firstLevel >= standardMaximum) throw new IllegalArgumentException("Fusion would exceed the standard maximum.");
        return new Plan(List.of(firstLevel, secondLevel), List.of(firstLevel + 1),
                List.of(new FusionStep(firstLevel, firstLevel + 1)));
    }

    /**
     * Compresses equal levels like binary carries, stopping at the standard maximum.
     * Inputs above the maximum are rejected; books at the maximum remain as individual outputs.
     */
    public static Plan fuseAll(List<Integer> levels, int standardMaximum) {
        if (levels == null || levels.isEmpty()) throw new IllegalArgumentException("At least two books are required.");
        if (standardMaximum < 1) throw new IllegalArgumentException("Standard maximum must be positive.");
        TreeMap<Integer, List<Token>> tokens = new TreeMap<>();
        for (int index = 0; index < levels.size(); index++) {
            Integer level = levels.get(index);
            if (level == null || level < 1 || level > standardMaximum) {
                throw new IllegalArgumentException("Every book level must be within the standard maximum.");
            }
            tokens.computeIfAbsent(level, ignored -> new ArrayList<>()).add(new Token(java.util.Set.of(index), false));
        }

        List<FusionStep> steps = new ArrayList<>();
        for (int level = tokens.firstKey(); level < standardMaximum; level++) {
            List<Token> atLevel = tokens.computeIfAbsent(level, ignored -> new ArrayList<>());
            while (atLevel.size() >= 2) {
                Token first = atLevel.removeFirst();
                Token second = atLevel.removeFirst();
                java.util.Set<Integer> origins = new java.util.LinkedHashSet<>(first.origins());
                origins.addAll(second.origins());
                tokens.computeIfAbsent(level + 1, ignored -> new ArrayList<>()).add(new Token(origins, true));
                steps.add(new FusionStep(level, level + 1));
            }
        }
        if (steps.isEmpty()) throw new IllegalArgumentException("No matching pair can be fused.");

        List<Integer> outputs = new ArrayList<>();
        List<Integer> createdOutputs = new ArrayList<>();
        java.util.Set<Integer> consumed = new java.util.TreeSet<>();
        tokens.forEach((level, atLevel) -> atLevel.forEach(token -> {
            outputs.add(level);
            if (token.fused()) {
                createdOutputs.add(level);
                consumed.addAll(token.origins());
            }
        }));
        outputs.sort(Collections.reverseOrder());
        createdOutputs.sort(Collections.reverseOrder());
        return new Plan(levels, outputs, steps, List.copyOf(consumed), createdOutputs);
    }

    private record Token(java.util.Set<Integer> origins, boolean fused) {}
}
