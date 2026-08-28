package com.infinitypickaxes.core.enchant;

import com.infinitypickaxes.utils.TextUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class EnchantSocket {

    private final String id;
    private final String keyString;
    private final NamespacedKey namespacedKey;
    private final String displayName;
    private final Material icon;
    private final int slot;
    private final boolean enabled;
    private final int unlockPickaxeLevel;
    private final int maxLevel;
    private final NavigableMap<Integer, Integer> levelScaling;
    private final List<String> description;
    private final Integer customModelData;
    private final Set<String> additionalConflicts;
    private final boolean supportsLimitBreak;

    public EnchantSocket(String id, String keyString, NamespacedKey namespacedKey, String displayName,
                         Material icon, int slot, boolean enabled, int unlockPickaxeLevel, int maxLevel,
                         NavigableMap<Integer, Integer> levelScaling, List<String> description,
                         Integer customModelData, Set<String> additionalConflicts) {
        this(id, keyString, namespacedKey, displayName, icon, slot, enabled,
                unlockPickaxeLevel, maxLevel, levelScaling, description,
                customModelData, additionalConflicts, true);
    }

    public EnchantSocket(String id, String keyString, NamespacedKey namespacedKey, String displayName,
                         Material icon, int slot, boolean enabled, int unlockPickaxeLevel, int maxLevel,
                         NavigableMap<Integer, Integer> levelScaling, List<String> description,
                         Integer customModelData, Set<String> additionalConflicts,
                         boolean supportsLimitBreak) {
        this.id = id.toLowerCase();
        this.keyString = keyString != null ? keyString.toLowerCase() : "minecraft:" + this.id;
        this.namespacedKey = namespacedKey;
        this.displayName = displayName != null ? displayName : this.id;
        this.icon = icon != null ? icon : Material.ENCHANTED_BOOK;
        this.slot = slot;
        this.enabled = enabled;
        this.unlockPickaxeLevel = Math.max(0, unlockPickaxeLevel);
        this.maxLevel = Math.max(1, maxLevel);
        this.levelScaling = levelScaling != null ? new TreeMap<>(levelScaling) : new TreeMap<>();
        this.description = description != null ? description : Collections.emptyList();
        this.customModelData = customModelData;
        this.additionalConflicts = additionalConflicts == null ? Set.of() : additionalConflicts.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        this.supportsLimitBreak = supportsLimitBreak;
    }

    public String getId() {
        return id;
    }

    public String getKeyString() {
        return keyString;
    }

    public NamespacedKey getNamespacedKey() {
        return namespacedKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCleanName() {
        return TextUtil.stripFormatting(displayName);
    }

    public Material getIcon() {
        return icon;
    }

    public int getSlot() {
        return slot;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getUnlockPickaxeLevel() {
        return unlockPickaxeLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public NavigableMap<Integer, Integer> getLevelScaling() {
        return levelScaling;
    }

    public List<String> getDescription() {
        return description;
    }

    public Integer getCustomModelData() {
        return customModelData;
    }

    public Set<String> getAdditionalConflicts() {
        return additionalConflicts;
    }

    public boolean supportsLimitBreak() {
        return supportsLimitBreak;
    }

    public boolean additionallyConflictsWith(EnchantSocket other) {
        if (other == null) return false;
        return additionallyConflictsWith(other.keyString);
    }

    public boolean additionallyConflictsWith(String enchantmentKey) {
        if (enchantmentKey == null || enchantmentKey.isBlank()) return false;
        String normalized = enchantmentKey.toLowerCase(Locale.ROOT);
        String idOnly = normalized.contains(":")
                ? normalized.substring(normalized.indexOf(':') + 1) : normalized;
        return additionalConflicts.contains(normalized) || additionalConflicts.contains(idOnly);
    }

    public boolean isUnlocked(int pickaxeLevel) {
        return pickaxeLevel >= unlockPickaxeLevel;
    }

    /**
     * Calculates the maximum enchantment level allowed on a pickaxe with the given level.
     * Uses levelScaling table if defined; otherwise defaults to maxLevel.
     */
    public int getMaxAllowedLevel(int pickaxeLevel) {
        if (!isUnlocked(pickaxeLevel)) {
            return 0;
        }
        return getMaximumAtLevel(pickaxeLevel);
    }

    /** Resolves level scaling independently from the global unlock threshold. */
    public int getMaximumAtLevel(int gearLevel) {
        if (levelScaling.isEmpty()) {
            return maxLevel;
        }
        var entry = levelScaling.floorEntry(gearLevel);
        if (entry != null) {
            return Math.min(maxLevel, entry.getValue());
        }
        return Math.min(maxLevel, 1);
    }
}
