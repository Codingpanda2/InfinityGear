package com.infinitygear.inventory;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventoryTransactionTest {
    @Test void outputCapacitySimulationIsAllOrNothing() {
        ItemStack[] full = new ItemStack[2];
        full[0] = item(Material.STONE, 64);
        full[1] = item(Material.DIRT, 64);
        assertFalse(InventoryTransaction.placeAll(full, List.of(item(Material.BOOK, 1))));
        assertEquals(Material.STONE, full[0].getType());
        assertEquals(Material.DIRT, full[1].getType());
    }

    @Test void outputMergesBeforeUsingEmptySlot() {
        ItemStack[] contents = {item(Material.BOOK, 63), null};
        assertTrue(InventoryTransaction.placeAll(contents, List.of(item(Material.BOOK, 2))));
        assertEquals(64, contents[0].getAmount());
        assertEquals(1, contents[1].getAmount());
    }

    @Test void staleLiveSlotIsRejectedWithoutAnyInventoryWrite() {
        org.bukkit.inventory.Inventory inventory = mock(org.bukkit.inventory.Inventory.class);
        ItemStack expected = item(Material.BOOK, 1);
        ItemStack swapped = item(Material.DIAMOND, 1);
        when(inventory.getItem(4)).thenReturn(swapped);

        var result = new InventoryTransaction().execute(inventory, Map.of(4, expected), Map.of(), List.of());

        assertEquals(InventoryTransaction.Failure.STALE_INPUT, result.failure());
        verify(inventory, never()).setContents(any(ItemStack[].class));
    }

    @Test void inventoryWriteFailureReturnsAtomicMutationFailure() {
        org.bukkit.inventory.Inventory inventory = mock(org.bukkit.inventory.Inventory.class);
        when(inventory.getContents()).thenReturn(new ItemStack[2]);
        doThrow(new IllegalStateException("write failed")).when(inventory).setContents(any(ItemStack[].class));

        var result = new InventoryTransaction().execute(inventory, Map.of(), Map.of(), List.of());

        assertEquals(InventoryTransaction.Failure.MUTATION_FAILED, result.failure());
        verify(inventory, times(2)).setContents(any(ItemStack[].class));
    }

    private ItemStack item(Material material, int amount) {
        ItemStack item = mock(ItemStack.class);
        AtomicInteger current = new AtomicInteger(amount);
        when(item.getType()).thenReturn(material);
        when(item.getAmount()).thenAnswer(ignored -> current.get());
        doAnswer(invocation -> { current.set(invocation.getArgument(0)); return null; }).when(item).setAmount(anyInt());
        when(item.getMaxStackSize()).thenReturn(64);
        when(item.isSimilar(any())).thenAnswer(invocation -> {
            ItemStack other = invocation.getArgument(0);
            return other != null && other.getType() == material;
        });
        when(item.clone()).thenAnswer(ignored -> item(material, current.get()));
        return item;
    }
}
