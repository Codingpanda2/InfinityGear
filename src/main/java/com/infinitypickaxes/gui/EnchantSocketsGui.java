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
    private int currentPage = 0;
    private static final int[] INNER_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 41, 42, 43
    };

    public EnchantSocketsGui(InfinityPickaxes plugin, Player player, InfinityPickaxe pickaxe) {
        super(
                plugin,
                player,
                pickaxe,
                TextUtil.parse(plugin.getConfigManager().getEnchantsMenuConfig().getString("title", "<gradient:#00E5FF:#0077FE><b>Infinity Pickaxe</b></gradient> <dark_gray>»</dark_gray> <gray>Encantamientos")),
                plugin.getConfigManager().getEnchantsMenuConfig().getInt("size", 54)
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
        int backSlot = menuConfig.getInt("items.back-button.slot", 49);
        if (backSlot >= inventory.getSize()) backSlot = 40;
        Material backMat = Material.matchMaterial(menuConfig.getString("items.back-button.material", "ARROW"));
        String backName = menuConfig.getString("items.back-button.name", "<yellow><b>Volver al Menú Principal</b></yellow>");
        List<String> backLore = menuConfig.getStringList("items.back-button.lore");
        inventory.setItem(backSlot, new ItemBuilder(backMat != null ? backMat : Material.ARROW)
                .name(backName)
                .lore(backLore)
                .build());

        // 4. Collect all enabled sockets
        List<EnchantSocket> allSockets = new ArrayList<>();
        for (EnchantSocket s : plugin.getEnchantManager().getAllSockets()) {
            if (s.isEnabled()) {
                allSockets.add(s);
            }
        }

        int pageSize = INNER_SLOTS.length;
        int totalPages = Math.max(1, (int) Math.ceil((double) allSockets.size() / pageSize));
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;

        // 5. Pagination Buttons
        int prevSlot = 45;
        int nextSlot = 53;
        if (currentPage > 0) {
            inventory.setItem(prevSlot, new ItemBuilder(Material.SPECTRAL_ARROW)
                    .name("<yellow><b>« Página Anterior (" + currentPage + "/" + totalPages + ")</b></yellow>")
                    .build());
        }
        if (currentPage < totalPages - 1) {
            inventory.setItem(nextSlot, new ItemBuilder(Material.SPECTRAL_ARROW)
                    .name("<yellow><b>Página Siguiente (" + (currentPage + 2) + "/" + totalPages + ") »</b></yellow>")
                    .build());
        }

        // 6. Populate Sockets on Current Page
        int startIndex = currentPage * pageSize;
        int endIndex = Math.min(startIndex + pageSize, allSockets.size());

        for (int i = startIndex; i < endIndex; i++) {
            EnchantSocket socket = allSockets.get(i);
            int targetSlot;

            // If on first page and socket has a custom valid slot configured, try to use it
            if (currentPage == 0 && socket.getSlot() >= 0 && socket.getSlot() < inventory.getSize() && !slotToSocket.containsKey(socket.getSlot())) {
                targetSlot = socket.getSlot();
            } else {
                targetSlot = INNER_SLOTS[i - startIndex];
            }

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
                        .replace("%enchant_name%", socket.getCleanName())
                        .replace("%enchant_clean_name%", socket.getCleanName())
                        .replace("%enchant_raw_name%", socket.getCleanName())
                        .replace("%current_level%", String.valueOf(currentLvl))
                        .replace("%current_level_roman%", (currentLvl > 0) ? TextUtil.toRoman(currentLvl) : "0")
                        .replace("%max_level%", String.valueOf(globalMax))
                        .replace("%max_level_roman%", TextUtil.toRoman(globalMax))
                        .replace("%current_max_for_pickaxe%", String.valueOf(maxForPickaxe))
                        .replace("%required_book_level%", TextUtil.toRoman(requiredBookLevel))
                        .replace("%required_book_level_num%", String.valueOf(requiredBookLevel))
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

        int backSlot = menuConfig.getInt("items.back-button.slot", 49);
        if (backSlot >= inventory.getSize()) backSlot = 40;

        if (slot == backSlot) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            new MainPickaxeGui(plugin, player, pickaxe).open();
            return;
        }

        // Previous Page
        if (slot == 45 && currentPage > 0) {
            currentPage--;
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.2f);
            setupItems();
            return;
        }

        // Next Page
        List<EnchantSocket> allSockets = new ArrayList<>(plugin.getEnchantManager().getAllSockets());
        int totalPages = Math.max(1, (int) Math.ceil((double) allSockets.size() / INNER_SLOTS.length));
        if (slot == 53 && currentPage < totalPages - 1) {
            currentPage++;
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.2f);
            setupItems();
            return;
        }

        EnchantSocket socket = slotToSocket.get(slot);
        if (socket != null) {
            ItemStack cursorItem = event.getCursor();
            if (cursorItem != null && !cursorItem.getType().isAir()) {
                boolean success = plugin.getEnchantManager().handleSocketUpgrade(player, pickaxe, socket, cursorItem);
                if (success) {
                    event.getView().setCursor(cursorItem);
                    setupItems();
                }
            } else {
                plugin.getMessageManager().sendMessage(player, "messages.enchant-no-book-in-hand");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            }
        }
    }
}
