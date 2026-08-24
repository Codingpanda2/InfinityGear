package com.infinitypickaxes.gui;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import com.infinitypickaxes.utils.ItemBuilder;
import com.infinitypickaxes.utils.ProgressBarUtil;
import com.infinitypickaxes.utils.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MainPickaxeGui extends CustomGui {

    private final FileConfiguration menuConfig;

    public MainPickaxeGui(InfinityPickaxes plugin, Player player, InfinityPickaxe pickaxe) {
        super(
                plugin,
                player,
                pickaxe,
                TextUtil.parse(plugin.getConfigManager().getMainMenuConfig().getString("title", "<gradient:#00E5FF:#0077FE><b>Infinity Pickaxe</b></gradient> <dark_gray>»</dark_gray> <gray>Main Menu")),
                plugin.getConfigManager().getMainMenuConfig().getInt("size", 36)
        );
        this.menuConfig = plugin.getConfigManager().getMainMenuConfig();
    }

    @Override
    public void setupItems() {
        inventory.clear();

        // 1. Fill background panes
        Material fillMat = Material.matchMaterial(menuConfig.getString("fill-item.material", "BLACK_STAINED_GLASS_PANE"));
        if (fillMat == null) fillMat = Material.BLACK_STAINED_GLASS_PANE;
        ItemStack filler = new ItemBuilder(fillMat).name(" ").build();
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        // 2. Pickaxe Display
        int pickSlot = menuConfig.getInt("items.pickaxe-display.slot", 13);
        if (pickaxe.getItemStack() != null) {
            inventory.setItem(pickSlot, pickaxe.getItemStack().clone());
        }

        // 3. Enchants Button
        int enchantSlot = menuConfig.getInt("items.enchants-button.slot", 20);
        Material enchMat = Material.matchMaterial(menuConfig.getString("items.enchants-button.material", "ENCHANTED_BOOK"));
        String enchName = menuConfig.getString("items.enchants-button.name", "<#00E5FF><b>Enchantment Sockets</b></#00E5FF>");
        List<String> enchLore = processPlaceholders(menuConfig.getStringList("items.enchants-button.lore"));
        inventory.setItem(enchantSlot, new ItemBuilder(enchMat != null ? enchMat : Material.ENCHANTED_BOOK)
                .name(enchName)
                .lore(enchLore)
                .build());

        // 4. Perks Button
        int perkSlot = menuConfig.getInt("items.perks-button.slot", 24);
        Material perkMat = Material.matchMaterial(menuConfig.getString("items.perks-button.material", "NETHER_STAR"));
        String perkName = menuConfig.getString("items.perks-button.name", "<#FFA500><b>Abilities & Perks</b></#FFA500>");
        List<String> perkLore = processPlaceholders(menuConfig.getStringList("items.perks-button.lore"));
        inventory.setItem(perkSlot, new ItemBuilder(perkMat != null ? perkMat : Material.NETHER_STAR)
                .name(perkName)
                .lore(perkLore)
                .build());

        // 5. Stats Info
        int statsSlot = menuConfig.getInt("items.stats-info.slot", 22);
        Material statsMat = Material.matchMaterial(menuConfig.getString("items.stats-info.material", "BOOK"));
        String statsName = menuConfig.getString("items.stats-info.name", "<#00FF88><b>Mining Statistics</b></#00FF88>");
        List<String> statsLore = processPlaceholders(menuConfig.getStringList("items.stats-info.lore"));
        inventory.setItem(statsSlot, new ItemBuilder(statsMat != null ? statsMat : Material.BOOK)
                .name(statsName)
                .lore(statsLore)
                .build());

        // 6. Close Button
        int closeSlot = menuConfig.getInt("items.close-button.slot", 31);
        Material closeMat = Material.matchMaterial(menuConfig.getString("items.close-button.material", "BARRIER"));
        String closeName = menuConfig.getString("items.close-button.name", "<red><b>Close Menu</b></red>");
        List<String> closeLore = menuConfig.getStringList("items.close-button.lore");
        inventory.setItem(closeSlot, new ItemBuilder(closeMat != null ? closeMat : Material.BARRIER)
                .name(closeName)
                .lore(closeLore)
                .build());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        int enchantSlot = menuConfig.getInt("items.enchants-button.slot", 20);
        int perkSlot = menuConfig.getInt("items.perks-button.slot", 24);
        int closeSlot = menuConfig.getInt("items.close-button.slot", 31);

        if (slot == enchantSlot) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            new EnchantSocketsGui(plugin, player, pickaxe).open();
        } else if (slot == perkSlot) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            new PerksGui(plugin, player, pickaxe).open();
        } else if (slot == closeSlot) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 0.9f);
            player.closeInventory();
        }
    }

    private List<String> processPlaceholders(List<String> lines) {
        List<String> result = new ArrayList<>();
        double reqXp = plugin.getLevelManager().getRequiredXp(pickaxe.getLevel());
        FileConfiguration config = plugin.getConfigManager().getConfig();
        String bar = ProgressBarUtil.getProgressBar(
                pickaxe.getXp(),
                reqXp,
                config.getInt("progress-bar.total-bars", 20),
                config.getString("progress-bar.completed-symbol", "■"),
                config.getString("progress-bar.uncompleted-symbol", "□"),
                config.getString("progress-bar.completed-color", "<#00FF88>"),
                config.getString("progress-bar.uncompleted-color", "<#555555>")
        );

        int maxSockets = config.getInt("settings.max-sockets", 10);
        int maxPerks = plugin.getLevelManager().getMaxPerksForLevel(pickaxe.getLevel());

        for (String line : lines) {
            result.add(line
                    .replace("%player%", player.getName())
                    .replace("%level%", String.valueOf(pickaxe.getLevel()))
                    .replace("%max_level%", String.valueOf(plugin.getLevelManager().getMaxLevel()))
                    .replace("%current_xp%", String.format("%.0f", pickaxe.getXp()))
                    .replace("%required_xp%", String.format("%.0f", reqXp))
                    .replace("%xp_bar%", bar)
                    .replace("%blocks_mined%", String.format("%,d", pickaxe.getBlocksMined()))
                    .replace("%enchant_count%", String.valueOf(pickaxe.getEnchantments().size()))
                    .replace("%max_sockets%", String.valueOf(maxSockets))
                    .replace("%perks_count%", String.valueOf(pickaxe.getEquippedPerks().size()))
                    .replace("%max_perks%", String.valueOf(maxPerks))
            );
        }
        return result;
    }
}
