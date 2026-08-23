package com.infinitypickaxes.api.events;

import com.infinitypickaxes.core.enchant.EnchantSocket;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PickaxeEnchantUpgradeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final InfinityPickaxe pickaxe;
    private final EnchantSocket socket;
    private final int oldLevel;
    private final int newLevel;
    private boolean cancelled = false;

    public PickaxeEnchantUpgradeEvent(Player player, InfinityPickaxe pickaxe, EnchantSocket socket, int oldLevel, int newLevel) {
        this.player = player;
        this.pickaxe = pickaxe;
        this.socket = socket;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public Player getPlayer() {
        return player;
    }

    public InfinityPickaxe getPickaxe() {
        return pickaxe;
    }

    public EnchantSocket getSocket() {
        return socket;
    }

    public int getOldLevel() {
        return oldLevel;
    }

    public int getNewLevel() {
        return newLevel;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
