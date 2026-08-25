package com.infinitypickaxes.listeners;

/** Coalesces a burst of scan requests while retaining the most recent audit actor. */
final class ScanDebouncer {
    private boolean pending;
    private String latestActor;

    boolean request(String actor) {
        latestActor = actor;
        if (pending) return false;
        pending = true;
        return true;
    }

    String consume() {
        if (!pending) return null;
        String actor = latestActor;
        latestActor = null;
        pending = false;
        return actor;
    }

    void clear() {
        pending = false;
        latestActor = null;
    }
}
