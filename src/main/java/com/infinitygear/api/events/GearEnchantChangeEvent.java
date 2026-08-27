package com.infinitygear.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class GearEnchantChangeEvent extends Event implements Cancellable {
    public enum Operation { APPLY, LIMIT_BREAK, REMOVE, TRANSFER }
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final ItemStack gear;
    private final String profileId;
    private final String enchantmentKey;
    private final int oldLevel;
    private final int newLevel;
    private final Operation operation;
    private boolean cancelled;

    public GearEnchantChangeEvent(Player player, ItemStack gear, String profileId, String enchantmentKey,
                                  int oldLevel, int newLevel, Operation operation) {
        this.player = player;
        this.gear = gear;
        this.profileId = profileId;
        this.enchantmentKey = enchantmentKey;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
        this.operation = operation;
    }
    public Player getPlayer() { return player; }
    public ItemStack getGear() { return gear; }
    public String getProfileId() { return profileId; }
    public String getEnchantmentKey() { return enchantmentKey; }
    public int getOldLevel() { return oldLevel; }
    public int getNewLevel() { return newLevel; }
    public Operation getOperation() { return operation; }
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancel) { cancelled = cancel; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}
