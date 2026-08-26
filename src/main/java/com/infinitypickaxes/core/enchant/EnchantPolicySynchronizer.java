package com.infinitypickaxes.core.enchant;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Additive synchronization plus narrowly-scoped, one-time legacy migrations. */
final class EnchantPolicySynchronizer {

    static final String REPAIRING_MIGRATION =
            "migrations.disable-legacy-repairing-on-unbreakable-pickaxes";

    private EnchantPolicySynchronizer() {
    }

    static SyncResult synchronize(FileConfiguration policy, Collection<EnchantDescriptor> liveEnchants) {
        var entries = policy.getConfigurationSection("enchants");
        if (entries == null) entries = policy.createSection("enchants");

        Set<String> liveIds = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        List<String> added = new ArrayList<>();
        List<String> updated = new ArrayList<>();
        List<String> migrated = new ArrayList<>();
        boolean repairingMigrationPending = !policy.contains(REPAIRING_MIGRATION);
        boolean repairingDescriptorProcessed = false;
        for (EnchantDescriptor ecoEnchant : liveEnchants) {
            if (ecoEnchant == null) continue;
            String id = ecoEnchant.id();
            if (id.equalsIgnoreCase("repairing")) repairingDescriptorProcessed = true;
            liveIds.add(id);
            String path = "enchants." + id;
            if (entries.isConfigurationSection(id)) {
                if (repairingMigrationPending && isLegacyRepairingDefault(policy, ecoEnchant)) {
                    policy.set(path + ".enabled", false);
                    migrated.add(id);
                }
                if (!policy.contains(path + ".display-color")) {
                    policy.set(path + ".display-color", ecoEnchant.defaultDisplayColor());
                    updated.add(id);
                }
                continue;
            }

            policy.set(path + ".key", ecoEnchant.key());
            // Repairing cannot benefit an Infinity Pickaxe because managed
            // pickaxes are unbreakable by design. Keep it visible to policy
            // synchronization, but do not offer a no-op socket by default.
            policy.set(path + ".enabled", !id.equalsIgnoreCase("repairing"));
            policy.set(path + ".unlock-pickaxe-level", 0);
            policy.set(path + ".max-level", "inherit");
            policy.set(path + ".display-color", ecoEnchant.defaultDisplayColor());
            policy.set(path + ".additional-conflicts", List.of());
            added.add(id);
        }

        boolean migrationMarkerAdded = repairingMigrationPending && repairingDescriptorProcessed;
        if (migrationMarkerAdded) policy.set(REPAIRING_MIGRATION, true);

        List<String> orphaned = entries.getKeys(false).stream()
                .filter(id -> !liveIds.contains(id))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        return new SyncResult(List.copyOf(added), orphaned, List.copyOf(updated),
                List.copyOf(migrated), migrationMarkerAdded);
    }

    private static boolean isLegacyRepairingDefault(FileConfiguration policy, EnchantDescriptor enchant) {
        if (!enchant.id().equalsIgnoreCase("repairing")) return false;
        String path = "enchants." + enchant.id();
        var section = policy.getConfigurationSection(path);
        if (section == null) return false;
        Set<String> legacyKeys = Set.of("key", "enabled", "unlock-pickaxe-level", "max-level",
                "additional-conflicts", "display-color");
        if (section.getKeys(false).stream().anyMatch(key -> !legacyKeys.contains(key))) return false;
        return enchant.key().equalsIgnoreCase(policy.getString(path + ".key", ""))
                && policy.getBoolean(path + ".enabled", true)
                && policy.getInt(path + ".unlock-pickaxe-level", 0) == 0
                && "inherit".equalsIgnoreCase(policy.getString(path + ".max-level", "inherit"))
                && policy.getStringList(path + ".additional-conflicts").isEmpty();
    }

    static int effectiveMaximum(Object configured, int nativeMaximum) {
        int safeNativeMaximum = Math.max(1, nativeMaximum);
        if (configured == null || "inherit".equalsIgnoreCase(String.valueOf(configured))) {
            return safeNativeMaximum;
        }
        try {
            int administratorMaximum = Integer.parseInt(String.valueOf(configured));
            return administratorMaximum < 1
                    ? safeNativeMaximum : Math.min(safeNativeMaximum, administratorMaximum);
        } catch (NumberFormatException exception) {
            return safeNativeMaximum;
        }
    }

    record SyncResult(List<String> added, List<String> orphaned, List<String> updated,
                      List<String> migrated, boolean migrationMarkerAdded) {
        boolean changed() {
            return !added.isEmpty() || !updated.isEmpty() || !migrated.isEmpty()
                    || migrationMarkerAdded;
        }
    }

    record EnchantDescriptor(String id, String key, String defaultDisplayColor) {
        EnchantDescriptor(String id, String key) {
            this(id, key, "<gray>");
        }
    }
}
