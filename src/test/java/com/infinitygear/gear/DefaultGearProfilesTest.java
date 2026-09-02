package com.infinitygear.gear;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultGearProfilesTest {
    @Test void everyGenericProfileShipsWithLoreButNoForcedName() {
        YamlConfiguration profiles = YamlConfiguration.loadConfiguration(
                new File("src/main/resources/profiles.yml"));

        for (String id : List.of("pickaxe", "axe", "sword", "shovel", "hoe", "bow", "armor")) {
            String root = "profiles.infinitygear:" + id;
            assertTrue(profiles.getString(root + ".display-name", "").isBlank(), id);
            assertFalse(profiles.getStringList(root + ".lore").isEmpty(), id);
        }
        for (String id : List.of("axe", "sword", "shovel", "hoe", "bow", "armor")) {
            String root = "profiles.infinitygear:" + id;
            assertTrue(profiles.getStringList(root + ".lore").stream()
                    .anyMatch(line -> line.contains("%sockets%")), id);
        }
    }
}
