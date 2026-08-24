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

    public List<String> getEnchantmentDescription(Enchantment ench) {
        return getEnchantmentDescription(ench, 1);
    }

    /**
     * Fetches enchantment description lines directly from EcoEnchants (API or YAML) with exact evaluated placeholders.
     */
    public List<String> getEnchantmentDescription(Enchantment ench, int level) {
        List<String> desc = new ArrayList<>();
        if (ench == null || ench.getKey() == null) return desc;

        String id = ench.getKey().getKey().toLowerCase();
        int safeLevel = Math.max(1, level);

        // 1. Try Direct EcoEnchants API evaluation
        if (ecoEnchantsPresent) {
            Object ecoEnchantObj = findEcoEnchantObject(ench);
            if (ecoEnchantObj != null) {
                // A. Try getFormattedDescription(level)
                try {
                    Method getFormatted = ecoEnchantObj.getClass().getMethod("getFormattedDescription", int.class);
                    Object res = getFormatted.invoke(ecoEnchantObj, safeLevel);
                    if (res instanceof List<?> list && !list.isEmpty()) {
                        for (Object o : list) {
                            if (o != null) desc.add(convertObjectToString(o));
                        }
                    }
                } catch (Throwable ignored) {}

                // B. Try getDescription(level) or getDescription()
                if (desc.isEmpty()) {
                    try {
                        Method getDesc = null;
                        try {
                            getDesc = ecoEnchantObj.getClass().getMethod("getDescription", int.class);
                        } catch (NoSuchMethodException e) {
                            getDesc = ecoEnchantObj.getClass().getMethod("getDescription");
                        }

                        if (getDesc != null) {
                            Object descResult = (getDesc.getParameterCount() == 1) ? getDesc.invoke(ecoEnchantObj, safeLevel) : getDesc.invoke(ecoEnchantObj);
                            if (descResult instanceof List<?> list && !list.isEmpty()) {
                                for (Object o : list) {
                                    if (o != null) desc.add(convertObjectToString(o));
                                }
                            } else if (descResult instanceof String s && !s.isEmpty()) {
                                desc.add(s);
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }
        }

        // 2. Try Exact EcoEnchants Plugin YAML files on server disk
        if (desc.isEmpty()) {
            try {
                Plugin ecoPlugin = Bukkit.getPluginManager().getPlugin("EcoEnchants");
                if (ecoPlugin != null && ecoPlugin.getDataFolder().exists()) {
                    File enchantsFolder = new File(ecoPlugin.getDataFolder(), "enchants");
                    File ymlFile = findYamlFile(enchantsFolder, id);
                    if (ymlFile != null) {
                        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(ymlFile);
                        List<String> rawLines = new ArrayList<>();
                        if (yaml.isList("description")) {
                            rawLines.addAll(yaml.getStringList("description"));
                        } else if (yaml.isString("description")) {
                            rawLines.add(yaml.getString("description"));
                        }

                        // Read placeholders mapping from YAML
                        Map<String, String> placeholdersMap = new HashMap<>();
                        if (yaml.isConfigurationSection("placeholders")) {
                            ConfigurationSection pSec = yaml.getConfigurationSection("placeholders");
                            for (String pKey : pSec.getKeys(false)) {
                                placeholdersMap.put(pKey.toLowerCase(), pSec.getString(pKey, ""));
                            }
                        }

                        for (String raw : rawLines) {
                            if (raw != null && !raw.trim().isEmpty()) {
                                desc.add(evaluateEcoPlaceholders(raw, placeholdersMap, safeLevel));
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 3. Fallback to standard clean English descriptions
        if (desc.isEmpty()) {
            desc.addAll(getKnownDescription(id, safeLevel));
        }

        // 4. Final Clean Sanitize: Balance tags cleanly and ensure valid MiniMessage
        List<String> resolved = new ArrayList<>();
        for (String line : desc) {
            if (line == null || line.trim().isEmpty()) continue;
            String clean = cleanAndBalanceLine(line);
            if (!clean.isEmpty()) {
                resolved.add(clean);
            }
        }

        return resolved;
    }

    private String convertObjectToString(Object obj) {
        if (obj == null) return "";
        if (obj instanceof Component comp) {
            return MiniMessage.miniMessage().serialize(comp);
        }
        return obj.toString();
    }

    private Object findEcoEnchantObject(Enchantment ench) {
        try {
            Class<?> ecoEnchantsClass = Class.forName("com.willfp.ecoenchants.enchantments.EcoEnchants");
            String id = ench.getKey().getKey().toLowerCase();

            // Check static getByKey
            try {
                Method m = ecoEnchantsClass.getMethod("getByKey", NamespacedKey.class);
                Object res = m.invoke(null, ench.getKey());
                if (res != null) return res;
            } catch (Throwable ignored) {}

            // Check static getByID
            try {
                Method m = ecoEnchantsClass.getMethod("getByID", String.class);
                Object res = m.invoke(null, id);
                if (res != null) return res;
            } catch (Throwable ignored) {}

            // Check Kotlin INSTANCE object
            try {
                Field instanceField = ecoEnchantsClass.getField("INSTANCE");
                Object instance = instanceField.get(null);
                if (instance != null) {
                    try {
                        Method m = instance.getClass().getMethod("getByKey", NamespacedKey.class);
                        Object res = m.invoke(instance, ench.getKey());
                        if (res != null) return res;
                    } catch (Throwable ignored) {}
                    try {
                        Method m = instance.getClass().getMethod("getByID", String.class);
                        Object res = m.invoke(instance, id);
                        if (res != null) return res;
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}

            // Check Eco Core Enchantments class
            try {
                Class<?> coreEnchants = Class.forName("com.willfp.eco.core.enchantments.Enchantments");
                Method m = coreEnchants.getMethod("getByKey", NamespacedKey.class);
                Object res = m.invoke(null, ench.getKey());
                if (res != null) return res;
            } catch (Throwable ignored) {}

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

        // Strip rogue closing tags that EcoEnchants puts at the end of its YAML lines
        result = result.replaceAll("(?i)</?gr[ae]y>", "").trim();

        // 1. Evaluate specific placeholder keys from YAML
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

        // 2. Built-in defaults for common EcoEnchants placeholder names if not mapped
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

        // Replace any %level% remaining
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

    private String cleanAndBalanceLine(String line) {
        if (line == null) return "";
        String s = line.trim();

        // Strip legacy codes
        s = s.replaceAll("(?i)[&§][0-9a-fk-or]", "");

        // Remove rogue closing tags like </gray> or </white> at the start or end
        s = s.replaceAll("(?i)^</?(gray|grey)>", "").replaceAll("(?i)</?(gray|grey)>$", "").trim();

        if (s.isEmpty()) return "";

        // If line has no color tag at start, wrap in <gray>
        if (!s.startsWith("<")) {
            s = "<gray>" + s + "</gray>";
        } else if (!s.endsWith(">")) {
            s = s + "</gray>";
        }

        return s;
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

    private List<String> getKnownDescription(String id, int level) {
        List<String> lines = new ArrayList<>();
        switch (id) {
            case "efficiency" -> lines.add("Increases mining speed significantly.");
            case "fortune" -> lines.add("Gives a boost to certain block drops.");
            case "silk_touch" -> lines.add("Allows blocks to drop themselves when mined.");
            case "blast_mining" -> lines.add("<green>" + Math.min(100, Math.max(5, level * 5)) + "%</green> chance to mine blocks in a <green>3x3</green> area");
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
     * Extracts pure display name in English with authentic colors from EcoEnchants or defaults.
     */
    public String getEnchantmentDisplayName(Enchantment ench) {
        if (ench == null || ench.getKey() == null) return "Enchantment";
        String id = ench.getKey().getKey().toLowerCase();

        // 1. Known authentic standard colors
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
        if (ecoEnchantsPresent) {
            Object ecoEnchantObj = findEcoEnchantObject(ench);
            if (ecoEnchantObj != null) {
                try {
                    Method getDName = ecoEnchantObj.getClass().getMethod("getDisplayName");
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
        }

        return "<gray>" + capitalize(id.replace("_", " ")) + "</gray>";
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
