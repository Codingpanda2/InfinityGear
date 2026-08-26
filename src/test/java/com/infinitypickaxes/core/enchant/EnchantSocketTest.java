package com.infinitypickaxes.core.enchant;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantSocketTest {

    @Test
    void additionalConflictsAcceptIdsAndCanonicalKeys() {
        EnchantSocket socket = socket("telekinesis",
                Set.of("explosive", "minecraft:fortune"));

        assertTrue(socket.additionallyConflictsWith("ecoenchants:explosive"));
        assertTrue(socket.additionallyConflictsWith("minecraft:fortune"));
        assertFalse(socket.additionallyConflictsWith("minecraft:efficiency"));
    }

    @Test
    void silkTouchCanExplicitlyRejectLimitBreak() {
        NamespacedKey key = NamespacedKey.minecraft("silk_touch");
        EnchantSocket silkTouch = new EnchantSocket(
                "silk_touch", key.toString(), key, "Silk Touch", Material.ENCHANTED_BOOK,
                -1, true, 0, 1, new TreeMap<>(), List.of(), null, Set.of(), false);

        assertFalse(silkTouch.supportsLimitBreak());
        assertTrue(socket("fortune", Set.of()).supportsLimitBreak());
    }

    private static EnchantSocket socket(String id, Set<String> conflicts) {
        NamespacedKey key = new NamespacedKey("ecoenchants", id);
        return new EnchantSocket(id, key.toString(), key, id, Material.ENCHANTED_BOOK,
                -1, true, 0, 5, new TreeMap<>(), List.of(), null, conflicts);
    }
}
