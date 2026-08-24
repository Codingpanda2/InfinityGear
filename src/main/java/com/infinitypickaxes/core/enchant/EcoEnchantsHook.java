package com.infinitypickaxes.core.enchant;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.utils.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;

public class EcoEnchantsHook {

    private final InfinityPickaxes plugin;
    private final boolean ecoEnchantsPresent;

    public EcoEnchantsHook(InfinityPickaxes plugin) {
        this.plugin = plugin;
        this.ecoEnchantsPresent = Bukkit.getPluginManager().isPluginEnabled("EcoEnchants");
        if (ecoEnchantsPresent) {
            plugin.getLogger().info("EcoEnchants integration successfully initialized.");
        }
    }

    public boolean isEcoEnchantsPresent() {
        return ecoEnchantsPresent;
    }

    /**
     * Extracts all enchantments and levels present on a book ItemStack.
     */
    public Map<String, Integer> extractEnchantsFromBook(ItemStack bookItem) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (bookItem == null || !bookItem.hasItemMeta()) {
            return result;
        }

        ItemMeta meta = bookItem.getItemMeta();

        // 1. Check Bukkit Stored Enchants
        if (meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta storageMeta) {
            for (Map.Entry<Enchantment, Integer> entry : storageMeta.getStoredEnchants().entrySet()) {
                if (entry.getKey() != null && entry.getKey().getKey() != null) {
                    result.put(entry.getKey().getKey().toString().toLowerCase(), entry.getValue());
                }
            }
        }

