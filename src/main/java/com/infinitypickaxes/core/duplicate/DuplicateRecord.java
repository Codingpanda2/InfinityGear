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
        UUID replacementUuid
) {}
