package com.infinitygear.data;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TrackedItemDataTest {
    @Test void stampsAndReadsUniqueSingletonButRejectsStack() {
        Harness harness = new Harness(1);
        UUID uuid = UUID.randomUUID();
        TrackedItemData.stamp(harness.item, TrackedKind.RUNIC_ERASER, "runic_eraser", uuid);
        var identity = TrackedItemData.read(harness.item);
        assertNotNull(identity);
        assertEquals(uuid, identity.uuid());
        assertEquals(TrackedKind.RUNIC_ERASER, identity.kind());
        assertEquals("runic_eraser", identity.type());
        assertFalse(GearData.isGear(harness.item), "a protected catalyst must never be treated as gear");

        when(harness.item.getAmount()).thenReturn(2);
        assertNull(TrackedItemData.read(harness.item));
        assertEquals(uuid, TrackedItemData.readRaw(harness.item).uuid());
    }

    @Test void ordinaryBookWithoutMarkerIsNeverTracked() {
        Harness harness = new Harness(16);
        when(harness.item.getType()).thenReturn(Material.BOOK);
        assertNull(TrackedItemData.readRaw(harness.item));
    }

    private static final class Harness {
        final Map<NamespacedKey, Object> values = new HashMap<>();
        final PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        final ItemMeta meta = mock(ItemMeta.class);
        final ItemStack item = mock(ItemStack.class);
        Harness(int amount) {
            when(item.getAmount()).thenReturn(amount);
            when(item.hasItemMeta()).thenReturn(true);
            when(item.getItemMeta()).thenReturn(meta);
            when(meta.getPersistentDataContainer()).thenReturn(pdc);
            when(pdc.has(any(), any())).thenAnswer(call -> values.containsKey(call.getArgument(0)));
            when(pdc.get(any(), any())).thenAnswer(call -> values.get(call.getArgument(0)));
            when(pdc.getOrDefault(any(), any(), any())).thenAnswer(call ->
                    values.getOrDefault(call.getArgument(0), call.getArgument(2)));
            doAnswer(call -> { values.put(call.getArgument(0), call.getArgument(2)); return null; })
                    .when(pdc).set(any(), any(), any());
        }
    }
}
