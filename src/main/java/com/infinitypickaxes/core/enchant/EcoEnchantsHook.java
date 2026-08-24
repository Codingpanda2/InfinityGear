package com.infinitypickaxes.core.enchant;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.utils.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

public class EcoEnchantsHook {

    private final InfinityPickaxes plugin;
    private boolean ecoEnchantsPresent = false;
    private boolean ecoFrameworkPresent = false;

    private static final Set<String> NON_PICKAXE_KEYWORDS = Set.of(
            "protection", "fire_protection", "blast_protection", "projectile_protection", "feather_falling",
            "respiration", "aqua_affinity", "thorns", "depth_strider", "frost_walker", "soul_speed", "swift_sneak",
            "sharpness", "smite", "bane_of_arthropods", "knockback", "fire_aspect", "looting", "sweeping",
            "power", "punch", "flame", "infinity", "loyalty", "impaling", "riptide", "channeling", "multishot",
            "quick_charge", "piercing", "density", "breach", "wind_burst", "lure", "luck_of_the_sea",
            "unbreaking", "mending", "curse", "vanishing", "binding"
    );

    public EcoEnchantsHook(InfinityPickaxes plugin) {
        this.plugin = plugin;
        checkPlugins();
    }

    public void checkPlugins() {
        this.ecoEnchantsPresent = Bukkit.getPluginManager().isPluginEnabled("EcoEnchants");
        this.ecoFrameworkPresent = Bukkit.getPluginManager().isPluginEnabled("eco");

        if (ecoEnchantsPresent) {
            plugin.getLogger().info("EcoEnchants detected successfully. Compatibility enabled.");
        } else {
            plugin.getLogger().info("EcoEnchants not detected. Operating in standard Bukkit mode.");
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
                                .replace("book", "")
                                .replace("enchanted", "")
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
     * Strictly discovers only pickaxe-compatible enchantments currently registered on the server.
     */
    public List<Enchantment> discoverPickaxeEnchants() {
        List<Enchantment> pickaxeEnchants = new ArrayList<>();
        ItemStack pickaxe = new ItemStack(Material.NETHERITE_PICKAXE);
        ItemStack diamondPickaxe = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemStack chestplate = new ItemStack(Material.NETHERITE_CHESTPLATE);
        ItemStack bow = new ItemStack(Material.BOW);

        try {
            for (Enchantment ench : Bukkit.getRegistry(Enchantment.class)) {
                if (ench == null || ench.getKey() == null) continue;
                String keyOnly = ench.getKey().getKey().toLowerCase();

                // 1. Explicit keyword blacklist for non-pickaxe categories
                boolean blacklisted = false;
                for (String word : NON_PICKAXE_KEYWORDS) {
                    if (keyOnly.contains(word)) {
                        blacklisted = true;
                        break;
                    }
                }
                if (blacklisted) continue;

                // 2. Strict item capability check
                boolean canPickaxe = false;
                try {
                    canPickaxe = ench.canEnchantItem(pickaxe) || ench.canEnchantItem(diamondPickaxe);
                } catch (Throwable ignored) {}

                // Check if it's actually an armor or sword enchant that falsely returned true
                boolean isArmorOrWeaponOnly = false;
                try {
                    if ((ench.canEnchantItem(sword) || ench.canEnchantItem(chestplate) || ench.canEnchantItem(bow)) && !canPickaxe) {
                        isArmorOrWeaponOnly = true;
                    }
                } catch (Throwable ignored) {}

                if (isArmorOrWeaponOnly) continue;

                // 3. Name or target check
                boolean nameSuggestsPickaxe = keyOnly.contains("pickaxe") || keyOnly.contains("mine")
                        || keyOnly.contains("drill") || keyOnly.contains("explosive") || keyOnly.contains("dynamite")
                        || keyOnly.contains("jackhammer") || keyOnly.contains("telepathy") || keyOnly.contains("telekinesis")
                        || keyOnly.contains("efficiency") || keyOnly.contains("fortune") || keyOnly.contains("silk_touch")
                        || keyOnly.contains("smelt") || keyOnly.contains("auto_smelt") || keyOnly.contains("infernal")
                        || keyOnly.contains("vein") || keyOnly.contains("haste") || keyOnly.contains("speed")
                        || keyOnly.contains("quarry") || keyOnly.contains("laser");

                if (canPickaxe || nameSuggestsPickaxe) {
                    pickaxeEnchants.add(ench);
                }
            }
        } catch (Throwable ignored) {}

        return pickaxeEnchants;
    }

    public List<String> getEnchantmentDescription(Enchantment ench) {
        return getEnchantmentDescription(ench, 1);
    }

    /**
     * Extracts and resolves authentic description of an enchantment, replacing dynamic level formulas and placeholders.
     */
    public List<String> getEnchantmentDescription(Enchantment ench, int level) {
        List<String> desc = new ArrayList<>();
        if (ench == null || ench.getKey() == null) return desc;

        String id = ench.getKey().getKey().toLowerCase();
        int safeLevel = Math.max(1, level);

        // 1. Try EcoEnchants API reflection
        try {
            Class<?> ecoEnchantsClass = Class.forName("com.willfp.ecoenchants.enchantments.EcoEnchants");
            java.lang.reflect.Method getByKeyMethod = null;
            try {
                getByKeyMethod = ecoEnchantsClass.getMethod("getByKey", NamespacedKey.class);
            } catch (NoSuchMethodException e) {
                try {
                    getByKeyMethod = ecoEnchantsClass.getMethod("getByID", String.class);
                } catch (NoSuchMethodException ignored) {}
            }

            Object ecoEnchantObj = null;
            if (getByKeyMethod != null) {
                if (getByKeyMethod.getParameterTypes()[0].equals(NamespacedKey.class)) {
                    ecoEnchantObj = getByKeyMethod.invoke(null, ench.getKey());
                } else {
                    ecoEnchantObj = getByKeyMethod.invoke(null, id);
                }
            }

            if (ecoEnchantObj != null) {
                // Try getFormattedDescription(level)
                try {
                    java.lang.reflect.Method getFormatted = ecoEnchantObj.getClass().getMethod("getFormattedDescription", int.class);
                    Object res = getFormatted.invoke(ecoEnchantObj, safeLevel);
                    if (res instanceof List<?> list) {
                        for (Object o : list) {
                            if (o != null) desc.add(o.toString());
                        }
                    }
                } catch (Throwable ignored) {}

                // Try getDescription(level) or getDescription()
                if (desc.isEmpty()) {
                    try {
                        java.lang.reflect.Method getDesc = null;
                        try {
                            getDesc = ecoEnchantObj.getClass().getMethod("getDescription", int.class);
                        } catch (NoSuchMethodException e) {
                            getDesc = ecoEnchantObj.getClass().getMethod("getDescription");
                        }

                        if (getDesc != null) {
                            Object descResult = (getDesc.getParameterCount() == 1) ? getDesc.invoke(ecoEnchantObj, safeLevel) : getDesc.invoke(ecoEnchantObj);
                            if (descResult instanceof List<?> list) {
                                for (Object o : list) {
                                    if (o != null) desc.add(o.toString());
                                }
                            } else if (descResult instanceof String s && !s.isEmpty()) {
                                desc.add(s);
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}

        // 2. Try EcoEnchants Plugin YAML files on disk
        if (desc.isEmpty()) {
            try {
                Plugin ecoPlugin = Bukkit.getPluginManager().getPlugin("EcoEnchants");
                if (ecoPlugin != null && ecoPlugin.getDataFolder().exists()) {
                    File enchantsFolder = new File(ecoPlugin.getDataFolder(), "enchants");
                    if (enchantsFolder.exists() && enchantsFolder.isDirectory()) {
                        File ymlFile = findYamlFile(enchantsFolder, id);
                        if (ymlFile != null) {
                            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(ymlFile);
                            List<String> list = yaml.getStringList("description");
                            if (!list.isEmpty()) {
                                for (String s : list) {
                                    desc.add(s);
                                }
                            } else {
                                String single = yaml.getString("description");
                                if (single != null && !single.isEmpty()) {
                                    desc.add(single);
                                }
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 3. Known Standard Fallback descriptions (Pure English by default)
        if (desc.isEmpty()) {
            desc.addAll(getKnownDescription(id));
        }

        // 4. Resolve EcoEnchants dynamic %placeholder% variables cleanly without unmatched tags
        List<String> resolved = new ArrayList<>();
        for (String line : desc) {
            if (line == null || line.trim().isEmpty()) continue;
            String clean = resolvePlaceholders(line, id, safeLevel);
            // Ensure proper enclosing tags
            clean = clean.replace("</gray>", "").replace("<gray>", "");
            resolved.add("<gray>" + clean + "</gray>");
        }

        return resolved;
    }

    private String resolvePlaceholders(String text, String enchantId, int level) {
        String result = text;

        if (result.contains("%placeholder%x%placeholder%")) {
            result = result.replace("%placeholder%x%placeholder%", "<green>3x3</green>");
        }

        if (result.contains("%placeholder%%")) {
            int chance = Math.min(100, Math.max(5, level * 5));
            result = result.replace("%placeholder%%", "<green>" + chance + "%</green>");
        }

        if (result.contains("%placeholder%")) {
            result = result.replace("%placeholder%", "<green>" + level + "</green>");
        }

        // Clean any leftover raw placeholder tags
        result = result.replaceAll("%[a-zA-Z0-9_]+%", String.valueOf(level));

        return result;
    }

    private File findYamlFile(File dir, String id) {
        if (dir == null || !dir.isDirectory()) return null;
        File exact = new File(dir, id + ".yml");
        if (exact.exists()) return exact;

        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    File found = findYamlFile(f, id);
                    if (found != null) return found;
                } else if (f.getName().equalsIgnoreCase(id + ".yml") || f.getName().replace(".yml", "").equalsIgnoreCase(id)) {
                    return f;
                }
            }
        }
        return null;
    }

    private List<String> getKnownDescription(String id) {
        List<String> lines = new ArrayList<>();
        switch (id) {
            case "efficiency" -> lines.add("Increases mining speed significantly.");
            case "fortune" -> lines.add("Gives a boost to certain block drops.");
            case "silk_touch" -> lines.add("Allows blocks to drop themselves when mined.");
            case "blast_mining" -> lines.add("<green>15%</green> chance to mine blocks in a <green>3x3</green> area");
            case "dynamite" -> lines.add("Mines blocks in a <green>3x3</green> area");
            case "infernal_touch", "autosmelt" -> lines.add("Automatically smelts mined blocks");
            case "telekinesis", "telepathy" -> lines.add("Drops and experience go directly into your inventory");
            case "drill" -> lines.add("Drills continuous tunnels while mining.");
            case "jackhammer" -> lines.add("Breaks entire layers of blocks at once.");
            case "laser" -> lines.add("Fires a continuous beam that breaks blocks.");
            case "vein_miner" -> lines.add("Mines the entire connected ore vein.");
            default -> lines.add("Advanced mining enchantment.");
        }
        return lines;
    }

    /**
     * Extracts pure display name in English (e.g. "Blast Mining", "Dynamite", "Efficiency", "Fortune").
     */
    public String getEnchantmentDisplayName(Enchantment ench) {
        if (ench == null || ench.getKey() == null) return "Enchantment";
        String id = ench.getKey().getKey().toLowerCase();

        // 1. Check known English standards with authentic colors
        switch (id) {
            case "blast_mining" -> { return "<#FF00E5>Blast Mining</#FF00E5>"; }
            case "dynamite" -> { return "<#00E5FF>Dynamite</#00E5FF>"; }
            case "efficiency" -> { return "<#00E5FF>Efficiency</#00E5FF>"; }
            case "fortune" -> { return "<#FFA500>Fortune</#FFA500>"; }
            case "silk_touch" -> { return "<#9966FF>Silk Touch</#9966FF>"; }
            case "infernal_touch", "autosmelt" -> { return "<gray>Infernal Touch</gray>"; }
            case "telekinesis", "telepathy" -> { return "<gray>Telekinesis</gray>"; }
            case "drill" -> { return "<#FFAA00>Drill</#FFAA00>"; }
            case "jackhammer" -> { return "<#FF5555>Jackhammer</#FF5555>"; }
            case "laser" -> { return "<#FF0055>Laser</#FF0055>"; }
            case "vein_miner" -> { return "<#00FF88>Vein Miner</#00FF88>"; }
        }

        // 2. Try EcoEnchants API reflection
        try {
            Class<?> ecoEnchantsClass = Class.forName("com.willfp.ecoenchants.enchantments.EcoEnchants");
            java.lang.reflect.Method getByKeyMethod = null;
            try {
                getByKeyMethod = ecoEnchantsClass.getMethod("getByKey", NamespacedKey.class);
            } catch (NoSuchMethodException e) {
                try {
                    getByKeyMethod = ecoEnchantsClass.getMethod("getByID", String.class);
                } catch (NoSuchMethodException ignored) {}
            }

            Object ecoEnchantObj = null;
            if (getByKeyMethod != null) {
                if (getByKeyMethod.getParameterTypes()[0].equals(NamespacedKey.class)) {
                    ecoEnchantObj = getByKeyMethod.invoke(null, ench.getKey());
                } else {
                    ecoEnchantObj = getByKeyMethod.invoke(null, id);
                }
            }

            if (ecoEnchantObj != null) {
                try {
                    java.lang.reflect.Method getDName = ecoEnchantObj.getClass().getMethod("getDisplayName");
                    Object nameRes = getDName.invoke(ecoEnchantObj);
                    if (nameRes != null && !nameRes.toString().isEmpty()) {
                        String clean = cleanEnchantmentName(nameRes.toString());
                        if (nameRes.toString().startsWith("<") && nameRes.toString().contains(">")) {
                            String tag = nameRes.toString().substring(0, nameRes.toString().indexOf(">") + 1);
                            return tag + clean + "</" + tag.replace("<", "");
                        }
                        return "<gray>" + clean + "</gray>";
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        // 3. Fallback capitalize clean ID
        return "<gray>" + capitalize(id.replace("_", " ")) + "</gray>";
    }

    /**
     * Completely strips trailing numbers, Roman numerals, and tag noise from enchantment names.
     */
    public static String cleanEnchantmentName(String text) {
        if (text == null || text.isEmpty()) return "";
        String plain = TextUtil.stripFormatting(text).trim();
        // Strip trailing roman numerals like I, II, III, IV, V, VI, XXV, etc. or numbers
        plain = plain.replaceAll("(?i)\\s+(M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{1,3})|[0-9]+)$", "").trim();
        return plain;
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String part : text.split(" ")) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase()).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
