package com.infinitygear.gear;

import org.bukkit.Material;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record GearProfile(
        String id,
        Set<Material> acceptedMaterials,
        Map<String, Set<String>> externalItemIds,
        Material defaultMaterial,
        String defaultExternalProvider,
        String defaultExternalItemId,
        GearProgressionMode progressionMode,
        int maximumLevel,
        int baseSockets,
        int maximumExpandedSockets,
        Map<Integer, Integer> socketMilestones,
        Set<String> compatibleTargets,
        Map<String, EnchantmentOverride> enchantmentOverrides,
        List<String> lore,
        String displayName,
        boolean unbreakable,
        boolean autoConvert,
        Map<String, Double> xpSources,
        double costMultiplier,
        boolean enabled
) {
    public record EnchantmentOverride(Boolean enabled, Integer standardMaximum,
                                      Integer absoluteMaximum, Double costWeight) {}

    public GearProfile {
        id = normalizeId(id);
        acceptedMaterials = acceptedMaterials == null ? Set.of() : Set.copyOf(acceptedMaterials);
        externalItemIds = externalItemIds == null ? Map.of() : Map.copyOf(externalItemIds);
        Objects.requireNonNull(defaultMaterial, "defaultMaterial");
        defaultExternalProvider = defaultExternalProvider == null ? "" : defaultExternalProvider.toLowerCase(Locale.ROOT);
        defaultExternalItemId = defaultExternalItemId == null ? "" : defaultExternalItemId.toLowerCase(Locale.ROOT);
        if (defaultExternalProvider.isBlank() != defaultExternalItemId.isBlank()) {
            throw new IllegalArgumentException("default external provider and item id must be configured together");
        }
        Objects.requireNonNull(progressionMode, "progressionMode");
        maximumLevel = Math.max(0, maximumLevel);
        baseSockets = Math.max(0, baseSockets);
        maximumExpandedSockets = Math.max(baseSockets, maximumExpandedSockets);
        socketMilestones = socketMilestones == null ? Map.of() : Map.copyOf(socketMilestones);
        compatibleTargets = compatibleTargets == null ? Set.of() : Set.copyOf(compatibleTargets);
        enchantmentOverrides = enchantmentOverrides == null ? Map.of() : Map.copyOf(enchantmentOverrides);
        lore = lore == null ? List.of() : List.copyOf(lore);
        displayName = displayName == null ? "" : displayName;
        xpSources = xpSources == null ? Map.of() : Map.copyOf(xpSources);
        if (!Double.isFinite(costMultiplier) || costMultiplier < 0) {
            throw new IllegalArgumentException("Profile cost multiplier must be non-negative.");
        }
    }

    public boolean accepts(Material material) {
        return material != null && acceptedMaterials.contains(material);
    }

    public boolean acceptsExternal(String provider, String itemId) {
        if (provider == null || itemId == null) return false;
        return externalItemIds.getOrDefault(provider.toLowerCase(Locale.ROOT), Set.of()).stream()
                .anyMatch(itemId::equalsIgnoreCase);
    }

    public int socketCapacityAtLevel(int level) {
        int capacity = baseSockets;
        for (var entry : socketMilestones.entrySet()) {
            if (entry.getKey() <= level) capacity = Math.max(capacity, entry.getValue());
        }
        return Math.min(maximumExpandedSockets, capacity);
    }

    private static String normalizeId(String id) {
        if (id == null || !id.toLowerCase(Locale.ROOT).matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Profile id must be a stable namespaced id.");
        }
        return id.toLowerCase(Locale.ROOT);
    }
}
