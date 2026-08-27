package com.infinitygear.enchant;

import com.infinitygear.gear.GearProfile;
import com.infinitypickaxes.core.enchant.EnchantSocket;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Immutable effective policy produced from the global managed-enchantment
 * policy and one sparse gear-profile override. Mutation paths should consume
 * this result rather than interpreting profile YAML independently.
 */
public record ResolvedEnchantmentPolicy(
        String enchantmentKey,
        boolean enabled,
        int unlockLevel,
        int standardMaximum,
        int absoluteMaximum,
        int socketCost,
        Set<String> additionalConflicts,
        boolean removable,
        double costWeight
) {
    public ResolvedEnchantmentPolicy {
        enchantmentKey = normalize(enchantmentKey);
        unlockLevel = Math.max(0, unlockLevel);
        standardMaximum = Math.max(1, standardMaximum);
        absoluteMaximum = Math.max(standardMaximum, absoluteMaximum);
        socketCost = Math.max(0, socketCost);
        additionalConflicts = additionalConflicts == null ? Set.of() : additionalConflicts.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(ResolvedEnchantmentPolicy::normalize)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (!Double.isFinite(costWeight) || costWeight < 0) costWeight = 1.0;
    }

    public boolean unlockedAt(int gearLevel) {
        return gearLevel >= unlockLevel;
    }

    public boolean additionallyConflictsWith(String otherKey) {
        String normalized = normalize(otherKey);
        String id = idOnly(normalized);
        return additionalConflicts.contains(normalized) || additionalConflicts.contains(id);
    }

    public static ResolvedEnchantmentPolicy resolve(
            GearProfile profile,
            EnchantSocket socket,
            int gearLevel,
            int defaultLimitBreakExtra,
            boolean globallyRemovable,
            double globalCostWeight
    ) {
        if (socket == null) throw new IllegalArgumentException("Managed enchantment socket is required.");
        GearProfile.EnchantmentOverride override = profile == null || profile.enchantmentOverrides() == null ? null
                : profile.enchantmentOverrides().get(normalize(socket.getKeyString()));

        boolean enabled = override != null && override.enabled() != null
                ? override.enabled() : socket.isEnabled();
        int unlock = override != null && override.unlockLevel() != null
                ? override.unlockLevel() : socket.getUnlockPickaxeLevel();
        int standard = override != null && override.standardMaximum() != null
                ? Math.min(socket.getMaxLevel(), override.standardMaximum())
                : socket.getMaximumAtLevel(Math.max(0, gearLevel));
        int progressionAbsolute = standard
                + (socket.supportsLimitBreak() ? Math.max(0, defaultLimitBreakExtra) : 0);
        int absolute = override != null && override.absoluteMaximum() != null
                ? Math.min(override.absoluteMaximum(), progressionAbsolute)
                : progressionAbsolute;
        int socketCost = override != null && override.socketCost() != null ? override.socketCost() : 1;
        boolean removable = override != null && override.removable() != null
                ? override.removable() : globallyRemovable;
        double costWeight = override != null && override.costWeight() != null
                ? override.costWeight() : globalCostWeight;

        Set<String> conflicts = new LinkedHashSet<>(socket.getAdditionalConflicts());
        if (override != null && override.additionalConflicts() != null) {
            conflicts.addAll(override.additionalConflicts());
        }
        return new ResolvedEnchantmentPolicy(socket.getKeyString(), enabled, unlock, standard,
                absolute, socketCost, conflicts, removable, costWeight);
    }

    private static String normalize(String key) {
        return key == null ? "" : key.toLowerCase(Locale.ROOT);
    }

    private static String idOnly(String key) {
        int separator = key.indexOf(':');
        return separator < 0 ? key : key.substring(separator + 1);
    }
}
