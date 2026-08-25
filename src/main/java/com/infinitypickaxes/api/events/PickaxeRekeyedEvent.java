package com.infinitypickaxes.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class PickaxeRekeyedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player administrator;
    private final ItemStack pickaxe;
    private final UUID oldUuid;
    private final UUID newUuid;

    public PickaxeRekeyedEvent(Player administrator, ItemStack pickaxe, UUID oldUuid, UUID newUuid) {
        this.administrator = administrator;
        this.pickaxe = pickaxe;
        this.oldUuid = oldUuid;
        this.newUuid = newUuid;
    }

    public Player getAdministrator() { return administrator; }
    public ItemStack getPickaxe() { return pickaxe; }
    public UUID getOldUuid() { return oldUuid; }
    public UUID getNewUuid() { return newUuid; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}
