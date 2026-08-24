package com.infinitypickaxes.core.enchant;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.utils.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
                Method getEnchantsOnItem = ecoEnchantsClass.getMethod("getEnchantmentsOnItem", ItemStack.class);
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
                                Method getKeyMethod = keyObj.getClass().getMethod("getKey");
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
                Object[] values = getEcoEnchantValues();
                if (values != null) {
                    for (Object ecoEnchantObj : values) {
                        try {
                            Method getTargetMethod = ecoEnchantObj.getClass().getMethod("getTarget");
                            Object targetObj = getTargetMethod.invoke(ecoEnchantObj);
                            String targetStr = targetObj != null ? targetObj.toString().toLowerCase() : "";

                            Method getKeyMethod = ecoEnchantObj.getClass().getMethod("getKey");
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
     * Extracts pure display header in exact EcoEnchants format with authentic colors & numerals.
     */
    public String getEnchantmentHeader(Enchantment ench, int level) {
        if (ench == null || ench.getKey() == null) return "Enchantment";
        String id = ench.getKey().getKey().toLowerCase();
        int maxLevel = ench.getMaxLevel();

        // 1. Try EcoEnchants Live API
        if (ecoEnchantsPresent) {
            Object ecoEnchantObj = findEcoEnchantObject(ench);
            if (ecoEnchantObj != null) {
                // Try getFormattedDisplayName(level)
                Object res = invokeMethodQuietly(ecoEnchantObj.getClass(), ecoEnchantObj, "getFormattedDisplayName", new Class<?>[]{int.class}, level);
                if (res != null) {
                    String str = stringifyComponentOrObject(res);
                    if (!str.trim().isEmpty()) {
                        return formatHeaderWithLevel(str, level, maxLevel);
                    }
                }

                // Try getDisplayName(level)
                res = invokeMethodQuietly(ecoEnchantObj.getClass(), ecoEnchantObj, "getDisplayName", new Class<?>[]{int.class}, level);
                if (res != null) {
                    String str = stringifyComponentOrObject(res);
                    if (!str.trim().isEmpty()) {
                        return formatHeaderWithLevel(str, level, maxLevel);
                    }
                }

                // Try getDisplayName()
                res = invokeMethodQuietly(ecoEnchantObj.getClass(), ecoEnchantObj, "getDisplayName", new Class<?>[]{});
                if (res != null) {
                    String str = stringifyComponentOrObject(res);
                    if (!str.trim().isEmpty()) {
                        return formatHeaderWithLevel(str, level, maxLevel);
                    }
                }
            }
        }

        // 2. Try EcoEnchants YAML files on disk
        String yamlHeader = findHeaderInEcoYaml(id, ench.getKey().toString(), level, maxLevel);
        if (yamlHeader != null && !yamlHeader.isEmpty()) {
            return yamlHeader;
        }

        // 3. Fallback standard colors matching EcoEnchants default rarities
        return getFallbackHeader(id, level, maxLevel);
    }

    private String formatHeaderWithLevel(String rawDisplayName, int level, int maxLevel) {
        String clean = cleanEnchantmentName(rawDisplayName);
        String roman = TextUtil.toRoman(level);

        if (maxLevel <= 1 && level <= 1) {
            if (rawDisplayName.startsWith("<") && rawDisplayName.contains(">")) {
                String openTag = rawDisplayName.substring(0, rawDisplayName.indexOf(">") + 1);
                String closeTag = "</" + openTag.substring(1);
                return openTag + clean + closeTag;
            }
            return "<gray>" + clean + "</gray>";
        }

        // Check if rawDisplayName has a custom hex or color tag (e.g. <#FF00E5>, <#00E5FF>)
        if (rawDisplayName.startsWith("<#") || rawDisplayName.startsWith("<gradient") || rawDisplayName.startsWith("<color")) {
            String openTag = rawDisplayName.substring(0, rawDisplayName.indexOf(">") + 1);
            String closeTag = "</" + openTag.substring(1);
            return openTag + clean + " " + roman + closeTag;
        } else if (rawDisplayName.startsWith("<") && !rawDisplayName.startsWith("<gray>") && !rawDisplayName.startsWith("<white>")) {
            String openTag = rawDisplayName.substring(0, rawDisplayName.indexOf(">") + 1);
            String closeTag = "</" + openTag.substring(1);
            return openTag + clean + " " + roman + closeTag;
        } else {
            // Vanilla style: <gray>Name</gray> <#00E5FF>Roman</#00E5FF>
            return "<gray>" + clean + "</gray> <#00E5FF>" + roman + "</#00E5FF>";
        }
    }

    private String getFallbackHeader(String id, int level, int maxLevel) {
        String roman = TextUtil.toRoman(level);
        return switch (id) {
            case "blast_mining" -> "<#FF00E5>Blast Mining " + roman + "</#FF00E5>";
            case "dynamite" -> "<#00E5FF>Dynamite " + roman + "</#00E5FF>";
            case "drill" -> "<#FFAA00>Drill " + roman + "</#FFAA00>";
            case "jackhammer" -> "<#FF5555>Jackhammer " + roman + "</#FF5555>";
            case "laser" -> "<#FF0055>Laser " + roman + "</#FF0055>";
            case "vein_miner" -> "<#00FF88>Vein Miner " + roman + "</#00FF88>";
            case "telekinesis", "telepathy", "infernal_touch", "autosmelt" ->
                (maxLevel > 1 || level > 1) ? "<gray>" + capitalize(id.replace("_", " ")) + "</gray> <#00E5FF>" + roman + "</#00E5FF>"
                                            : "<gray>" + capitalize(id.replace("_", " ")) + "</gray>";
            default ->
                (maxLevel > 1 || level > 1) ? "<gray>" + capitalize(id.replace("_", " ")) + "</gray> <#00E5FF>" + roman + "</#00E5FF>"
                                            : "<gray>" + capitalize(id.replace("_", " ")) + "</gray>";
        };
    }

    private String findHeaderInEcoYaml(String id, String namespacedKeyStr, int level, int maxLevel) {
        try {
            List<File> searchDirs = getEcoSearchDirectories();
            File matchedFile = null;
            for (File dir : searchDirs) {
                matchedFile = findYamlFile(dir, id, namespacedKeyStr);
                if (matchedFile != null) break;
            }

            if (matchedFile != null) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(matchedFile);
                String dName = yaml.getString("display_name", yaml.getString("name", ""));
                if (!dName.isEmpty()) {
                    return formatHeaderWithLevel(dName, level, maxLevel);
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public List<String> getEnchantmentDescription(Enchantment ench) {
        return getEnchantmentDescription(ench, 1);
    }

    /**
     * Fetches enchantment description lines directly from EcoEnchants (API or YAML) with exact evaluated placeholders.
     */
    public List<String> getEnchantmentDescription(Enchantment ench, int level) {
        List<String> rawLines = new ArrayList<>();
        if (ench == null || ench.getKey() == null) return rawLines;

        String id = ench.getKey().getKey().toLowerCase();
        int safeLevel = Math.max(1, level);

        // 1. Try EcoEnchants Live API (direct runtime evaluation with level)
        if (ecoEnchantsPresent) {
            rawLines.addAll(fetchFromEcoEnchantsApi(ench, safeLevel));
        }

        // 2. Try EcoEnchants YAML config files on server disk
        if (rawLines.isEmpty()) {
            rawLines.addAll(fetchFromEcoEnchantsFiles(id, ench.getKey().toString(), safeLevel));
        }

        // 3. Fallback to standard clean English descriptions
        if (rawLines.isEmpty()) {
            rawLines.addAll(getKnownDescription(id, safeLevel));
        }

        // 4. Clean, format and balance every line
        List<String> formatted = new ArrayList<>();
        for (String line : rawLines) {
            String clean = formatDescriptionLine(line);
            if (!clean.isEmpty()) {
                formatted.add(clean);
            }
        }

        return formatted;
    }

    private List<String> fetchFromEcoEnchantsApi(Enchantment ench, int level) {
        List<String> results = new ArrayList<>();
        if (ench == null || ench.getKey() == null) return results;

        String id = ench.getKey().getKey().toLowerCase();
        NamespacedKey key = ench.getKey();

        try {
            Class<?> ecoEnchantsClass = null;
            try {
                ecoEnchantsClass = Class.forName("com.willfp.ecoenchants.enchantments.EcoEnchants");
            } catch (ClassNotFoundException ignored) {}

            Class<?> coreEnchantsClass = null;
            try {
                coreEnchantsClass = Class.forName("com.willfp.eco.core.enchantments.Enchantments");
            } catch (ClassNotFoundException ignored) {}

            Object ecoEnchantObj = null;

            if (ecoEnchantsClass != null) {
                ecoEnchantObj = invokeMethodQuietly(ecoEnchantsClass, null, "getByKey", new Class<?>[]{NamespacedKey.class}, key);
                if (ecoEnchantObj == null) {
                    ecoEnchantObj = invokeMethodQuietly(ecoEnchantsClass, null, "getByKey", new Class<?>[]{NamespacedKey.class}, new NamespacedKey("ecoenchants", id));
                }
                if (ecoEnchantObj == null) {
                    ecoEnchantObj = invokeMethodQuietly(ecoEnchantsClass, null, "getByKey", new Class<?>[]{NamespacedKey.class}, NamespacedKey.minecraft(id));
                }
                if (ecoEnchantObj == null) {
                    ecoEnchantObj = invokeMethodQuietly(ecoEnchantsClass, null, "getByID", new Class<?>[]{String.class}, id);
                }
                if (ecoEnchantObj == null) {
                    ecoEnchantObj = invokeMethodQuietly(ecoEnchantsClass, null, "getByName", new Class<?>[]{String.class}, id);
                }

                if (ecoEnchantObj == null) {
                    try {
                        Field instanceField = ecoEnchantsClass.getField("INSTANCE");
                        Object instance = instanceField.get(null);
                        if (instance != null) {
                            ecoEnchantObj = invokeMethodQuietly(instance.getClass(), instance, "getByKey", new Class<?>[]{NamespacedKey.class}, key);
                            if (ecoEnchantObj == null) {
                                ecoEnchantObj = invokeMethodQuietly(instance.getClass(), instance, "getByKey", new Class<?>[]{NamespacedKey.class}, new NamespacedKey("ecoenchants", id));
                            }
                            if (ecoEnchantObj == null) {
                                ecoEnchantObj = invokeMethodQuietly(instance.getClass(), instance, "getByID", new Class<?>[]{String.class}, id);
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }

            if (ecoEnchantObj == null && coreEnchantsClass != null) {
                ecoEnchantObj = invokeMethodQuietly(coreEnchantsClass, null, "getByKey", new Class<?>[]{NamespacedKey.class}, key);
                if (ecoEnchantObj == null) {
                    ecoEnchantObj = invokeMethodQuietly(coreEnchantsClass, null, "getByKey", new Class<?>[]{NamespacedKey.class}, new NamespacedKey("ecoenchants", id));
                }
                if (ecoEnchantObj == null) {
                    ecoEnchantObj = invokeMethodQuietly(coreEnchantsClass, null, "get", new Class<?>[]{NamespacedKey.class}, key);
                }
            }

            if (ecoEnchantObj != null) {
                // Try 1: getFormattedDescription(int level)
                Object res = invokeMethodQuietly(ecoEnchantObj.getClass(), ecoEnchantObj, "getFormattedDescription", new Class<?>[]{int.class}, level);
                extractLinesFromObject(res, results);

                // Try 2: getFormattedDescription()
                if (results.isEmpty()) {
                    res = invokeMethodQuietly(ecoEnchantObj.getClass(), ecoEnchantObj, "getFormattedDescription", new Class<?>[]{});
                    extractLinesFromObject(res, results);
                }

                // Try 3: getDescription(int level)
                if (results.isEmpty()) {
                    res = invokeMethodQuietly(ecoEnchantObj.getClass(), ecoEnchantObj, "getDescription", new Class<?>[]{int.class}, level);
                    extractLinesFromObject(res, results);
                }

                // Try 4: getDescription()
                if (results.isEmpty()) {
                    res = invokeMethodQuietly(ecoEnchantObj.getClass(), ecoEnchantObj, "getDescription", new Class<?>[]{});
                    extractLinesFromObject(res, results);
                }

                // Try 5: getRawDescription()
                if (results.isEmpty()) {
                    res = invokeMethodQuietly(ecoEnchantObj.getClass(), ecoEnchantObj, "getRawDescription", new Class<?>[]{});
                    extractLinesFromObject(res, results);
                }
            }
        } catch (Throwable ignored) {}

        return results;
    }

    private List<File> getEcoSearchDirectories() {
        List<File> searchDirs = new ArrayList<>();
        Plugin ecoPlugin = Bukkit.getPluginManager().getPlugin("EcoEnchants");
        if (ecoPlugin != null && ecoPlugin.getDataFolder().exists()) {
            searchDirs.add(ecoPlugin.getDataFolder());
            File enchantsDir = new File(ecoPlugin.getDataFolder(), "enchants");
            if (enchantsDir.exists()) searchDirs.add(enchantsDir);
        }

        File pluginsDir = plugin.getDataFolder().getParentFile();
        if (pluginsDir != null && pluginsDir.exists()) {
            for (String dirName : new String[]{"EcoEnchants", "ecoenchants", "Eco", "eco"}) {
                File d = new File(pluginsDir, dirName);
                if (d.exists() && !searchDirs.contains(d)) {
                    searchDirs.add(d);
                    File eDir = new File(d, "enchants");
                    if (eDir.exists() && !searchDirs.contains(eDir)) searchDirs.add(eDir);
                }
            }
        }
        return searchDirs;
    }

    private List<String> fetchFromEcoEnchantsFiles(String id, String namespacedKeyStr, int level) {
        List<String> results = new ArrayList<>();
        try {
            List<File> searchDirs = getEcoSearchDirectories();
            File matchedFile = null;
            for (File dir : searchDirs) {
                matchedFile = findYamlFile(dir, id, namespacedKeyStr);
                if (matchedFile != null) break;
            }

            if (matchedFile != null) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(matchedFile);
                List<String> rawDesc = new ArrayList<>();
                if (yaml.isList("description")) {
                    rawDesc.addAll(yaml.getStringList("description"));
                } else if (yaml.isString("description")) {
                    rawDesc.add(yaml.getString("description"));
                }

                Map<String, String> placeholders = new HashMap<>();
                if (yaml.isConfigurationSection("placeholders")) {
                    ConfigurationSection pSec = yaml.getConfigurationSection("placeholders");
                    for (String key : pSec.getKeys(false)) {
                        placeholders.put(key.toLowerCase(), pSec.getString(key, ""));
                    }
                }

                for (String rawLine : rawDesc) {
                    if (rawLine == null || rawLine.trim().isEmpty()) continue;
                    results.add(evaluateEcoPlaceholders(rawLine, placeholders, level));
                }
            }
        } catch (Throwable ignored) {}

        return results;
    }

    private void extractLinesFromObject(Object obj, List<String> target) {
        if (obj == null) return;
        if (obj instanceof Collection<?> col) {
            for (Object item : col) {
                if (item != null) {
                    String str = stringifyComponentOrObject(item);
                    if (!str.trim().isEmpty()) {
                        target.add(str);
                    }
                }
            }
        } else if (obj instanceof Object[] arr) {
            for (Object item : arr) {
                if (item != null) {
                    String str = stringifyComponentOrObject(item);
                    if (!str.trim().isEmpty()) {
                        target.add(str);
                    }
                }
            }
        } else {
            String str = stringifyComponentOrObject(obj);
            if (!str.trim().isEmpty()) {
                target.add(str);
            }
        }
    }

    private String stringifyComponentOrObject(Object item) {
        if (item == null) return "";
        if (item instanceof Component comp) {
            return MiniMessage.miniMessage().serialize(comp);
        }
        try {
            Method m = item.getClass().getMethod("asComponent");
            Object c = m.invoke(item);
            if (c instanceof Component comp) {
                return MiniMessage.miniMessage().serialize(comp);
            }
        } catch (Throwable ignored) {}

        try {
            Method m = item.getClass().getMethod("getMiniMessage");
            Object s = m.invoke(item);
            if (s != null) return s.toString();
        } catch (Throwable ignored) {}

        return item.toString();
    }

    private Object invokeMethodQuietly(Class<?> clazz, Object instance, String methodName, Class<?>[] paramTypes, Object... args) {
        try {
            Method m = clazz.getMethod(methodName, paramTypes);
            return m.invoke(instance, args);
        } catch (Throwable ignored) {}
        try {
            Method m = clazz.getDeclaredMethod(methodName, paramTypes);
            m.setAccessible(true);
            return m.invoke(instance, args);
        } catch (Throwable ignored) {}
        return null;
    }

    private Object[] getEcoEnchantValues() {
        try {
            Class<?> ecoEnchantsClass = Class.forName("com.willfp.ecoenchants.enchantments.EcoEnchants");
            try {
                Method m = ecoEnchantsClass.getMethod("values");
                Object res = m.invoke(null);
                if (res instanceof Object[] arr) return arr;
                if (res instanceof Collection<?> col) return col.toArray();
            } catch (Throwable ignored) {}

            Field instanceField = ecoEnchantsClass.getField("INSTANCE");
            Object instance = instanceField.get(null);
            if (instance != null) {
                Method m = instance.getClass().getMethod("values");
                Object res = m.invoke(instance);
                if (res instanceof Object[] arr) return arr;
                if (res instanceof Collection<?> col) return col.toArray();
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private String evaluateEcoPlaceholders(String rawLine, Map<String, String> placeholdersMap, int level) {
        String result = rawLine;

        // 1. Strip rogue closing tags that EcoEnchants puts at the end of its YAML lines
        result = result.replaceAll("(?i)</?gr[ae]y>", "").trim();

        // 2. Evaluate specific placeholder keys from YAML
        for (Map.Entry<String, String> entry : placeholdersMap.entrySet()) {
            String pKey = entry.getKey();
            String formula = entry.getValue();
            String evaluatedVal;

            if (formula.contains("%level%") || formula.matches(".*[0-9+*/^()-].*")) {
                double val = evaluateMath(formula, level);
                evaluatedVal = (val % 1 == 0) ? String.valueOf((long) val) : String.format(Locale.US, "%.1f", val);
            } else {
                evaluatedVal = formula.replace("%level%", String.valueOf(level));
            }

            result = result.replace("%" + pKey + "%", "<green>" + evaluatedVal + "</green>");
        }

        // 3. Built-in defaults for common EcoEnchants placeholder names if not mapped in YAML
        if (result.contains("%placeholder%x%placeholder%")) {
            result = result.replace("%placeholder%x%placeholder%", "<green>3x3</green>");
        }
        if (result.contains("%placeholder%%")) {
            int val = Math.min(100, Math.max(5, level * 5));
            result = result.replace("%placeholder%%", "<green>" + val + "%</green>");
        }
        if (result.contains("%placeholder%")) {
            result = result.replace("%placeholder%", "<green>" + level + "</green>");
        }

        // Replace any remaining %level%
        result = result.replace("%level%", String.valueOf(level));

        return result;
    }

    public static double evaluateMath(String expression, int level) {
        if (expression == null || expression.trim().isEmpty()) return level;
        String expr = expression.replace("%level%", String.valueOf(level))
                                .replace("%enchant_level%", String.valueOf(level))
                                .replace("%lvl%", String.valueOf(level))
                                .trim();

        try {
            return new Object() {
                int pos = -1, ch;

                void nextChar() {
                    ch = (++pos < expr.length()) ? expr.charAt(pos) : -1;
                }

                boolean eat(int charToEat) {
                    while (ch == ' ') nextChar();
                    if (ch == charToEat) {
                        nextChar();
                        return true;
                    }
                    return false;
                }

                double parse() {
                    nextChar();
                    double x = parseExpression();
                    if (pos < expr.length()) throw new RuntimeException("Unexpected: " + (char) ch);
                    return x;
                }

                double parseExpression() {
                    double x = parseTerm();
                    for (;;) {
                        if (eat('+')) x += parseTerm();
                        else if (eat('-')) x -= parseTerm();
                        else return x;
                    }
                }

                double parseTerm() {
                    double x = parseFactor();
                    for (;;) {
                        if (eat('*')) x *= parseFactor();
                        else if (eat('/')) x /= parseFactor();
                        else if (eat('%')) x %= parseFactor();
                        else return x;
                    }
                }

                double parseFactor() {
                    if (eat('+')) return +parseFactor();
                    if (eat('-')) return -parseFactor();

                    double x;
                    int startPos = this.pos;
                    if (eat('(')) {
                        x = parseExpression();
                        eat(')');
                    } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                        while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                        x = Double.parseDouble(expr.substring(startPos, this.pos));
                    } else if (ch >= 'a' && ch <= 'z') {
                        while (ch >= 'a' && ch <= 'z') nextChar();
                        String func = expr.substring(startPos, this.pos);
                        if (eat('(')) {
                            double arg1 = parseExpression();
                            double arg2 = 0;
                            if (eat(',')) {
                                arg2 = parseExpression();
                            }
                            eat(')');
                            x = switch (func) {
                                case "min" -> Math.min(arg1, arg2);
                                case "max" -> Math.max(arg1, arg2);
                                case "sqrt" -> Math.sqrt(arg1);
                                case "floor" -> Math.floor(arg1);
                                case "ceil" -> Math.ceil(arg1);
                                case "round" -> Math.round(arg1);
                                case "abs" -> Math.abs(arg1);
                                default -> arg1;
                            };
                        } else {
                            x = 0;
                        }
                    } else {
                        x = 0;
                    }

                    if (eat('^')) x = Math.pow(x, parseFactor());

                    return x;
                }
            }.parse();
        } catch (Throwable e) {
            try {
                return Double.parseDouble(expr.replaceAll("[^0-9.]", ""));
            } catch (Throwable ignored) {
                return level;
            }
        }
    }

    public static String formatDescriptionLine(String rawLine) {
        if (rawLine == null || rawLine.trim().isEmpty()) return "";
        String s = rawLine.trim();

        // 1. Convert legacy color codes if present
        if (s.contains("&") || s.contains("§")) {
            s = s.replaceAll("(?i)[&§]0", "<black>")
                 .replaceAll("(?i)[&§]1", "<dark_blue>")
                 .replaceAll("(?i)[&§]2", "<dark_green>")
                 .replaceAll("(?i)[&§]3", "<dark_aqua>")
                 .replaceAll("(?i)[&§]4", "<dark_red>")
                 .replaceAll("(?i)[&§]5", "<dark_purple>")
                 .replaceAll("(?i)[&§]6", "<gold>")
                 .replaceAll("(?i)[&§]7", "<gray>")
                 .replaceAll("(?i)[&§]8", "<dark_gray>")
                 .replaceAll("(?i)[&§]9", "<blue>")
                 .replaceAll("(?i)[&§]a", "<green>")
                 .replaceAll("(?i)[&§]b", "<aqua>")
                 .replaceAll("(?i)[&§]c", "<red>")
                 .replaceAll("(?i)[&§]d", "<light_purple>")
                 .replaceAll("(?i)[&§]e", "<yellow>")
                 .replaceAll("(?i)[&§]f", "<white>");
        }

        // 2. Strip any rogue </gray>, <gray>, </grey>, <grey>
        s = s.replaceAll("(?i)</?gr[ae]y>", "").trim();

        if (s.isEmpty()) return "";

        // 3. Return cleanly wrapped in <gray>
        return "<gray>" + s + "</gray>";
    }

    private File findYamlFile(File dir, String id, String namespacedKeyStr) {
        if (dir == null || !dir.isDirectory()) return null;
        File exact = new File(dir, id + ".yml");
        if (exact.exists()) return exact;

        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    File found = findYamlFile(f, id, namespacedKeyStr);
                    if (found != null) return found;
                } else if (f.getName().endsWith(".yml")) {
                    String baseName = f.getName().replace(".yml", "").toLowerCase();
                    if (baseName.equalsIgnoreCase(id) || baseName.replace("_", "").equalsIgnoreCase(id.replace("_", ""))) {
                        return f;
                    }
                    try {
                        YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
                        String fileId = yml.getString("id", "");
                        String fileKey = yml.getString("key", "");
                        if (fileId.equalsIgnoreCase(id) || fileKey.equalsIgnoreCase(namespacedKeyStr) || fileKey.equalsIgnoreCase("ecoenchants:" + id) || fileKey.equalsIgnoreCase("minecraft:" + id)) {
                            return f;
                        }
                    } catch (Throwable ignored) {}
                }
            }
        }
        return null;
    }

    private List<String> getKnownDescription(String id, int level) {
        List<String> lines = new ArrayList<>();
        switch (id) {
            case "efficiency" -> lines.add("Increases mining speed by <green>" + (20 + (level - 1) * 5) + "%</green>");
            case "fortune" -> {
                lines.add("Gives a <green>" + (100 + level * 7) + "%</green> boost to certain");
                lines.add("block drops");
            }
            case "silk_touch" -> lines.add("Allows blocks to drop themselves when mined.");
            case "blast_mining" -> {
                lines.add("<green>" + Math.min(100, Math.max(5, level * 5)) + "%</green> chance to mine blocks in");
                lines.add("a 3x3 area");
            }
            case "dynamite" -> lines.add("Mines blocks in a <green>" + (level <= 1 ? "3x3" : "9x9") + "</green> area");
            case "infernal_touch", "autosmelt" -> lines.add("Automatically smelts mined blocks");
            case "telekinesis", "telepathy" -> {
                lines.add("Drops and experience go directly");
                lines.add("into your inventory");
            }
            case "drill" -> lines.add("Drills continuous tunnels while mining.");
            case "jackhammer" -> lines.add("Breaks entire layers of blocks at once.");
            case "laser" -> lines.add("Fires a continuous beam that breaks blocks.");
            case "vein_miner" -> lines.add("Mines the entire connected ore vein.");
            default -> lines.add("Advanced mining enchantment.");
        }
        return lines;
    }

    /**
     * Extracts pure display name in English with authentic colors from EcoEnchants or defaults.
     */
    public String getEnchantmentDisplayName(Enchantment ench) {
        return getEnchantmentHeader(ench, 1);
    }

    public static String cleanEnchantmentName(String text) {
        if (text == null || text.isEmpty()) return "";
        String plain = TextUtil.stripFormatting(text).trim();
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
