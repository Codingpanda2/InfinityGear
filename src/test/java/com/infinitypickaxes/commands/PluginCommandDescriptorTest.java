package com.infinitypickaxes.commands;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginCommandDescriptorTest {

    @Test
    void publicCommandAvoidsWorldEditAliasAndDefaultsToAllPlayers() {
        var resource = getClass().getClassLoader().getResourceAsStream("plugin.yml");
        assertNotNull(resource);
        YamlConfiguration pluginYaml = YamlConfiguration.loadConfiguration(
                new InputStreamReader(resource, StandardCharsets.UTF_8));

        assertEquals("InfinityGear", pluginYaml.getString("name"));
        assertEquals("com.infinitygear.InfinityGearPlugin", pluginYaml.getString("main"));
        var aliases = pluginYaml.getStringList("commands.infinitypickaxes.aliases");
        assertTrue(aliases.contains("ipickaxe"));
        assertFalse(aliases.contains("pickaxe"));
        assertEquals("infinitypickaxes.use",
                pluginYaml.getString("commands.infinitypickaxes.permission"));
        assertTrue(pluginYaml.getBoolean("permissions.infinitypickaxes.use.default"));
        assertTrue(pluginYaml.getStringList("commands.infinitygear.aliases").contains("igear"));
        assertTrue(pluginYaml.getStringList("provides").contains("InfinityPickaxes"));
    }
}
