package com.infinitygear.gear;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

public final class GearProfileRegistry {
    private final Map<String, GearProfile> profiles = new LinkedHashMap<>();

    public void load(FileConfiguration config, Logger logger) {
        profiles.clear();
        ConfigurationSection root = config == null ? null : config.getConfigurationSection("profiles");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            try {
                ConfigurationSection section = root.getConfigurationSection(id);
                if (section == null) continue;
                Set<Material> materials = new LinkedHashSet<>();
                for (String raw : section.getStringList("accepted-materials")) {
                    Material material = Material.matchMaterial(raw);
                    if (material == null) logger.warning("Unknown material '" + raw + "' in profile " + id);
                    else materials.add(material);
                }
                Material defaultMaterial = Material.matchMaterial(section.getString("default-material", "STONE"));
                if (defaultMaterial == null) throw new IllegalArgumentException("invalid default material");
                GearProgressionMode mode = GearProgressionMode.valueOf(
                        section.getString("progression", "STATIC").toUpperCase(Locale.ROOT));
                Map<Integer, Integer> milestones = new LinkedHashMap<>();
                ConfigurationSection milestoneSection = section.getConfigurationSection("socket-milestones");
                if (milestoneSection != null) for (String key : milestoneSection.getKeys(false)) {
                    milestones.put(Integer.parseInt(key), milestoneSection.getInt(key));
                }
                Map<String, Set<String>> externalItems = new LinkedHashMap<>();
                ConfigurationSection external = section.getConfigurationSection("external-items");
                if (external != null) for (String provider : external.getKeys(false)) {
                    externalItems.put(provider.toLowerCase(Locale.ROOT), Set.copyOf(external.getStringList(provider)));
                }
                Map<String, GearProfile.EnchantmentOverride> enchantmentOverrides = new LinkedHashMap<>();
                ConfigurationSection overrideSection = section.getConfigurationSection("enchantment-overrides");
                if (overrideSection != null) for (String enchantment : overrideSection.getKeys(false)) {
                    ConfigurationSection override = overrideSection.getConfigurationSection(enchantment);
                    if (override == null) continue;
                    enchantmentOverrides.put(enchantment.toLowerCase(Locale.ROOT), new GearProfile.EnchantmentOverride(
                            override.contains("enabled") ? override.getBoolean("enabled") : null,
                            override.contains("standard-maximum") ? override.getInt("standard-maximum") : null,
                            override.contains("absolute-maximum") ? override.getInt("absolute-maximum") : null,
                            override.contains("cost-weight") ? override.getDouble("cost-weight") : null));
                }
                GearProfile profile = new GearProfile(id, materials, externalItems, defaultMaterial,
                        section.getString("default-item.provider", ""),
                        section.getString("default-item.id", ""), mode,
                        section.getInt("maximum-level", 0), section.getInt("base-sockets", 0),
                        section.getInt("maximum-expanded-sockets", section.getInt("base-sockets", 0)),
                        milestones,
                        section.getStringList("compatible-targets").stream()
                                .map(value -> value.toUpperCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet()),
                        enchantmentOverrides, section.getStringList("lore"), section.getString("display-name", ""),
                        section.getBoolean("unbreakable", true), section.getBoolean("auto-convert", false),
                        readDoubleMap(section.getConfigurationSection("xp-sources")),
                        section.getDouble("cost-multiplier", 1.0), section.getBoolean("enabled", true));
                profiles.put(profile.id(), profile);
            } catch (RuntimeException invalid) {
                logger.warning("Disabled invalid gear profile '" + id + "': " + invalid.getMessage());
            }
        }
    }

    private static Map<String, Double> readDoubleMap(ConfigurationSection section) {
        if (section == null) return Map.of();
        Map<String, Double> result = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) result.put(key, section.getDouble(key));
        return result;
    }

    public Optional<GearProfile> find(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(profiles.get(id.toLowerCase(Locale.ROOT)));
    }

    public Collection<GearProfile> all() { return List.copyOf(profiles.values()); }

    public List<GearProfile> accepting(Material material, boolean autoConvertOnly) {
        List<GearProfile> result = new ArrayList<>();
        for (GearProfile profile : profiles.values()) {
            if (profile.enabled() && profile.accepts(material) && (!autoConvertOnly || profile.autoConvert())) result.add(profile);
        }
        return List.copyOf(result);
    }
}
