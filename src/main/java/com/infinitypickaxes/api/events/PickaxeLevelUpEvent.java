package com.infinitypickaxes.api.events;

import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PickaxeLevelUpEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final InfinityPickaxe pickaxe;
    private final int oldLevel;
    private final int newLevel;

    public PickaxeLevelUpEvent(Player player, InfinityPickaxe pickaxe, int oldLevel, int newLevel) {
        this.player = player;
        this.pickaxe = pickaxe;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public Player getPlayer() {
        return player;
    }

    public InfinityPickaxe getPickaxe() {
        return pickaxe;
    }

    public int getOldLevel() {
        return oldLevel;
    }

    public int getNewLevel() {
        return newLevel;
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
