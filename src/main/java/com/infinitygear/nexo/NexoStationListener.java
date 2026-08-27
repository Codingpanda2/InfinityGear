package com.infinitygear.nexo;

import com.infinitygear.gui.StationGui;
import com.infinitygear.station.StationManager;
import com.infinitypickaxes.InfinityPickaxes;
import com.nexomc.nexo.api.events.custom_block.NexoBlockInteractEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureBreakEvent;
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
        org.bukkit.Location origin = event.getBaseEntity().getLocation();
        if (stations.hasPendingFurnitureBinding(event.getPlayer())) {
            event.setCancelled(true);
            var bound = stations.completeFurnitureBinding(event.getPlayer(), event.getMechanic().getItemID(), origin);
            event.getPlayer().sendMessage(bound
                    .map(type -> "§aBound this Nexo furniture as §f" + type.configKey() + "§a.")
                    .orElse("§cThat furniture ID does not match the armed station type."));
            return;
        }
        stations.identifyNexo(event.getPlayer(), event.getMechanic().getItemID(), origin)
                .ifPresent(type -> {
                    event.setCancelled(true);
                    new StationGui(plugin, event.getPlayer(), new com.infinitygear.station.StationSession(
                            type, origin, event.getMechanic().getItemID(), false)).open();
                });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnitureBreak(NexoFurnitureBreakEvent event) {
        stations.unbind(event.getBaseEntity().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemsLoaded(NexoItemsLoadedEvent event) {
        plugin.refreshCostProviders();
    }
}
