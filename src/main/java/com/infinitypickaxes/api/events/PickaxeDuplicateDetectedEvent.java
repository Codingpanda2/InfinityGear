package com.infinitypickaxes.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public final class PickaxeDuplicateDetectedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID pickaxeUuid;
    private final List<String> sightings;

    public PickaxeDuplicateDetectedEvent(UUID pickaxeUuid, List<String> sightings) {
        this.pickaxeUuid = pickaxeUuid;
        this.sightings = List.copyOf(sightings);
    }

    public UUID getPickaxeUuid() { return pickaxeUuid; }
    public List<String> getSightings() { return sightings; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}
