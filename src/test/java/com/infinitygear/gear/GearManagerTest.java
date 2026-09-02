package com.infinitygear.gear;

import com.infinitygear.data.GearData;
import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.config.ConfigManager;
import com.infinitypickaxes.core.duplicate.PickaxeDuplicateService;
import com.infinitypickaxes.core.pickaxe.PickaxeManager;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GearManagerTest {
    @Test void migratedPickaxeProfileDelegatesToLegacyFactoryAndPreservesItsDefaults() {
        InfinityPickaxes plugin = mock(InfinityPickaxes.class);
        GearProfileRegistry profiles = mock(GearProfileRegistry.class);
        GearProfile profile = mock(GearProfile.class);
        PickaxeManager pickaxes = mock(PickaxeManager.class);
        ItemStack expected = mock(ItemStack.class);
        when(profiles.find(GearData.LEGACY_PICKAXE_PROFILE)).thenReturn(Optional.of(profile));
        when(profile.enabled()).thenReturn(true);
        when(profile.id()).thenReturn(GearData.LEGACY_PICKAXE_PROFILE);
        when(profile.maximumLevel()).thenReturn(100);
        when(plugin.getPickaxeManager()).thenReturn(pickaxes);
        when(pickaxes.createPickaxe(12)).thenReturn(expected);

        ItemStack created = new GearManager(plugin, profiles).create(GearData.LEGACY_PICKAXE_PROFILE, 12);

        assertSame(expected, created);
        verify(pickaxes).createPickaxe(12);
    }

    @Test void customGearCapacityUsesTheClampedStartingLevel() {
        InfinityPickaxes plugin = mock(InfinityPickaxes.class);
        GearProfileRegistry profiles = mock(GearProfileRegistry.class);
        GearProfile profile = mock(GearProfile.class);
        when(profiles.find("test:gear")).thenReturn(Optional.of(profile));
        when(profile.enabled()).thenReturn(true);
        when(profile.id()).thenReturn("test:gear");
        when(profile.maximumLevel()).thenReturn(100);
        when(profile.defaultExternalProvider()).thenReturn("");
        when(profile.defaultMaterial()).thenReturn(Material.DIAMOND_PICKAXE);

        try (var gearData = mockStatic(GearData.class);
             var itemStacks = mockConstruction(ItemStack.class)) {
            new GearManager(plugin, profiles).create("test:gear", 250);
        }

        verify(profile).socketCapacityAtLevel(100);
        verify(profile, never()).socketCapacityAtLevel(250);
    }

    @Test void optedInArmorIsConvertedInPlaceAndKeepsItsMaterial() {
        InfinityPickaxes plugin = mock(InfinityPickaxes.class);
        GearProfileRegistry profiles = mock(GearProfileRegistry.class);
        GearProfile profile = mock(GearProfile.class);
        ItemStack armor = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        when(armor.getType()).thenReturn(Material.NETHERITE_BOOTS);
        when(armor.getAmount()).thenReturn(1);
        when(armor.getItemMeta()).thenReturn(meta);
        when(profiles.accepting(Material.NETHERITE_BOOTS, true)).thenReturn(List.of(profile));
        when(profile.id()).thenReturn("infinitygear:armor");
        when(profile.socketCapacityAtLevel(0)).thenReturn(3);
        when(profile.unbreakable()).thenReturn(true);
        when(profile.displayName()).thenReturn("");
        when(profile.lore()).thenReturn(List.of());

        try (var gearData = mockStatic(GearData.class)) {
            Optional<GearInstance> converted = new GearManager(plugin, profiles).autoConvert(armor);

            assertTrue(converted.isPresent());
            assertEquals("infinitygear:armor", converted.orElseThrow().profileId());
            assertEquals(3, converted.orElseThrow().socketCapacity());
            gearData.verify(() -> GearData.save(any(GearInstance.class), eq(false), eq(false)));
        }
        verify(armor, never()).setType(any());
        verify(meta, atLeastOnce()).setUnbreakable(true);
    }

    @Test void profilePresentationSupportsMaterialAndSocketPlaceholders() {
        ItemStack item = mock(ItemStack.class);
        GearProfile profile = mock(GearProfile.class);
        when(item.getType()).thenReturn(Material.DIAMOND_CHESTPLATE);
        when(profile.maximumLevel()).thenReturn(0);
        when(profile.maximumExpandedSockets()).thenReturn(8);
        GearInstance gear = new GearInstance(item, UUID.randomUUID(), "infinitygear:armor", 0, 0, 0, 3);

        assertEquals("Infinity Diamond Chestplate — 3/8", GearManager.render(
                "Infinity %material% — %sockets%/%max_sockets%", gear, profile));
    }

    @Test void createdGenericGearWritesConfiguredLoreToItemMeta() {
        InfinityPickaxes plugin = mock(InfinityPickaxes.class);
        GearProfileRegistry profiles = mock(GearProfileRegistry.class);
        GearProfile profile = mock(GearProfile.class);
        ItemMeta meta = mock(ItemMeta.class);
        when(profiles.find("infinitygear:bow")).thenReturn(Optional.of(profile));
        when(profile.enabled()).thenReturn(true);
        when(profile.id()).thenReturn("infinitygear:bow");
        when(profile.maximumLevel()).thenReturn(0);
        when(profile.defaultExternalProvider()).thenReturn("");
        when(profile.defaultMaterial()).thenReturn(Material.BOW);
        when(profile.socketCapacityAtLevel(0)).thenReturn(3);
        when(profile.maximumExpandedSockets()).thenReturn(8);
        when(profile.unbreakable()).thenReturn(true);
        when(profile.displayName()).thenReturn("");
        when(profile.lore()).thenReturn(List.of("<gray>Runic Sockets: %sockets%/%max_sockets%</gray>"));
        try (var gearData = mockStatic(GearData.class);
             var itemStacks = mockConstruction(ItemStack.class, (constructed, context) -> {
                 when(constructed.getItemMeta()).thenReturn(meta);
                 when(constructed.getType()).thenReturn(Material.BOW);
             })) {
            new GearManager(plugin, profiles).create("infinitygear:bow", 0);
        }

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<net.kyori.adventure.text.Component>> lore = ArgumentCaptor.forClass(List.class);
        verify(meta).lore(lore.capture());
        assertEquals(1, lore.getValue().size());
        assertEquals("Runic Sockets: 3/8", net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(lore.getValue().getFirst()));
    }

    @Test void observingExistingGenericGearRebuildsProfileLore() {
        InfinityPickaxes plugin = mock(InfinityPickaxes.class);
        GearProfileRegistry profiles = mock(GearProfileRegistry.class);
        GearProfile profile = mock(GearProfile.class);
        ItemStack bow = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        GearInstance gear = new GearInstance(bow, UUID.randomUUID(), "infinitygear:bow", 0, 0, 0, 3);
        when(bow.getType()).thenReturn(Material.BOW);
        when(bow.getAmount()).thenReturn(1);
        when(bow.getItemMeta()).thenReturn(meta);
        when(profiles.find("infinitygear:bow")).thenReturn(Optional.of(profile));
        when(profile.enabled()).thenReturn(true);
        when(profile.id()).thenReturn("infinitygear:bow");
        when(profile.accepts(Material.BOW)).thenReturn(true);
        when(profile.unbreakable()).thenReturn(true);
        when(profile.displayName()).thenReturn("");
        when(profile.maximumExpandedSockets()).thenReturn(8);
        when(profile.lore()).thenReturn(List.of("<gray>Updated lore: %sockets%/%max_sockets%</gray>"));

        try (var gearData = mockStatic(GearData.class)) {
            gearData.when(() -> GearData.isGear(bow)).thenReturn(true);
            gearData.when(() -> GearData.read(bow, 0, true))
                    .thenReturn(new GearData.ReadResult(GearData.State.VALID_NEW, gear, null));
            assertTrue(new GearManager(plugin, profiles).autoConvert(bow).isPresent());
        }

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<net.kyori.adventure.text.Component>> lore = ArgumentCaptor.forClass(List.class);
        verify(meta).lore(lore.capture());
        assertEquals("Updated lore: 3/8", net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(lore.getValue().getFirst()));
    }

    @Test void restrictedGenericGearPrependsConfiguredQuarantineLore() {
        InfinityPickaxes plugin = mock(InfinityPickaxes.class);
        GearProfileRegistry profiles = mock(GearProfileRegistry.class);
        GearProfile profile = mock(GearProfile.class);
        PickaxeDuplicateService duplicates = mock(PickaxeDuplicateService.class);
        ConfigManager configManager = mock(ConfigManager.class);
        FileConfiguration config = mock(FileConfiguration.class);
        ItemStack armor = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        UUID uuid = UUID.randomUUID();
        GearInstance gear = new GearInstance(armor, uuid, "infinitygear:armor", 0, 0, 0, 3);
        when(plugin.getDuplicateService()).thenReturn(duplicates);
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(configManager.getConfig()).thenReturn(config);
        when(duplicates.isRestricted(uuid)).thenReturn(true);
        when(config.isList("gear-lore.quarantine-lore")).thenReturn(true);
        when(config.getStringList("gear-lore.quarantine-lore"))
                .thenReturn(List.of("<red>Duplicate %uuid%</red>"));
        when(profiles.find("infinitygear:armor")).thenReturn(Optional.of(profile));
        when(profile.enabled()).thenReturn(true);
        when(profile.id()).thenReturn("infinitygear:armor");
        when(profile.displayName()).thenReturn("");
        when(profile.unbreakable()).thenReturn(true);
        when(profile.lore()).thenReturn(List.of("<gray>Armor lore</gray>"));
        when(armor.getItemMeta()).thenReturn(meta);
        when(armor.getType()).thenReturn(Material.NETHERITE_CHESTPLATE);

        new GearManager(plugin, profiles).refreshPresentation(gear);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<net.kyori.adventure.text.Component>> lore = ArgumentCaptor.forClass(List.class);
        verify(meta).lore(lore.capture());
        assertEquals(List.of("Duplicate " + uuid, "Armor lore"), lore.getValue().stream()
                .map(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()::serialize)
                .toList());
    }
}
