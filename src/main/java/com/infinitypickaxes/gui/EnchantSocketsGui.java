package com.infinitypickaxes.gui;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.core.enchant.EnchantSocket;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import com.infinitypickaxes.utils.ItemBuilder;
import com.infinitypickaxes.utils.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class EnchantSocketsGui extends CustomGui {

    private final FileConfiguration menuConfig;
    private final Map<Integer, EnchantSocket> slotToSocket = new HashMap<>();

    public EnchantSocketsGui(InfinityPickaxes plugin, Player player, InfinityPickaxe pickaxe) {
        super(
                plugin,
                player,
                pickaxe,
                TextUtil.parse(plugin.getConfigManager().getEnchantsMenuConfig().getString("title", "<gradient:#00E5FF:#0077FE><b>Infinity Pickaxe</b></gradient> <dark_gray>»</dark_gray> <gray>Encantamientos")),
                plugin.getConfigManager().getEnchantsMenuConfig().getInt("size", 45)
        );
        this.menuConfig = plugin.getConfigManager().getEnchantsMenuConfig();
    }

    @Override
    public void setupItems() {
        inventory.clear();
        slotToSocket.clear();

        // 1. Background filler
        Material fillMat = Material.matchMaterial(menuConfig.getString("fill-item.material", "BLACK_STAINED_GLASS_PANE"));
        if (fillMat == null) fillMat = Material.BLACK_STAINED_GLASS_PANE;
        ItemStack filler = new ItemBuilder(fillMat).name(" ").build();
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        // 2. Info guide book
        int infoSlot = menuConfig.getInt("items.info-book.slot", 4);
        Material infoMat = Material.matchMaterial(menuConfig.getString("items.info-book.material", "KNOWLEDGE_BOOK"));
        String infoName = menuConfig.getString("items.info-book.name", "<#00FF88><b>¿Cómo Mejorar Sockets?</b></#00FF88>");
        List<String> infoLore = menuConfig.getStringList("items.info-book.lore");
        inventory.setItem(infoSlot, new ItemBuilder(infoMat != null ? infoMat : Material.KNOWLEDGE_BOOK)
                .name(infoName)
                .lore(infoLore)
                .build());

        // 3. Back Button
        int backSlot = menuConfig.getInt("items.back-button.slot", 40);
        Material backMat = Material.matchMaterial(menuConfig.getString("items.back-button.material", "ARROW"));
        String backName = menuConfig.getString("items.back-button.name", "<yellow><b>Volver al Menú Principal</b></yellow>");
        List<String> backLore = menuConfig.getStringList("items.back-button.lore");
        inventory.setItem(backSlot, new ItemBuilder(backMat != null ? backMat : Material.ARROW)
                .name(backName)
                .lore(backLore)
                .build());

        // 4. Render Enchantment Sockets
        int fallbackSlot = 10;
        for (EnchantSocket socket : plugin.getEnchantManager().getAllSockets()) {
            if (!socket.isEnabled()) continue;

            int targetSlot = socket.getSlot() >= 0 ? socket.getSlot() : fallbackSlot++;
            if (targetSlot >= inventory.getSize()) continue;

            slotToSocket.put(targetSlot, socket);
            inventory.setItem(targetSlot, buildSocketItem(socket));
        }
    }

    private ItemStack buildSocketItem(EnchantSocket socket) {
        int pickaxeLvl = pickaxe.getLevel();
        boolean unlocked = socket.isUnlocked(pickaxeLvl);
        int currentLvl = pickaxe.getEnchantmentLevel(socket.getKeyString());
        int maxForPickaxe = socket.getMaxAllowedLevel(pickaxeLvl);
        int globalMax = socket.getMaxLevel();

        ItemBuilder builder = new ItemBuilder(socket.getIcon());
        if (socket.getCustomModelData() != null) {
            builder.customModelData(socket.getCustomModelData());
        }

        if (!unlocked) {
            String name = menuConfig.getString("enchant-format.locked-name", "<dark_gray>🔒</dark_gray> %enchant_display_name% <red>(Bloqueado)</red>")
                    .replace("%enchant_display_name%", socket.getDisplayName());
            List<String> rawLore = menuConfig.getStringList("enchant-format.lore-locked");
            builder.name(name).lore(formatLoreList(rawLore, socket, currentLvl, maxForPickaxe, globalMax));
        } else if (currentLvl >= maxForPickaxe && maxForPickaxe > 0) {
            String name = menuConfig.getString("enchant-format.unlocked-name", "%enchant_display_name% <gray>[<yellow>Nv. %current_level%<dark_gray>/<gold>%max_level%<gray>]")
                    .replace("%enchant_display_name%", socket.getDisplayName())
                    .replace("%current_level%", String.valueOf(currentLvl))
                    .replace("%max_level%", String.valueOf(globalMax));
            List<String> rawLore = menuConfig.getStringList("enchant-format.lore-maxed");
            builder.name(name).lore(formatLoreList(rawLore, socket, currentLvl, maxForPickaxe, globalMax));
        } else {
            String name = menuConfig.getString("enchant-format.unlocked-name", "%enchant_display_name% <gray>[<yellow>Nv. %current_level%<dark_gray>/<gold>%max_level%<gray>]")
                    .replace("%enchant_display_name%", socket.getDisplayName())
                    .replace("%current_level%", String.valueOf(currentLvl))
                    .replace("%max_level%", String.valueOf(globalMax));
            List<String> rawLore = menuConfig.getStringList("enchant-format.lore-unlocked");
            builder.name(name).lore(formatLoreList(rawLore, socket, currentLvl, maxForPickaxe, globalMax));
        }

        return builder.build();
    }

    private List<String> formatLoreList(List<String> rawLore, EnchantSocket socket, int currentLvl, int maxForPickaxe, int globalMax) {
        List<String> formatted = new ArrayList<>();
        int requiredBookLevel = (currentLvl == 0) ? 1 : currentLvl;

        for (String line : rawLore) {
            if (line.contains("%enchant_description%")) {
                formatted.addAll(socket.getDescription());
            } else {
                formatted.add(line
                        .replace("%enchant_display_name%", socket.getDisplayName())
                        .replace("%enchant_raw_name%", socket.getId())
                        .replace("%current_level%", String.valueOf(currentLvl))
                        .replace("%max_level%", String.valueOf(globalMax))
                        .replace("%current_max_for_pickaxe%", String.valueOf(maxForPickaxe))
                        .replace("%required_book_level%", String.valueOf(requiredBookLevel))
                        .replace("%unlock_level%", String.valueOf(socket.getUnlockPickaxeLevel()))
                        .replace("%pickaxe_level%", String.valueOf(pickaxe.getLevel()))
                );
            }
        }
        return formatted;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        int backSlot = menuConfig.getInt("items.back-button.slot", 40);
        if (slot == backSlot) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            new MainPickaxeGui(plugin, player, pickaxe).open();
            return;
        }

        EnchantSocket socket = slotToSocket.get(slot);
        if (socket != null) {
            ItemStack cursorItem = event.getCursor();
            if (cursorItem != null && !cursorItem.getType().isAir()) {
                boolean success = plugin.getEnchantManager().handleSocketUpgrade(player, pickaxe, socket, cursorItem);
                if (success) {
                    // Update cursor item reference in event
                    event.getView().setCursor(cursorItem);
                    // Refresh GUI
                    setupItems();
                }
            } else {
                // If cursor is empty, inform the player
                int currentLvl = pickaxe.getEnchantmentLevel(socket.getKeyString());
                int reqBookLvl = (currentLvl == 0) ? 1 : currentLvl;
                plugin.getMessageManager().sendMessage(player, "messages.enchant-no-book-in-hand");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            }
        }
    }
}
