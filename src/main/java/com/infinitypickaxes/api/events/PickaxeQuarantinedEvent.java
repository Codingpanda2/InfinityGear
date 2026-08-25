package com.infinitypickaxes.api.events;

import com.infinitypickaxes.core.duplicate.DuplicateStatus;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class PickaxeQuarantinedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID pickaxeUuid;
    private final DuplicateStatus status;
    private final String reason;
    private final String actor;

    public PickaxeQuarantinedEvent(UUID pickaxeUuid, DuplicateStatus status, String reason, String actor) {
        this.pickaxeUuid = pickaxeUuid;
        this.status = status;
        this.reason = reason;
        this.actor = actor;
    }

    public UUID getPickaxeUuid() { return pickaxeUuid; }
    public DuplicateStatus getStatus() { return status; }
    public String getReason() { return reason; }
    public String getActor() { return actor; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}
