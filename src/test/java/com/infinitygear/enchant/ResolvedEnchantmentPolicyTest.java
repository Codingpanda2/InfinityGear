package com.infinitygear.enchant;

import com.infinitygear.gear.GearProfile;
import com.infinitypickaxes.core.enchant.EnchantSocket;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResolvedEnchantmentPolicyTest {

    @Test
    void sparseProfileOverrideResolvesEverySupportedField() {
        EnchantSocket socket = socket(true, 20, 5, Set.of("minecraft:silk_touch"));
        GearProfile profile = mock(GearProfile.class);
        when(profile.enchantmentOverrides()).thenReturn(Map.of("minecraft:fortune",
                new GearProfile.EnchantmentOverride(false, 75, 3, 7, 2,
                        Set.of("minecraft:dynamite"), false, 2.5)));

        var policy = ResolvedEnchantmentPolicy.resolve(profile, socket, 80, 5, true, 1.0);

        assertFalse(policy.enabled());
        assertEquals(75, policy.unlockLevel());
        assertTrue(policy.unlockedAt(80));
        assertEquals(3, policy.standardMaximum());
        assertEquals(7, policy.absoluteMaximum());
        assertEquals(2, policy.socketCost());
        assertFalse(policy.removable());
        assertEquals(2.5, policy.costWeight());
        assertTrue(policy.additionallyConflictsWith("minecraft:dynamite"));
        assertTrue(policy.additionallyConflictsWith("minecraft:silk_touch"));
    }

    @Test
    void explicitBooleanOverridesReplaceGlobalDefaultsWhileOmittedValuesInherit() {
        EnchantSocket socket = socket(false, 20, 5, Set.of());
        GearProfile profile = mock(GearProfile.class);
        when(profile.enchantmentOverrides()).thenReturn(Map.of("minecraft:fortune",
                new GearProfile.EnchantmentOverride(true, null, null, null, null,
                        null, true, null)));

        var policy = ResolvedEnchantmentPolicy.resolve(profile, socket, 25, 3, false, 4.0);

        assertTrue(policy.enabled());
        assertEquals(20, policy.unlockLevel());
        assertEquals(5, policy.standardMaximum());
        assertEquals(8, policy.absoluteMaximum());
        assertEquals(1, policy.socketCost());
        assertTrue(policy.removable());
        assertEquals(4.0, policy.costWeight());
    }

    @Test
    void profileCapsCannotLoosenGlobalStandardOrCurrentProgressionAllowance() {
        EnchantSocket socket = socket(true, 0, 5, Set.of());
        GearProfile profile = mock(GearProfile.class);
        when(profile.enchantmentOverrides()).thenReturn(Map.of("minecraft:fortune",
                new GearProfile.EnchantmentOverride(null, null, 10, 20, null,
                        null, null, null)));

        var policy = ResolvedEnchantmentPolicy.resolve(profile, socket, 100, 2, true, 1.0);

        assertEquals(5, policy.standardMaximum());
        assertEquals(7, policy.absoluteMaximum());
    }

    @Test
    void profileNeutralBookUsesGlobalMaximumInsteadOfLevelZeroScaling() {
        EnchantSocket socket = new EnchantSocket("fortune", "minecraft:fortune",
                NamespacedKey.minecraft("fortune"), "Fortune", Material.ENCHANTED_BOOK, -1,
                true, 0, 5, new TreeMap<>(Map.of(0, 1, 100, 5)), List.of(), null, Set.of());

        var gearPolicy = ResolvedEnchantmentPolicy.resolve(null, socket, 0, 0, true, 1.0);
        var bookPolicy = ResolvedEnchantmentPolicy.resolveProfileNeutral(socket, 0, true, 1.0);

        assertEquals(1, gearPolicy.standardMaximum());
        assertEquals(5, bookPolicy.standardMaximum());
    }

    private static EnchantSocket socket(boolean enabled, int unlock, int maximum, Set<String> conflicts) {
        return new EnchantSocket("fortune", "minecraft:fortune", NamespacedKey.minecraft("fortune"),
                "Fortune", Material.ENCHANTED_BOOK, -1, enabled, unlock, maximum,
                new TreeMap<>(), List.of(), null, conflicts);
    }
}
