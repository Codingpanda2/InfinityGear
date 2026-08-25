package com.infinitypickaxes.core.duplicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuplicateStoreTest {

    @TempDir
    Path tempDirectory;

    @Test
    void quarantinePersistsAcrossReopen() throws Exception {
        UUID uuid = UUID.randomUUID();
        Path database = tempDirectory.resolve("duplicates.db");

        try (DuplicateStore store = new DuplicateStore(database)) {
            store.quarantine(uuid, "two copies", "test", List.of("slot:1", "slot:2"));
            assertTrue(store.loadRestrictedUuids().contains(uuid));
            assertEquals(DuplicateStatus.QUARANTINED, store.find(uuid).orElseThrow().status());
        }

        try (DuplicateStore reopened = new DuplicateStore(database)) {
            assertTrue(reopened.loadRestrictedUuids().contains(uuid));
            assertEquals("two copies", reopened.find(uuid).orElseThrow().reason());
        }
    }

    @Test
    void revokedUuidCannotBeDowngradedBackToQuarantine() throws Exception {
        UUID compromised = UUID.randomUUID();
        UUID replacement = UUID.randomUUID();

        try (DuplicateStore store = new DuplicateStore(tempDirectory.resolve("states.db"))) {
            store.revoke(compromised, "resolved", "admin", replacement);
            store.quarantine(compromised, "seen again", "scanner", List.of("chest:4"));

            DuplicateRecord record = store.find(compromised).orElseThrow();
            assertEquals(DuplicateStatus.REVOKED, record.status());
            assertEquals("resolved", record.reason());
            assertEquals("admin", record.resolvedBy());
            assertEquals(replacement, record.replacementUuid());
            assertFalse(store.find(replacement).isPresent());
        }
    }
}
