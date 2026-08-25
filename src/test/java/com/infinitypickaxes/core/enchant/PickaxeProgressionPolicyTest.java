package com.infinitypickaxes.core.enchant;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PickaxeProgressionPolicyTest {

    @Test
    void defaultMilestonesMatchProgressionContract() {
        PickaxeProgressionPolicy policy = PickaxeProgressionPolicy.from(new YamlConfiguration());

        assertEquals(3, policy.getSocketLimit(0));
        assertEquals(4, policy.getSocketLimit(10));
        assertEquals(6, policy.getSocketLimit(49));
        assertEquals(8, policy.getSocketLimit(50));
        assertEquals(10, policy.getSocketLimit(100));
        assertFalse(policy.isLimitBreakUnlocked(49));
        assertTrue(policy.isLimitBreakUnlocked(50));
        assertEquals(0, policy.getLimitBreakExtraLevels(49));
        assertEquals(1, policy.getLimitBreakExtraLevels(50));
        assertEquals(3, policy.getLimitBreakExtraLevels(75));
        assertEquals(5, policy.getLimitBreakExtraLevels(100));
    }

    @Test
    void administratorMilestonesReplaceDefaultsAndUseFloorLookup() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("socket-milestones.0", 2);
        config.set("socket-milestones.20", 7);
        config.set("limitbreak.unlock-level", 30);
        config.set("limitbreak.extra-levels.30", 2);
        config.set("limitbreak.extra-levels.60", 4);

        PickaxeProgressionPolicy policy = PickaxeProgressionPolicy.from(config);

        assertEquals(2, policy.getSocketLimit(19));
        assertEquals(7, policy.getSocketLimit(20));
        assertFalse(policy.isLimitBreakUnlocked(29));
        assertEquals(2, policy.getLimitBreakExtraLevels(30));
        assertEquals(4, policy.getMaximumLimitBreakExtraLevels());
    }
}
