package com.infinitygear.data;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LegacyGearPayloadTest {
    @Test void preservesEveryValidatedLegacyStatistic() {
        UUID uuid = UUID.randomUUID();
        var result = LegacyGearPayload.parse(uuid.toString(), 42, 123.5, 999L, (byte) 1);
        assertTrue(result.valid());
        assertEquals(uuid, result.payload().uuid());
        assertEquals(42, result.payload().level());
        assertEquals(123.5, result.payload().xp());
        assertEquals(999, result.payload().blocksMined());
        assertTrue(result.payload().quarantined());
    }

    @Test void malformedUuidNeverGeneratesReplacementIdentity() {
        var result = LegacyGearPayload.parse("not-a-uuid", 1, 0.0, 0L, null);
        assertFalse(result.valid());
        assertNull(result.payload());
        assertEquals("malformed_uuid", result.failure());
    }
}
