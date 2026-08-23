package com.infinitypickaxes.core.enchant;

import com.infinitypickaxes.InfinityPickaxes;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class EcoEnchantsHook {

    private final InfinityPickaxes plugin;
    private boolean ecoEnchantsPresent = false;
    private boolean ecoFrameworkPresent = false;

    public EcoEnchantsHook(InfinityPickaxes plugin) {
        this.plugin = plugin;
        checkPlugins();
    }

    public void checkPlugins() {
        this.ecoEnchantsPresent = Bukkit.getPluginManager().isPluginEnabled("EcoEnchants");
        this.ecoFrameworkPresent = Bukkit.getPluginManager().isPluginEnabled("eco");

        if (ecoEnchantsPresent) {
            plugin.getLogger().info("EcoEnchants detectado exitosamente. Habilitando compatibilidad con encantamientos custom.");
        } else {
            plugin.getLogger().info("EcoEnchants no está presente. Operando en modo Vanilla/Bukkit Registry estándar.");
        }
    }

    public boolean isEcoEnchantsPresent() {
        return ecoEnchantsPresent;
    }

    public boolean isEcoFrameworkPresent() {
        return ecoFrameworkPresent;
    }

    /**
     * Extracts enchantment key and level from any book (Vanilla EnchantedBook, EcoEnchants book, or custom PDC).
     */
    public Map<String, Integer> extractEnchantsFromBook(ItemStack bookItem) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (bookItem == null || !bookItem.hasItemMeta()) return result;

        ItemMeta meta = bookItem.getItemMeta();

        // 1. Check Vanilla EnchantmentStorageMeta (Stored Enchants on Book)
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            for (Map.Entry<Enchantment, Integer> entry : storageMeta.getStoredEnchants().entrySet()) {
                if (entry.getKey() != null && entry.getKey().getKey() != null) {
                    result.put(entry.getKey().getKey().toString().toLowerCase(), entry.getValue());
                }
            }
        }

        // 2. Check Direct Enchants on Item
        if (meta.hasEnchants()) {
            for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                if (entry.getKey() != null && entry.getKey().getKey() != null) {
                    result.put(entry.getKey().getKey().toString().toLowerCase(), entry.getValue());
                }
            }
        }

        // 3. Check PersistentDataContainer for Eco / EcoEnchants custom tags if applicable
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        for (NamespacedKey key : pdc.getKeys()) {
            if (key.getNamespace().equalsIgnoreCase("ecoenchants") || key.getNamespace().equalsIgnoreCase("eco")) {
                Integer lvl = pdc.get(key, PersistentDataType.INTEGER);
                if (lvl != null && lvl > 0) {
                    result.put(key.toString().toLowerCase(), lvl);
                }
            }
        }

        return result;
    }

    /**
     * Discovers all pickaxe-compatible enchantments currently registered on the server.
     */
    public List<Enchantment> discoverPickaxeEnchants() {
        List<Enchantment> pickaxeEnchants = new ArrayList<>();
        try {
            for (Enchantment ench : Bukkit.getRegistry(Enchantment.class)) {
                if (ench == null || ench.getKey() == null) continue;
                String keyStr = ench.getKey().toString().toLowerCase();

                // Check if target is pickaxe/tool/digger
                if (ench.getItemTarget() != null && (
                        ench.getItemTarget().name().contains("TOOL") ||
                        ench.getItemTarget().name().contains("DIGGER") ||
                        ench.getItemTarget().name().contains("BREAKABLE")
                )) {
                    pickaxeEnchants.add(ench);
                } else if (keyStr.contains("pickaxe") || keyStr.contains("mine") || keyStr.contains("drill") || keyStr.contains("explosive") || keyStr.contains("jackhammer") || keyStr.contains("telepathy") || keyStr.contains("efficiency") || keyStr.contains("fortune") || keyStr.contains("silk_touch")) {
                    pickaxeEnchants.add(ench);
                }
            }
        } catch (Throwable ignored) {
            // Fallback for custom Bukkit setups
        }
        return pickaxeEnchants;
    }
}
