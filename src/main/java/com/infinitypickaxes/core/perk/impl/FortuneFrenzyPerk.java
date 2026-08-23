package com.infinitypickaxes.core.perk.impl;

import com.infinitypickaxes.core.perk.PickaxePerk;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.Random;

public class FortuneFrenzyPerk implements PickaxePerk {

    private final String id = "fortune_frenzy";
    private final String displayName;
    private final Material icon;
    private final int slot;
    private final boolean enabled;
    private final int requiredLevel;
    private final List<String> description;
    private final double chance;
    private final int multiplier;
    private final Random random = new Random();

    public FortuneFrenzyPerk(String displayName, Material icon, int slot, boolean enabled, int requiredLevel, List<String> description, double chance, int multiplier) {
        this.displayName = displayName;
        this.icon = icon != null ? icon : Material.EMERALD;
        this.slot = slot;
        this.enabled = enabled;
        this.requiredLevel = requiredLevel;
        this.description = description;
        this.chance = chance;
        this.multiplier = Math.max(2, multiplier);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Material getIcon() {
        return icon;
    }

    @Override
    public int getSlot() {
        return slot;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public int getRequiredLevel() {
        return requiredLevel;
    }

    @Override
    public List<String> getDescription() {
        return description;
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event, InfinityPickaxe pickaxe, Player player) {
        if (event.isCancelled() || chance <= 0) return;

        Block block = event.getBlock();
        String typeName = block.getType().name();
        if (typeName.contains("ORE") || typeName.contains("DEBRIS") || typeName.contains("RAW")) {
            if (random.nextDouble() * 100.0 <= chance) {
                Collection<ItemStack> drops = block.getDrops(pickaxe.getItemStack(), player);
                for (ItemStack drop : drops) {
                    drop.setAmount(drop.getAmount() * (multiplier - 1));
                    block.getWorld().dropItemNaturally(block.getLocation(), drop);
                }
                block.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, block.getLocation().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3, 0.1);
                block.getWorld().playSound(block.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.8f);
            }
        }
    }
}
