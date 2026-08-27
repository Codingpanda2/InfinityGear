package com.infinitygear.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class LegacyDataFolderMigratorTest {
    @TempDir Path temp;

    @Test void backsUpCopiesMissingAndIsIdempotentWithoutOverwritingNewConfig() throws Exception {
        Path legacy = temp.resolve("InfinityPickaxes");
        Path current = temp.resolve("InfinityGear");
        Files.createDirectories(legacy);
        Files.createDirectories(current);
        Files.writeString(legacy.resolve("config.yml"), "legacy: true");
        Files.writeString(legacy.resolve("duplicates.db"), "database");
        Files.writeString(current.resolve("config.yml"), "new: true");
        Clock clock = Clock.fixed(Instant.parse("2026-08-26T12:00:00Z"), ZoneOffset.UTC);

        var first = LegacyDataFolderMigrator.migrate(legacy, current, clock);
        assertTrue(first.migrated());
        assertEquals("new: true", Files.readString(current.resolve("config.yml")));
        assertEquals("database", Files.readString(current.resolve("duplicates.db")));
        assertEquals("legacy: true", Files.readString(first.backup().resolve("config.yml")));
        assertTrue(Files.exists(current.resolve(LegacyDataFolderMigrator.MARKER)));

        var second = LegacyDataFolderMigrator.migrate(legacy, current, clock);
        assertFalse(second.migrated());
    }
}
