package com.infinitypickaxes.core.perk.impl;

import com.infinitypickaxes.core.perk.PickaxePerk;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class AutoSmeltPerk implements PickaxePerk {

    private final String id = "autosmelt";
    private final String displayName;
    private final Material icon;
    private final int slot;
    private final boolean enabled;
    private final int requiredLevel;
    private final List<String> description;

    public AutoSmeltPerk(String displayName, Material icon, int slot, boolean enabled, int requiredLevel, List<String> description) {
        this.displayName = displayName;
        this.icon = icon != null ? icon : Material.LAVA_BUCKET;
        this.slot = slot;
        this.enabled = enabled;
        this.requiredLevel = requiredLevel;
        this.description = description;
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
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        Material type = block.getType();
        Material smelted = getSmeltedResult(type);

        if (smelted != null) {
            event.setDropItems(false);
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(smelted, 1));
        }
    }

    private Material getSmeltedResult(Material raw) {
        return switch (raw) {
            case RAW_IRON, RAW_IRON_BLOCK, IRON_ORE, DEEPSLATE_IRON_ORE -> Material.IRON_INGOT;
            case RAW_GOLD, RAW_GOLD_BLOCK, GOLD_ORE, DEEPSLATE_GOLD_ORE, NETHER_GOLD_ORE -> Material.GOLD_INGOT;
            case RAW_COPPER, RAW_COPPER_BLOCK, COPPER_ORE, DEEPSLATE_COPPER_ORE -> Material.COPPER_INGOT;
            case COBBLESTONE -> Material.STONE;
            case COBBLED_DEEPSLATE -> Material.DEEPSLATE;
            case SAND, RED_SAND -> Material.GLASS;
            case ANCIENT_DEBRIS -> Material.NETHERITE_SCRAP;
            case CLAY -> Material.TERRACOTTA;
            default -> null;
        };
    }
}
