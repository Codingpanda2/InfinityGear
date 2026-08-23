package com.infinitypickaxes.gui;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.core.perk.PickaxePerk;
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

public class PerksGui extends CustomGui {

    private final FileConfiguration menuConfig;
    private final Map<Integer, PickaxePerk> slotToPerk = new HashMap<>();

    public PerksGui(InfinityPickaxes plugin, Player player, InfinityPickaxe pickaxe) {
        super(
                plugin,
                player,
                pickaxe,
                TextUtil.parse(plugin.getConfigManager().getPerksMenuConfig().getString("title", "<gradient:#00E5FF:#0077FE><b>Infinity Pickaxe</b></gradient> <dark_gray>»</dark_gray> <gray>Perks & Habilidades")),
                plugin.getConfigManager().getPerksMenuConfig().getInt("size", 36)
        );
        this.menuConfig = plugin.getConfigManager().getPerksMenuConfig();
    }

    @Override
    public void setupItems() {
        inventory.clear();
        slotToPerk.clear();

        // 1. Background filler
        Material fillMat = Material.matchMaterial(menuConfig.getString("fill-item.material", "BLACK_STAINED_GLASS_PANE"));
        if (fillMat == null) fillMat = Material.BLACK_STAINED_GLASS_PANE;
        ItemStack filler = new ItemBuilder(fillMat).name(" ").build();
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        // 2. Info Item
        int infoSlot = menuConfig.getInt("items.info-item.slot", 4);
        Material infoMat = Material.matchMaterial(menuConfig.getString("items.info-item.material", "NETHER_STAR"));
        String infoName = menuConfig.getString("items.info-item.name", "<#FFA500><b>Ranuras de Habilidades (Perks)</b></#FFA500>");
        List<String> infoLore = menuConfig.getStringList("items.info-item.lore");
        inventory.setItem(infoSlot, new ItemBuilder(infoMat != null ? infoMat : Material.NETHER_STAR)
                .name(infoName)
                .lore(infoLore)
                .build());

        // 3. Back Button
        int backSlot = menuConfig.getInt("items.back-button.slot", 31);
        Material backMat = Material.matchMaterial(menuConfig.getString("items.back-button.material", "ARROW"));
        String backName = menuConfig.getString("items.back-button.name", "<yellow><b>Volver al Menú Principal</b></yellow>");
        List<String> backLore = menuConfig.getStringList("items.back-button.lore");
        inventory.setItem(backSlot, new ItemBuilder(backMat != null ? backMat : Material.ARROW)
                .name(backName)
                .lore(backLore)
                .build());

        // 4. Render Perks
        int fallbackSlot = 11;
        for (PickaxePerk perk : plugin.getPerkManager().getAllPerks()) {
            if (!perk.isEnabled()) continue;

            int targetSlot = perk.getSlot() >= 0 ? perk.getSlot() : fallbackSlot++;
            if (targetSlot >= inventory.getSize()) continue;

            slotToPerk.put(targetSlot, perk);
            inventory.setItem(targetSlot, buildPerkItem(perk));
        }
    }

    private ItemStack buildPerkItem(PickaxePerk perk) {
        int pickaxeLvl = pickaxe.getLevel();
        boolean unlocked = pickaxeLvl >= perk.getRequiredLevel();
        boolean active = pickaxe.hasPerk(perk.getId());

        ItemBuilder builder = new ItemBuilder(perk.getIcon());

        if (!unlocked) {
            String name = menuConfig.getString("perk-format.locked-name", "<dark_gray>🔒</dark_gray> %perk_display_name% <red>(Bloqueado)</red>")
                    .replace("%perk_display_name%", perk.getDisplayName());
            List<String> rawLore = menuConfig.getStringList("perk-format.lore-locked");
            builder.name(name).lore(formatPerkLore(rawLore, perk));
        } else if (active) {
            String name = menuConfig.getString("perk-format.active-name", "%perk_display_name% <green>(Equipado)</green>")
                    .replace("%perk_display_name%", perk.getDisplayName());
            List<String> rawLore = menuConfig.getStringList("perk-format.lore-active");
            builder.name(name).lore(formatPerkLore(rawLore, perk));
        } else {
            String name = menuConfig.getString("perk-format.inactive-name", "%perk_display_name% <yellow>(Disponible)</yellow>")
                    .replace("%perk_display_name%", perk.getDisplayName());
            List<String> rawLore = menuConfig.getStringList("perk-format.lore-inactive");
            builder.name(name).lore(formatPerkLore(rawLore, perk));
        }

        return builder.build();
    }

    private List<String> formatPerkLore(List<String> rawLore, PickaxePerk perk) {
        List<String> formatted = new ArrayList<>();
        for (String line : rawLore) {
            if (line.contains("%perk_description%")) {
                formatted.addAll(perk.getDescription());
            } else {
                formatted.add(line
                        .replace("%perk_display_name%", perk.getDisplayName())
                        .replace("%required_level%", String.valueOf(perk.getRequiredLevel()))
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

        int backSlot = menuConfig.getInt("items.back-button.slot", 31);
        if (slot == backSlot) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            new MainPickaxeGui(plugin, player, pickaxe).open();
            return;
        }

        PickaxePerk perk = slotToPerk.get(slot);
        if (perk != null) {
            boolean success = plugin.getPerkManager().togglePerk(player, pickaxe, perk);
            if (success) {
                setupItems();
            }
        }
    }
}
