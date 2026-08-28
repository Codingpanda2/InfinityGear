package com.infinitygear.gear;

import com.infinitygear.data.GearData;
import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.core.pickaxe.PickaxeManager;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
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
}
