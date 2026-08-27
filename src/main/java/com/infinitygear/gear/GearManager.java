package com.infinitygear.gear;

import com.infinitygear.data.GearData;
import com.infinitypickaxes.InfinityPickaxes;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Optional;
import java.util.UUID;

public final class GearManager {
    private final InfinityPickaxes plugin;
    private final GearProfileRegistry profiles;

    public GearManager(InfinityPickaxes plugin, GearProfileRegistry profiles) {
        this.plugin = plugin;
        this.profiles = profiles;
    }

    public Optional<GearInstance> inspect(ItemStack item, boolean migrate) {
        var legacy = com.infinitypickaxes.core.pickaxe.PickaxeData.fromItemStack(item);
        int legacySockets = plugin.getEnchantManager() == null ? 0
                : plugin.getEnchantManager().getSocketLimit(legacy == null ? 0 : legacy.getLevel());
        GearData.ReadResult read = GearData.read(item, legacySockets, migrate);
        if (!read.valid()) {
            if (read.state() == GearData.State.MALFORMED_LEGACY || read.state() == GearData.State.MALFORMED_NEW) {
                plugin.getLogger().warning("Preserved malformed gear without assigning a new UUID: " + read.diagnostic());
            }
            return Optional.empty();
        }
        return profiles.find(read.gear().profileId()).filter(GearProfile::enabled)
                .filter(profile -> accepts(profile, item)).map(profile -> read.gear());
    }

    public ItemStack create(String profileId, int startingLevel) {
        GearProfile profile = profiles.find(profileId).filter(GearProfile::enabled)
                .orElseThrow(() -> new IllegalArgumentException("Unknown or disabled gear profile: " + profileId));
        ItemStack item;
        if (!profile.defaultExternalProvider().isBlank()) {
            if (!"nexo".equals(profile.defaultExternalProvider())
                    || !plugin.getServer().getPluginManager().isPluginEnabled("Nexo")) {
                throw new IllegalStateException("Default external item provider is unavailable: "
                        + profile.defaultExternalProvider());
            }
            var nexo = new com.infinitygear.nexo.NexoProvider();
            item = nexo.createItem(profile.defaultExternalItemId());
            if (item == null) throw new IllegalStateException("Invalid default Nexo item: "
                    + profile.defaultExternalItemId());
            item.setAmount(1);
        } else {
            item = new ItemStack(profile.defaultMaterial());
        }
        GearInstance gear = new GearInstance(item, UUID.randomUUID(), profile.id(),
                Math.min(profile.maximumLevel(), Math.max(0, startingLevel)), 0, 0,
                profile.socketCapacityAtLevel(startingLevel));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(profile.unbreakable());
            item.setItemMeta(meta);
        }
        GearData.save(gear, false, GearData.LEGACY_PICKAXE_PROFILE.equals(profile.id()));
        if (GearData.LEGACY_PICKAXE_PROFILE.equals(profile.id())) {
            var legacy = com.infinitypickaxes.core.pickaxe.PickaxeData.fromItemStack(item);
            if (legacy != null) plugin.getPickaxeManager().syncPickaxe(legacy);
        }
        return item;
    }

    private boolean accepts(GearProfile profile, ItemStack item) {
        if (profile.accepts(item.getType())) return true;
        if (profile.externalItemIds().isEmpty()
                || !plugin.getServer().getPluginManager().isPluginEnabled("Nexo")) return false;
        try {
            String id = new com.infinitygear.nexo.NexoProvider().itemId(item);
            return id != null && profile.acceptsExternal("nexo", id);
        } catch (LinkageError unavailable) {
            return false;
        }
    }
}
