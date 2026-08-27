package com.infinitygear.enchant;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnchantmentTransformsTest {
    @Test void removalPreservesEveryUnselectedEnchant() {
        var result = EnchantmentTransforms.remove(Map.of("eco:a", 3, "eco:b", 7), "eco:a", true, false);
        assertTrue(result.allowed());
        assertEquals(Map.of("eco:b", 7), result.sourceEnchantments());
        assertFalse(result.blankSourceBook());
    }

    @Test void finalBookEnchantReturnsBlankBook() {
        assertTrue(EnchantmentTransforms.remove(Map.of("eco:a", 3), "eco:a", true, true).blankSourceBook());
    }

    @Test void nonRemovablePolicyDoesNotMutatePreview() {
        var result = EnchantmentTransforms.remove(Map.of("minecraft:binding_curse", 1),
                "minecraft:binding_curse", false, false);
        assertEquals(EnchantmentTransforms.Failure.NON_REMOVABLE, result.failure());
        assertEquals(Map.of("minecraft:binding_curse", 1), result.sourceEnchantments());
    }

    @Test void transferPreservesExactLevelAndOtherSourceEnchants() {
        var result = EnchantmentTransforms.transfer(Map.of("eco:a", 3, "eco:b", 7), "eco:a", 5, true);
        assertTrue(result.allowed());
        assertEquals(Map.of("eco:b", 7), result.sourceEnchantments());
        assertEquals(Map.of("eco:a", 3), result.outputBookEnchantments());
    }

    @Test void transferRequiresBlankBookAndRejectsLimitBrokenLevel() {
        assertEquals(EnchantmentTransforms.Failure.BLANK_BOOK_REQUIRED,
                EnchantmentTransforms.transfer(Map.of("eco:a", 3), "eco:a", 5, false).failure());
        assertEquals(EnchantmentTransforms.Failure.OVERCAP_TRANSFER,
                EnchantmentTransforms.transfer(Map.of("eco:a", 6), "eco:a", 5, true).failure());
    }

    @Test void transferCannotBypassNonRemovablePolicy() {
        var result = EnchantmentTransforms.transfer(Map.of("minecraft:binding_curse", 1),
                "minecraft:binding_curse", 1, true, false);
        assertEquals(EnchantmentTransforms.Failure.NON_REMOVABLE, result.failure());
        assertEquals(Map.of("minecraft:binding_curse", 1), result.sourceEnchantments());
        assertTrue(result.outputBookEnchantments().isEmpty());
    }
}
