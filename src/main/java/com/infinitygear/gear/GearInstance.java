package com.infinitygear.gear;

import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.UUID;

/** Mutable server-thread domain instance backed by one live ItemStack. */
public final class GearInstance {
    private final ItemStack item;
    private final UUID uuid;
    private final String profileId;
    private int level;
    private double xp;
    private long blocksMined;
    private int socketCapacity;

    public GearInstance(ItemStack item, UUID uuid, String profileId, int level, double xp,
                        long blocksMined, int socketCapacity) {
        this.item = Objects.requireNonNull(item, "item");
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.profileId = Objects.requireNonNull(profileId, "profileId");
        this.level = Math.max(0, level);
        this.xp = Math.max(0, xp);
        this.blocksMined = Math.max(0, blocksMined);
        this.socketCapacity = Math.max(0, socketCapacity);
    }

    public ItemStack item() { return item; }
    public UUID uuid() { return uuid; }
    public String profileId() { return profileId; }
    public int level() { return level; }
    public double xp() { return xp; }
    public long blocksMined() { return blocksMined; }
    public int socketCapacity() { return socketCapacity; }
    public void level(int value) { level = Math.max(0, value); }
    public void xp(double value) { xp = Math.max(0, value); }
    public void blocksMined(long value) { blocksMined = Math.max(0, value); }
    public void socketCapacity(int value) { socketCapacity = Math.max(0, value); }
}
