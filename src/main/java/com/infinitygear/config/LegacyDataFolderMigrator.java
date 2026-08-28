package com.infinitygear.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

/** First-start, additive, idempotent migration. The legacy folder is never deleted. */
public final class LegacyDataFolderMigrator {
    public static final String MARKER = ".infinitypickaxes-migration-v1";
    private static final DateTimeFormatter STAMP = DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private LegacyDataFolderMigrator() {}

    public record Result(boolean migrated, Path backup, int copiedFiles) {}

    public static Result migrate(Path legacy, Path destination, Clock clock) throws IOException {
        Path marker = destination.resolve(MARKER);
        if (!Files.isDirectory(legacy) || Files.exists(marker)) return new Result(false, null, 0);
        Files.createDirectories(destination);

        String stamp = STAMP.format(Instant.now(clock));
        Path backup = uniqueSibling(legacy.resolveSibling(legacy.getFileName() + "-backup-" + stamp));
        copyTree(legacy, backup, false);
        int copied = copyTree(legacy, destination, true);

        Path temporary = destination.resolve(MARKER + ".tmp");
        Files.writeString(temporary, "source=" + legacy.toAbsolutePath() + "\nbackup="
                + backup.toAbsolutePath() + "\ncompleted=" + Instant.now(clock) + "\n",
                StandardCharsets.UTF_8);
        try {
            Files.move(temporary, marker, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException unsupported) {
            Files.move(temporary, marker, StandardCopyOption.REPLACE_EXISTING);
        }
        return new Result(true, backup, copied);
    }

    private static Path uniqueSibling(Path desired) {
        if (!Files.exists(desired)) return desired;
        int suffix = 2;
        while (Files.exists(desired.resolveSibling(desired.getFileName() + "-" + suffix))) suffix++;
        return desired.resolveSibling(desired.getFileName() + "-" + suffix);
    }

    private static int copyTree(Path source, Path destination, boolean missingOnly) throws IOException {
        int[] copied = {0};
        try (var paths = Files.walk(source)) {
            for (Path path : paths.sorted(Comparator.naturalOrder()).toList()) {
                Path target = destination.resolve(source.relativize(path));
                if (Files.isDirectory(path)) Files.createDirectories(target);
                else if (!missingOnly || !Files.exists(target)) {
                    Files.copy(path, target, StandardCopyOption.COPY_ATTRIBUTES);
                    copied[0]++;
                }
            }
        }
        return copied[0];
    }
}
