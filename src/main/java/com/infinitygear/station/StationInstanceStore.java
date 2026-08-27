package com.infinitygear.station;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Persistent ownership registry for exact InfinityGear station block coordinates. */
public final class StationInstanceStore {
    private final File file;
    private final Logger logger;
    private final Map<Key, StationType> instances = new LinkedHashMap<>();

    public StationInstanceStore(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
        load();
    }

    public Optional<StationType> find(Block block) {
        return block == null ? Optional.empty() : find(block.getLocation());
    }

    public Optional<StationType> find(Location location) {
        return location == null || location.getWorld() == null ? Optional.empty()
                : Optional.ofNullable(instances.get(Key.from(location)));
    }

    public void bind(Block block, StationType type) {
        if (block == null || type == null || block.getWorld() == null) {
            throw new IllegalArgumentException("A loaded block and station type are required.");
        }
        bind(block.getLocation(), type);
    }

    public void bind(Location location, StationType type) {
        if (location == null || location.getWorld() == null || type == null) {
            throw new IllegalArgumentException("A loaded location and station type are required.");
        }
        Key key = Key.from(location);
        StationType previous = instances.put(key, type);
        try { save(); }
        catch (RuntimeException failure) {
            if (previous == null) instances.remove(key); else instances.put(key, previous);
            throw failure;
        }
    }

    public Optional<StationType> unbind(Block block) {
        return block == null ? Optional.empty() : unbind(block.getLocation());
    }

    public Optional<StationType> unbind(Location location) {
        if (location == null || location.getWorld() == null) return Optional.empty();
        Key key = Key.from(location);
        StationType removed = instances.remove(key);
        if (removed != null) try { save(); }
        catch (RuntimeException failure) { instances.put(key, removed); throw failure; }
        return Optional.ofNullable(removed);
    }

    private void load() {
        instances.clear();
        if (file == null || !file.isFile()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        var root = yaml.getConfigurationSection("instances");
        if (root == null) return;
        for (String encoded : root.getKeys(false)) {
            try {
                Key key = Key.decode(encoded);
                StationType type = StationType.valueOf(root.getString(encoded, "").toUpperCase(Locale.ROOT));
                instances.put(key, type);
            } catch (RuntimeException invalid) {
                logger.warning("Ignored malformed station instance '" + encoded + "' in " + file.getName());
            }
        }
    }

    private void save() {
        if (file == null) return;
        YamlConfiguration yaml = new YamlConfiguration();
        instances.forEach((key, type) -> yaml.set("instances." + key.encode(), type.name()));
        try {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            yaml.save(file);
        } catch (IOException failure) {
            logger.log(Level.SEVERE, "Could not persist InfinityGear station instances", failure);
            throw new IllegalStateException("Could not persist station binding.", failure);
        }
    }

    record Key(UUID world, int x, int y, int z) {
        static Key from(Location location) {
            if (location == null || location.getWorld() == null) throw new IllegalArgumentException("Loaded world required.");
            return new Key(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }

        String encode() { return world + ";" + x + ";" + y + ";" + z; }

        static Key decode(String encoded) {
            String[] parts = encoded.split(";", -1);
            if (parts.length != 4) throw new IllegalArgumentException("Invalid station key");
            return new Key(UUID.fromString(parts[0]), Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        }
    }
}
