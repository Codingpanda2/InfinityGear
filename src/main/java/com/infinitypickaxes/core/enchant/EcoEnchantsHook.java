package com.infinitypickaxes.core.enchant;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.utils.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.regex.Pattern;

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
     * Extracts enchantment key and level from any book (Vanilla EnchantedBook, EcoEnchants book, or custom PDC/Lore).
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

        // 3. Check PersistentDataContainer for Eco / EcoEnchants / Custom tags
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        for (NamespacedKey key : pdc.getKeys()) {
            String ns = key.getNamespace().toLowerCase();
            if (ns.contains("eco") || ns.contains("enchant") || ns.contains("libreforge")) {
                Integer lvl = pdc.get(key, PersistentDataType.INTEGER);
                if (lvl != null && lvl > 0) {
                    result.put(key.toString().toLowerCase(), lvl);
                }
            }
        }

        // 4. Fallback: Parse Display Name & Lore lines for custom/translated/renamed enchantment books
        if (plugin.getEnchantManager() != null) {
            List<String> textLines = new ArrayList<>();
            if (meta.hasDisplayName()) {
                Component dName = meta.displayName();
                if (dName != null) {
                    textLines.add(TextUtil.stripFormatting(LegacyComponentSerializer.legacySection().serialize(dName)));
                }
            }
            if (meta.hasLore() && meta.lore() != null) {
                for (Component c : meta.lore()) {
                    if (c != null) {
                        textLines.add(TextUtil.stripFormatting(LegacyComponentSerializer.legacySection().serialize(c)));
                    }
                }
            }

            for (EnchantSocket socket : plugin.getEnchantManager().getAllSockets()) {
                String cleanName = socket.getCleanName().toLowerCase();
                String rawId = socket.getId().toLowerCase();
                String keyStr = socket.getKeyString().toLowerCase();

                // If already detected via NBT/Meta, skip lore guessing for this key
                if (result.containsKey(keyStr)) continue;

                for (String line : textLines) {
                    String lowerLine = line.toLowerCase();
                    if (lowerLine.contains(cleanName) || lowerLine.contains(rawId)) {
                        int level = extractLevelFromText(line, cleanName, rawId);
                        if (level > 0) {
                            result.put(keyStr, level);
                            break;
                        }
                    }
                }
            }
        }

        return result;
    }

    private int extractLevelFromText(String line, String cleanName, String rawId) {
        String clean = TextUtil.stripFormatting(line).trim();
        String remainder = clean.replaceFirst("(?i)" + Pattern.quote(cleanName), "")
                                .replaceFirst("(?i)" + Pattern.quote(rawId), "")
                                .replace(":", "")
                                .replace("-", "")
                                .replace("libro", "")
                                .replace("encantado", "")
                                .replace("de", "")
                                .trim();

        String[] tokens = remainder.split("\\s+");
        for (String token : tokens) {
            token = token.replaceAll("[^a-zA-Z0-9]", "").trim();
            if (token.isEmpty()) continue;
            try {
                int num = Integer.parseInt(token);
                if (num > 0 && num <= 100) return num;
            } catch (NumberFormatException ignored) {
                int num = TextUtil.fromRoman(token);
                if (num > 0 && num <= 100) return num;
            }
        }
        return 1;
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

                if (keyStr.contains("pickaxe") || keyStr.contains("mine") || keyStr.contains("drill") || keyStr.contains("explosive") || keyStr.contains("jackhammer") || keyStr.contains("telepathy") || keyStr.contains("efficiency") || keyStr.contains("fortune") || keyStr.contains("silk_touch")) {
                    pickaxeEnchants.add(ench);
                }
            }
        } catch (Throwable ignored) {}
        return pickaxeEnchants;
    }
}
