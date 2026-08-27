package com.infinitygear.gui;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StationMessageCatalogTest {

    @Test
    void everySpecificStationFailureHasAnEnglishFallback() {
        var resource = getClass().getClassLoader().getResourceAsStream("locales/en.yml");
        assertNotNull(resource);
        var locale = YamlConfiguration.loadConfiguration(
                new InputStreamReader(resource, StandardCharsets.UTF_8));

        List<String> keys = List.of(
                "messages.station.session-invalid", "messages.station.stale-input", "messages.station.stale-confirmation",
                "messages.station.cancelled-by-plugin",
                "messages.station.gear-malformed", "messages.station.gear-restricted",
                "messages.station.inventory-full", "messages.station.application.missing-gear",
                "messages.station.application.missing-book", "messages.station.application.unmanaged-book",
                "messages.station.application.multiple-enchantments",
                "messages.station.limitbreak.pickaxe-profile-required",
                "messages.station.limitbreak.no-installed-enchantment",
                "messages.station.limitbreak.target-unavailable", "messages.station.limitbreak.disabled",
                "messages.station.limitbreak.enchantment-locked", "messages.station.removal.missing-source",
                "messages.station.removal.non-removable", "messages.station.transfer.missing-source",
                "messages.station.transfer.blank-book-required", "messages.station.transfer.overcap-not-supported",
                "messages.station.fusion.exactly-two-books", "messages.station.fusion.invalid-book",
                "messages.station.fusion.different-enchantments", "messages.station.fusion.different-levels",
                "messages.station.fusion.standard-maximum", "messages.station.fusion.no-matching-pair",
                "messages.station.forge.missing-gear", "messages.station.forge.at-maximum",
                "messages.station.forge.grandfathered-over-maximum", "messages.station.payment.no-option",
                "messages.station.payment.provider-unavailable", "messages.station.payment.insufficient",
                "messages.station.payment.changed", "messages.enchant.application.disabled",
                "messages.enchant.application.locked", "messages.enchant.application.equal_or_lower_level",
                "messages.enchant.application.above_standard_maximum", "messages.enchant.application.sockets_full",
                "messages.enchant.application.conflict", "messages.enchant.application.incompatible_target");

        for (String key : keys) {
            String message = locale.getString(key);
            assertNotNull(message, () -> "Missing locale fallback: " + key);
            assertFalse(message.isBlank(), () -> "Blank locale fallback: " + key);
        }
    }
}
