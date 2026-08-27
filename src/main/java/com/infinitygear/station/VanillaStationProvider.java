package com.infinitygear.station;

import org.bukkit.Material;
import org.bukkit.block.Block;

public final class VanillaStationProvider implements StationProvider {
    public String id() { return "VANILLA"; }
    public boolean available() { return true; }
    public boolean matches(Block block, String configuredId) {
        Material material = configuredId == null ? null : Material.matchMaterial(configuredId);
        return block != null && material != null && block.getType() == material;
    }
}
