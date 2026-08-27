package com.infinitygear.station;

import com.infinitypickaxes.InfinityPickaxes;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public final class StationManager {
    private final InfinityPickaxes plugin;
    private final Map<StationType, Definition> definitions = new EnumMap<>(StationType.class);
    private final Map<String, StationProvider> providers = new java.util.HashMap<>();
    private final StationInstanceStore instances;

    public record Definition(boolean enabled, String provider, String providerId,
                             Material vanillaMaterial, double distance, String bypassPermission,
                             boolean requireRegisteredInstance) {}

    public StationManager(InfinityPickaxes plugin) {
        this.plugin = plugin;
        providers.put("VANILLA", new VanillaStationProvider());
        if (plugin.getServer().getPluginManager().isPluginEnabled("Nexo")) {
            providers.put("NEXO", new com.infinitygear.nexo.NexoProvider());
        }
        this.instances = new StationInstanceStore(plugin.getDataFolder() == null ? null
                : new java.io.File(plugin.getDataFolder(), "station-instances.yml"), plugin.getLogger());
        reload();
    }

    public void reload() {
        definitions.clear();
        Logger logger = plugin.getLogger();
        for (StationType type : StationType.values()) {
            ConfigurationSection section = plugin.getConfigManager().getStationsConfig()
                    .getConfigurationSection("stations." + type.configKey());
            if (section == null) continue;
            String provider = section.getString("provider", "VANILLA").toUpperCase(Locale.ROOT);
            Material material = Material.matchMaterial(section.getString("material", "AIR"));
            String providerId = section.getString("nexo-id", "");
            boolean enabled = section.getBoolean("enabled", true);
            if (enabled && "VANILLA".equals(provider) && material == null) {
                logger.severe("Station " + type.configKey() + " is disabled: invalid vanilla material.");
                enabled = false;
            }
            if (enabled && "NEXO".equals(provider) && providerId.isBlank()) {
                logger.severe("Station " + type.configKey() + " is disabled: nexo-id is blank.");
                enabled = false;
            }
            if (enabled && !providers.containsKey(provider)) {
                logger.severe("Station " + type.configKey() + " is disabled: provider " + provider + " is unavailable.");
                enabled = false;
            }
            Definition definition = new Definition(enabled, provider, providerId,
                    material, Math.max(1, section.getDouble("interaction-distance", 6)),
                    section.getString("bypass-permission", "infinitygear.station." + type.configKey() + ".bypass"),
                    section.getBoolean("require-registered-instance", true));
            definitions.put(type, definition);
        }
    }

    public boolean authorized(StationType type, Player player, Block block) {
        Definition definition = definitions.get(type);
        if (definition == null || !definition.enabled() || player == null) return false;
        if (definition.requireRegisteredInstance() && !instances.find(block).filter(type::equals).isPresent()) return false;
        if (block == null || block.getWorld() != player.getWorld()
                || block.getLocation().distanceSquared(player.getLocation()) > definition.distance() * definition.distance()) return false;
        StationProvider provider = providers.get(definition.provider());
        String id = definition.provider().equals("VANILLA")
                ? (definition.vanillaMaterial() == null ? "" : definition.vanillaMaterial().name())
                : definition.providerId();
        return provider != null && provider.available() && provider.matches(block, id);
    }

    public Optional<StationType> identify(Player player, Block block) {
        for (StationType type : StationType.values()) if (authorized(type, player, block)) return Optional.of(type);
        return Optional.empty();
    }

    public Optional<StationType> identifyNexo(Player player, String itemId, org.bukkit.Location location) {
        if (player == null || itemId == null || location == null || location.getWorld() != player.getWorld()) return Optional.empty();
        for (StationType type : StationType.values()) {
            Definition definition = definitions.get(type);
            if (definition == null || !definition.enabled() || !"NEXO".equals(definition.provider())) continue;
            if (definition.requireRegisteredInstance()) {
                if (!instances.find(location).filter(type::equals).isPresent()) continue;
            }
            if (itemId.equalsIgnoreCase(definition.providerId())
                    && location.distanceSquared(player.getLocation()) <= definition.distance() * definition.distance()) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    /** Permission bypass is intentionally command-only; it never turns arbitrary clicked blocks into stations. */
    public boolean hasBypass(StationType type, Player player) {
        Definition definition = definitions.get(type);
        return definition != null && definition.enabled() && player != null
                && definition.bypassPermission() != null && !definition.bypassPermission().isBlank()
                && player.hasPermission(definition.bypassPermission());
    }

    public boolean bind(StationType type, Player actor, Block block) {
        Definition definition = definitions.get(type);
        if (definition == null || !definition.enabled() || actor == null || block == null) return false;
        if (!canManageBindings(actor)) return false;
        if (!matchesDefinition(definition, block)) return false;
        instances.bind(block, type);
        return true;
    }

    public Optional<StationType> unbind(Block block) { return instances.unbind(block); }

    public Optional<StationType> boundType(Block block) { return instances.find(block); }

    public boolean bindTargetedFurniture(StationType type, Player actor) {
        Definition definition = definitions.get(type);
        if (definition == null || !definition.enabled() || !"NEXO".equals(definition.provider())
                || !canManageBindings(actor)) return false;
        StationProvider provider = providers.get("NEXO");
        if (!(provider instanceof com.infinitygear.nexo.NexoProvider nexo)) return false;
        var target = nexo.findTargetFurniture(actor);
        if (target == null || !definition.providerId().equalsIgnoreCase(target.itemId())) return false;
        instances.bind(target.origin(), type);
        return true;
    }

    public Optional<StationType> targetedFurnitureBinding(Player player) {
        StationProvider provider = providers.get("NEXO");
        if (!(provider instanceof com.infinitygear.nexo.NexoProvider nexo)) return Optional.empty();
        var target = nexo.findTargetFurniture(player);
        return target == null ? Optional.empty() : instances.find(target.origin());
    }

    public Optional<StationType> unbindTargetedFurniture(Player player) {
        StationProvider provider = providers.get("NEXO");
        if (!(provider instanceof com.infinitygear.nexo.NexoProvider nexo)) return Optional.empty();
        var target = nexo.findTargetFurniture(player);
        return target == null ? Optional.empty() : instances.unbind(target.origin());
    }

    public Optional<StationType> unbind(Location location) { return instances.unbind(location); }

    private boolean canManageBindings(Player actor) {
        return actor != null && (actor.hasPermission("infinitygear.admin.station")
                || actor.hasPermission("infinitygear.admin") || actor.hasPermission("infinitypickaxes.admin"));
    }

    private boolean matchesDefinition(Definition definition, Block block) {
        StationProvider provider = providers.get(definition.provider());
        String id = definition.provider().equals("VANILLA")
                ? (definition.vanillaMaterial() == null ? "" : definition.vanillaMaterial().name())
                : definition.providerId();
        return provider != null && provider.available() && provider.matches(block, id);
    }

    public Definition definition(StationType type) { return definitions.get(type); }
}
