package com.infinitypickaxes.listeners;

import com.infinitygear.data.GearData;
import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.config.MessageManager;
import com.infinitypickaxes.core.duplicate.PickaxeDuplicateService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class QuarantineListenerTest {

    @Test
    void blockBreakGateRunsAtLowestAndIgnoresCancelledEvents() throws Exception {
        EventHandler handler = QuarantineListener.class
                .getMethod("onBlockBreak", BlockBreakEvent.class)
                .getAnnotation(EventHandler.class);

        assertEquals(EventPriority.LOWEST, handler.priority());
        assertTrue(handler.ignoreCancelled());
    }

    @Test
    void restrictedGenericGearIsBlocked() {
        InfinityPickaxes plugin = mock(InfinityPickaxes.class);
        PickaxeDuplicateService duplicates = mock(PickaxeDuplicateService.class);
        MessageManager messages = mock(MessageManager.class);
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack axe = mock(ItemStack.class);
        BlockBreakEvent event = mock(BlockBreakEvent.class);
        when(plugin.getDuplicateService()).thenReturn(duplicates);
        when(plugin.getMessageManager()).thenReturn(messages);
        when(event.getPlayer()).thenReturn(player);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(axe);
        when(duplicates.isUsable(axe)).thenReturn(false);

        try (var gearData = mockStatic(GearData.class)) {
            gearData.when(() -> GearData.isGear(axe)).thenReturn(true);
            new QuarantineListener(plugin).onBlockBreak(event);
        }

        verify(event).setCancelled(true);
        verify(messages).sendMessage(player, "messages.gear-quarantined");
    }

    @Test
    void restrictedBowCannotFire() {
        InfinityPickaxes plugin = mock(InfinityPickaxes.class);
        PickaxeDuplicateService duplicates = mock(PickaxeDuplicateService.class);
        MessageManager messages = mock(MessageManager.class);
        Player player = mock(Player.class);
        ItemStack bow = mock(ItemStack.class);
        EntityShootBowEvent event = mock(EntityShootBowEvent.class);
        when(plugin.getDuplicateService()).thenReturn(duplicates);
        when(plugin.getMessageManager()).thenReturn(messages);
        when(event.getEntity()).thenReturn(player);
        when(event.getBow()).thenReturn(bow);
        when(duplicates.isUsable(bow)).thenReturn(false);

        try (var gearData = mockStatic(GearData.class)) {
            gearData.when(() -> GearData.isGear(bow)).thenReturn(true);
            new QuarantineListener(plugin).onShootBow(event);
        }

        verify(event).setCancelled(true);
        verify(messages).sendMessage(player, "messages.gear-quarantined");
    }

    @Test
    void restrictedArmorCannotBePlacedIntoArmorSlot() {
        InfinityPickaxes plugin = mock(InfinityPickaxes.class);
        PickaxeDuplicateService duplicates = mock(PickaxeDuplicateService.class);
        MessageManager messages = mock(MessageManager.class);
        Player player = mock(Player.class);
        ItemStack armor = mock(ItemStack.class);
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(plugin.getDuplicateService()).thenReturn(duplicates);
        when(plugin.getMessageManager()).thenReturn(messages);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getSlotType()).thenReturn(InventoryType.SlotType.ARMOR);
        when(event.getClick()).thenReturn(ClickType.LEFT);
        when(event.getCursor()).thenReturn(armor);
        when(duplicates.isUsable(armor)).thenReturn(false);

        try (var gearData = mockStatic(GearData.class)) {
            gearData.when(() -> GearData.isGear(armor)).thenReturn(true);
            new QuarantineListener(plugin).onArmorInventoryClick(event);
        }

        verify(event).setCancelled(true);
        verify(messages).sendMessage(player, "messages.gear-quarantined");
    }
}
