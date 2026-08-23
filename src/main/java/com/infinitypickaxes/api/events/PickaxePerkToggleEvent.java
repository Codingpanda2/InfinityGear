package com.infinitypickaxes.api.events;

import com.infinitypickaxes.core.perk.PickaxePerk;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PickaxePerkToggleEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final InfinityPickaxe pickaxe;
    private final PickaxePerk perk;
    private final boolean equipped;
    private boolean cancelled = false;

    public PickaxePerkToggleEvent(Player player, InfinityPickaxe pickaxe, PickaxePerk perk, boolean equipped) {
        this.player = player;
        this.pickaxe = pickaxe;
        this.perk = perk;
        this.equipped = equipped;
    }

    public Player getPlayer() {
        return player;
    }

    public InfinityPickaxe getPickaxe() {
        return pickaxe;
    }

    public PickaxePerk getPerk() {
        return perk;
    }

    public boolean isEquipped() {
        return equipped;
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
