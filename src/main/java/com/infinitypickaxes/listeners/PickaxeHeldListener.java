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

public class PickaxeHeldListener implements Listener {

    private final InfinityPickaxes plugin;

    public PickaxeHeldListener(InfinityPickaxes plugin) {
        this.plugin = plugin;
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
}
