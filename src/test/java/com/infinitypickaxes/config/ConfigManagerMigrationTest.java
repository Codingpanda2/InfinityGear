package com.infinitypickaxes.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerMigrationTest {

    @Test
    void obsoletePickaxeCommandReferencesAreMigratedWithoutChangingOtherValues() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("message", "Open /pickaxe while holding the tool");
        config.set("lore", List.of("Run /pickaxe reload", "Keep this text"));
        config.set("number", 4);

        assertTrue(ConfigManager.replaceLegacyCommandAlias(config));
        assertEquals("Open /ipickaxe while holding the tool", config.getString("message"));
        assertEquals(List.of("Run /ipickaxe reload", "Keep this text"), config.getStringList("lore"));
        assertEquals(4, config.getInt("number"));
        assertFalse(ConfigManager.replaceLegacyCommandAlias(config));
    }
}
