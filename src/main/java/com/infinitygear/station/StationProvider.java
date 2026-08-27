package com.infinitygear.station;

import org.bukkit.block.Block;

public interface StationProvider {
    String id();
    boolean available();
    boolean matches(Block block, String configuredId);
}
