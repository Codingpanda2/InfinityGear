package com.infinitygear.inventory;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Revalidates exact live slots and applies replacements/outputs as one rollback-capable contents update. */
public final class InventoryTransaction {
    public enum Failure { NONE, STALE_INPUT, OUTPUTS_DO_NOT_FIT, MUTATION_FAILED }
    public record Result(Failure failure) { public boolean success() { return failure == Failure.NONE; } }

    public Result execute(Inventory inventory, Map<Integer, ItemStack> expected,
                          Map<Integer, ItemStack> replacements, List<ItemStack> outputs) {
        if (inventory == null) return new Result(Failure.MUTATION_FAILED);
        for (var entry : expected.entrySet()) {
            ItemStack live = inventory.getItem(entry.getKey());
            if (!exact(live, entry.getValue())) return new Result(Failure.STALE_INPUT);
        }
        boolean playerStorage = inventory instanceof org.bukkit.inventory.PlayerInventory;
        ItemStack[] before = cloneContents(playerStorage
                ? ((org.bukkit.inventory.PlayerInventory) inventory).getStorageContents()
                : inventory.getContents());
        ItemStack[] planned = cloneContents(before);
        try {
            for (var entry : replacements.entrySet()) planned[entry.getKey()] = cloneOrNull(entry.getValue());
            if (!placeAll(planned, outputs)) return new Result(Failure.OUTPUTS_DO_NOT_FIT);
            setContents(inventory, planned, playerStorage);
            return new Result(Failure.NONE);
        } catch (RuntimeException failure) {
            try { setContents(inventory, before, playerStorage); }
            catch (RuntimeException rollbackFailure) {
                if (rollbackFailure != failure) failure.addSuppressed(rollbackFailure);
            }
            return new Result(Failure.MUTATION_FAILED);
        }
    }

    static boolean placeAll(ItemStack[] contents, List<ItemStack> outputs) {
        for (ItemStack original : outputs == null ? List.<ItemStack>of() : outputs) {
            if (isAir(original) || original.getAmount() <= 0) continue;
            ItemStack remaining = original.clone();
            for (int slot = 0; slot < contents.length && remaining.getAmount() > 0; slot++) {
                ItemStack current = contents[slot];
                if (isAir(current)) continue;
                if (!current.isSimilar(remaining)) continue;
                int room = current.getMaxStackSize() - current.getAmount();
                int moved = Math.min(room, remaining.getAmount());
                current.setAmount(current.getAmount() + moved);
                remaining.setAmount(remaining.getAmount() - moved);
            }
            for (int slot = 0; slot < contents.length && remaining.getAmount() > 0; slot++) {
                ItemStack current = contents[slot];
                if (!isAir(current)) continue;
                int moved = Math.min(remaining.getMaxStackSize(), remaining.getAmount());
                ItemStack placed = remaining.clone(); placed.setAmount(moved); contents[slot] = placed;
                remaining.setAmount(remaining.getAmount() - moved);
            }
            if (remaining.getAmount() > 0) return false;
        }
        return true;
    }

    private static boolean exact(ItemStack live, ItemStack expected) {
        if (live == null || expected == null) return live == expected;
        return live.getAmount() == expected.getAmount() && live.isSimilar(expected);
    }
    private static ItemStack cloneOrNull(ItemStack item) { return item == null ? null : item.clone(); }
    private static boolean isAir(ItemStack item) {
        if (item == null) return true;
        org.bukkit.Material type = item.getType();
        return type == org.bukkit.Material.AIR || type == org.bukkit.Material.CAVE_AIR
                || type == org.bukkit.Material.VOID_AIR;
    }
    private static ItemStack[] cloneContents(ItemStack[] input) {
        ItemStack[] result = Arrays.copyOf(input, input.length);
        for (int i = 0; i < result.length; i++) result[i] = cloneOrNull(result[i]);
        return result;
    }
    private static void setContents(Inventory inventory, ItemStack[] contents, boolean playerStorage) {
        if (playerStorage) ((org.bukkit.inventory.PlayerInventory) inventory).setStorageContents(contents);
        else inventory.setContents(contents);
    }
}
