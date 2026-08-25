package com.infinitypickaxes.core.duplicate;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuplicateSafetyTest {

    @Test
    void observationsOnlyFlagSimultaneouslyVisibleCopies() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        DuplicateObservations<String> observations = new DuplicateObservations<>();

        observations.observe(first, "pickaxe-a", "player:one:slot=0", 1);
        observations.observe(second, "pickaxe-b", "player:two:slot=0", 1);

        assertEquals(2, observations.observedCopies());
        assertEquals(1, observations.entries().get(first).size());
        assertEquals(1, observations.entries().get(second).size());
    }

    @Test
    void illegalStackCountsAsMultiplePhysicalCopies() {
        UUID uuid = UUID.randomUUID();
        DuplicateObservations<String> observations = new DuplicateObservations<>();

        observations.observe(uuid, "stack", "creative:slot=4", 3);

        assertEquals(3, observations.observedCopies());
        assertEquals(3, observations.entries().get(uuid).size());
        assertTrue(observations.entries().get(uuid).get(2).location().endsWith("stack-copy=2"));
    }

    @Test
    void arbitraryPluginInventoryHolderIsNotPhysicalStorage() {
        InventoryHolder pluginGui = new InventoryHolder() {
            @Override
            public Inventory getInventory() {
                return null;
            }
        };

        assertFalse(PickaxeDuplicateService.isPhysicalStorageHolder(pluginGui));
        assertFalse(PickaxeDuplicateService.isPhysicalStorageHolder(null));
    }

    @Test
    void canonicalRekeyRequiresExactlyOneItem() {
        PickaxeDuplicateService.validateRekeyAmount(1);
        assertThrows(IllegalArgumentException.class,
                () -> PickaxeDuplicateService.validateRekeyAmount(0));
        assertThrows(IllegalArgumentException.class,
                () -> PickaxeDuplicateService.validateRekeyAmount(2));
    }
}
