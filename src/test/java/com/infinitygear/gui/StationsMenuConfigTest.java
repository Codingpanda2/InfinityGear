package com.infinitygear.gui;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StationsMenuConfigTest {

    @Test
    void stationGuiTextHasConfigurableDefaultsAndDocumentedPlaceholders() {
        var resource = getClass().getClassLoader().getResourceAsStream("menus/stations_menu.yml");
        assertNotNull(resource);
        var menu = YamlConfiguration.loadConfiguration(
                new InputStreamReader(resource, StandardCharsets.UTF_8));

        List<String> scalarKeys = List.of(
                "titles.runic-table", "titles.fusion-altar", "titles.gear-forge",
                "items.operations.apply-enchantment", "items.operations.remove-enchantment",
                "items.operations.transfer-enchantment", "items.operations.view-policy",
                "items.operations.fuse-pair", "items.operations.fuse-all-matching",
                "items.operations.expand-socket-capacity", "items.payment.name",
                "items.payment.option-format", "items.selected-enchantment.name",
                "items.confirm.name", "items.close.name", "previews.application.title",
                "previews.limitbreak.title", "previews.fusion.title", "previews.forge.title",
                "previews.view.title", "previews.view.enchantment-line");
        for (String key : scalarKeys) {
            assertFalse(menu.getString(key, "").isBlank(), () -> "Missing station menu text: " + key);
        }

        assertFalse(menu.getStringList("items.operations.lore").isEmpty());
        assertFalse(menu.getStringList("items.confirm.lore").isEmpty());
        List<String> applicationLore = menu.getStringList("previews.application.lore");
        for (String placeholder : List.of("%uuid%", "%enchantment%", "%current_level%",
                "%resulting_level%", "%current_sockets%", "%resulting_sockets%",
                "%socket_capacity%", "%standard_maximum%", "%absolute_maximum%",
                "%invalid_reason%")) {
            assertTrue(applicationLore.stream().anyMatch(line -> line.contains(placeholder)),
                    () -> "Application preview does not expose " + placeholder);
        }
    }
}
