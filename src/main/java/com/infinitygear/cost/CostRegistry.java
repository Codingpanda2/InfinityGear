package com.infinitygear.cost;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/** Additive configuration loader which disables an entire malformed option, never individual components. */
public final class CostRegistry {
    private final Map<String, List<PaymentOption>> operations = new LinkedHashMap<>();

    public void load(FileConfiguration config, Logger logger) {
        operations.clear();
        ConfigurationSection root = config == null ? null : config.getConfigurationSection("operations");
        if (root == null) { logger.severe("costs.yml has no operations section; mutation operations are disabled."); return; }
        for (String operation : root.getKeys(false)) {
            ConfigurationSection options = root.getConfigurationSection(operation + ".options");
            List<PaymentOption> parsed = new ArrayList<>();
            if (options != null) for (String optionId : options.getKeys(false)) {
                try {
                    List<CostComponent> components = new ArrayList<>();
                    if (!options.contains(optionId + ".components")) {
                        throw new IllegalArgumentException("components must be present (use [] for explicit free)");
                    }
                    for (Map<?, ?> raw : options.getMapList(optionId + ".components")) {
                        CostComponent.Type type = CostComponent.Type.valueOf(
                                String.valueOf(raw.get("type")).toUpperCase(Locale.ROOT));
                        String provider = String.valueOf(raw.containsKey("provider") ? raw.get("provider") : "");
                        String item = String.valueOf(raw.containsKey("item") ? raw.get("item") : "");
                        Object amountValue = raw.get("amount");
                        if (!(amountValue instanceof Number amount)) throw new IllegalArgumentException("amount is required");
                        boolean consumed = !raw.containsKey("consumed") || Boolean.parseBoolean(String.valueOf(raw.get("consumed")));
                        components.add(new CostComponent(type, provider, item, amount.doubleValue(), consumed));
                    }
                    parsed.add(new PaymentOption(optionId, components));
                } catch (RuntimeException invalid) {
                    logger.severe("Disabled invalid payment option " + operation + '.' + optionId + ": " + invalid.getMessage());
                }
            }
            if (parsed.isEmpty()) logger.severe("Operation " + operation + " has no valid payment option and is disabled.");
            operations.put(operation.toLowerCase(Locale.ROOT), List.copyOf(parsed));
        }
    }

    public List<PaymentOption> options(String operation) {
        return operation == null ? List.of() : operations.getOrDefault(operation.toLowerCase(Locale.ROOT), List.of());
    }

    public void disableUnavailableProviders(MoneyGateway money,
            com.infinitygear.nexo.NexoProvider nexo, Logger logger) {
        disableUnavailableProviders(money, nexo, null, logger);
    }

    public void disableUnavailableProviders(MoneyGateway money,
            com.infinitygear.nexo.NexoProvider nexo, FileConfiguration items, Logger logger) {
        operations.replaceAll((operation, options) -> {
            List<PaymentOption> usable = options.stream().filter(option -> {
                for (CostComponent component : option.components()) {
                    boolean available = switch (component.type()) {
                        case MONEY -> money != null && money.available();
                        case VANILLA_ITEM -> org.bukkit.Material.matchMaterial(component.itemId()) != null;
                        case EXTERNAL_ITEM -> "nexo".equals(component.provider()) && nexo != null
                                && nexo.itemExists(component.itemId());
                        case TRACKED_ARTIFACT -> items == null || validTrackedArtifact(items, component.itemId());
                        default -> true;
                    };
                    if (!available) {
                        logger.severe("Disabled payment option " + operation + '.' + option.id()
                                + " because provider/item is unavailable for " + component.type()
                                + (component.itemId().isBlank() ? "" : " " + component.itemId()) + '.');
                        return false;
                    }
                }
                return true;
            }).toList();
            if (usable.isEmpty()) logger.severe("Operation " + operation
                    + " has no provider-usable payment option and is disabled.");
            return usable;
        });
    }

    private static boolean validTrackedArtifact(FileConfiguration items, String id) {
        String root = "artifacts." + id + '.';
        return items.getConfigurationSection("artifacts." + id) != null
                && items.getBoolean(root + "enabled", true)
                && items.getBoolean(root + "unique-tracked-identity", true);
    }
}
