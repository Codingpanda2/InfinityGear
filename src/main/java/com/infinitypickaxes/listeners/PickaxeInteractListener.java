package com.infinitypickaxes.listeners;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import com.infinitypickaxes.gui.MainPickaxeGui;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class PickaxeInteractListener implements Listener {

    private final InfinityPickaxes plugin;

    public PickaxeInteractListener(InfinityPickaxes plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        InfinityPickaxe pickaxe = plugin.getPickaxeManager().getOrCreatePickaxe(item, player);
        if (pickaxe == null) return;

        FileConfiguration config = plugin.getConfigManager().getConfig();
        String trigger = config.getString("interaction.trigger", "SHIFT_RIGHT_CLICK");
        boolean allowAir = config.getBoolean("interaction.allow-air-click", true);

        Action action = event.getAction();
        boolean isRightClick = (action == Action.RIGHT_CLICK_BLOCK || (allowAir && action == Action.RIGHT_CLICK_AIR));
        boolean isLeftClick = (action == Action.LEFT_CLICK_BLOCK || (allowAir && action == Action.LEFT_CLICK_AIR));

        boolean matched = switch (trigger.toUpperCase()) {
            case "SHIFT_LEFT_CLICK" -> isLeftClick;
            case "BOTH" -> isRightClick || isLeftClick;
            default -> isRightClick; // SHIFT_RIGHT_CLICK
        };

        if (matched) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);

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
}
