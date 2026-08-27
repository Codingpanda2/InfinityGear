package com.infinitygear.station;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StationInstanceStoreTest {
    @TempDir Path temporary;

    @Test void exactBindingPersistsAndCanBeRemoved() {
        UUID worldId = UUID.randomUUID();
        World world = mock(World.class);
        when(world.getUID()).thenReturn(worldId);
        Block station = block(world, 12, 64, -8);
        Block adjacent = block(world, 13, 64, -8);
        var file = temporary.resolve("station-instances.yml").toFile();

        StationInstanceStore first = new StationInstanceStore(file, Logger.getLogger("stations-test"));
        first.bind(station, StationType.RUNIC_TABLE);
        assertEquals(StationType.RUNIC_TABLE, first.find(station).orElseThrow());
        assertTrue(first.find(adjacent).isEmpty());

        StationInstanceStore reloaded = new StationInstanceStore(file, Logger.getLogger("stations-test"));
        assertEquals(StationType.RUNIC_TABLE, reloaded.find(station).orElseThrow());
        assertEquals(StationType.RUNIC_TABLE, reloaded.unbind(station).orElseThrow());
        assertTrue(new StationInstanceStore(file, Logger.getLogger("stations-test")).find(station).isEmpty());
    }

    private Block block(World world, int x, int y, int z) {
        Block block = mock(Block.class);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(new Location(world, x, y, z));
        return block;
    }
}
