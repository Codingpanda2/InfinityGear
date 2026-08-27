package com.infinitygear.station;

import com.infinitygear.gui.StationGui;
import com.infinitypickaxes.InfinityPickaxes;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class StationListener implements Listener {
    private final InfinityPickaxes plugin;
    private final StationManager stations;

    public StationListener(InfinityPickaxes plugin, StationManager stations) {
        this.plugin = plugin;
        this.stations = stations;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        stations.identify(event.getPlayer(), event.getClickedBlock())
                .filter(type -> "VANILLA".equals(stations.definition(type).provider())).ifPresent(type -> {
            event.setCancelled(true);
            new StationGui(plugin, event.getPlayer(), new StationSession(
                    type, event.getClickedBlock().getLocation(), null, false)).open();
        });
    }
}
