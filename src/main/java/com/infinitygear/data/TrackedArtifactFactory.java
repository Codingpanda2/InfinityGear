package com.infinitygear.data;

import com.infinitygear.nexo.NexoProvider;
import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.UUID;

public final class TrackedArtifactFactory {
    private final InfinityPickaxes plugin;
    public TrackedArtifactFactory(InfinityPickaxes plugin) { this.plugin = plugin; }

    public ItemStack create(TrackedKind kind, String type) {
        String key = type == null || type.isBlank() ? kind.name().toLowerCase(Locale.ROOT) : type.toLowerCase(Locale.ROOT);
        ConfigurationSection config = plugin.getConfigManager().getItemsConfig()
                .getConfigurationSection("artifacts." + key);
        if (config == null || !config.getBoolean("enabled", true)) {
            throw new IllegalArgumentException("Unknown or disabled artifact: " + key);
        }
        String provider = config.getString("provider", "VANILLA").toUpperCase(Locale.ROOT);
        ItemStack item;
        if (provider.equals("NEXO")) {
            if (!plugin.getServer().getPluginManager().isPluginEnabled("Nexo")) {
                throw new IllegalStateException("Nexo is unavailable for artifact " + key + '.');
            }
            String nexoId = config.getString("nexo-item-id", "");
            NexoProvider nexo = new NexoProvider();
            if (!nexo.itemExists(nexoId) || (item = nexo.createItem(nexoId)) == null) {
                throw new IllegalStateException("Invalid Nexo item ID for artifact " + key + ": " + nexoId);
            }
            item.setAmount(1);
        } else {
            Material material = Material.matchMaterial(config.getString("material", "AMETHYST_SHARD"));
            if (material == null) throw new IllegalStateException("Invalid vanilla material for artifact " + key + '.');
            ItemBuilder builder = new ItemBuilder(material).name(config.getString("display-name", key))
                    .lore(config.getStringList("lore"));
            if (config.contains("custom-model-data")) builder.customModelData(config.getInt("custom-model-data"));
            item = builder.build();
        }
        if (!config.getBoolean("unique-tracked-identity", true)) {
            throw new IllegalStateException("Protected artifact " + key + " must use unique-tracked-identity.");
        }
        TrackedItemData.stamp(item, kind, key, UUID.randomUUID());
        return item;
    }
}
