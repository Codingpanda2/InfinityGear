package com.infinitypickaxes.core.pickaxe;

import com.infinitypickaxes.InfinityPickaxes;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public final class PickaxeData {

    public static final NamespacedKey KEY_IS_INFINITY;
    public static final NamespacedKey KEY_UUID;
    public static final NamespacedKey KEY_OWNER_UUID;
    public static final NamespacedKey KEY_OWNER_NAME;
    public static final NamespacedKey KEY_LEVEL;
    public static final NamespacedKey KEY_XP;
    public static final NamespacedKey KEY_BLOCKS_MINED;
    public static final NamespacedKey KEY_ENCHANTS;
    public static final NamespacedKey KEY_PERKS;

    static {
        InfinityPickaxes plugin = InfinityPickaxes.getInstance();
        KEY_IS_INFINITY = new NamespacedKey(plugin, "is_infinity_pickaxe");
        KEY_UUID = new NamespacedKey(plugin, "pickaxe_uuid");
        KEY_OWNER_UUID = new NamespacedKey(plugin, "owner_uuid");
        KEY_OWNER_NAME = new NamespacedKey(plugin, "owner_name");
        KEY_LEVEL = new NamespacedKey(plugin, "level");
        KEY_XP = new NamespacedKey(plugin, "xp");
        KEY_BLOCKS_MINED = new NamespacedKey(plugin, "blocks_mined");
        KEY_ENCHANTS = new NamespacedKey(plugin, "enchants_data");
        KEY_PERKS = new NamespacedKey(plugin, "perks_data");
    }

    private PickaxeData() {}

    /**
     * Checks if an ItemStack is an Infinity Pickaxe.
     */
    public static boolean isInfinityPickaxe(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(KEY_IS_INFINITY, PersistentDataType.BYTE);
    }

    /**
     * Reads and parses an InfinityPickaxe instance from an ItemStack.
     */
    public static InfinityPickaxe fromItemStack(ItemStack item) {
        if (!isInfinityPickaxe(item)) return null;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        UUID uuid;
        String uuidStr = pdc.get(KEY_UUID, PersistentDataType.STRING);
        if (uuidStr != null) {
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (Exception e) {
                uuid = UUID.randomUUID();
            }
        } else {
            uuid = UUID.randomUUID();
        }

        UUID ownerUuid = null;
        String ownerUuidStr = pdc.get(KEY_OWNER_UUID, PersistentDataType.STRING);
        if (ownerUuidStr != null) {
            try {
                ownerUuid = UUID.fromString(ownerUuidStr);
            } catch (Exception ignored) {}
        }

        String ownerName = pdc.getOrDefault(KEY_OWNER_NAME, PersistentDataType.STRING, "Desconocido");
        int level = pdc.getOrDefault(KEY_LEVEL, PersistentDataType.INTEGER, 0);
        double xp = pdc.getOrDefault(KEY_XP, PersistentDataType.DOUBLE, 0.0);
        long blocksMined = pdc.getOrDefault(KEY_BLOCKS_MINED, PersistentDataType.LONG, 0L);

        Map<String, Integer> enchants = deserializeEnchants(pdc.get(KEY_ENCHANTS, PersistentDataType.STRING));
        Set<String> perks = deserializePerks(pdc.get(KEY_PERKS, PersistentDataType.STRING));

        return new InfinityPickaxe(item, uuid, ownerUuid, ownerName, level, xp, blocksMined, enchants, perks);
    }

    /**
     * Saves the InfinityPickaxe data into the ItemStack's PDC.
     */
    public static void saveToItemStack(InfinityPickaxe pickaxe, ItemStack item) {
        if (item == null || pickaxe == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_IS_INFINITY, PersistentDataType.BYTE, (byte) 1);
        pdc.set(KEY_UUID, PersistentDataType.STRING, pickaxe.getUuid().toString());
        if (pickaxe.getOwnerUuid() != null) {
            pdc.set(KEY_OWNER_UUID, PersistentDataType.STRING, pickaxe.getOwnerUuid().toString());
        }
        pdc.set(KEY_OWNER_NAME, PersistentDataType.STRING, pickaxe.getOwnerName());
        pdc.set(KEY_LEVEL, PersistentDataType.INTEGER, pickaxe.getLevel());
        pdc.set(KEY_XP, PersistentDataType.DOUBLE, pickaxe.getXp());
        pdc.set(KEY_BLOCKS_MINED, PersistentDataType.LONG, pickaxe.getBlocksMined());
        pdc.set(KEY_ENCHANTS, PersistentDataType.STRING, serializeEnchants(pickaxe.getEnchantments()));
        pdc.set(KEY_PERKS, PersistentDataType.STRING, serializePerks(pickaxe.getEquippedPerks()));

        // Ensure unbreakable
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
    }

    public static String serializeEnchants(Map<String, Integer> enchants) {
        if (enchants == null || enchants.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
            if (sb.length() > 0) sb.append(";");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    public static Map<String, Integer> deserializeEnchants(String data) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (data == null || data.trim().isEmpty()) return map;

        String[] pairs = data.split(";");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                try {
                    map.put(kv[0].toLowerCase(), Integer.parseInt(kv[1]));
                } catch (NumberFormatException ignored) {}
            }
        }
        return map;
    }

    public static String serializePerks(Set<String> perks) {
        if (perks == null || perks.isEmpty()) return "";
        return String.join(",", perks);
    }

    public static Set<String> deserializePerks(String data) {
        Set<String> set = new HashSet<>();
        if (data == null || data.trim().isEmpty()) return set;
        String[] split = data.split(",");
        for (String s : split) {
            if (!s.trim().isEmpty()) {
                set.add(s.trim().toLowerCase());
            }
        }
        return set;
    }
}
