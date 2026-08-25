package com.infinitypickaxes.core.duplicate;

import java.util.Set;
import java.util.UUID;

public record DuplicateScanResult(int itemsScanned, Set<UUID> duplicatesDetected) {
    public DuplicateScanResult {
        duplicatesDetected = Set.copyOf(duplicatesDetected);
    }
}
