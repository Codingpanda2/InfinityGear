package com.infinitypickaxes.core.perk.impl;

import com.infinitypickaxes.core.perk.PickaxePerk;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class HasteSurgePerk implements PickaxePerk {

    private final String id = "haste_surge";
    private final String displayName;
    private final Material icon;
    private final int slot;
    private final boolean enabled;
    private final int requiredLevel;
    private final List<String> description;
    private final int amplifier;

    public HasteSurgePerk(String displayName, Material icon, int slot, boolean enabled, int requiredLevel, List<String> description, int amplifier) {
        this.displayName = displayName;
        this.icon = icon != null ? icon : Material.GOLDEN_PICKAXE;
        this.slot = slot;
        this.enabled = enabled;
        this.requiredLevel = requiredLevel;
        this.description = description;
        this.amplifier = amplifier;
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
    public void onTick(Player player, InfinityPickaxe pickaxe) {
        if (player == null || pickaxe == null) return;
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 40, amplifier, false, false, true));
    }
}
