package com.infinitygear.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class GearSocketExpansionEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final ItemStack gear;
    private final int oldCapacity;
    private final int newCapacity;
    private boolean cancelled;
    public GearSocketExpansionEvent(Player player, ItemStack gear, int oldCapacity, int newCapacity) {
        this.player = player; this.gear = gear; this.oldCapacity = oldCapacity; this.newCapacity = newCapacity;
    }
    public Player getPlayer() { return player; }
    public ItemStack getGear() { return gear; }
    public int getOldCapacity() { return oldCapacity; }
    public int getNewCapacity() { return newCapacity; }
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancel) { cancelled = cancel; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}
