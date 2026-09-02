package com.infinitypickaxes.listeners;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitygear.data.GearData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import io.papermc.paper.event.player.PlayerSwapWithEquipmentSlotEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** Blocks compromised gear before progression, combat, or enchantment listeners run. */
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
        blockIfRestricted(event.getPlayer(), event.getItem(), () -> event.setCancelled(true));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player) {
            blockIfRestricted(player, event.getBow(), () -> event.setCancelled(true));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMelee(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            blockIfRestricted(player, player.getInventory().getItemInMainHand(),
                    () -> event.setCancelled(true));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onArmorInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack candidate = null;
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            candidate = switch (event.getClick()) {
                case NUMBER_KEY -> event.getHotbarButton() < 0 ? null
                        : player.getInventory().getItem(event.getHotbarButton());
                case SWAP_OFFHAND -> player.getInventory().getItemInOffHand();
                default -> event.getCursor();
            };
        } else if (event.isShiftClick()
                && (event.getView().getType() == InventoryType.CRAFTING
                || event.getView().getType() == InventoryType.CREATIVE)
                && isArmorGear(event.getCurrentItem())) {
            // In the player inventory screen, shift-clicking wearable gear equips it.
            candidate = event.getCurrentItem();
        }
        ItemStack item = candidate;
        blockIfRestricted(player, item, () -> event.setCancelled(true));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onArmorInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        boolean touchesArmor = event.getRawSlots().stream()
                .anyMatch(slot -> event.getView().getSlotType(slot) == InventoryType.SlotType.ARMOR);
        if (touchesArmor) blockIfRestricted(player, event.getOldCursor(), () -> event.setCancelled(true));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEquipmentSwap(PlayerSwapWithEquipmentSlotEvent event) {
        if (event.getSlot() == EquipmentSlot.HEAD || event.getSlot() == EquipmentSlot.CHEST
                || event.getSlot() == EquipmentSlot.LEGS || event.getSlot() == EquipmentSlot.FEET) {
            blockIfRestricted(event.getPlayer(), event.getItemInHand(), () -> event.setCancelled(true));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDispenseArmor(BlockDispenseArmorEvent event) {
        if (event.getTargetEntity() instanceof Player player) {
            blockIfRestricted(player, event.getItem(), () -> event.setCancelled(true));
        }
    }

    private void blockIfRestricted(Player player, ItemStack item, Runnable cancel) {
        if (!GearData.isGear(item) || plugin.getDuplicateService().isUsable(item)) return;
        cancel.run();
        plugin.getMessageManager().sendMessage(player, "messages.gear-quarantined");
    }

    private boolean isArmorGear(ItemStack item) {
        if (!GearData.isGear(item)) return false;
        var read = GearData.read(item, 0, false);
        return read.valid() && plugin.getGearProfiles().find(read.gear().profileId())
                .map(profile -> profile.compatibleTargets().contains("ARMOR")).orElse(false);
    }
}
