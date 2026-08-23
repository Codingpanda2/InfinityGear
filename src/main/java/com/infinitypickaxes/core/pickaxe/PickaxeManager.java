package com.infinitypickaxes.core.pickaxe;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.core.enchant.EnchantSocket;
import com.infinitypickaxes.core.perk.PickaxePerk;
import com.infinitypickaxes.utils.ItemBuilder;
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
    public ItemStack createPickaxe(UUID ownerUuid, String ownerName, int startingLevel) {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        Material material = Material.matchMaterial(config.getString("settings.default-material", "NETHERITE_PICKAXE"));
        if (material == null) material = Material.NETHERITE_PICKAXE;

        ItemStack item = new ItemStack(material);
        InfinityPickaxe pickaxe = new InfinityPickaxe(
                item,
                UUID.randomUUID(),
                ownerUuid,
                ownerName,
                startingLevel,
                0.0,
                0L,
                new LinkedHashMap<>(),
                new HashSet<>()
        );

        // Save PDC and update lore & enchants
        syncPickaxe(pickaxe);
        return pickaxe.getItemStack();
    }

    /**
     * Converts a vanilla pickaxe into an Infinity Pickaxe on the fly, preserving any existing enchantments.
     */
    public InfinityPickaxe convertVanillaPickaxe(ItemStack item, Player owner) {
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
                owner != null ? owner.getUniqueId() : null,
                owner != null ? owner.getName() : "Desconocido",
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

        // 2. Real Enchantments synchronization (applies Bukkit/Eco enchantments to item so effects work)
        // Clear old enchantments first
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

        // 3. Display Name
        String nameTemplate = config.getString("pickaxe-lore.display-name", "<gradient:#00E5FF:#0077FE><b>INFINITY PICKAXE</b></gradient> <gray>[<yellow>Nv.%level%<gray>]");
        nameTemplate = nameTemplate.replace("%level%", String.valueOf(pickaxe.getLevel()))
                                   .replace("%player%", pickaxe.getOwnerName());
        meta.displayName(TextUtil.parse(nameTemplate));

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

        // 5. Enchantments List format
        String enchantLineFormat = config.getString("formats.enchant-line", "  <gray>• <aqua>%enchant_name%</aqua> <yellow>%enchant_level%</yellow>");
        String noEnchantsText = config.getString("formats.no-enchants", "  <dark_gray><i>Sin encantamientos aplicados</i></dark_gray>");
        List<String> enchantLines = new ArrayList<>();
        if (pickaxe.getEnchantments().isEmpty()) {
            enchantLines.add(noEnchantsText);
        } else {
            for (Map.Entry<String, Integer> entry : pickaxe.getEnchantments().entrySet()) {
                EnchantSocket socket = plugin.getEnchantManager().getSocketByKey(entry.getKey());
                String dName = (socket != null) ? socket.getDisplayName() : entry.getKey();
                String line = enchantLineFormat
                        .replace("%enchant_name%", dName)
                        .replace("%enchant_level%", TextUtil.toRoman(entry.getValue()))
                        .replace("%enchant_raw_level%", String.valueOf(entry.getValue()));
                enchantLines.add(line);
            }
        }

        // 6. Perks List format
        String perkLineFormat = config.getString("formats.perk-line", "  <gray>• <gold>%perk_name%</gold> <green>(Activo)</green>");
        String noPerksText = config.getString("formats.no-perks", "  <dark_gray><i>Sin perks equipados</i></dark_gray>");
        List<String> perkLines = new ArrayList<>();
        if (pickaxe.getEquippedPerks().isEmpty()) {
            perkLines.add(noPerksText);
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
                        .replace("%player%", pickaxe.getOwnerName())
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
}
