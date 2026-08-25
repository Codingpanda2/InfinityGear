package com.infinitypickaxes.listeners;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.core.pickaxe.PickaxeData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** Blocks compromised pickaxes before progression or enchantment listeners run. */
public final class QuarantineListener implements Listener {
    private final InfinityPickaxes plugin;

    public QuarantineListener(InfinityPickaxes plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        blockIfRestricted(event.getPlayer(), event.getPlayer().getInventory().getItemInMainHand(),
                () -> event.setCancelled(true));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        blockIfRestricted(event.getPlayer(), event.getItem(), () -> event.setCancelled(true));
    }

    private void blockIfRestricted(Player player, ItemStack item, Runnable cancel) {
        if (!PickaxeData.isInfinityPickaxe(item) || plugin.getDuplicateService().isUsable(item)) return;
        cancel.run();
        plugin.getMessageManager().sendMessage(player, "messages.pickaxe-quarantined");
    }
}
