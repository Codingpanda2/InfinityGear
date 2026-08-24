package com.infinitypickaxes.gui;

import com.infinitypickaxes.InfinityPickaxes;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

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
                gui.handleClick(event);
            }
        } catch (Throwable ignored) {}
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory inv = event.getInventory();
        if (inv == null) return;
        try {
            InventoryHolder holder = inv.getHolder();
            if (holder instanceof CustomGui gui) {
                gui.handleDrag(event);
            }
        } catch (Throwable ignored) {}
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
        } catch (Throwable ignored) {}
    }
}
