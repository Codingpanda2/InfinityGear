package com.infinitypickaxes.core.enchant;

import com.willfp.ecoenchants.target.EnchantmentTarget;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EcoEnchantsHookTest {

    @Test
    void canonicalPickaxeTargetDoesNotDependOnItemMatcher() {
        EnchantmentTarget target = mock(EnchantmentTarget.class);
        ItemStack probe = mock(ItemStack.class);
        when(target.getID()).thenReturn("pickaxe");

        assertTrue(EcoEnchantsHook.isPickaxeTarget(target, probe));
        verify(target, never()).matches(probe);
    }

    @Test
    void customTargetCanStillMatchPickaxes() {
        EnchantmentTarget target = mock(EnchantmentTarget.class);
        ItemStack probe = mock(ItemStack.class);
        when(target.getID()).thenReturn("custom-tools");
        when(target.matches(probe)).thenReturn(true);

        assertTrue(EcoEnchantsHook.isPickaxeTarget(target, probe));
    }

    @Test
    void unrelatedTargetIsRejected() {
        EnchantmentTarget target = mock(EnchantmentTarget.class);
        ItemStack probe = mock(ItemStack.class);
        when(target.getID()).thenReturn("sword");
        when(target.matches(probe)).thenReturn(false);

        assertFalse(EcoEnchantsHook.isPickaxeTarget(target, probe));
    }
}
