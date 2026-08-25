package com.infinitypickaxes.core.duplicate;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.ChestBoat;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;
import java.util.Optional;

/** Stable identity for a physical storage inventory across Bukkit wrapper instances. */
public record PhysicalStorageKey(String value) {

    public static Optional<PhysicalStorageKey> from(Inventory inventory) {
        return inventory == null ? Optional.empty() : from(inventory.getHolder());
    }

    static Optional<PhysicalStorageKey> from(InventoryHolder holder) {
        if (holder instanceof DoubleChest doubleChest) {
            Optional<String> left = blockLocation(doubleChest.getLeftSide());
            Optional<String> right = blockLocation(doubleChest.getRightSide());
            if (left.isEmpty() || right.isEmpty()) return Optional.empty();
            List<String> sides = List.of(left.get(), right.get()).stream().sorted().toList();
            return Optional.of(new PhysicalStorageKey("double-block:" + sides.get(0) + ":" + sides.get(1)));
        }
        if (holder instanceof Container container) {
            return blockLocation(container).map(location -> new PhysicalStorageKey("block:" + location));
        }
        if (holder instanceof Entity entity && isStorageEntity(holder)) {
            return Optional.of(new PhysicalStorageKey("entity:" + entity.getUniqueId()));
        }
        return Optional.empty();
    }

    private static Optional<String> blockLocation(InventoryHolder holder) {
        if (!(holder instanceof Container container)) return Optional.empty();
        Location location = container.getLocation();
        World world = container.getWorld();
        if (world == null) return Optional.empty();
        return Optional.of(world.getUID() + ":"
                + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ());
    }

    private static boolean isStorageEntity(InventoryHolder holder) {
        return holder instanceof StorageMinecart
                || holder instanceof HopperMinecart
                || holder instanceof ChestedHorse
                || holder instanceof ChestBoat;
    }
}
