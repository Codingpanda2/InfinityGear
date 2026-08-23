package com.infinitypickaxes.core.perk;

import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.List;

public interface PickaxePerk {

    String getId();

    String getDisplayName();

    Material getIcon();

    int getSlot();

    boolean isEnabled();

    int getRequiredLevel();

    List<String> getDescription();

    default void onBlockBreak(BlockBreakEvent event, InfinityPickaxe pickaxe, Player player) {}

    default void onTick(Player player, InfinityPickaxe pickaxe) {}
}
