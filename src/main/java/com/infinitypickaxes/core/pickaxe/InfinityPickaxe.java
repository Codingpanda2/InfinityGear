package com.infinitypickaxes.core.pickaxe;

import com.infinitypickaxes.InfinityPickaxes;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class InfinityPickaxe {

    private ItemStack itemStack;
    private final UUID uuid;
    private UUID ownerUuid;
    private String ownerName;
    private int level;
    private double xp;
    private long blocksMined;
    private final Map<String, Integer> enchantments;
    private final Set<String> equippedPerks;

    public InfinityPickaxe(ItemStack itemStack, UUID uuid, UUID ownerUuid, String ownerName, int level, double xp, long blocksMined, Map<String, Integer> enchantments, Set<String> equippedPerks) {
        this.itemStack = itemStack;
        this.uuid = uuid != null ? uuid : UUID.randomUUID();
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName != null ? ownerName : "Desconocido";
        this.level = Math.max(0, level);
        this.xp = Math.max(0.0, xp);
        this.blocksMined = Math.max(0L, blocksMined);
        this.enchantments = enchantments != null ? new LinkedHashMap<>(enchantments) : new LinkedHashMap<>();
        this.equippedPerks = equippedPerks != null ? new HashSet<>(equippedPerks) : new HashSet<>();
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

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.min(100, Math.max(0, level));
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
        return enchantments;
    }

    public int getEnchantmentLevel(String enchantKey) {
        if (enchantKey == null) return 0;
        return enchantments.getOrDefault(enchantKey.toLowerCase(), 0);
    }

    public void setEnchantmentLevel(String enchantKey, int level) {
        if (enchantKey == null) return;
        if (level <= 0) {
            enchantments.remove(enchantKey.toLowerCase());
        } else {
            enchantments.put(enchantKey.toLowerCase(), level);
        }
    }

    public Set<String> getEquippedPerks() {
        return equippedPerks;
    }

    public boolean hasPerk(String perkId) {
        if (perkId == null) return false;
        return equippedPerks.contains(perkId.toLowerCase());
    }

    public void addPerk(String perkId) {
        if (perkId != null) {
            equippedPerks.add(perkId.toLowerCase());
        }
    }

    public void removePerk(String perkId) {
        if (perkId != null) {
            equippedPerks.remove(perkId.toLowerCase());
        }
    }

    /**
     * Synchronizes this pickaxe's state into the ItemStack PDC and refreshes lore.
     */
    public void saveAndSync() {
        InfinityPickaxes.getInstance().getPickaxeManager().syncPickaxe(this);
    }
}