        // 2. Check Standard Bukkit Enchants on the item
        if (meta.hasEnchants()) {
            for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                if (entry.getKey() != null && entry.getKey().getKey() != null) {
                    result.put(entry.getKey().getKey().toString().toLowerCase(), entry.getValue());
                }
            }
        }

        // 3. Check EcoEnchants custom PDC / API if present
        if (ecoEnchantsPresent) {
            try {
                Class<?> ecoEnchantsClass = Class.forName("com.willfp.ecoenchants.enchantments.EcoEnchants");
                java.lang.reflect.Method getEnchantsOnItem = ecoEnchantsClass.getMethod("getEnchantmentsOnItem", ItemStack.class);
                Object enchantsMapObj = getEnchantsOnItem.invoke(null, bookItem);

                if (enchantsMapObj instanceof Map<?, ?> ecoMap) {
                    for (Map.Entry<?, ?> entry : ecoMap.entrySet()) {
                        Object keyObj = entry.getKey();
                        Object lvlObj = entry.getValue();

                        if (keyObj != null && lvlObj instanceof Number num) {
                            String keyStr;
                            if (keyObj instanceof Enchantment ench) {
                                keyStr = ench.getKey().toString().toLowerCase();
                            } else {
                                java.lang.reflect.Method getKeyMethod = keyObj.getClass().getMethod("getKey");
                                Object namespacedKeyObj = getKeyMethod.invoke(keyObj);
                                keyStr = namespacedKeyObj.toString().toLowerCase();
                            }
                            result.put(keyStr, num.intValue());
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 4. Fallback check: Extract name & Roman numeral from DisplayName if not detected by API
        if (result.isEmpty() && meta.hasDisplayName()) {
            String plainName = TextUtil.stripFormatting(meta.getDisplayName());
            for (Enchantment registered : Bukkit.getRegistry(Enchantment.class)) {
                String enchantName = registered.getKey().getKey().replace("_", " ");
                if (plainName.toLowerCase().contains(enchantName.toLowerCase())) {
                    int level = 1;
                    String[] parts = plainName.split(" ");
                    if (parts.length > 1) {
                        level = TextUtil.fromRoman(parts[parts.length - 1]);
                    }
                    result.put(registered.getKey().toString().toLowerCase(), level);
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Discovers all pickaxe-compatible enchantments currently active on the server.
     */
    public List<Enchantment> discoverPickaxeEnchants() {
        List<Enchantment> list = new ArrayList<>();

        // 1. Query EcoEnchants API if available
        if (ecoEnchantsPresent) {
            try {
                Class<?> ecoEnchantsClass = Class.forName("com.willfp.ecoenchants.enchantments.EcoEnchants");
                java.lang.reflect.Method getValues = ecoEnchantsClass.getMethod("values");
                Object[] values = (Object[]) getValues.invoke(null);

                if (values != null) {
                    for (Object ecoEnchantObj : values) {
                        try {
                            java.lang.reflect.Method getTargetMethod = ecoEnchantObj.getClass().getMethod("getTarget");
                            Object targetObj = getTargetMethod.invoke(ecoEnchantObj);
                            String targetStr = targetObj != null ? targetObj.toString().toLowerCase() : "";

                            java.lang.reflect.Method getKeyMethod = ecoEnchantObj.getClass().getMethod("getKey");
                            NamespacedKey key = (NamespacedKey) getKeyMethod.invoke(ecoEnchantObj);

                            if (targetStr.contains("pickaxe") || targetStr.contains("tool") || targetStr.contains("digger") || targetStr.contains("breaker") || targetStr.contains("mining") || targetStr.contains("all")) {
                                Enchantment ench = Bukkit.getRegistry(Enchantment.class).get(key);
                                if (ench != null && !list.contains(ench)) {
                                    list.add(ench);
                                }
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 2. Discover from Registry (all ecoenchants / custom enchants with matching keys)
        for (Enchantment ench : Bukkit.getRegistry(Enchantment.class)) {
            if (list.contains(ench)) continue;
            String keyStr = ench.getKey().toString().toLowerCase();

            if (keyStr.startsWith("ecoenchants:")) {
                String sub = ench.getKey().getKey().toLowerCase();
                if (isMiningEnchantName(sub)) {
                    list.add(ench);
                }
            }
        }

        return list;
    }

    private boolean isMiningEnchantName(String name) {
        return name.contains("blast") || name.contains("dynamite") || name.contains("drill") ||
               name.contains("jackhammer") || name.contains("laser") || name.contains("telepathy") ||
               name.contains("telekinesis") || name.contains("autosmelt") || name.contains("smelt") ||
               name.contains("vein") || name.contains("mine") || name.contains("speed") ||
               name.contains("haste") || name.contains("fortune") || name.contains("efficiency") ||
               name.contains("layer") || name.contains("cuboid") || name.contains("excavat");
    }

    /**
     * Fetches enchantment description lines safely in English with clean formatting.
     */
    public List<String> getEnchantmentDescription(Enchantment ench) {
        return getEnchantmentDescription(ench, 1);
    }

    /**
     * Fetches enchantment description lines safely in English with clean formatting.
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
                try {
                    java.lang.reflect.Method getFormatted = ecoEnchantObj.getClass().getMethod("getFormattedDescription", int.class);
                    Object res = getFormatted.invoke(ecoEnchantObj, safeLevel);
                    if (res instanceof List<?> list) {
                        for (Object o : list) {
                            if (o != null) desc.add(o.toString());
                        }
                    }
                } catch (Throwable ignored) {}

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
            // Fully sanitize line by stripping all existing color/closing tags and legacy codes
            String clean = sanitizeDescriptionRaw(line);
            // Resolve placeholders
            clean = resolvePlaceholders(clean, id, safeLevel);
            // Re-clean any rogue closing tags
            clean = clean.replaceAll("(?i)</?gr[ae]y>", "").trim();
            // Wrap cleanly with <gray>
            resolved.add("<gray>" + clean + "</gray>");
        }

        return resolved;
    }

    public static String sanitizeDescriptionRaw(String raw) {
        if (raw == null) return "";
        // Strip legacy codes
        String s = raw.replaceAll("(?i)[&§][0-9a-fk-or]", "");
        // Strip any rogue </gray>, </grey>, <gray>, etc.
        s = s.replaceAll("(?i)</?(gray|grey|white|yellow|gold|green|red|blue|aqua|dark_[a-z]+|light_purple|b|i|u|st|obf|reset|color|colour)(:[^>]+)?>", "");
        s = s.replaceAll("(?i)</?#[0-9a-fA-F]{6}>", "");
        s = s.replaceAll("(?i)</?gradient(:#[0-9a-fA-F]{6})+>", "");
        return s.trim();
    }

    private String resolvePlaceholders(String text, String enchantId, int level) {
        String result = text;

        if (result.contains("%placeholder%x%placeholder%")) {
            result = result.replace("%placeholder%x%placeholder%", "<green>3x3</green>");
        }
        if (result.contains("3x3")) {
            result = result.replaceAll("(?i)\\b3x3\\b", "<green>3x3</green>");
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
