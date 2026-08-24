package com.infinitypickaxes.core.duplicate;

public enum DuplicateStatus {
    ACTIVE,
    QUARANTINED,
    REVOKED;

    public boolean isRestricted() {
        return this != ACTIVE;
    }
}
