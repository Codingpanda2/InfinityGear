package com.infinitygear.api;

import com.infinitygear.data.TrackedKind;
import com.infinitygear.gear.GearProfile;
import com.infinitypickaxes.core.enchant.EnchantSocket;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Optional;

/** Stable capability API. Every mutation method requires Bukkit's primary thread. */
public interface InfinityGearService {
    boolean isGear(ItemStack item);
    Optional<GearSnapshot> inspect(ItemStack item);
    Optional<GearProfile> resolveProfile(ItemStack item);
    OperationResult<ItemStack> createGear(String profileId, int startingLevel);
    OperationResult<Integer> validateEnchantmentApplication(ItemStack gear, ItemStack book, String enchantmentKey);
    OperationResult<Integer> applyEnchantment(ItemStack gear, ItemStack book, String enchantmentKey);
    int usedSockets(ItemStack gear);
    int socketLimit(ItemStack gear);
    boolean quarantined(ItemStack item);
    OperationResult<ItemStack> createTrackedArtifact(TrackedKind kind, String type);
    Collection<EnchantSocket> eligibleEnchantments(String profileId);
}
