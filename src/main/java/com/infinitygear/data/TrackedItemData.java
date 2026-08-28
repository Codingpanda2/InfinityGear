package com.infinitygear.data;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public final class TrackedItemData {
    public record Identity(UUID uuid, TrackedKind kind, String type, int schemaVersion, boolean quarantined) {}
    private TrackedItemData() {}

    public static Identity read(ItemStack item) {
        if (item == null || item.getAmount() != 1) return null;
        return readRaw(item);
    }

    /** Scanner-only read which exposes an identity even when illegally stacked. */
    public static Identity readRaw(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(GearData.KEY_MARKER, PersistentDataType.BYTE)) return null;
        try {
            UUID uuid = UUID.fromString(pdc.get(GearData.KEY_UUID, PersistentDataType.STRING));
            TrackedKind kind = TrackedKind.valueOf(pdc.get(GearData.KEY_KIND, PersistentDataType.STRING));
            String type = pdc.getOrDefault(GearData.KEY_PROFILE, PersistentDataType.STRING, kind.name().toLowerCase());
            int schema = pdc.getOrDefault(GearData.KEY_SCHEMA, PersistentDataType.INTEGER, 0);
            if (schema < 1) return null;
            boolean quarantined = pdc.getOrDefault(GearData.KEY_QUARANTINED, PersistentDataType.BYTE, (byte) 0) != 0;
            return new Identity(uuid, kind, type, schema, quarantined);
        } catch (RuntimeException invalid) { return null; }
    }

    public static void stamp(ItemStack item, TrackedKind kind, String type, UUID uuid) {
        if (item == null || item.getAmount() != 1 || kind == null || uuid == null) {
            throw new IllegalArgumentException("Tracked items must be a single item with a kind and UUID.");
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) throw new IllegalArgumentException("Tracked item has no metadata.");
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(GearData.KEY_MARKER, PersistentDataType.BYTE, (byte) 1);
        pdc.set(GearData.KEY_KIND, PersistentDataType.STRING, kind.name());
        pdc.set(GearData.KEY_UUID, PersistentDataType.STRING, uuid.toString());
        pdc.set(GearData.KEY_PROFILE, PersistentDataType.STRING, type == null ? kind.name().toLowerCase() : type);
        pdc.set(GearData.KEY_SCHEMA, PersistentDataType.INTEGER, GearData.SCHEMA_VERSION);
        item.setItemMeta(meta);
    }
}
