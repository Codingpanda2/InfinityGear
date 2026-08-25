package com.infinitypickaxes.listeners;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.config.MessageManager;
import com.infinitypickaxes.core.enchant.EcoEnchantsHook;
import com.infinitypickaxes.core.enchant.EnchantManager;
import com.infinitypickaxes.core.pickaxe.PickaxeData;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PickaxeInteractListenerTest {

    @Test
    void directEcoBookDropOntoManagedPickaxeIsCancelled() {
        InfinityPickaxes plugin = mock(InfinityPickaxes.class);
        EnchantManager enchantManager = mock(EnchantManager.class);
        EcoEnchantsHook ecoHook = mock(EcoEnchantsHook.class);
        MessageManager messages = mock(MessageManager.class);
        when(plugin.getEnchantManager()).thenReturn(enchantManager);
        when(plugin.getMessageManager()).thenReturn(messages);
        when(enchantManager.getEcoHook()).thenReturn(ecoHook);

        Player player = mock(Player.class);
        Inventory bottom = mock(Inventory.class);
        InventoryView view = mock(InventoryView.class);
        ItemStack pickaxe = mock(ItemStack.class);
        ItemStack book = mock(ItemStack.class);
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getClickedInventory()).thenReturn(bottom);
        when(event.getView()).thenReturn(view);
        when(view.getBottomInventory()).thenReturn(bottom);
        when(event.getCurrentItem()).thenReturn(pickaxe);
        when(event.getCursor()).thenReturn(book);
        when(ecoHook.extractEnchantsFromBook(book)).thenReturn(Map.of("ecoenchants:test", 1));

        try (MockedStatic<PickaxeData> pickaxeData = mockStatic(PickaxeData.class)) {
            pickaxeData.when(() -> PickaxeData.isInfinityPickaxe(pickaxe)).thenReturn(true);
            new PickaxeInteractListener(plugin).onDirectEcoEnchantDrop(event);
        }

        verify(event).setCancelled(true);
        verify(messages).sendMessage(player, "messages.enchant-use-socket-menu");
    }

    @Test
    void anvilEcoBookResultForManagedPickaxeIsCleared() {
        InfinityPickaxes plugin = mock(InfinityPickaxes.class);
        EnchantManager enchantManager = mock(EnchantManager.class);
        EcoEnchantsHook ecoHook = mock(EcoEnchantsHook.class);
        when(plugin.getEnchantManager()).thenReturn(enchantManager);
        when(enchantManager.getEcoHook()).thenReturn(ecoHook);

        ItemStack pickaxe = mock(ItemStack.class);
        ItemStack book = mock(ItemStack.class);
        AnvilInventory inventory = mock(AnvilInventory.class);
        when(inventory.getFirstItem()).thenReturn(pickaxe);
        when(inventory.getSecondItem()).thenReturn(book);
        when(ecoHook.extractEnchantsFromBook(book)).thenReturn(Map.of("ecoenchants:test", 1));
        PrepareAnvilEvent event = mock(PrepareAnvilEvent.class);
        when(event.getInventory()).thenReturn(inventory);

        try (MockedStatic<PickaxeData> pickaxeData = mockStatic(PickaxeData.class)) {
            pickaxeData.when(() -> PickaxeData.isInfinityPickaxe(pickaxe)).thenReturn(true);
            new PickaxeInteractListener(plugin).onPrepareAnvil(event);
        }

        verify(event).setResult(null);
    }
}
