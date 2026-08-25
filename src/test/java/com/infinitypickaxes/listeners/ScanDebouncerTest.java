package com.infinitypickaxes.listeners;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScanDebouncerTest {

    @Test
    void burstSchedulesOnceAndRetainsLatestActor() {
        ScanDebouncer debouncer = new ScanDebouncer();

        assertTrue(debouncer.request("pickup:first"));
        assertFalse(debouncer.request("drop:second"));
        assertFalse(debouncer.request("storage:third"));
        assertEquals("storage:third", debouncer.consume());
        assertNull(debouncer.consume());
        assertTrue(debouncer.request("pickup:next-burst"));
    }

    @Test
    void clearAllowsFreshScheduling() {
        ScanDebouncer debouncer = new ScanDebouncer();
        assertTrue(debouncer.request("join"));

        debouncer.clear();

        assertTrue(debouncer.request("reload"));
        assertEquals("reload", debouncer.consume());
    }
}
