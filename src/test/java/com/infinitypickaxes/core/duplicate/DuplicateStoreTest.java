package com.infinitypickaxes.core.duplicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuplicateStoreTest {

    @Test
    void legacyDatabaseMigratesInPlaceAndPreservesRecord(@TempDir Path temp) throws Exception {
        Path database = temp.resolve("legacy.db");
        UUID uuid = UUID.randomUUID();
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE duplicate_pickaxes (uuid TEXT PRIMARY KEY, status TEXT NOT NULL, "
                    + "first_detected INTEGER NOT NULL, last_updated INTEGER NOT NULL, reason TEXT NOT NULL, "
                    + "resolved_by TEXT, replacement_uuid TEXT)");
            statement.execute("CREATE TABLE duplicate_sightings (id INTEGER PRIMARY KEY AUTOINCREMENT, uuid TEXT NOT NULL, "
                    + "observed_at INTEGER NOT NULL, location TEXT NOT NULL, actor TEXT)");
            statement.execute("INSERT INTO duplicate_pickaxes VALUES ('" + uuid
                    + "','QUARANTINED',10,20,'legacy reason','admin',NULL)");
        }
        try (DuplicateStore store = new DuplicateStore(database)) {
            DuplicateRecord record = store.find(uuid).orElseThrow();
            assertEquals("legacy reason", record.reason());
            assertEquals("admin", record.resolvedBy());
            assertEquals("GEAR", record.trackedKind());
            assertEquals("infinitygear:pickaxe", record.trackedType());
        }
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             var result = connection.createStatement().executeQuery("SELECT COUNT(*) FROM schema_migrations WHERE version=1")) {
            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
        }
    }

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
