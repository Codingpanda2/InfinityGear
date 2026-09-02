package com.infinitypickaxes.gui;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantMenuDisplayConfigTest {

    @Test
    void limitBreakPaginationAndFallbackDisplaysHaveConfigurableDefaults() {
        var menuResource = getClass().getClassLoader().getResourceAsStream("menus/enchants_menu.yml");
        assertNotNull(menuResource);
        var menu = YamlConfiguration.loadConfiguration(
                new InputStreamReader(menuResource, StandardCharsets.UTF_8));

        for (String key : List.of(
                "enchant-format.limitbreak-badges.active",
                "enchant-format.limitbreak-badges.maximum",
                "items.pagination.previous.name", "items.pagination.next.name",
                "items.empty-sockets.none-discovered.name",
                "items.empty-sockets.all-disabled.name")) {
            assertFalse(menu.getString(key, "").isBlank(), () -> "Missing configurable display: " + key);
        }
        assertTrue(menu.getString("enchant-format.limitbreak-badges.active", "")
                .contains("%extra_level%"));
        for (String direction : List.of("previous", "next")) {
            String name = menu.getString("items.pagination." + direction + ".name", "");
            assertTrue(name.contains("%target_page%"));
            assertTrue(name.contains("%total_pages%"));
        }
        assertFalse(menu.getStringList("items.empty-sockets.none-discovered.lore").isEmpty());
        assertFalse(menu.getStringList("items.empty-sockets.all-disabled.lore").isEmpty());
    }

    @Test
    void quarantineLoreIsConfigurableAndExposesStableIdentity() {
        var configResource = getClass().getClassLoader().getResourceAsStream("config.yml");
        assertNotNull(configResource);
        var config = YamlConfiguration.loadConfiguration(
                new InputStreamReader(configResource, StandardCharsets.UTF_8));
        for (String root : List.of("pickaxe-lore", "gear-lore")) {
            List<String> lore = config.getStringList(root + ".quarantine-lore");
            assertFalse(lore.isEmpty(), root);
            assertTrue(lore.stream().anyMatch(line -> line.contains("%uuid%")), root);
        }
    }
}
