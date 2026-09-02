package com.infinitygear.gear;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class GearProfileRegistryOverrideTest {

    @Test
    void autoConvertMaterialOverlapIsReportedOnceAtLoad() {
        YamlConfiguration config = new YamlConfiguration();
        for (String id : java.util.List.of("first", "second")) {
            String root = "profiles.test:" + id;
            config.set(root + ".enabled", true);
            config.set(root + ".accepted-materials", java.util.List.of("DIAMOND_AXE"));
            config.set(root + ".default-material", "DIAMOND_AXE");
            config.set(root + ".progression", "STATIC");
            config.set(root + ".auto-convert", true);
        }
        Logger logger = org.mockito.Mockito.mock(Logger.class);

        new GearProfileRegistry().load(config, logger);

        org.mockito.Mockito.verify(logger, org.mockito.Mockito.times(1))
                .warning(org.mockito.ArgumentMatchers.contains("Auto-convert material DIAMOND_AXE"));
    }

    @Test
    void loaderParsesTheCompleteSparseOverrideShape() {
        YamlConfiguration config = new YamlConfiguration();
        String root = "profiles.infinitygear:test";
        config.set(root + ".enabled", true);
        config.set(root + ".accepted-materials", java.util.List.of("DIAMOND_PICKAXE"));
        config.set(root + ".default-material", "DIAMOND_PICKAXE");
        config.set(root + ".progression", "STATIC");
        config.set(root + ".base-sockets", 3);
        config.set(root + ".maximum-expanded-sockets", 8);
        String override = root + ".enchantment-overrides.minecraft:fortune";
        config.set(override + ".enabled", false);
        config.set(override + ".unlock-level", 75);
        config.set(override + ".standard-maximum", 3);
        config.set(override + ".absolute-maximum", 7);
        config.set(override + ".socket-cost", 2);
        config.set(override + ".additional-conflicts", java.util.List.of("minecraft:dynamite"));
        config.set(override + ".removable", false);
        config.set(override + ".cost-weight", 2.5);

        GearProfileRegistry registry = new GearProfileRegistry();
        registry.load(config, Logger.getAnonymousLogger());
        var parsed = registry.find("infinitygear:test").orElseThrow()
                .enchantmentOverrides().get("minecraft:fortune");

        assertNotNull(parsed);
        assertEquals(false, parsed.enabled());
        assertEquals(75, parsed.unlockLevel());
        assertEquals(3, parsed.standardMaximum());
        assertEquals(7, parsed.absoluteMaximum());
        assertEquals(2, parsed.socketCost());
        assertEquals(java.util.Set.of("minecraft:dynamite"), parsed.additionalConflicts());
        assertEquals(false, parsed.removable());
        assertEquals(2.5, parsed.costWeight());
    }
}
