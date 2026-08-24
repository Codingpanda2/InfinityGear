package com.infinitypickaxes.listeners;

import com.infinitypickaxes.InfinityPickaxes;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.scheduler.BukkitTask;

public final class DuplicateDetectionListener implements Listener {
    private final InfinityPickaxes plugin;
    private BukkitTask periodicScan;

    public DuplicateDetectionListener(InfinityPickaxes plugin) {
        this.plugin = plugin;
        start();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        scheduleScan("automatic:join:" + event.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        scheduleScan("automatic:inventory-open:" + event.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Player player) {
            scheduleScan("automatic:pickup:" + player.getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        scheduleScan("automatic:drop:" + event.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreativeInventory(InventoryCreativeEvent event) {
        scheduleScan("automatic:creative:" + event.getWhoClicked().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (com.infinitypickaxes.core.pickaxe.PickaxeData.isInfinityPickaxe(event.getCurrentItem())
                || com.infinitypickaxes.core.pickaxe.PickaxeData.isInfinityPickaxe(event.getCursor())) {
            scheduleScan("automatic:inventory-click:" + event.getWhoClicked().getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (com.infinitypickaxes.core.pickaxe.PickaxeData.isInfinityPickaxe(event.getOldCursor())
                || event.getNewItems().values().stream().anyMatch(
                com.infinitypickaxes.core.pickaxe.PickaxeData::isInfinityPickaxe)) {
            scheduleScan("automatic:inventory-drag:" + event.getWhoClicked().getName());
        }
    }

    private void scheduleScan(String actor) {
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getDuplicateService().scanOnline(actor));
    }

    public void stop() {
        if (periodicScan != null) periodicScan.cancel();
        periodicScan = null;
    }

    public void reload() {
        stop();
        start();
    }

    private void start() {
        long interval = Math.max(100L, plugin.getConfigManager().getConfig()
                .getLong("duplicate-protection.scan-interval-ticks", 200L));
        periodicScan = Bukkit.getScheduler().runTaskTimer(plugin,
                () -> plugin.getDuplicateService().scanOnline("automatic:periodic"), interval, interval);
    }
}
