package com.infinitypickaxes.core.pickaxe;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.core.enchant.EcoEnchantsHook;
import com.infinitypickaxes.core.enchant.EnchantSocket;
import com.infinitypickaxes.core.perk.PickaxePerk;
import com.infinitypickaxes.utils.ProgressBarUtil;
import com.infinitypickaxes.utils.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class PickaxeManager {

    private final InfinityPickaxes plugin;

    public PickaxeManager(InfinityPickaxes plugin) {
        this.plugin = plugin;
    }

    /**
     * Checks whether a Material is any recognized pickaxe.
     */
    public boolean isPickaxeMaterial(Material material) {
        if (material == null) return false;
        String name = material.name();
        return name.endsWith("_PICKAXE");
    }

    /**
     * Creates a brand new Infinity Pickaxe item stack.
     */
    public ItemStack createPickaxe(int startingLevel) {
        return createPickaxe(null, null, startingLevel);
    }

    /**
     * Creates a brand new Infinity Pickaxe item stack with optional metadata.
     */
    public ItemStack createPickaxe(UUID ownerUuid, String ownerName, int startingLevel) {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        Material material = Material.matchMaterial(config.getString("settings.default-material", "NETHERITE_PICKAXE"));
        if (material == null) material = Material.NETHERITE_PICKAXE;

        ItemStack item = new ItemStack(material);
        InfinityPickaxe pickaxe = new InfinityPickaxe(
                item,
                UUID.randomUUID(),
                ownerUuid,
                ownerName != null ? ownerName : "",
                startingLevel,
                0.0,
                0L,
                new LinkedHashMap<>(),
                new HashSet<>()
        );

        syncPickaxe(pickaxe);
        return pickaxe.getItemStack();
    }

    /**
     * Converts a vanilla pickaxe into an Infinity Pickaxe on the fly, preserving any existing enchantments.
     */
    public InfinityPickaxe convertVanillaPickaxe(ItemStack item, Player player) {
        if (item == null || !isPickaxeMaterial(item.getType())) return null;
        if (PickaxeData.isInfinityPickaxe(item)) {
            return PickaxeData.fromItemStack(item);
        }

        Map<String, Integer> enchants = new LinkedHashMap<>();
        if (item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
            for (Map.Entry<Enchantment, Integer> entry : item.getItemMeta().getEnchants().entrySet()) {
                if (entry.getKey() != null && entry.getKey().getKey() != null) {
                    enchants.put(entry.getKey().getKey().toString().toLowerCase(), entry.getValue());
                }
            }
        }

        InfinityPickaxe pickaxe = new InfinityPickaxe(
                item,
                UUID.randomUUID(),
                null,
                "",
                0,
                0.0,
                0L,
                enchants,
                new HashSet<>()
        );

        syncPickaxe(pickaxe);
        return pickaxe;
    }

    /**
     * Gets existing InfinityPickaxe or auto-converts a vanilla pickaxe if auto-conversion is enabled.
     */
    public InfinityPickaxe getOrCreatePickaxe(ItemStack item, Player player) {
        if (item == null || !isPickaxeMaterial(item.getType())) return null;

        if (PickaxeData.isInfinityPickaxe(item)) {
            return PickaxeData.fromItemStack(item);
        }

        FileConfiguration config = plugin.getConfigManager().getConfig();
        if (config.getBoolean("settings.auto-convert-vanilla", true)) {
            return convertVanillaPickaxe(item, player);
        }

        return null;
    }

    /**
     * Synchronizes the pickaxe state to its underlying ItemStack.
     */
    public void syncPickaxe(InfinityPickaxe pickaxe) {
        if (pickaxe == null) return;
        ItemStack item = pickaxe.getItemStack();
        if (item == null) return;

        // 1. Save data into PDC
        PickaxeData.saveToItemStack(pickaxe, item);

        // 2. Refresh Lore, Display Name, Unbreakable & Enchants
        updateLore(pickaxe);
    }

    /**
     * Rebuilds the pickaxe ItemMeta, including dynamic Lore, Display Name, Real Enchantments, and Unbreakable tags.
     */
    public void updateLore(InfinityPickaxe pickaxe) {
        if (pickaxe == null || pickaxe.getItemStack() == null) return;

        ItemStack item = pickaxe.getItemStack();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        FileConfiguration config = plugin.getConfigManager().getConfig();

        // 1. Unbreakable & Flags
        meta.setUnbreakable(true);
        if (config.getBoolean("settings.hide-flags", true)) {
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        }

        // 2. Real Enchantments synchronization (applies Bukkit/Eco enchantments to item so vanilla/eco effects work)
        for (Enchantment e : new ArrayList<>(meta.getEnchants().keySet())) {
            meta.removeEnchant(e);
        }
        for (Map.Entry<String, Integer> entry : pickaxe.getEnchantments().entrySet()) {
            String keyStr = entry.getKey();
            int level = entry.getValue();
            if (level <= 0) continue;

            Enchantment enchantment = getEnchantment(keyStr);
            if (enchantment != null) {
                meta.addEnchant(enchantment, level, true);
            }
        }

        // 3. Display Name (Only modified if custom-display-name is enabled)
        if (config.getBoolean("pickaxe-lore.custom-display-name", false)) {
            String nameTemplate = config.getString("pickaxe-lore.display-name", "");
            if (!nameTemplate.isEmpty()) {
                nameTemplate = nameTemplate.replace("%level%", String.valueOf(pickaxe.getLevel()));
                meta.displayName(TextUtil.parse(nameTemplate));
            }
        }

        // 4. Progress bar calculation
        double reqXp = plugin.getLevelManager().getRequiredXp(pickaxe.getLevel());
        String bar = ProgressBarUtil.getProgressBar(
                pickaxe.getXp(),
                reqXp,
                config.getInt("progress-bar.total-bars", 20),
                config.getString("progress-bar.completed-symbol", "■"),
                config.getString("progress-bar.uncompleted-symbol", "□"),
                config.getString("progress-bar.completed-color", "<#00FF88>"),
                config.getString("progress-bar.uncompleted-color", "<#555555>")
        );

        // 5. Enchantments List format (Exact layout: Title + Roman Numerals + Multi-line Descriptions)
        List<String> enchantLines = new ArrayList<>();
        if (pickaxe.getEnchantments().isEmpty()) {
            String noEnchantsText = config.getString("formats.no-enchants", "");
            if (noEnchantsText != null && !noEnchantsText.trim().isEmpty()) {
                enchantLines.add(noEnchantsText);
            }
        } else {
            for (Map.Entry<String, Integer> entry : pickaxe.getEnchantments().entrySet()) {
                String keyStr = entry.getKey();
                int level = entry.getValue();
                if (level <= 0) continue;

                EnchantSocket socket = plugin.getEnchantManager().getSocketByKey(keyStr);
                if (socket == null && keyStr.contains(":")) {
                    socket = plugin.getEnchantManager().getSocket(keyStr.substring(keyStr.indexOf(":") + 1));
                }

                Enchantment ench = getEnchantment(keyStr);
                int maxLevel = 1;
                if (socket != null) {
                    maxLevel = socket.getMaxLevel();
                } else if (ench != null) {
                    maxLevel = ench.getMaxLevel();
                }

                String baseNameWithColor = plugin.getEnchantManager().getEcoHook().getEnchantmentDisplayName(ench);
                String roman = TextUtil.toRoman(level);
                String header;

                if (maxLevel > 1 || level > 1) {
                    if (baseNameWithColor.startsWith("<#") || baseNameWithColor.startsWith("<gradient")) {
                        int closeTag = baseNameWithColor.lastIndexOf("</");
                        if (closeTag > 0) {
                            String openTag = baseNameWithColor.substring(0, baseNameWithColor.indexOf(">") + 1);
                            String closeTagStr = baseNameWithColor.substring(closeTag);
                            String content = baseNameWithColor.substring(openTag.length(), closeTag);
                            header = openTag + content + " " + roman + closeTagStr;
                        } else {
                            header = baseNameWithColor + " <#00E5FF>" + roman + "</#00E5FF>";
                        }
                    } else {
                        String clean = EcoEnchantsHook.cleanEnchantmentName(baseNameWithColor);
                        header = "<gray>" + clean + "</gray> <#00E5FF>" + roman + "</#00E5FF>";
                    }
                } else {
                    String clean = EcoEnchantsHook.cleanEnchantmentName(baseNameWithColor);
                    header = "<gray>" + clean + "</gray>";
                }

                enchantLines.add(header);

                // Add multi-line description underneath
                List<String> desc = plugin.getEnchantManager().getEcoHook().getEnchantmentDescription(ench, level);
                if (desc != null && !desc.isEmpty()) {
                    for (String d : desc) {
                        if (d != null && !d.trim().isEmpty()) {
                            enchantLines.add(d);
                        }
                    }
                }
            }
        }

        // 6. Perks List format (if used in custom lore templates)
        String perkLineFormat = config.getString("formats.perk-line", "  <gray>• <gold>%perk_name%</gold> <green>(Active)</green>");
        String noPerksText = config.getString("formats.no-perks", "");
        List<String> perkLines = new ArrayList<>();
        if (pickaxe.getEquippedPerks().isEmpty()) {
            if (noPerksText != null && !noPerksText.trim().isEmpty()) {
                perkLines.add(noPerksText);
            }
        } else {
            for (String perkId : pickaxe.getEquippedPerks()) {
                PickaxePerk perk = plugin.getPerkManager().getPerk(perkId);
                String pName = (perk != null) ? perk.getDisplayName() : perkId;
                String line = perkLineFormat.replace("%perk_name%", pName);
                perkLines.add(line);
            }
        }

        // 7. Assemble Lore
        List<String> loreTemplates = config.getStringList("pickaxe-lore.lore");
        List<Component> finalLore = new ArrayList<>();

        int maxSockets = config.getInt("settings.max-sockets", 10);
        int maxPerks = plugin.getLevelManager().getMaxPerksForLevel(pickaxe.getLevel());

        for (String template : loreTemplates) {
            if (template.contains("%enchants_list%")) {
                for (String eLine : enchantLines) {
                    finalLore.add(TextUtil.parse(eLine));
                }
            } else if (template.contains("%perks_list%")) {
                for (String pLine : perkLines) {
                    finalLore.add(TextUtil.parse(pLine));
                }
            } else {
                String processed = template
                        .replace("%level%", String.valueOf(pickaxe.getLevel()))
                        .replace("%max_level%", String.valueOf(plugin.getLevelManager().getMaxLevel()))
                        .replace("%current_xp%", String.format("%.0f", pickaxe.getXp()))
                        .replace("%required_xp%", String.format("%.0f", reqXp))
                        .replace("%xp_bar%", bar)
                        .replace("%blocks_mined%", String.format("%,d", pickaxe.getBlocksMined()))
                        .replace("%enchant_count%", String.valueOf(pickaxe.getEnchantments().size()))
                        .replace("%max_sockets%", String.valueOf(maxSockets))
                        .replace("%perks_count%", String.valueOf(pickaxe.getEquippedPerks().size()))
                        .replace("%max_perks%", String.valueOf(maxPerks));
                finalLore.add(TextUtil.parse(processed));
            }
        }

        meta.lore(finalLore);
        item.setItemMeta(meta);
    }

    private Enchantment getEnchantment(String keyStr) {
        try {
            String[] parts = keyStr.split(":", 2);
            NamespacedKey key = (parts.length == 2) ? new NamespacedKey(parts[0], parts[1]) : NamespacedKey.minecraft(parts[0]);
            return Bukkit.getRegistry(Enchantment.class).get(key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Checks if the item held by a player in main hand is an Infinity Pickaxe (or converts vanilla) and returns the model.
     */
    public InfinityPickaxe getHeldPickaxe(Player player) {
        if (player == null) return null;
        ItemStack held = player.getInventory().getItemInMainHand();
        return getOrCreatePickaxe(held, player);
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
