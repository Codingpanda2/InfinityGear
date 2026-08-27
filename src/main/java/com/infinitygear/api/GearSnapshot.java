package com.infinitygear.api;

import java.util.Map;
import java.util.UUID;

public record GearSnapshot(UUID uuid, String profileId, int level, double xp, long blocksMined,
                           int socketCapacity, int usedSockets, boolean quarantined,
                           Map<String, Integer> enchantments) {
    public GearSnapshot { enchantments = Map.copyOf(enchantments); }
}
