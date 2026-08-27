package com.infinitygear.nexo;

import com.infinitygear.gui.StationGui;
import com.infinitygear.station.StationManager;
import com.infinitypickaxes.InfinityPickaxes;
import com.nexomc.nexo.api.events.custom_block.NexoBlockInteractEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import com.nexomc.nexo.api.events.NexoItemsLoadedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/** Typed listeners for Nexo custom blocks and furniture stations. */
public final class NexoStationListener implements Listener {
    private final InfinityPickaxes plugin;
    private final StationManager stations;

    public NexoStationListener(InfinityPickaxes plugin, StationManager stations) {
        this.plugin = plugin;
        this.stations = stations;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlock(NexoBlockInteractEvent event) {
        stations.identifyNexo(event.getPlayer(), event.getMechanic().getItemID(), event.getBlock().getLocation())
                .ifPresent(type -> {
                    event.setCancelled(true);
                    new StationGui(plugin, event.getPlayer(), new com.infinitygear.station.StationSession(
                            type, event.getBlock().getLocation(), event.getMechanic().getItemID(), false)).open();
                });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFurniture(NexoFurnitureInteractEvent event) {
        stations.identifyNexo(event.getPlayer(), event.getMechanic().getItemID(), event.getInteractionPoint())
                .ifPresent(type -> {
                    event.setCancelled(true);
                    new StationGui(plugin, event.getPlayer(), new com.infinitygear.station.StationSession(
                            type, event.getInteractionPoint(), event.getMechanic().getItemID(), false)).open();
                });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemsLoaded(NexoItemsLoadedEvent event) {
        plugin.refreshCostProviders();
    }
}
