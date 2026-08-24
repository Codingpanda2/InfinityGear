package com.infinitypickaxes.api.events;

import com.infinitypickaxes.core.enchant.EnchantSocket;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class LimitBreakApplyEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    private final InfinityPickaxe pickaxe;
    private final EnchantSocket socket;
    private final ItemStack bookItem;
    private final boolean universal;
    private final int oldLevel;
    private final int newLevel;

    public LimitBreakApplyEvent(@NotNull Player player,
                                @NotNull InfinityPickaxe pickaxe,
                                @NotNull EnchantSocket socket,
                                @NotNull ItemStack bookItem,
                                boolean universal,
                                int oldLevel,
                                int newLevel) {
        super(player);
        this.pickaxe = pickaxe;
        this.socket = socket;
        this.bookItem = bookItem;
        this.universal = universal;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    @NotNull
    public InfinityPickaxe getPickaxe() {
        return pickaxe;
    }

    @NotNull
    public EnchantSocket getSocket() {
        return socket;
    }

    @NotNull
    public ItemStack getBookItem() {
        return bookItem;
    }

    public boolean isUniversal() {
        return universal;
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

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
