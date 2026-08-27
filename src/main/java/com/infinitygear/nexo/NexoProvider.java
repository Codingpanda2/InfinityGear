package com.infinitygear.nexo;

import com.infinitygear.station.StationProvider;
import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.api.NexoFurniture;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Typed Nexo 1.27 adapter. InfinityGear never dispatches commands or reflects into Nexo. */
public final class NexoProvider implements StationProvider {
    public record FurnitureTarget(String itemId, Location origin) {}
    public String id() { return "NEXO"; }
    public boolean available() { return true; }

    public boolean matches(Block block, String configuredId) {
        if (block == null || configuredId == null) return false;
        var mechanic = NexoBlocks.customBlockMechanic(block);
        return mechanic != null && configuredId.equalsIgnoreCase(mechanic.getItemID());
    }

    public boolean itemExists(String id) { return id != null && NexoItems.exists(id); }
    public String itemId(ItemStack item) { return item == null ? null : NexoItems.idFromItem(item); }
    public ItemStack createItem(String id) {
        var builder = NexoItems.itemFromId(id);
        return builder == null ? null : builder.build();
    }

    /** Resolves the looked-at furniture through Nexo and canonicalizes it to its stable base entity. */
    public FurnitureTarget findTargetFurniture(Player player) {
        if (player == null) return null;
        var base = NexoFurniture.findTargetFurniture(player);
        if (base == null) return null;
        var mechanic = NexoFurniture.furnitureMechanic(base);
        return mechanic == null ? null : new FurnitureTarget(mechanic.getItemID(), base.getLocation());
    }
}
