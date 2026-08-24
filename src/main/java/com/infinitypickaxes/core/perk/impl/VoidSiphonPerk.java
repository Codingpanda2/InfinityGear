package com.infinitypickaxes.core.perk.impl;

import com.infinitypickaxes.core.perk.PickaxePerk;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.List;

public class VoidSiphonPerk implements PickaxePerk {

    private final String id = "void_siphon";
    private final String displayName;
    private final Material icon;
    private final int slot;
    private final boolean enabled;
    private final int requiredLevel;
    private final List<String> description;
    private final double xpMultiplier;

    public VoidSiphonPerk(String displayName, Material icon, int slot, boolean enabled, int requiredLevel, List<String> description, double xpMultiplier) {
        this.displayName = displayName;
        this.icon = icon != null ? icon : Material.END_CRYSTAL;
        this.slot = slot;
        this.enabled = enabled;
        this.requiredLevel = requiredLevel;
        this.description = description;
        this.xpMultiplier = Math.max(1.0, xpMultiplier);
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

    public double getXpMultiplier() {
        return xpMultiplier;
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event, InfinityPickaxe pickaxe, Player player) {
        if (event.isCancelled()) return;
        event.getBlock().getWorld().spawnParticle(Particle.PORTAL, event.getBlock().getLocation().add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, 0.5);
    }
}
