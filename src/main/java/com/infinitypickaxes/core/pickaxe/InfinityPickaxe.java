package com.infinitypickaxes.core.pickaxe;

import com.infinitypickaxes.InfinityPickaxes;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class InfinityPickaxe {

    private ItemStack itemStack;
    private final UUID uuid;
    private int level;
    private double xp;
    private long blocksMined;

    public InfinityPickaxe(ItemStack itemStack, UUID uuid, int level, double xp, long blocksMined) {
        this.itemStack = itemStack;
        this.uuid = uuid != null ? uuid : UUID.randomUUID();
        this.level = 0;
        setLevel(level);
        this.xp = Math.max(0.0, xp);
        this.blocksMined = Math.max(0L, blocksMined);
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        int maximum = 100;
        InfinityPickaxes instance = InfinityPickaxes.getInstance();
        if (instance != null && instance.getLevelManager() != null) {
            maximum = instance.getLevelManager().getMaxLevel();
        }
        this.level = Math.min(maximum, Math.max(0, level));
    }

    public double getXp() {
        return xp;
    }

    public void setXp(double xp) {
        this.xp = Math.max(0.0, xp);
    }

    public void addXp(double amount) {
        if (amount > 0) {
            this.xp += amount;
        }
    }

    public long getBlocksMined() {
        return blocksMined;
    }

    public void incrementBlocksMined() {
        this.blocksMined++;
    }

    public void addBlocksMined(long amount) {
        if (amount > 0) {
            this.blocksMined += amount;
        }
    }

    public Map<String, Integer> getEnchantments() {
        Map<String, Integer> enchantments = new LinkedHashMap<>();
        if (itemStack == null || !itemStack.hasItemMeta()) return enchantments;
        for (Map.Entry<Enchantment, Integer> entry : itemStack.getItemMeta().getEnchants().entrySet()) {
            enchantments.put(entry.getKey().getKey().toString().toLowerCase(Locale.ROOT), entry.getValue());
        }
        return enchantments;
    }

    public int getEnchantmentLevel(String enchantKey) {
        if (enchantKey == null) return 0;
        Enchantment enchantment = resolveEnchantment(enchantKey);
        return enchantment == null || itemStack == null ? 0 : itemStack.getEnchantmentLevel(enchantment);
    }

    public void setEnchantmentLevel(String enchantKey, int level) {
        if (itemStack == null) return;
        Enchantment enchantment = resolveEnchantment(enchantKey);
        ItemMeta meta = itemStack.getItemMeta();
        if (enchantment == null || meta == null) return;
        if (level <= 0) meta.removeEnchant(enchantment);
        else meta.addEnchant(enchantment, level, true);
        itemStack.setItemMeta(meta);
    }

    /**
     * Synchronizes this pickaxe's state into the ItemStack PDC and refreshes lore.
     */
    public void saveAndSync() {
        InfinityPickaxes.getInstance().getPickaxeManager().syncPickaxe(this);
    }

    private Enchantment resolveEnchantment(String keyString) {
        if (keyString == null || keyString.isBlank()) return null;
        try {
            NamespacedKey key = NamespacedKey.fromString(keyString);
            return key == null ? null : Bukkit.getRegistry(Enchantment.class).get(key);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
