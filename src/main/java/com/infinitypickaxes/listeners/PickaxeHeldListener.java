package com.infinitypickaxes.listeners;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import com.infinitypickaxes.core.pickaxe.PickaxeData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
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

    public void stopTickTask() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }
}
