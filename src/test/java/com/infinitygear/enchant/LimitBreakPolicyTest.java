package com.infinitygear.enchant;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LimitBreakPolicyTest {
    private LimitBreakPolicy.Request request(int current) {
        return new LimitBreakPolicy.Request(true, true, true, true, true, current, 5, 8);
    }
    @Test void requiresInstalledEnchantAtStandardMaximum() {
        assertEquals(LimitBreakPolicy.Failure.ENCHANTMENT_MISSING, LimitBreakPolicy.validate(request(0)));
        assertEquals(LimitBreakPolicy.Failure.PREMATURE, LimitBreakPolicy.validate(request(4)));
        assertEquals(LimitBreakPolicy.Failure.NONE, LimitBreakPolicy.validate(request(5)));
    }
    @Test void stopsAtAbsoluteMaximumAndValidatesSpecificTarget() {
        assertEquals(LimitBreakPolicy.Failure.ABSOLUTE_MAXIMUM, LimitBreakPolicy.validate(request(8)));
        assertEquals(LimitBreakPolicy.Failure.WRONG_SPECIFIC_TARGET, LimitBreakPolicy.validate(
                new LimitBreakPolicy.Request(true, true, true, true, false, 5, 5, 8)));
    }
}
