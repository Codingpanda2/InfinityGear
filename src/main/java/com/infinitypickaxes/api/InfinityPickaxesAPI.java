package com.infinitypickaxes.api;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.core.enchant.EnchantManager;
import com.infinitypickaxes.core.level.LevelManager;
import com.infinitypickaxes.core.perk.PerkManager;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import com.infinitypickaxes.core.pickaxe.PickaxeData;
import com.infinitypickaxes.core.pickaxe.PickaxeManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class InfinityPickaxesAPI {

    private InfinityPickaxesAPI() {}

    public static boolean isInfinityPickaxe(ItemStack item) {
        return PickaxeData.isInfinityPickaxe(item);
    }

    public static InfinityPickaxe getPickaxe(ItemStack item) {
        return PickaxeData.fromItemStack(item);
    }

    public static InfinityPickaxe getHeldPickaxe(Player player) {
        return InfinityPickaxes.getInstance().getPickaxeManager().getHeldPickaxe(player);
    }

    public static ItemStack createPickaxe(UUID ownerUuid, String ownerName, int startingLevel) {
        return InfinityPickaxes.getInstance().getPickaxeManager().createPickaxe(ownerUuid, ownerName, startingLevel);
    }

    public static LevelManager getLevelManager() {
        return InfinityPickaxes.getInstance().getLevelManager();
    }

    public static EnchantManager getEnchantManager() {
        return InfinityPickaxes.getInstance().getEnchantManager();
    }

    public static PerkManager getPerkManager() {
        return InfinityPickaxes.getInstance().getPerkManager();
    }

    public static PickaxeManager getPickaxeManager() {
        return InfinityPickaxes.getInstance().getPickaxeManager();
    }
}
