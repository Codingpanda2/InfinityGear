package com.infinitypickaxes.listeners;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.core.pickaxe.PickaxeData;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.scheduler.BukkitTask;

public final class DuplicateDetectionListener implements Listener {
    private final InfinityPickaxes plugin;
    private final ScanDebouncer debouncer = new ScanDebouncer();
    private BukkitTask periodicScan;
    private BukkitTask pendingScan;

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
        if (plugin.getDuplicateService().isPhysicalStorageInventory(event.getInventory())
                && plugin.getDuplicateService().containsInfinityPickaxe(event.getInventory())) {
            scheduleScan("automatic:storage-open:" + event.getPlayer().getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Player player
                && PickaxeData.isInfinityPickaxe(event.getItem().getItemStack())) {
            scheduleScan("automatic:pickup:" + player.getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (PickaxeData.isInfinityPickaxe(event.getItemDrop().getItemStack())) {
            scheduleScan("automatic:drop:" + event.getPlayer().getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreativeInventory(InventoryCreativeEvent event) {
        if (PickaxeData.isInfinityPickaxe(event.getCurrentItem())
                || PickaxeData.isInfinityPickaxe(event.getCursor())) {
            scheduleScan("automatic:creative:" + event.getWhoClicked().getName());
        }
    }

    private void scheduleScan(String actor) {
        if (!debouncer.request(actor)) return;
        long delay = Math.max(1L, plugin.getConfigManager().getConfig()
                .getLong("duplicate-protection.debounce-ticks", 10L));
        pendingScan = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String scanActor = debouncer.consume();
            pendingScan = null;
            plugin.getDuplicateService().scanOnline(scanActor);
        }, delay);
    }

    public void stop() {
        if (periodicScan != null) periodicScan.cancel();
        if (pendingScan != null) pendingScan.cancel();
        periodicScan = null;
        pendingScan = null;
        debouncer.clear();
    }

    public void reload() {
        stop();
        start();
    }

    private void start() {
        long interval = Math.max(200L, plugin.getConfigManager().getConfig()
                .getLong("duplicate-protection.scan-interval-ticks", 1200L));
        periodicScan = Bukkit.getScheduler().runTaskTimer(plugin,
                () -> plugin.getDuplicateService().scanOnline("automatic:periodic"), interval, interval);
    }
}
