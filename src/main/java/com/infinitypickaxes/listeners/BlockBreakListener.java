package com.infinitypickaxes.listeners;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class BlockBreakListener implements Listener {

    private final InfinityPickaxes plugin;
    private final BlockPlaceListener placeListener;

    public BlockBreakListener(InfinityPickaxes plugin, BlockPlaceListener placeListener) {
        this.plugin = plugin;
        this.placeListener = placeListener;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();

        InfinityPickaxe pickaxe = plugin.getPickaxeManager().getOrCreatePickaxe(held, player);
        if (pickaxe == null) return;

        FileConfiguration config = plugin.getConfigManager().getConfig();

        // 1. Creative check
        if (player.getGameMode() == GameMode.CREATIVE && config.getBoolean("anti-exploit.ignore-creative", true)) {
            return;
        }

        Block block = event.getBlock();

        // 2. Anti-exploit placed block check
        if (placeListener != null && placeListener.isPlacedByPlayer(block.getLocation())) {
            return;
        }

        // 3. Determine XP reward from blocks.yml
        FileConfiguration blocksConfig = plugin.getConfigManager().getBlocksConfig();
        double defaultXp = blocksConfig.getDouble("default-xp", 1.0);
        String matName = block.getType().name();
        double xp = blocksConfig.getDouble("blocks." + matName, defaultXp);

        // 4. Apply Perk Multipliers
        double multiplier = plugin.getPerkManager().getActiveXpMultiplier(pickaxe);
        double totalXp = xp * multiplier;

        // 5. Add XP & update pickaxe progression
        plugin.getLevelManager().addXp(pickaxe, totalXp, player);

        // 6. Dispatch block break to active perks (e.g. AutoSmelt, BlastRadius, FortuneFrenzy)
        plugin.getPerkManager().dispatchBlockBreak(event, pickaxe, player);
    }
}
