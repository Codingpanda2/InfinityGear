package com.infinitypickaxes.listeners;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import com.infinitypickaxes.core.pickaxe.PickaxeData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

public class PickaxeHeldListener implements Listener {

    private final InfinityPickaxes plugin;
    private BukkitTask tickTask;

    public PickaxeHeldListener(InfinityPickaxes plugin) {
        this.plugin = plugin;
        startTickTask();
    }

    public void startTickTask() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        // Run every 20 ticks (1 second)
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                ItemStack mainHand = player.getInventory().getItemInMainHand();
                if (PickaxeData.isInfinityPickaxe(mainHand)) {
                    InfinityPickaxe pickaxe = PickaxeData.fromItemStack(mainHand);
                    if (pickaxe != null) {
                        plugin.getPerkManager().dispatchTick(player, pickaxe);
                    }
                }
            }
        }, 20L, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        if (PickaxeData.isInfinityPickaxe(newItem)) {
            InfinityPickaxe pickaxe = plugin.getPickaxeManager().getOrCreatePickaxe(newItem, player);
            if (pickaxe != null) {
                plugin.getPickaxeManager().syncPickaxe(pickaxe);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> refreshHeldPickaxe(event.getPlayer()));
    }

    public void refreshAllHeldPickaxes() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshHeldPickaxe(player);
        }
    }

    private void refreshHeldPickaxe(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (PickaxeData.isInfinityPickaxe(mainHand)) {
            InfinityPickaxe pickaxe = plugin.getPickaxeManager().getOrCreatePickaxe(mainHand, player);
            if (pickaxe != null) {
                plugin.getPickaxeManager().syncPickaxe(pickaxe);
            }
        }
    }

    public void stopTickTask() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }
}
