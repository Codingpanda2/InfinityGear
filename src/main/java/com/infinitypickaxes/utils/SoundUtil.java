package com.infinitypickaxes.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;

import java.util.Locale;

public final class SoundUtil {
    private SoundUtil() {}

    public static Sound resolve(String configured, Sound fallback) {
        if (configured == null || configured.isBlank()) return fallback;
        NamespacedKey key = NamespacedKey.fromString(configured.toLowerCase(Locale.ROOT));
        Sound byKey = key == null ? null : Registry.SOUNDS.get(key);
        if (byKey != null) return byKey;

        try {
            Object constant = Sound.class.getField(configured.toUpperCase(Locale.ROOT)).get(null);
            return constant instanceof Sound sound ? sound : fallback;
        } catch (ReflectiveOperationException ignored) {
            return fallback;
        }
    }
}
