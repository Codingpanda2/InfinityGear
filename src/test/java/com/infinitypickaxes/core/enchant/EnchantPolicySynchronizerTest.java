package com.infinitypickaxes.core.enchant;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantPolicySynchronizerTest {

    @Test
    void addsOnlyMissingEntriesAndPreservesAdministratorEditsAndOrphans() {
        YamlConfiguration policy = new YamlConfiguration();
        policy.set("enchants.telekinesis.key", "administrator:do-not-overwrite");
        policy.set("enchants.telekinesis.enabled", false);
        policy.set("enchants.telekinesis.unlock-pickaxe-level", 42);
        policy.set("enchants.telekinesis.max-level", 3);
        policy.set("enchants.telekinesis.additional-conflicts", List.of("explosive"));
        policy.set("enchants.removed_enchant.enabled", true);

        EnchantPolicySynchronizer.SyncResult result = EnchantPolicySynchronizer.synchronize(
                policy, List.of(enchant("telekinesis"), enchant("replenish")));

        assertEquals(List.of("replenish"), result.added());
        assertEquals(List.of("removed_enchant"), result.orphaned());
        assertEquals("administrator:do-not-overwrite", policy.getString("enchants.telekinesis.key"));
        assertFalse(policy.getBoolean("enchants.telekinesis.enabled"));
        assertEquals(42, policy.getInt("enchants.telekinesis.unlock-pickaxe-level"));
        assertEquals(3, policy.getInt("enchants.telekinesis.max-level"));
        assertEquals(List.of("explosive"), policy.getStringList("enchants.telekinesis.additional-conflicts"));
        assertEquals("ecoenchants:replenish", policy.getString("enchants.replenish.key"));
        assertTrue(policy.getBoolean("enchants.replenish.enabled"));
        assertEquals("inherit", policy.getString("enchants.replenish.max-level"));
    }

    @Test
    void secondSynchronizationIsIdempotent() {
        YamlConfiguration policy = new YamlConfiguration();
        EnchantPolicySynchronizer.EnchantDescriptor enchant = enchant("telekinesis");

        assertTrue(EnchantPolicySynchronizer.synchronize(policy, List.of(enchant)).changed());
        assertFalse(EnchantPolicySynchronizer.synchronize(policy, List.of(enchant)).changed());
    }

    @Test
    void newlyDiscoveredRepairingIsDisabledBecausePickaxesAreUnbreakable() {
        YamlConfiguration policy = new YamlConfiguration();

        EnchantPolicySynchronizer.synchronize(policy, List.of(enchant("repairing")));

        assertFalse(policy.getBoolean("enchants.repairing.enabled"));
    }

    @Test
    void vanillaManagedEnchantmentsReceiveNormalPolicyEntries() {
        YamlConfiguration policy = new YamlConfiguration();

        EnchantPolicySynchronizer.synchronize(policy,
                List.of(new EnchantPolicySynchronizer.EnchantDescriptor(
                                "fortune", "minecraft:fortune"),
                        new EnchantPolicySynchronizer.EnchantDescriptor(
                                "silk_touch", "minecraft:silk_touch")));

        assertTrue(policy.getBoolean("enchants.fortune.enabled"));
        assertEquals("minecraft:fortune", policy.getString("enchants.fortune.key"));
        assertTrue(policy.getBoolean("enchants.silk_touch.enabled"));
        assertEquals("minecraft:silk_touch", policy.getString("enchants.silk_touch.key"));
    }

    @Test
    void configuredMaximumCanOnlyTightenEcoMaximum() {
        assertEquals(10, EnchantPolicySynchronizer.effectiveMaximum("inherit", 10));
        assertEquals(6, EnchantPolicySynchronizer.effectiveMaximum(6, 10));
        assertEquals(10, EnchantPolicySynchronizer.effectiveMaximum(30, 10));
        assertEquals(10, EnchantPolicySynchronizer.effectiveMaximum("invalid", 10));
    }

    private static EnchantPolicySynchronizer.EnchantDescriptor enchant(String id) {
        return new EnchantPolicySynchronizer.EnchantDescriptor(id, "ecoenchants:" + id);
    }
}
