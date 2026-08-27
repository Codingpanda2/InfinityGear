package com.infinitygear.data;

import java.util.UUID;

/** Validated legacy fields; parsing never invents an identity. */
public record LegacyGearPayload(UUID uuid, int level, double xp, long blocksMined, boolean quarantined) {
    public static ParseResult parse(String uuid, Integer level, Double xp, Long blocksMined, Byte quarantined) {
        if (uuid == null || uuid.isBlank()) return new ParseResult(null, "missing_uuid");
        final UUID parsed;
        try { parsed = UUID.fromString(uuid); }
        catch (IllegalArgumentException invalid) { return new ParseResult(null, "malformed_uuid"); }
        if (level == null || level < 0) return new ParseResult(null, "invalid_level");
        if (xp == null || !Double.isFinite(xp) || xp < 0) return new ParseResult(null, "invalid_xp");
        if (blocksMined == null || blocksMined < 0) return new ParseResult(null, "invalid_blocks_mined");
        return new ParseResult(new LegacyGearPayload(parsed, level, xp, blocksMined,
                quarantined != null && quarantined != 0), null);
    }

    public record ParseResult(LegacyGearPayload payload, String failure) {
        public boolean valid() { return payload != null; }
    }
}
