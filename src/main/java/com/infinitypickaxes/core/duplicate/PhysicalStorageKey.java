package com.infinitypickaxes.core.duplicate;

import io.papermc.paper.block.TileStateInventoryHolder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
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
import java.util.UUID;

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
        if (holder instanceof TileStateInventoryHolder tileState) {
            return blockLocation(tileState).map(location -> new PhysicalStorageKey("block:" + location));
        }
        if (holder instanceof Entity entity && isStorageEntity(holder)) {
            return Optional.of(new PhysicalStorageKey("entity:" + entity.getUniqueId()));
        }
        return Optional.empty();
    }

    private static Optional<String> blockLocation(InventoryHolder holder) {
        if (!(holder instanceof TileStateInventoryHolder tileState)) return Optional.empty();
        Location location = tileState.getLocation();
        World world = tileState.getWorld();
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

    /** Resolves the current live inventory without retaining a Bukkit inventory wrapper. */
    public Optional<Inventory> resolveInventory() {
        try {
            if (value.startsWith("block:")) {
                return resolveBlock(BlockAddress.parse(value.substring("block:".length())));
            }
            if (value.startsWith("double-block:")) {
                String[] parts = value.substring("double-block:".length()).split(":");
                if (parts.length != 8) return Optional.empty();
                BlockAddress first = BlockAddress.parse(parts, 0);
                BlockAddress second = BlockAddress.parse(parts, 4);
                Optional<Inventory> firstInventory = resolveBlock(first);
                if (firstInventory.filter(inventory -> from(inventory).filter(this::equals).isPresent()).isPresent()) {
                    return firstInventory;
                }
                return resolveBlock(second)
                        .filter(inventory -> from(inventory).filter(this::equals).isPresent());
            }
            if (value.startsWith("entity:")) {
                Entity entity = Bukkit.getEntity(UUID.fromString(value.substring("entity:".length())));
                if (!(entity instanceof InventoryHolder holder) || !isStorageEntity(holder)) {
                    return Optional.empty();
                }
                return Optional.of(holder.getInventory());
            }
        } catch (IllegalArgumentException ignored) {
            // Invalid or obsolete keys are treated as storage that no longer exists.
        }
        return Optional.empty();
    }

    private static Optional<Inventory> resolveBlock(BlockAddress address) {
        World world = Bukkit.getWorld(address.worldUuid());
        if (world == null || !world.isChunkLoaded(address.x() >> 4, address.z() >> 4)) {
            return Optional.empty();
        }
        BlockState state = world.getBlockAt(address.x(), address.y(), address.z()).getState(false);
        if (!(state instanceof TileStateInventoryHolder holder)) return Optional.empty();
        return Optional.of(holder.getInventory());
    }

    private record BlockAddress(UUID worldUuid, int x, int y, int z) {
        private static BlockAddress parse(String encoded) {
            String[] parts = encoded.split(":");
            if (parts.length != 4) throw new IllegalArgumentException("Invalid block storage key");
            return parse(parts, 0);
        }

        private static BlockAddress parse(String[] parts, int offset) {
            return new BlockAddress(
                    UUID.fromString(parts[offset]),
                    Integer.parseInt(parts[offset + 1]),
                    Integer.parseInt(parts[offset + 2]),
                    Integer.parseInt(parts[offset + 3])
            );
        }
    }
}
