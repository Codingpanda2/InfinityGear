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
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class EnchantSocketsGui extends CustomGui {

    private final FileConfiguration menuConfig;
    private final Map<Integer, EnchantSocket> slotToSocket = new HashMap<>();
    private int currentPage = 0;

    public EnchantSocketsGui(InfinityPickaxes plugin, Player player, InfinityPickaxe pickaxe) {
        super(
                plugin,
                player,
                pickaxe,
                TextUtil.parse(plugin.getConfigManager().getEnchantsMenuConfig().getString("title", "<gradient:#00E5FF:#0077FE><b>Infinity Pickaxe</b></gradient> <dark_gray>»</dark_gray> <gray>Enchantments")),
                plugin.getConfigManager().getEnchantsMenuConfig().getInt("size", 54)
        );
        this.menuConfig = plugin.getConfigManager().getEnchantsMenuConfig();
    }

    private int getBackSlot() {
        int configured = menuConfig.getInt("items.back-button.slot", -1);
        if (configured >= 0 && configured < inventory.getSize()) {
            return configured;
        }
        return inventory.getSize() - 5;
    }

    private int getPrevSlot() {
        return inventory.getSize() - 9;
    }

    private int getNextSlot() {
        return inventory.getSize() - 1;
    }

    private List<Integer> getUsableInnerSlots() {
        List<Integer> slots = new ArrayList<>();
        int rows = inventory.getSize() / 9;
        for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c <= 7; c++) {
                slots.add(r * 9 + c);
            }
        }
        return slots;
    }

    @Override
    public void setupItems() {
        inventory.clear();
        slotToSocket.clear();

        int invSize = inventory.getSize();

        // 1. Background filler
        Material fillMat = Material.matchMaterial(menuConfig.getString("fill-item.material", "BLACK_STAINED_GLASS_PANE"));
        if (fillMat == null) fillMat = Material.BLACK_STAINED_GLASS_PANE;
        ItemStack filler = new ItemBuilder(fillMat).name(" ").build();
        for (int i = 0; i < invSize; i++) {
            inventory.setItem(i, filler);
        }

        // 2. Info guide book
        int infoSlot = menuConfig.getInt("items.info-book.slot", 4);
        if (infoSlot >= 0 && infoSlot < invSize) {
            Material infoMat = Material.matchMaterial(menuConfig.getString("items.info-book.material", "KNOWLEDGE_BOOK"));
            String infoName = menuConfig.getString("items.info-book.name", "<#00FF88><b>How to Upgrade Sockets?</b></#00FF88>");
            int usedSockets = plugin.getEnchantManager().countUsedSockets(pickaxe);
            int maxSockets = plugin.getEnchantManager().getSocketLimit(pickaxe.getLevel());
            int limitBreakUnlock = plugin.getLimitBreakManager().getUnlockLevel();
            int limitBreakExtra = plugin.getLimitBreakManager().getMaxExtraLevels(pickaxe.getLevel());
            List<String> infoLore = menuConfig.getStringList("items.info-book.lore").stream()
                    .map(line -> line
                            .replace("%used_sockets%", String.valueOf(usedSockets))
                            .replace("%max_sockets%", String.valueOf(maxSockets))
                            .replace("%limitbreak_unlock_level%", String.valueOf(limitBreakUnlock))
                            .replace("%limitbreak_extra%", String.valueOf(limitBreakExtra)))
                    .toList();
            inventory.setItem(infoSlot, new ItemBuilder(infoMat != null ? infoMat : Material.KNOWLEDGE_BOOK)
                    .name(infoName)
                    .lore(infoLore)
                    .build());
        }

        // 3. Back Button
        int backSlot = getBackSlot();
        if (backSlot >= 0 && backSlot < invSize) {
            Material backMat = Material.matchMaterial(menuConfig.getString("items.back-button.material", "ARROW"));
            String backName = menuConfig.getString("items.back-button.name", "<yellow><b>Back to Main Menu</b></yellow>");
            List<String> backLore = menuConfig.getStringList("items.back-button.lore");
            inventory.setItem(backSlot, new ItemBuilder(backMat != null ? backMat : Material.ARROW)
                    .name(backName)
                    .lore(backLore)
                    .build());
        }

        // 4. Collect all enabled sockets
        List<EnchantSocket> allSockets = new ArrayList<>();
        for (EnchantSocket s : plugin.getEnchantManager().getAllSockets()) {
            if (s.isEnabled()) {
                allSockets.add(s);
            }
        }

        if (allSockets.isEmpty()) {
            int emptySlot = Math.min(invSize - 1, 22);
            boolean discoveredAny = !plugin.getEnchantManager().getAllSockets().isEmpty();
            String reason = discoveredAny
                    ? "<red>All discovered enchantments are disabled in enchants.yml.</red>"
                    : "<red>No pickaxe EcoEnchants were discovered.</red>";
            inventory.setItem(emptySlot, new ItemBuilder(Material.BARRIER)
                    .name("<red><b>No Enchantment Sockets Available</b></red>")
                    .lore(List.of(
                            reason,
                            "",
                            "<gray>Check the server log for the loaded socket count.</gray>",
                            "<gray>After EcoEnchants loads or reloads, run:</gray>",
                            "<yellow>/pickaxe reload</yellow>"
                    ))
                    .build());
        }

        List<Integer> usableSlots = getUsableInnerSlots();
        int pageSize = Math.max(1, usableSlots.size());
        int totalPages = Math.max(1, (int) Math.ceil((double) allSockets.size() / pageSize));
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;

        // 5. Pagination Buttons
        int prevSlot = getPrevSlot();
        int nextSlot = getNextSlot();
        if (currentPage > 0 && prevSlot >= 0 && prevSlot < invSize) {
            inventory.setItem(prevSlot, new ItemBuilder(Material.SPECTRAL_ARROW)
                    .name("<yellow><b>« Previous Page (" + currentPage + "/" + totalPages + ")</b></yellow>")
                    .build());
        }
        if (currentPage < totalPages - 1 && nextSlot >= 0 && nextSlot < invSize) {
            inventory.setItem(nextSlot, new ItemBuilder(Material.SPECTRAL_ARROW)
                    .name("<yellow><b>Next Page (" + (currentPage + 2) + "/" + totalPages + ") »</b></yellow>")
                    .build());
        }

        // 6. Populate Sockets on Current Page
        int startIndex = currentPage * pageSize;
        int endIndex = Math.min(startIndex + pageSize, allSockets.size());

        for (int i = startIndex; i < endIndex; i++) {
            EnchantSocket socket = allSockets.get(i);
            int targetSlot;

            if (currentPage == 0 && socket.getSlot() >= 0 && socket.getSlot() < invSize - 9 && !slotToSocket.containsKey(socket.getSlot())) {
                targetSlot = socket.getSlot();
            } else {
                int localIndex = i - startIndex;
                if (localIndex < usableSlots.size()) {
                    targetSlot = usableSlots.get(localIndex);
                } else {
                    continue;
                }
            }

            if (targetSlot >= 0 && targetSlot < invSize) {
                slotToSocket.put(targetSlot, socket);
                inventory.setItem(targetSlot, buildSocketItem(socket));
            }
        }
    }

    private ItemStack buildSocketItem(EnchantSocket socket) {
        int pickaxeLvl = pickaxe.getLevel();
        boolean unlocked = socket.isUnlocked(pickaxeLvl);
        int currentLvl = pickaxe.getEnchantmentLevel(socket.getKeyString());
        int maxForPickaxe = socket.getMaxAllowedLevel(pickaxeLvl);
        int globalMax = socket.getMaxLevel();
        int maxExtra = (plugin.getLimitBreakManager() != null)
                ? plugin.getLimitBreakManager().getMaxExtraLevels(pickaxeLvl) : 0;
        int absoluteMax = globalMax + maxExtra;

        ItemBuilder builder = new ItemBuilder(socket.getIcon());
        if (socket.getCustomModelData() != null) {
            builder.customModelData(socket.getCustomModelData());
        }

        if (!unlocked) {
            String name = menuConfig.getString("enchant-format.locked-name", "<dark_gray>🔒</dark_gray> %enchant_display_name% <red>(Locked)</red>")
                    .replace("%enchant_display_name%", socket.getDisplayName());
            List<String> rawLore = menuConfig.getStringList("enchant-format.lore-locked");
            builder.name(name).lore(formatLoreList(rawLore, socket, currentLvl, maxForPickaxe, globalMax, absoluteMax));
        } else if (currentLvl >= absoluteMax) {
            String name = menuConfig.getString("enchant-format.unlocked-name", "%enchant_display_name% <gray>[<yellow>Lv. %current_level%<dark_gray>/<gold>%max_level%<gray>]")
                    .replace("%enchant_display_name%", socket.getDisplayName())
                    .replace("%current_level%", String.valueOf(currentLvl))
                    .replace("%max_level%", String.valueOf(globalMax))
                    + " <gradient:#FF00FF:#00FFFF><b>[LB MAX]</b></gradient>";
            List<String> rawLore = menuConfig.getStringList("enchant-format.lore-maxed");
            builder.name(name).lore(formatLoreList(rawLore, socket, currentLvl, maxForPickaxe, globalMax, absoluteMax));
        } else if (currentLvl > globalMax) {
            int extra = currentLvl - globalMax;
            String name = menuConfig.getString("enchant-format.unlocked-name", "%enchant_display_name% <gray>[<yellow>Lv. %current_level%<dark_gray>/<gold>%max_level%<gray>]")
                    .replace("%enchant_display_name%", socket.getDisplayName())
                    .replace("%current_level%", String.valueOf(currentLvl))
                    .replace("%max_level%", String.valueOf(globalMax))
                    + " <gradient:#FF00FF:#FFAA00><b>[LB +" + extra + "]</b></gradient>";
            List<String> rawLore = menuConfig.getStringList("enchant-format.lore-unlocked");
            builder.name(name).lore(formatLoreList(rawLore, socket, currentLvl, maxForPickaxe, globalMax, absoluteMax));
        } else if (currentLvl >= maxForPickaxe && maxForPickaxe > 0) {
            String name = menuConfig.getString("enchant-format.unlocked-name", "%enchant_display_name% <gray>[<yellow>Lv. %current_level%<dark_gray>/<gold>%max_level%<gray>]")
                    .replace("%enchant_display_name%", socket.getDisplayName())
                    .replace("%current_level%", String.valueOf(currentLvl))
                    .replace("%max_level%", String.valueOf(globalMax));
            List<String> rawLore = menuConfig.getStringList("enchant-format.lore-maxed");
            builder.name(name).lore(formatLoreList(rawLore, socket, currentLvl, maxForPickaxe, globalMax, absoluteMax));
        } else {
            String name = menuConfig.getString("enchant-format.unlocked-name", "%enchant_display_name% <gray>[<yellow>Lv. %current_level%<dark_gray>/<gold>%max_level%<gray>]")
                    .replace("%enchant_display_name%", socket.getDisplayName())
                    .replace("%current_level%", String.valueOf(currentLvl))
                    .replace("%max_level%", String.valueOf(globalMax));
            List<String> rawLore = menuConfig.getStringList("enchant-format.lore-unlocked");
            builder.name(name).lore(formatLoreList(rawLore, socket, currentLvl, maxForPickaxe, globalMax, absoluteMax));
        }

        return builder.build();
    }

    private List<String> formatLoreList(List<String> rawLore, EnchantSocket socket, int currentLvl, int maxForPickaxe, int globalMax, int absoluteMax) {
        List<String> formatted = new ArrayList<>();
        int requiredBookLevel = (currentLvl == 0) ? 1 : currentLvl;

        for (String line : rawLore) {
            if (line.contains("%enchant_description%")) {
                List<String> desc = socket.getDescription();
                if (desc == null || desc.isEmpty()) {
                    desc = plugin.getEnchantManager().getEcoHook().getEnchantmentDescription(plugin.getEnchantManager().getEnchantment(socket.getKeyString()), currentLvl);
                }
                formatted.addAll(desc);
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
                        .replace("%absolute_max%", String.valueOf(absoluteMax))
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
        int rawSlot = event.getRawSlot();

        // 1. Player clicked in their OWN player inventory (Bottom Inventory)
        if (rawSlot >= inventory.getSize() || event.getClickedInventory() == event.getView().getBottomInventory()) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && !clickedItem.getType().isAir()) {

                    // Check if clicked item is a LimitBreak Specific Book
                    if (plugin.getLimitBreakManager() != null && plugin.getLimitBreakManager().isLimitBreakBook(clickedItem)) {
                        if (!plugin.getLimitBreakManager().isUniversalBook(clickedItem)) {
                            String target = plugin.getLimitBreakManager().getTargetEnchantKey(clickedItem);
                            EnchantSocket targetSocket = plugin.getEnchantManager().getSocketByKey(target);
                            if (targetSocket == null && target != null && target.contains(":")) {
                                targetSocket = plugin.getEnchantManager().getSocket(target.substring(target.indexOf(":") + 1));
                            }
                            if (targetSocket != null) {
                                if (plugin.getLimitBreakManager().applyLimitBreak(player, pickaxe, targetSocket, clickedItem)) {
                                    setupItems();
                                    return;
                                }
                            }
                        }
                    }

                    // Otherwise regular book auto-apply
                    Map<String, Integer> bookEnchants = plugin.getEnchantManager().getEcoHook().extractEnchantsFromBook(clickedItem);
                    for (String keyStr : bookEnchants.keySet()) {
                        EnchantSocket s = plugin.getEnchantManager().getSocketByKey(keyStr);
                        if (s == null && keyStr.contains(":")) {
                            s = plugin.getEnchantManager().getSocket(keyStr.substring(keyStr.indexOf(":") + 1));
                        }
                        if (s != null) {
                            if (plugin.getEnchantManager().handleSocketUpgrade(player, pickaxe, s, clickedItem)) {
                                setupItems();
                                break;
                            }
                        }
                    }
                }
            } else if (isSafeBottomAction(event.getAction())) {
                // Let the player put a book on their cursor without exposing the
                // GUI inventory to shift, collect-to-cursor, hotbar, or drag moves.
                event.setCancelled(false);
            }
            return;
        }

        // 2. Player clicked in the GUI TOP Inventory
        event.setCancelled(true);
        if (rawSlot < 0 || rawSlot >= inventory.getSize()) return;

        int backSlot = getBackSlot();
        if (rawSlot == backSlot) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            new MainPickaxeGui(plugin, player, pickaxe).open();
            return;
        }

        // Previous Page
        int prevSlot = getPrevSlot();
        if (rawSlot == prevSlot && currentPage > 0) {
            currentPage--;
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.2f);
            setupItems();
            return;
        }

        // Next Page
        int nextSlot = getNextSlot();
        List<Integer> usableSlots = getUsableInnerSlots();
        List<EnchantSocket> allSockets = new ArrayList<>();
        for (EnchantSocket s : plugin.getEnchantManager().getAllSockets()) {
            if (s.isEnabled()) allSockets.add(s);
        }
        int totalPages = Math.max(1, (int) Math.ceil((double) allSockets.size() / Math.max(1, usableSlots.size())));
        if (rawSlot == nextSlot && currentPage < totalPages - 1) {
            currentPage++;
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.2f);
            setupItems();
            return;
        }

        // Socket Click
        EnchantSocket socket = slotToSocket.get(rawSlot);
        if (socket != null) {
            ItemStack cursorItem = event.getCursor();
            if (cursorItem != null && !cursorItem.getType().isAir()) {

                // A. Check LimitBreak / Universal Book upgrade first
                if (plugin.getLimitBreakManager() != null && plugin.getLimitBreakManager().isLimitBreakBook(cursorItem)) {
                    boolean success = plugin.getLimitBreakManager().applyLimitBreak(player, pickaxe, socket, cursorItem);
                    if (success) {
                        event.getView().setCursor(cursorItem);
                        setupItems();
                    }
                    return;
                }

                // B. Regular Enchantment Book upgrade
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

    static boolean isSafeBottomAction(InventoryAction action) {
        return action == InventoryAction.PICKUP_ALL
                || action == InventoryAction.PICKUP_HALF
                || action == InventoryAction.PICKUP_ONE
                || action == InventoryAction.PICKUP_SOME
                || action == InventoryAction.PLACE_ALL
                || action == InventoryAction.PLACE_ONE
                || action == InventoryAction.PLACE_SOME
                || action == InventoryAction.SWAP_WITH_CURSOR;
    }
}
