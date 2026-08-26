package com.infinitypickaxes.listeners;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.core.enchant.EnchantSocket;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import com.infinitypickaxes.core.pickaxe.PickaxeData;
import com.infinitypickaxes.gui.EnchantSocketsGui;
import com.infinitypickaxes.gui.MainPickaxeGui;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class PickaxeInteractListener implements Listener {

    private final InfinityPickaxes plugin;

    public PickaxeInteractListener(InfinityPickaxes plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("infinitypickaxes.use")) return;
        if (!player.isSneaking()) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        InfinityPickaxe pickaxe = plugin.getPickaxeManager().getOrCreatePickaxe(item, player);
        if (pickaxe == null) return;
        if (!plugin.getDuplicateService().isUsable(item)) {
            event.setCancelled(true);
            return;
        }

        FileConfiguration config = plugin.getConfigManager().getConfig();
        String trigger = config.getString("interaction.trigger", "SHIFT_RIGHT_CLICK");
        boolean allowAir = config.getBoolean("interaction.allow-air-click", true);

        if (matchesMenuTrigger(event.getAction(), trigger, allowAir)) {
            event.setCancelled(true);
            playMenuOpenSound(player);

            String providerType = config.getString("menu-provider.type", "NATIVE");
            if (providerType.equalsIgnoreCase("COMMAND") || providerType.equalsIgnoreCase("ZMENU") || providerType.equalsIgnoreCase("DELUXEMENUS")) {
                String cmd = config.getString("menu-provider.command", "zmenu open pickaxes %player%")
                        .replace("%player%", player.getName());
                org.bukkit.Bukkit.dispatchCommand(player, cmd);
            } else {
                new MainPickaxeGui(plugin, player, pickaxe).open();
            }
        }
    }

    void playMenuOpenSound(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
    }

    static boolean matchesMenuTrigger(Action action, String configuredTrigger, boolean allowAir) {
        if (action == null) return false;
        boolean isRightClick = action == Action.RIGHT_CLICK_BLOCK
                || (allowAir && action == Action.RIGHT_CLICK_AIR);
        boolean isLeftClick = action == Action.LEFT_CLICK_BLOCK
                || (allowAir && action == Action.LEFT_CLICK_AIR);
        String trigger = configuredTrigger == null ? "SHIFT_RIGHT_CLICK" : configuredTrigger;
        return switch (trigger.toUpperCase(java.util.Locale.ROOT)) {
            case "SHIFT_LEFT_CLICK" -> isLeftClick;
            case "BOTH" -> isRightClick || isLeftClick;
            default -> isRightClick;
        };
    }

    /**
     * Allows players to drag-and-drop LimitBreak books directly onto an InfinityPickaxe in their inventory.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDragDrop(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != event.getView().getBottomInventory()) return;

        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        if (cursor == null || cursor.getType().isAir() || current == null || current.getType().isAir()) {
            return;
        }

        if (plugin.getLimitBreakManager() == null || !plugin.getLimitBreakManager().isLimitBreakBook(cursor)) {
            return;
        }

        if (!PickaxeData.isInfinityPickaxe(current)) {
            return;
        }
        if (!plugin.getDuplicateService().isUsable(current)) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "messages.pickaxe-quarantined");
            return;
        }

        InfinityPickaxe pickaxe = PickaxeData.fromItemStack(current);
        if (pickaxe == null) return;

        // If it's a Specific Book, apply directly to the target enchant!
        if (!plugin.getLimitBreakManager().isUniversalBook(cursor)) {
            String target = plugin.getLimitBreakManager().getTargetEnchantKey(cursor);
            EnchantSocket socket = plugin.getEnchantManager().getSocketByKey(target);
            if (socket == null && target != null && target.contains(":")) {
                socket = plugin.getEnchantManager().getSocket(target.substring(target.indexOf(":") + 1));
            }

            if (socket != null) {
                event.setCancelled(true);
                boolean success = plugin.getLimitBreakManager().applyLimitBreak(player, pickaxe, socket, cursor);
                if (success) {
                    event.setCurrentItem(pickaxe.getItemStack());
                    event.getView().setCursor(cursor);
                }
            }
        } else {
            // If Universal Super Book, open enchants menu so player can select target socket
            event.setCancelled(true);
            new EnchantSocketsGui(plugin, player, pickaxe).open();
        }
    }

    /**
     * Managed enchantment books must go through the socket UI so capacity,
     * unlocks, configured maxima, and additional conflicts cannot be bypassed.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDirectManagedEnchantDrop(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack target = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        if (!PickaxeData.isInfinityPickaxe(target)
                || !plugin.getEnchantManager().containsManagedEnchantBook(cursor)) {
            return;
        }
        event.setCancelled(true);
        plugin.getMessageManager().sendMessage(player, "messages.enchant-use-socket-menu");
    }

    /** Prevents managed enchantment books from bypassing socket policy through anvils. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack pickaxe = event.getInventory().getFirstItem();
        ItemStack addition = event.getInventory().getSecondItem();
        if (PickaxeData.isInfinityPickaxe(pickaxe)
                && plugin.getEnchantManager().containsManagedEnchantBook(addition)) {
            event.setResult(null);
        }
    }
}
