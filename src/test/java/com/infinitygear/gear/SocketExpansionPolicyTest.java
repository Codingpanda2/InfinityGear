package com.infinitygear.gear;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SocketExpansionPolicyTest {
    @Test void expandsIndividualCapacityByOne() {
        var result = SocketExpansionPolicy.evaluate(3, 8, true);
        assertTrue(result.allowed());
        assertEquals(4, result.resulting());
    }
    @Test void maximumAndGrandfatheredOverMaximumRemainUnchanged() {
        assertEquals(SocketExpansionPolicy.Failure.AT_MAXIMUM, SocketExpansionPolicy.evaluate(8, 8, true).failure());
        assertEquals(SocketExpansionPolicy.Failure.GRANDFATHERED_OVER_MAXIMUM,
                SocketExpansionPolicy.evaluate(10, 8, true).failure());
    }
}
