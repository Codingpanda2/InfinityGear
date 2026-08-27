package com.infinitygear.station;

import com.infinitypickaxes.InfinityPickaxes;
import org.bukkit.Material;
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

    public record Definition(boolean enabled, String provider, String providerId,
                             Material vanillaMaterial, double distance, String bypassPermission) {}

    public StationManager(InfinityPickaxes plugin) {
        this.plugin = plugin;
        providers.put("VANILLA", new VanillaStationProvider());
        if (plugin.getServer().getPluginManager().isPluginEnabled("Nexo")) {
            providers.put("NEXO", new com.infinitygear.nexo.NexoProvider());
        }
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
                    section.getString("bypass-permission", "infinitygear.station." + type.configKey() + ".bypass"));
            definitions.put(type, definition);
        }
    }

    public boolean authorized(StationType type, Player player, Block block) {
        Definition definition = definitions.get(type);
        if (definition == null || !definition.enabled() || player == null) return false;
        if (!definition.bypassPermission().isBlank() && player.hasPermission(definition.bypassPermission())) return true;
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
            if (definition.bypassPermission() != null && player.hasPermission(definition.bypassPermission())) return Optional.of(type);
            if (itemId.equalsIgnoreCase(definition.providerId())
                    && location.distanceSquared(player.getLocation()) <= definition.distance() * definition.distance()) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    public Definition definition(StationType type) { return definitions.get(type); }
}
