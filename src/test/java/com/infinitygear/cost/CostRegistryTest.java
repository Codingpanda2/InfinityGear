package com.infinitygear.cost;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CostRegistryTest {
    @Test void missingComponentsNeverAccidentallyCreatesFreeOption() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("operations.test.options.typo.unrelated", true);
        CostRegistry registry = new CostRegistry();
        registry.load(config, mock(Logger.class));
        assertTrue(registry.options("test").isEmpty());
    }

    @Test void explicitEmptyComponentsRepresentsFree() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("operations.test.options.free.components", List.of());
        CostRegistry registry = new CostRegistry();
        registry.load(config, mock(Logger.class));
        assertEquals(1, registry.options("test").size());
        assertTrue(registry.options("test").getFirst().free());
    }

    @Test void unavailableMoneyProviderDisablesWholeAndOption() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("operations.test.options.money-and-xp.components", List.of(
                Map.of("type", "MONEY", "provider", "vault", "amount", 10),
                Map.of("type", "XP_LEVELS", "amount", 2)));
        CostRegistry registry = new CostRegistry();
        registry.load(config, mock(Logger.class));
        registry.disableUnavailableProviders(new UnavailableMoneyGateway(), null, mock(Logger.class));
        assertTrue(registry.options("test").isEmpty());
    }

    @Test void missingTrackedArtifactConfigurationDisablesOption() {
        YamlConfiguration costs = new YamlConfiguration();
        costs.set("operations.test.options.artifact.components", List.of(
                Map.of("type", "TRACKED_ARTIFACT", "item", "missing", "amount", 1)));
        CostRegistry registry = new CostRegistry();
        registry.load(costs, mock(Logger.class));
        registry.disableUnavailableProviders(new UnavailableMoneyGateway(), null,
                new YamlConfiguration(), mock(Logger.class));
        assertTrue(registry.options("test").isEmpty());
    }
}
