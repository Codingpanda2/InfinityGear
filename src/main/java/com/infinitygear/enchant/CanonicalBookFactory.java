package com.infinitygear.enchant;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

/** Produces a fresh canonical book without copying arbitrary source metadata or PDC. */
public final class CanonicalBookFactory {
    public ItemStack create(Enchantment enchantment, int level) {
        if (enchantment == null || level < 1) throw new IllegalArgumentException("A valid enchantment level is required.");
        ItemStack result = new ItemStack(Material.ENCHANTED_BOOK);
        if (!(result.getItemMeta() instanceof EnchantmentStorageMeta meta)) {
            throw new IllegalStateException("Bukkit did not provide enchanted-book metadata.");
        }
        if (!meta.addStoredEnchant(enchantment, level, true)) {
            throw new IllegalStateException("Bukkit rejected canonical enchanted-book creation.");
        }
        result.setItemMeta(meta);
        return result;
    }
}
