package com.infinitypickaxes.listeners;

import com.infinitypickaxes.InfinityPickaxes;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class BlockPlaceListener implements Listener {

    private final InfinityPickaxes plugin;
    private final Set<Location> placedBlocks;

    public BlockPlaceListener(InfinityPickaxes plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfigManager().getConfig();
        int maxCapacity = config.getInt("anti-exploit.placed-blocks-cache-size", 50000);

        // LRU Cache for placed block locations
        Map<Location, Boolean> lruMap = new LinkedHashMap<Location, Boolean>(maxCapacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Location, Boolean> eldest) {
                return size() > maxCapacity;
            }
        };
        this.placedBlocks = Collections.synchronizedSet(Collections.newSetFromMap(lruMap));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        if (!config.getBoolean("anti-exploit.prevent-placed-blocks", true)) return;

        Block block = event.getBlockPlaced();
        placedBlocks.add(block.getLocation());
    }

    public boolean isPlacedByPlayer(Location location) {
        return placedBlocks.remove(location);
    }
}
