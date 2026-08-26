package com.infinitypickaxes.core.enchant;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.utils.TextUtil;
import com.willfp.ecoenchants.display.EnchantmentFormattingKt;
import com.willfp.ecoenchants.enchant.EcoEnchant;
import com.willfp.ecoenchants.enchant.EcoEnchants;
import com.willfp.ecoenchants.target.EnchantmentTarget;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Typed bridge to EcoEnchants 2026.33. EcoEnchants owns all enchantment metadata. */
public final class EcoEnchantsHook {

    private static final Pattern FIRST_GRADIENT_COLOR = Pattern.compile(
            "(?i)<gradient:([^:>]+)(?=[:>])");

    private final InfinityPickaxes plugin;

    public EcoEnchantsHook(InfinityPickaxes plugin) {
        this.plugin = plugin;
    }

    public boolean isEcoEnchantsPresent() {
        return plugin.getServer().getPluginManager().isPluginEnabled("EcoEnchants");
    }

    public Collection<EcoEnchant> getPickaxeEnchants() {
        if (!isEcoEnchantsPresent()) return List.of();

        ItemStack probe = new ItemStack(Material.NETHERITE_PICKAXE);
        List<EcoEnchant> result = new ArrayList<>();
        for (EcoEnchant enchant : EcoEnchants.INSTANCE.values()) {
            if (enchant == null || enchant.isHiddenFromGui() || enchant.getEnchantment() == null) continue;
            if (enchant.getTargets().stream().anyMatch(target -> isPickaxeTarget(target, probe))) {
                result.add(enchant);
            }
        }
        result.sort(Comparator.comparing(EcoEnchant::getID, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(result);
    }

    /**
     * EcoEnchants declares pickaxe compatibility with the stable target IDs
     * "pickaxe" and "all". Prefer those identities over re-evaluating their
     * item matchers; the matcher remains a fallback for custom targets that
     * include pickaxes.
     */
    static boolean isPickaxeTarget(EnchantmentTarget target, ItemStack probe) {
        if (target == null) return false;
        String id = target.getID();
        if (id != null) {
            String normalized = id.toLowerCase(Locale.ROOT);
            if (normalized.equals("pickaxe") || normalized.endsWith(":pickaxe")
                    || normalized.equals("all") || normalized.endsWith(":all")) {
                return true;
            }
        }
        return probe != null && target.matches(probe);
    }

    public EcoEnchant findEcoEnchant(Enchantment enchantment) {
        if (enchantment == null || !isEcoEnchantsPresent()) return null;
        for (EcoEnchant candidate : EcoEnchants.INSTANCE.values()) {
            if (candidate.getEnchantment() != null
                    && candidate.getEnchantment().getKey().equals(enchantment.getKey())) {
                return candidate;
            }
        }
        return null;
    }

    public Map<String, Integer> extractEnchantsFromBook(ItemStack bookItem) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (bookItem == null || !bookItem.hasItemMeta()) return result;

        ItemMeta meta = bookItem.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            addEcoEnchants(result, storageMeta.getStoredEnchants());
        }
        addEcoEnchants(result, meta.getEnchants());
        return result;
    }

    public String getEnchantmentHeader(Enchantment enchantment, int level) {
        EcoEnchant ecoEnchant = findEcoEnchant(enchantment);
        return ecoEnchant == null ? "" : EnchantmentFormattingKt.getFormattedName(ecoEnchant, Math.max(1, level));
    }

    public List<String> getEnchantmentDescription(Enchantment enchantment) {
        return getEnchantmentDescription(enchantment, 1);
    }

    public List<String> getEnchantmentDescription(Enchantment enchantment, int level) {
        EcoEnchant ecoEnchant = findEcoEnchant(enchantment);
        if (ecoEnchant == null) return List.of();
        return List.copyOf(EnchantmentFormattingKt.getFormattedDescription(ecoEnchant, Math.max(1, level)));
    }

    public String getEnchantmentDisplayName(Enchantment enchantment, String displayColor) {
        EcoEnchant ecoEnchant = findEcoEnchant(enchantment);
        if (ecoEnchant == null) return "";
        return formatDisplayName(ecoEnchant.getRawDisplayName(), displayColor);
    }

    /**
     * EcoEnchants calls this a type format. Gradients look noisy on the socket
     * menu, so use their first color as a stable representative default.
     */
    public static String getDefaultDisplayColor(EcoEnchant ecoEnchant) {
        if (ecoEnchant == null || ecoEnchant.getType() == null) return "<gray>";
        return collapseGradientToFirstColor(ecoEnchant.getType().getFormat());
    }

    static String collapseGradientToFirstColor(String format) {
        if (format == null || format.isBlank()) return "<gray>";
        Matcher matcher = FIRST_GRADIENT_COLOR.matcher(format);
        return matcher.find() ? "<" + matcher.group(1) + ">" : format;
    }

    static String formatDisplayName(String rawName, String displayColor) {
        String color = displayColor == null || displayColor.isBlank() ? "<gray>" : displayColor;
        return color + (rawName == null ? "" : rawName) + "<reset>";
    }

    public boolean canApply(ItemStack item, Enchantment enchantment) {
        EcoEnchant ecoEnchant = findEcoEnchant(enchantment);
        return ecoEnchant != null && item != null
                && ecoEnchant.canEnchantItem(item, item.getEnchantments().keySet());
    }

    /** Checks EcoEnchants' live, authoritative conflict rules in both directions. */
    public boolean conflictsWith(Enchantment first, Enchantment second) {
        if (first == null || second == null) return false;
        EcoEnchant firstEco = findEcoEnchant(first);
        EcoEnchant secondEco = findEcoEnchant(second);
        return (firstEco != null && firstEco.conflictsWith(second))
                || (secondEco != null && secondEco.conflictsWith(first));
    }

    public static String cleanEnchantmentName(String text) {
        if (text == null || text.isBlank()) return "";
        return TextUtil.stripFormatting(text)
                .replaceAll("(?i)\\s+(M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{1,3})|[0-9]+)$", "")
                .trim();
    }

    private void addEcoEnchants(Map<String, Integer> target, Map<Enchantment, Integer> source) {
        for (Map.Entry<Enchantment, Integer> entry : source.entrySet()) {
            if (findEcoEnchant(entry.getKey()) != null) {
                target.put(entry.getKey().getKey().toString().toLowerCase(Locale.ROOT), entry.getValue());
            }
        }
    }
}
