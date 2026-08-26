package com.infinitypickaxes.core.enchant;

import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnchantManagerSecurityTest {

    @Test
    void enchantedEquipmentIsRejectedAsARegularSocketUpgradeBook() {
        EnchantManager manager = mock(EnchantManager.class, CALLS_REAL_METHODS);
        Player player = mock(Player.class);
        InfinityPickaxe pickaxe = mock(InfinityPickaxe.class);
        EnchantSocket socket = socket("fortune", true);
        ItemStack enchantedTool = mock(ItemStack.class);
        when(enchantedTool.getType()).thenReturn(Material.DIAMOND_PICKAXE);

        assertFalse(manager.handleSocketUpgrade(player, pickaxe, socket, enchantedTool));
        assertNull(manager.getBookLevel(enchantedTool, socket));
    }

    @Test
    void disabledRecognizedEnchantmentsStillConsumeCapacity() {
        EnchantSocket disabled = socket("telekinesis", false);

        assertEquals(1, EnchantManager.countRecognizedSockets(
                Set.of("minecraft:telekinesis"),
                Map.of("minecraft:telekinesis", disabled)));
    }

    @Test
    void enchantedEquipmentIncreaseIsDetectedIndependentlyOfIngredientType() {
        assertTrue(EnchantManager.hasAnyManagedLevelIncrease(
                Map.of("minecraft:fortune", 3),
                Map.of("minecraft:fortune", 4),
                List.of("minecraft:fortune")));
    }

    @Test
    void vanillaSocketUsesItsConfiguredDisplayColor() {
        YamlConfiguration policy = new YamlConfiguration();
        policy.set("enchants.fortune.display-color", "<gold>");

        assertEquals("<gold>Fortune<reset>", EnchantManager.configuredDisplayName(
                policy, "enchants.fortune", "Fortune"));
    }

    private static EnchantSocket socket(String id, boolean enabled) {
        return new EnchantSocket(id, "minecraft:" + id, NamespacedKey.minecraft(id),
                "<gray>" + id, Material.ENCHANTED_BOOK, -1, enabled, 0, 5,
                new TreeMap<>(), List.of(), null, Set.of());
    }
}
