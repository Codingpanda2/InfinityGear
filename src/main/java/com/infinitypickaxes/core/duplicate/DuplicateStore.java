package com.infinitypickaxes.core.duplicate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class DuplicateStore implements AutoCloseable {

    private final Connection connection;

    public DuplicateStore(Path databasePath) throws Exception {
        Path parent = databasePath.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
        initialize();
    }

    private void initialize() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA foreign_keys=ON");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS duplicate_pickaxes (
                        uuid TEXT PRIMARY KEY,
                        status TEXT NOT NULL,
                        first_detected INTEGER NOT NULL,
                        last_updated INTEGER NOT NULL,
                        reason TEXT NOT NULL,
                        resolved_by TEXT,
                        replacement_uuid TEXT
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS duplicate_sightings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid TEXT NOT NULL,
                        observed_at INTEGER NOT NULL,
                        location TEXT NOT NULL,
                        actor TEXT,
                        FOREIGN KEY(uuid) REFERENCES duplicate_pickaxes(uuid)
                    )
                    """);
        }
    }

    public synchronized void quarantine(UUID uuid, String reason, String actor, List<String> sightings) throws SQLException {
        long now = Instant.now().toEpochMilli();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO duplicate_pickaxes(uuid, status, first_detected, last_updated, reason, resolved_by, replacement_uuid)
                VALUES (?, 'QUARANTINED', ?, ?, ?, NULL, NULL)
                ON CONFLICT(uuid) DO UPDATE SET
                    status = CASE WHEN status = 'REVOKED' THEN status ELSE 'QUARANTINED' END,
                    last_updated = CASE WHEN status = 'REVOKED' THEN last_updated ELSE excluded.last_updated END,
                    reason = CASE WHEN status = 'REVOKED' THEN reason ELSE excluded.reason END
                """)) {
            statement.setString(1, uuid.toString());
            statement.setLong(2, now);
            statement.setLong(3, now);
            statement.setString(4, reason);
            statement.executeUpdate();
        }
        addSightings(uuid, actor, sightings, now);
    }

    public synchronized void revoke(UUID uuid, String reason, String actor, UUID replacementUuid) throws SQLException {
        long now = Instant.now().toEpochMilli();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO duplicate_pickaxes(uuid, status, first_detected, last_updated, reason, resolved_by, replacement_uuid)
                VALUES (?, 'REVOKED', ?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET status = 'REVOKED', last_updated = excluded.last_updated,
                    reason = excluded.reason, resolved_by = excluded.resolved_by,
                    replacement_uuid = excluded.replacement_uuid
                """)) {
            statement.setString(1, uuid.toString());
            statement.setLong(2, now);
            statement.setLong(3, now);
            statement.setString(4, reason);
            statement.setString(5, actor);
            statement.setString(6, replacementUuid == null ? null : replacementUuid.toString());
            statement.executeUpdate();
        }
    }

    public synchronized Optional<DuplicateRecord> find(UUID uuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM duplicate_pickaxes WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(readRecord(result)) : Optional.empty();
            }
        }
    }

    public synchronized List<DuplicateRecord> listRestricted() throws SQLException {
        List<DuplicateRecord> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM duplicate_pickaxes WHERE status != 'ACTIVE' ORDER BY last_updated DESC");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) result.add(readRecord(rows));
        }
        return result;
    }

    public synchronized Set<UUID> loadRestrictedUuids() throws SQLException {
        Set<UUID> result = new HashSet<>();
        for (DuplicateRecord record : listRestricted()) result.add(record.uuid());
        return result;
    }

    private void addSightings(UUID uuid, String actor, List<String> sightings, long now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO duplicate_sightings(uuid, observed_at, location, actor) VALUES (?, ?, ?, ?)")) {
            for (String sighting : sightings) {
                statement.setString(1, uuid.toString());
                statement.setLong(2, now);
                statement.setString(3, sighting);
                statement.setString(4, actor);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private DuplicateRecord readRecord(ResultSet result) throws SQLException {
        String replacement = result.getString("replacement_uuid");
        return new DuplicateRecord(
                UUID.fromString(result.getString("uuid")),
                DuplicateStatus.valueOf(result.getString("status")),
                Instant.ofEpochMilli(result.getLong("first_detected")),
                Instant.ofEpochMilli(result.getLong("last_updated")),
                result.getString("reason"),
                result.getString("resolved_by"),
                replacement == null ? null : UUID.fromString(replacement)
        );
    }

    @Override
    public synchronized void close() throws SQLException {
        connection.close();
    }
}
