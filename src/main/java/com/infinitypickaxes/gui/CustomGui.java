package com.infinitypickaxes.gui;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public abstract class CustomGui implements InventoryHolder {

    protected final InfinityPickaxes plugin;
    protected final Player player;
    protected final InfinityPickaxe pickaxe;
    protected final Inventory inventory;

    public CustomGui(InfinityPickaxes plugin, Player player, InfinityPickaxe pickaxe, Component title, int size) {
        this.plugin = plugin;
        this.player = player;
        this.pickaxe = pickaxe;
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    public abstract void setupItems();

    public abstract void handleClick(InventoryClickEvent event);

    public void handleDrag(InventoryDragEvent event) {}

    public void handleClose(InventoryCloseEvent event) {}

    public void open() {
        setupItems();
        player.openInventory(inventory);
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }

    public InfinityPickaxe getPickaxe() {
        return pickaxe;
    }
}
