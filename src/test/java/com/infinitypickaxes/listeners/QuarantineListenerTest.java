package com.infinitypickaxes.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuarantineListenerTest {

    @Test
    void blockBreakGateRunsAtLowestAndIgnoresCancelledEvents() throws Exception {
        EventHandler handler = QuarantineListener.class
                .getMethod("onBlockBreak", BlockBreakEvent.class)
                .getAnnotation(EventHandler.class);

        assertEquals(EventPriority.LOWEST, handler.priority());
        assertTrue(handler.ignoreCancelled());
    }
}
