package com.infinitypickaxes.gui;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.core.duplicate.PickaxeDuplicateService;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.logging.Logger;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GuiManagerTest {

    @ParameterizedTest
    @EnumSource(ClickType.class)
    void everyCustomGuiClickTypeIsCancelledBeforeDelegation(ClickType clickType) {
        Harness harness = new Harness();
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getInventory()).thenReturn(harness.inventory);
        when(event.getWhoClicked()).thenReturn(harness.viewer);
        when(event.getClick()).thenReturn(clickType);

        harness.manager.onInventoryClick(event);

        verify(event).setCancelled(true);
        verify(harness.gui).handleClick(event);
    }

    @ParameterizedTest
    @EnumSource(InventoryAction.class)
    void everyCustomGuiInventoryActionStartsCancelled(InventoryAction action) {
        Harness harness = new Harness();
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getInventory()).thenReturn(harness.inventory);
        when(event.getWhoClicked()).thenReturn(harness.viewer);
        when(event.getAction()).thenReturn(action);

        harness.manager.onInventoryClick(event);

        verify(event).setCancelled(true);
    }

    @Test
    void customGuiDragIsAlwaysCancelled() {
        Harness harness = new Harness();
        InventoryDragEvent event = mock(InventoryDragEvent.class);
        when(event.getInventory()).thenReturn(harness.inventory);
        when(event.getWhoClicked()).thenReturn(harness.viewer);

        harness.manager.onInventoryDrag(event);

        verify(event).setCancelled(true);
        verify(harness.gui).handleDrag(event);
    }

    @Test
    void handlerFailureStaysCancelledAndIsLogged() {
        Harness harness = new Harness();
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getInventory()).thenReturn(harness.inventory);
        when(event.getWhoClicked()).thenReturn(harness.viewer);
        when(harness.inventory.getViewers()).thenReturn(List.of());
        doThrow(new IllegalStateException("broken gui")).when(harness.gui).handleClick(event);

        harness.manager.onInventoryClick(event);

        verify(event, atLeastOnce()).setCancelled(true);
        verify(harness.plugin, atLeastOnce()).getLogger();
    }

    private static final class Harness {
        private final InfinityPickaxes plugin = mock(InfinityPickaxes.class);
        private final PickaxeDuplicateService duplicateService = mock(PickaxeDuplicateService.class);
        private final CustomGui gui = mock(CustomGui.class);
        private final InfinityPickaxe pickaxe = mock(InfinityPickaxe.class);
        private final ItemStack item = mock(ItemStack.class);
        private final Inventory inventory = mock(Inventory.class);
        private final HumanEntity viewer = mock(HumanEntity.class);
        private final GuiManager manager = new GuiManager(plugin);

        private Harness() {
            when(plugin.getDuplicateService()).thenReturn(duplicateService);
            when(plugin.getLogger()).thenReturn(mock(Logger.class));
            when(inventory.getHolder()).thenReturn(gui);
            when(gui.getPickaxe()).thenReturn(pickaxe);
            when(pickaxe.getItemStack()).thenReturn(item);
            when(duplicateService.isUsable(item)).thenReturn(true);
            when(viewer.getName()).thenReturn("tester");
        }
    }
}
