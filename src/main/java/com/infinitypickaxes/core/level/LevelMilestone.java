package com.infinitypickaxes.core.level;

import java.util.List;

public record LevelMilestone(
        int level,
        int unlockedPerkSlots,
        List<String> newlyUnlockedEnchants
) {}
