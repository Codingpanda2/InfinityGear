package com.infinitypickaxes.core.enchant;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/** Immutable pickaxe-level policy loaded from enchants.yml. */
public final class PickaxeProgressionPolicy {

    private static final NavigableMap<Integer, Integer> DEFAULT_SOCKET_MILESTONES = new TreeMap<>(Map.of(
            0, 3,
            10, 4,
            25, 6,
            50, 8,
            75, 10
    ));
    private static final NavigableMap<Integer, Integer> DEFAULT_LIMITBREAK_LEVELS = new TreeMap<>(Map.of(
            50, 1,
            75, 3,
            100, 5
    ));

    private final NavigableMap<Integer, Integer> socketMilestones;
    private final int limitBreakUnlockLevel;
    private final NavigableMap<Integer, Integer> limitBreakExtraLevels;

    private PickaxeProgressionPolicy(NavigableMap<Integer, Integer> socketMilestones,
                                     int limitBreakUnlockLevel,
                                     NavigableMap<Integer, Integer> limitBreakExtraLevels) {
        this.socketMilestones = new TreeMap<>(socketMilestones);
        this.limitBreakUnlockLevel = Math.max(0, limitBreakUnlockLevel);
        this.limitBreakExtraLevels = new TreeMap<>(limitBreakExtraLevels);
    }

    public static PickaxeProgressionPolicy from(ConfigurationSection config) {
        NavigableMap<Integer, Integer> sockets = readMilestones(
                config == null ? null : config.getConfigurationSection("socket-milestones"),
                DEFAULT_SOCKET_MILESTONES
        );
        int unlockLevel = config == null ? 50 : Math.max(0, config.getInt("limitbreak.unlock-level", 50));
        NavigableMap<Integer, Integer> extraLevels = readMilestones(
                config == null ? null : config.getConfigurationSection("limitbreak.extra-levels"),
                DEFAULT_LIMITBREAK_LEVELS
        );
        return new PickaxeProgressionPolicy(sockets, unlockLevel, extraLevels);
    }

    public int getSocketLimit(int pickaxeLevel) {
        return floorValue(socketMilestones, pickaxeLevel);
    }

    public int getLimitBreakUnlockLevel() {
        return limitBreakUnlockLevel;
    }

    public boolean isLimitBreakUnlocked(int pickaxeLevel) {
        return pickaxeLevel >= limitBreakUnlockLevel;
    }

    public int getLimitBreakExtraLevels(int pickaxeLevel) {
        if (!isLimitBreakUnlocked(pickaxeLevel)) return 0;
        return floorValue(limitBreakExtraLevels, pickaxeLevel);
    }

    public int getMaximumLimitBreakExtraLevels() {
        return limitBreakExtraLevels.isEmpty() ? 0 : limitBreakExtraLevels.lastEntry().getValue();
    }

    private static NavigableMap<Integer, Integer> readMilestones(
            ConfigurationSection section,
            NavigableMap<Integer, Integer> defaults) {
        if (section == null) return new TreeMap<>(defaults);
        NavigableMap<Integer, Integer> result = new TreeMap<>();
        for (String key : section.getKeys(false)) {
            try {
                int level = Math.max(0, Integer.parseInt(key));
                int value = Math.max(0, section.getInt(key));
                result.put(level, value);
            } catch (NumberFormatException ignored) {
                // Invalid milestones are ignored; valid administrator entries remain active.
            }
        }
        return result.isEmpty() ? new TreeMap<>(defaults) : result;
    }

    private static int floorValue(NavigableMap<Integer, Integer> milestones, int pickaxeLevel) {
        var entry = milestones.floorEntry(Math.max(0, pickaxeLevel));
        return entry == null ? 0 : entry.getValue();
    }
}
