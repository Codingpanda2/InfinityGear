package com.infinitypickaxes.core.duplicate;

import java.time.Instant;
import java.util.UUID;

public record DuplicateRecord(
        UUID uuid,
        DuplicateStatus status,
        Instant firstDetected,
        Instant lastUpdated,
        String reason,
        String resolvedBy,
        UUID replacementUuid,
        String trackedKind,
        String trackedType
) {
    /** Binary/source compatibility constructor for legacy pickaxe callers. */
    public DuplicateRecord(UUID uuid, DuplicateStatus status, Instant firstDetected, Instant lastUpdated,
                           String reason, String resolvedBy, UUID replacementUuid) {
        this(uuid, status, firstDetected, lastUpdated, reason, resolvedBy, replacementUuid,
                "GEAR", "infinitygear:pickaxe");
    }
}
