package com.infinitypickaxes.core.duplicate;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.config.ConfigManager;
import com.infinitypickaxes.core.pickaxe.PickaxeData;
import io.papermc.paper.block.TileStateInventoryHolder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.DecoratedPot;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Shelf;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.DecoratedPotInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PhysicalStorageScannerTest {

    @Test
    void twoPlayersViewingSameChestWrapperDoNotCreateDuplicate() throws Exception {
        InfinityPickaxes plugin = mock(InfinityPickaxes.class);
        DuplicateStore store = mock(DuplicateStore.class);
        when(store.loadRestrictedUuids()).thenReturn(Set.of());
        PickaxeDuplicateService service = new PickaxeDuplicateService(plugin, store);

        UUID worldUuid = UUID.randomUUID();
        Container firstHolder = blockContainer(worldUuid, 12, 64, -8);
        Container secondHolder = blockContainer(worldUuid, 12, 64, -8);
        Inventory firstWrapper = chestWrapper(firstHolder);
        Inventory secondWrapper = chestWrapper(secondHolder);

        ItemStack pickaxe = mock(ItemStack.class);
        when(pickaxe.getType()).thenReturn(Material.NETHERITE_PICKAXE);
        when(pickaxe.getAmount()).thenReturn(1);
        when(firstWrapper.getSize()).thenReturn(1);
        when(secondWrapper.getSize()).thenReturn(1);
        when(firstWrapper.getItem(0)).thenReturn(pickaxe);
        when(secondWrapper.getItem(0)).thenReturn(pickaxe);

        Player firstPlayer = playerViewing("first", firstWrapper);
        Player secondPlayer = playerViewing("second", secondWrapper);
        UUID pickaxeUuid = UUID.randomUUID();

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
                MockedStatic<PickaxeData> pickaxeData = mockStatic(PickaxeData.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(firstPlayer, secondPlayer));
            bukkit.when(Bukkit::getWorlds).thenReturn(List.of());
            pickaxeData.when(() -> PickaxeData.getPickaxeUuid(pickaxe)).thenReturn(pickaxeUuid);

            DuplicateScanResult result = service.scanOnline("test:shared-chest");

            assertEquals(1, result.itemsScanned());
            assertTrue(result.duplicatesDetected().isEmpty());
            verify(store, never()).quarantine(eq(pickaxeUuid), anyString(), anyString(), anyList());
        }
    }

    @Test
    void retainedOpenedStorageIsScannedAfterPlayerSwitchesAway() throws Exception {
        InfinityPickaxes plugin = mock(InfinityPickaxes.class);
        DuplicateStore store = mock(DuplicateStore.class);
        when(store.loadRestrictedUuids()).thenReturn(Set.of());
        PickaxeDuplicateService service = new PickaxeDuplicateService(plugin, store);

        UUID worldUuid = UUID.randomUUID();
        World world = mock(World.class);
        when(world.getUID()).thenReturn(worldUuid);
        DecoratedPot liveHolder = blockStorage(DecoratedPot.class, world, 5, 40, 9);
        DecoratedPotInventory retainedWrapper = mock(DecoratedPotInventory.class);
        when(retainedWrapper.getHolder()).thenReturn(liveHolder);
        when(liveHolder.getInventory()).thenReturn(retainedWrapper);
        Block block = mock(Block.class);
        when(world.isChunkLoaded(0, 0)).thenReturn(true);
        when(world.getBlockAt(5, 40, 9)).thenReturn(block);
        when(block.getState(false)).thenReturn(liveHolder);
        PhysicalStorageKey retainedKey = PhysicalStorageKey.from(retainedWrapper).orElseThrow();
        ItemStack pickaxe = mock(ItemStack.class);
        UUID pickaxeUuid = UUID.randomUUID();
        when(pickaxe.getType()).thenReturn(Material.NETHERITE_PICKAXE);
        when(pickaxe.getAmount()).thenReturn(1);
        when(retainedWrapper.getSize()).thenReturn(1);
        when(retainedWrapper.getItem(0)).thenReturn(pickaxe);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
                MockedStatic<PickaxeData> pickaxeData = mockStatic(PickaxeData.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of());
            bukkit.when(Bukkit::getWorlds).thenReturn(List.of());
            bukkit.when(() -> Bukkit.getWorld(worldUuid)).thenReturn(world);
            pickaxeData.when(() -> PickaxeData.getPickaxeUuid(pickaxe)).thenReturn(pickaxeUuid);

            DuplicateScanResult result = service.scanOnline(
                    "test:retained-storage", List.of(retainedKey));

            assertEquals(1, result.itemsScanned());
            assertTrue(result.duplicatesDetected().isEmpty());
            verify(store, never()).quarantine(eq(pickaxeUuid), anyString(), anyString(), anyList());
        }
    }

    @Test
    void doubleChestKeyIsStableRegardlessOfSideOrder() {
        UUID worldUuid = UUID.randomUUID();
        Container left = blockContainer(worldUuid, 0, 70, 0);
        Container right = blockContainer(worldUuid, 1, 70, 0);
        DoubleChest first = mock(DoubleChest.class);
        DoubleChest reversed = mock(DoubleChest.class);
        when(first.getLeftSide()).thenReturn(left);
        when(first.getRightSide()).thenReturn(right);
        when(reversed.getLeftSide()).thenReturn(right);
        when(reversed.getRightSide()).thenReturn(left);

        assertEquals(PhysicalStorageKey.from(first), PhysicalStorageKey.from(reversed));
    }

    @Test
    void hopperMinecartUsesEntityUuid() {
        HopperMinecart hopper = mock(HopperMinecart.class);
        UUID uuid = UUID.randomUUID();
        when(hopper.getUniqueId()).thenReturn(uuid);

        assertEquals("entity:" + uuid, PhysicalStorageKey.from(hopper).orElseThrow().value());
    }

    @Test
    void decoratedPotIsPhysicalBlockStorage() {
        UUID worldUuid = UUID.randomUUID();
        World world = mock(World.class);
        when(world.getUID()).thenReturn(worldUuid);
        DecoratedPot pot = blockStorage(DecoratedPot.class, world, 3, 70, 4);

        assertEquals("block:" + worldUuid + ":3:70:4",
                PhysicalStorageKey.from(pot).orElseThrow().value());
    }

    @Test
    void shelfIsPhysicalBlockStorage() {
        UUID worldUuid = UUID.randomUUID();
        World world = mock(World.class);
        when(world.getUID()).thenReturn(worldUuid);
        Shelf shelf = blockStorage(Shelf.class, world, -2, 81, 14);

        assertEquals("block:" + worldUuid + ":-2:81:14",
                PhysicalStorageKey.from(shelf).orElseThrow().value());
    }

    @Test
    void nestedDecoratedPotInventoryIsTraversed() throws Exception {
        InfinityPickaxes plugin = mock(InfinityPickaxes.class);
        ConfigManager configManager = mock(ConfigManager.class);
        FileConfiguration config = mock(FileConfiguration.class);
        DuplicateStore store = mock(DuplicateStore.class);
        when(store.loadRestrictedUuids()).thenReturn(Set.of());
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(configManager.getConfig()).thenReturn(config);
        when(config.getInt("duplicate-protection.container-recursion-depth", 3)).thenReturn(3);
        PickaxeDuplicateService service = new PickaxeDuplicateService(plugin, store);

        ItemStack nestedPickaxe = mock(ItemStack.class);
        when(nestedPickaxe.getAmount()).thenReturn(1);
        when(nestedPickaxe.getType()).thenReturn(Material.NETHERITE_PICKAXE);
        DecoratedPot pot = mock(DecoratedPot.class);
        DecoratedPotInventory potInventory = mock(DecoratedPotInventory.class);
        when(pot.getInventory()).thenReturn(potInventory);
        when(potInventory.getContents()).thenReturn(new ItemStack[] { nestedPickaxe });
        BlockStateMeta blockMeta = mock(BlockStateMeta.class);
        when(blockMeta.getBlockState()).thenReturn(pot);
        ItemStack potItem = mock(ItemStack.class);
        when(potItem.getAmount()).thenReturn(1);
        when(potItem.getType()).thenReturn(Material.DECORATED_POT);
        when(potItem.getItemMeta()).thenReturn(blockMeta);

        try (MockedStatic<PickaxeData> pickaxeData = mockStatic(PickaxeData.class)) {
            pickaxeData.when(() -> PickaxeData.isInfinityPickaxe(nestedPickaxe)).thenReturn(true);
            assertTrue(service.containsInfinityPickaxe(inventoryContaining(potItem)));
        }
    }

    private static Container blockContainer(UUID worldUuid, int x, int y, int z) {
        World world = mock(World.class);
        when(world.getUID()).thenReturn(worldUuid);
        return blockStorage(Container.class, world, x, y, z);
    }

    private static <T extends TileStateInventoryHolder> T blockStorage(
            Class<T> type, World world, int x, int y, int z) {
        T holder = mock(type);
        when(holder.getWorld()).thenReturn(world);
        when(holder.getLocation()).thenReturn(new Location(world, x, y, z));
        return holder;
    }

    private static Inventory chestWrapper(Container holder) {
        return storageWrapper(holder);
    }

    private static Inventory storageWrapper(TileStateInventoryHolder holder) {
        Inventory inventory = mock(Inventory.class);
        when(inventory.getHolder()).thenReturn(holder);
        return inventory;
    }

    private static Inventory inventoryContaining(ItemStack item) {
        Inventory inventory = mock(Inventory.class);
        when(inventory.getContents()).thenReturn(new ItemStack[] { item });
        return inventory;
    }

    private static Player playerViewing(String name, Inventory top) {
        Player player = mock(Player.class);
        PlayerInventory personal = mock(PlayerInventory.class);
        Inventory ender = mock(Inventory.class);
        InventoryView view = mock(InventoryView.class);
        when(player.getName()).thenReturn(name);
        when(player.getInventory()).thenReturn(personal);
        when(player.getEnderChest()).thenReturn(ender);
        when(player.getOpenInventory()).thenReturn(view);
        when(view.getTopInventory()).thenReturn(top);
        return player;
    }
}
