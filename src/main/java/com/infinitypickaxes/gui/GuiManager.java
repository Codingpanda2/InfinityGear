package com.infinitypickaxes.gui;

import com.infinitypickaxes.InfinityPickaxes;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;
import java.util.logging.Level;

public class GuiManager implements Listener {

    private final InfinityPickaxes plugin;

    public GuiManager(InfinityPickaxes plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        if (inv == null) return;
        try {
            InventoryHolder holder = inv.getHolder();
            if (holder instanceof CustomGui gui) {
                boolean incomingCancellation = event.isCancelled();
                event.setCancelled(true);
                // An earlier protection owns the action. Do not even delegate;
                // a confirm button could otherwise mutate despite cancellation.
                if (incomingCancellation) return;
                if (gui.getPickaxe() != null
                        && !plugin.getDuplicateService().isUsable(gui.getPickaxe().getItemStack())) {
                    gui.getPlayer().closeInventory();
                    plugin.getMessageManager().sendMessage(gui.getPlayer(), "messages.pickaxe-quarantined");
                    return;
                }
                gui.handleClick(event);
            }
        } catch (RuntimeException exception) {
            failClosed(event.getWhoClicked().getName(), inv, exception);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory inv = event.getInventory();
        if (inv == null) return;
        try {
            InventoryHolder holder = inv.getHolder();
            if (holder instanceof CustomGui gui) {
                event.setCancelled(true);
                gui.handleDrag(event);
            }
        } catch (RuntimeException exception) {
            failClosed(event.getWhoClicked().getName(), inv, exception);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        if (inv == null) return;
        try {
            InventoryHolder holder = inv.getHolder();
            if (holder instanceof CustomGui gui) {
                gui.handleClose(event);
            }
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE,
                    "Custom GUI close handler failed for " + event.getPlayer().getName(), exception);
        }
    }

    private void failClosed(String playerName, Inventory inventory, RuntimeException exception) {
        plugin.getLogger().log(Level.SEVERE, "Custom GUI handler failed for " + playerName, exception);
        if (inventory.getViewers().isEmpty()) return;
        Bukkit.getScheduler().runTask(plugin,
                () -> List.copyOf(inventory.getViewers()).forEach(viewer -> viewer.closeInventory()));
    }
}
