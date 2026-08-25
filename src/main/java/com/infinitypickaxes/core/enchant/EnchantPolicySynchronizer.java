package com.infinitypickaxes.core.enchant;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Additive-only synchronization for the administrator-owned enchant policy. */
final class EnchantPolicySynchronizer {

    private EnchantPolicySynchronizer() {
    }

    static SyncResult synchronize(FileConfiguration policy, Collection<EnchantDescriptor> liveEnchants) {
        var entries = policy.getConfigurationSection("enchants");
        if (entries == null) entries = policy.createSection("enchants");

        Set<String> liveIds = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        List<String> added = new ArrayList<>();
        for (EnchantDescriptor ecoEnchant : liveEnchants) {
            if (ecoEnchant == null) continue;
            String id = ecoEnchant.id();
            liveIds.add(id);
            if (entries.isConfigurationSection(id)) continue;

            String path = "enchants." + id;
            policy.set(path + ".key", ecoEnchant.key());
            policy.set(path + ".enabled", true);
            policy.set(path + ".unlock-pickaxe-level", 0);
            policy.set(path + ".max-level", "inherit");
            policy.set(path + ".additional-conflicts", List.of());
            added.add(id);
        }

        List<String> orphaned = entries.getKeys(false).stream()
                .filter(id -> !liveIds.contains(id))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        return new SyncResult(List.copyOf(added), orphaned);
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

    record SyncResult(List<String> added, List<String> orphaned) {
        boolean changed() {
            return !added.isEmpty();
        }
    }

    record EnchantDescriptor(String id, String key) {
    }
}
