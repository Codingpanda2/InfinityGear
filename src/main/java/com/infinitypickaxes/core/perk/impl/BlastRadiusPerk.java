package com.infinitypickaxes.core.perk.impl;

import com.infinitypickaxes.core.perk.PickaxePerk;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.List;
import java.util.Random;

public class BlastRadiusPerk implements PickaxePerk {

    private final String id = "blast_radius";
    private final String displayName;
    private final Material icon;
    private final int slot;
    private final boolean enabled;
    private final int requiredLevel;
    private final List<String> description;
    private final double chance;
    private final Random random = new Random();

    public BlastRadiusPerk(String displayName, Material icon, int slot, boolean enabled, int requiredLevel, List<String> description, double chance) {
        this.displayName = displayName;
        this.icon = icon != null ? icon : Material.TNT;
        this.slot = slot;
        this.enabled = enabled;
        this.requiredLevel = requiredLevel;
        this.description = description;
        this.chance = chance;
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

        if (random.nextDouble() * 100.0 <= chance) {
            Block center = event.getBlock();
            int radius = 1; // 3x3 area

            center.getWorld().spawnParticle(Particle.EXPLOSION, center.getLocation().add(0.5, 0.5, 0.5), 1);
            center.getWorld().playSound(center.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.4f);

            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;
                        Block relative = center.getRelative(x, y, z);
                        if (!relative.getType().isAir() && relative.getType().getHardness() >= 0 && relative.getType() != Material.BEDROCK) {
                            relative.breakNaturally(pickaxe.getItemStack());
                        }
                    }
                }
            }
        }
    }
}
