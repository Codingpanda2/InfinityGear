package com.infinitypickaxes.listeners;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.config.MessageManager;
import com.infinitypickaxes.config.ConfigManager;
import com.infinitypickaxes.core.duplicate.PickaxeDuplicateService;
import com.infinitypickaxes.core.enchant.EnchantManager;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import com.infinitypickaxes.core.pickaxe.PickaxeManager;
import com.infinitygear.data.GearData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PickaxeInteractListenerTest {

    @Test
    void directManagedBookDropOntoManagedPickaxeIsCancelled() {
        InfinityPickaxes plugin = mock(InfinityPickaxes.class);
        EnchantManager enchantManager = mock(EnchantManager.class);
        MessageManager messages = mock(MessageManager.class);
        when(plugin.getEnchantManager()).thenReturn(enchantManager);
        when(plugin.getMessageManager()).thenReturn(messages);

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
        when(enchantManager.containsManagedEnchantBook(book)).thenReturn(true);

        try (MockedStatic<GearData> gearData = mockStatic(GearData.class)) {
            gearData.when(() -> GearData.isGear(pickaxe)).thenReturn(true);
            new PickaxeInteractListener(plugin).onDirectManagedEnchantDrop(event);
        }

        verify(event).setCancelled(true);
        verify(messages).sendMessage(player, "messages.enchant-use-socket-menu");
    }

    @Test
    void anvilEnchantedToolResultThatRaisesManagedEnchantIsCleared() {
        InfinityPickaxes plugin = mock(InfinityPickaxes.class);
        EnchantManager enchantManager = mock(EnchantManager.class);
        when(plugin.getEnchantManager()).thenReturn(enchantManager);

        ItemStack pickaxe = mock(ItemStack.class);
        ItemStack enchantedTool = mock(ItemStack.class);
        ItemStack result = mock(ItemStack.class);
        AnvilInventory inventory = mock(AnvilInventory.class);
        when(inventory.getFirstItem()).thenReturn(pickaxe);
        when(inventory.getSecondItem()).thenReturn(enchantedTool);
        when(enchantedTool.getType()).thenReturn(Material.DIAMOND_PICKAXE);
        when(enchantManager.hasManagedEnchantIncrease(pickaxe, result)).thenReturn(true);
        PrepareAnvilEvent event = mock(PrepareAnvilEvent.class);
        when(event.getInventory()).thenReturn(inventory);
        when(event.getResult()).thenReturn(result);

        try (MockedStatic<GearData> gearData = mockStatic(GearData.class)) {
            gearData.when(() -> GearData.isGear(pickaxe)).thenReturn(true);
            new PickaxeInteractListener(plugin).onPrepareAnvil(event);
        }

        verify(event).setResult(null);
    }

    @Test
    void anvilResultTakeIsCancelledEvenIfAnotherPluginRestoresThePreview() {
        InfinityPickaxes plugin = mock(InfinityPickaxes.class);
        EnchantManager enchantManager = mock(EnchantManager.class);
        when(plugin.getEnchantManager()).thenReturn(enchantManager);
        ItemStack gear = mock(ItemStack.class), result = mock(ItemStack.class);
        AnvilInventory inventory = mock(AnvilInventory.class);
        when(inventory.getFirstItem()).thenReturn(gear);
        when(enchantManager.hasAnyEnchantIncrease(gear, result)).thenReturn(true);
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getInventory()).thenReturn(inventory);
        when(event.getRawSlot()).thenReturn(2);
        when(event.getCurrentItem()).thenReturn(result);

        try (MockedStatic<GearData> gearData = mockStatic(GearData.class)) {
            gearData.when(() -> GearData.isGear(gear)).thenReturn(true);
            new PickaxeInteractListener(plugin).onAnvilResultTake(event);
        }

        verify(event).setCancelled(true);
        verify(inventory).setResult(null);
    }

    @Test
    void enchantingTableUseForManagedPickaxeIsCancelled() {
        InfinityPickaxes plugin = mock(InfinityPickaxes.class);
        ItemStack pickaxe = mock(ItemStack.class);
        EnchantItemEvent event = mock(EnchantItemEvent.class);
        when(event.getItem()).thenReturn(pickaxe);

        try (MockedStatic<GearData> gearData = mockStatic(GearData.class)) {
            gearData.when(() -> GearData.isGear(pickaxe)).thenReturn(true);
            new PickaxeInteractListener(plugin).onEnchantItem(event);
        }

        verify(event).setCancelled(true);
    }

    @Test
    void configuredRightClickTriggerAcceptsAir() {
        assertTrue(PickaxeInteractListener.matchesMenuTrigger(
                Action.RIGHT_CLICK_AIR, "SHIFT_RIGHT_CLICK", true));
    }

    @Test
    void shiftRightClickAirActuallyDispatchesConfiguredMenuCommand() {
        InfinityPickaxes plugin = mock(InfinityPickaxes.class);
        ConfigManager configs = mock(ConfigManager.class);
        PickaxeManager pickaxes = mock(PickaxeManager.class);
        PickaxeDuplicateService duplicates = mock(PickaxeDuplicateService.class);
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack item = mock(ItemStack.class);
        InfinityPickaxe pickaxe = mock(InfinityPickaxe.class);
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("interaction.trigger", "SHIFT_RIGHT_CLICK");
        config.set("interaction.allow-air-click", true);
        config.set("menu-provider.type", "COMMAND");
        config.set("menu-provider.command", "menus open %player%");

        when(plugin.getConfigManager()).thenReturn(configs);
        when(configs.getConfig()).thenReturn(config);
        when(plugin.getPickaxeManager()).thenReturn(pickaxes);
        when(plugin.getDuplicateService()).thenReturn(duplicates);
        when(player.hasPermission("infinitypickaxes.use")).thenReturn(true);
        when(player.isSneaking()).thenReturn(true);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getName()).thenReturn("Miner");
        when(inventory.getItemInMainHand()).thenReturn(item);
        when(pickaxes.getOrCreatePickaxe(item, player)).thenReturn(pickaxe);
        when(duplicates.isUsable(item)).thenReturn(true);
        when(event.getHand()).thenReturn(EquipmentSlot.HAND);
        when(event.getPlayer()).thenReturn(player);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        PickaxeInteractListener listener = spy(new PickaxeInteractListener(plugin));
        doNothing().when(listener).playMenuOpenSound(player);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            listener.onPlayerInteract(event);

            bukkit.verify(() -> Bukkit.dispatchCommand(player, "menus open Miner"));
        }
        verify(event).setCancelled(true);
    }

    @Test
    void configuredRightClickTriggerRejectsAirWhenDisabled() {
        assertFalse(PickaxeInteractListener.matchesMenuTrigger(
                Action.RIGHT_CLICK_AIR, "SHIFT_RIGHT_CLICK", false));
    }

    @Test
    void menuListenerReceivesInteractionsCancelledByOtherPlugins() throws Exception {
        EventHandler handler = PickaxeInteractListener.class
                .getMethod("onPlayerInteract", PlayerInteractEvent.class)
                .getAnnotation(EventHandler.class);

        assertFalse(handler.ignoreCancelled());
    }

    @Test
    void cancelledMenuInteractionDoesNotDispatchOrResolveGear() {
        InfinityPickaxes plugin = mock(InfinityPickaxes.class);
        PickaxeManager pickaxes = mock(PickaxeManager.class);
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(plugin.getPickaxeManager()).thenReturn(pickaxes);
        when(event.getHand()).thenReturn(EquipmentSlot.HAND);
        when(event.isCancelled()).thenReturn(true);

        new PickaxeInteractListener(plugin).onPlayerInteract(event);

        verifyNoInteractions(pickaxes);
    }
}
