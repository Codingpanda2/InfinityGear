package com.infinitygear.enchant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnchantmentApplicationPolicyTest {

    private EnchantmentApplicationPolicy.Request request(int current, int book, int used, int limit) {
        return new EnchantmentApplicationPolicy.Request(1, true, true, true,
                current, book, used, limit, 5, 8, false, true);
    }

    @Test void bookInstallsItsTargetLevel() {
        var result = EnchantmentApplicationPolicy.evaluate(request(1, 4, 2, 3));
        assertTrue(result.allowed());
        assertEquals(4, result.resultingLevel());
        assertEquals(2, result.resultingSocketUsage());
    }

    @Test void newEnchantConsumesExactlyOneSocket() {
        var result = EnchantmentApplicationPolicy.evaluate(request(0, 3, 2, 3));
        assertTrue(result.allowed());
        assertEquals(3, result.resultingSocketUsage());
    }

    @Test void equalAndLowerBooksAreRejected() {
        assertEquals(EnchantmentApplicationPolicy.Failure.EQUAL_OR_LOWER_LEVEL,
                EnchantmentApplicationPolicy.evaluate(request(3, 3, 1, 3)).failure());
        assertEquals(EnchantmentApplicationPolicy.Failure.EQUAL_OR_LOWER_LEVEL,
                EnchantmentApplicationPolicy.evaluate(request(3, 2, 1, 3)).failure());
    }

    @Test void normalBooksCannotCrossStandardMaximum() {
        assertEquals(EnchantmentApplicationPolicy.Failure.ABOVE_STANDARD_MAXIMUM,
                EnchantmentApplicationPolicy.evaluate(request(4, 6, 1, 3)).failure());
    }

    @Test void existingEnchantCanUpgradeWhileGrandfatheredOversocketed() {
        assertTrue(EnchantmentApplicationPolicy.evaluate(request(2, 3, 7, 3)).allowed());
    }

    @Test void multipleManagedEnchantsAreRejected() {
        var request = new EnchantmentApplicationPolicy.Request(2, true, true, true,
                0, 1, 0, 3, 5, 8, false, true);
        assertEquals(EnchantmentApplicationPolicy.Failure.MULTIPLE_MANAGED_ENCHANTMENTS,
                EnchantmentApplicationPolicy.evaluate(request).failure());
    }
}
