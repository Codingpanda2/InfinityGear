package com.infinitypickaxes.core.perk;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.api.events.PickaxePerkToggleEvent;
import com.infinitypickaxes.core.perk.impl.*;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.*;

public class PerkManager {

    private final InfinityPickaxes plugin;
    private final Map<String, PickaxePerk> perks = new LinkedHashMap<>();

    public PerkManager(InfinityPickaxes plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        perks.clear();
        FileConfiguration config = plugin.getConfigManager().getPerksConfig();
        ConfigurationSection sec = config.getConfigurationSection("perks");

        if (sec != null) {
            // 1. Haste Surge
            if (sec.isConfigurationSection("haste_surge")) {
                ConfigurationSection s = sec.getConfigurationSection("haste_surge");
                String name = s.getString("display-name", "<#FFD700><b>Haste Surge</b></#FFD700>");
                Material icon = Material.matchMaterial(s.getString("icon", "GOLDEN_PICKAXE"));
                int slot = s.getInt("slot", 11);
                boolean enabled = s.getBoolean("enabled", true);
                int reqLvl = s.getInt("required-level", 10);
                List<String> desc = s.getStringList("description");
                int amp = s.getInt("amplifier", 1);
                registerPerk(new HasteSurgePerk(name, icon, slot, enabled, reqLvl, desc, amp));
            }

            // 2. AutoSmelt
            if (sec.isConfigurationSection("autosmelt")) {
                ConfigurationSection s = sec.getConfigurationSection("autosmelt");
                String name = s.getString("display-name", "<#FF4500><b>Auto Smelt</b></#FF4500>");
                Material icon = Material.matchMaterial(s.getString("icon", "LAVA_BUCKET"));
                int slot = s.getInt("slot", 12);
                boolean enabled = s.getBoolean("enabled", true);
                int reqLvl = s.getInt("required-level", 25);
                List<String> desc = s.getStringList("description");
                registerPerk(new AutoSmeltPerk(name, icon, slot, enabled, reqLvl, desc));
            }

            // 3. Blast Radius
            if (sec.isConfigurationSection("blast_radius")) {
                ConfigurationSection s = sec.getConfigurationSection("blast_radius");
                String name = s.getString("display-name", "<#DC143C><b>Blast Radius (3x3)</b></#DC143C>");
                Material icon = Material.matchMaterial(s.getString("icon", "TNT"));
                int slot = s.getInt("slot", 13);
                boolean enabled = s.getBoolean("enabled", true);
                int reqLvl = s.getInt("required-level", 50);
                List<String> desc = s.getStringList("description");
                double chance = s.getDouble("chance", 15.0);
                registerPerk(new BlastRadiusPerk(name, icon, slot, enabled, reqLvl, desc, chance));
            }

            // 4. Fortune Frenzy
            if (sec.isConfigurationSection("fortune_frenzy")) {
                ConfigurationSection s = sec.getConfigurationSection("fortune_frenzy");
                String name = s.getString("display-name", "<#00FA9A><b>Fortune Frenzy</b></#00FA9A>");
                Material icon = Material.matchMaterial(s.getString("icon", "EMERALD"));
                int slot = s.getInt("slot", 14);
                boolean enabled = s.getBoolean("enabled", true);
                int reqLvl = s.getInt("required-level", 75);
                List<String> desc = s.getStringList("description");
                double chance = s.getDouble("chance", 25.0);
                int mult = s.getInt("multiplier", 3);
                registerPerk(new FortuneFrenzyPerk(name, icon, slot, enabled, reqLvl, desc, chance, mult));
            }

            // 5. Void Siphon
            if (sec.isConfigurationSection("void_siphon")) {
                ConfigurationSection s = sec.getConfigurationSection("void_siphon");
                String name = s.getString("display-name", "<gradient:#9400D3:#4B0082><b>Void Siphon</b></gradient>");
                Material icon = Material.matchMaterial(s.getString("icon", "END_CRYSTAL"));
                int slot = s.getInt("slot", 15);
                boolean enabled = s.getBoolean("enabled", true);
                int reqLvl = s.getInt("required-level", 100);
                List<String> desc = s.getStringList("description");
                double xpMult = s.getDouble("xp-multiplier", 2.0);
                registerPerk(new VoidSiphonPerk(name, icon, slot, enabled, reqLvl, desc, xpMult));
            }
        }

        plugin.getLogger().info("Loaded " + perks.size() + " pickaxe perks.");
    }

    public void registerPerk(PickaxePerk perk) {
        if (perk != null) {
            perks.put(perk.getId().toLowerCase(), perk);
        }
    }

    public PickaxePerk getPerk(String id) {
        if (id == null) return null;
        return perks.get(id.toLowerCase());
    }

    public Collection<PickaxePerk> getAllPerks() {
        return perks.values();
    }

    /**
     * Toggles equip/unequip state of a perk on the given pickaxe.
     */
    public boolean togglePerk(Player player, InfinityPickaxe pickaxe, PickaxePerk perk) {
        if (player == null || pickaxe == null || perk == null) return false;
        if (plugin.getDuplicateService().isRestricted(pickaxe.getUuid())) {
            plugin.getMessageManager().sendMessage(player, "messages.pickaxe-quarantined");
            return false;
        }

        // Check level requirement
        if (pickaxe.getLevel() < perk.getRequiredLevel()) {
            plugin.getMessageManager().sendMessage(player, "messages.perk-locked",
                    "%required%", String.valueOf(perk.getRequiredLevel()));
            return false;
        }

        boolean currentlyEquipped = pickaxe.hasPerk(perk.getId());
        boolean targetState = !currentlyEquipped;

        if (targetState) {
            // Check max allowed perks
            int maxSlots = plugin.getLevelManager().getMaxPerksForLevel(pickaxe.getLevel());
            if (pickaxe.getEquippedPerks().size() >= maxSlots) {
                plugin.getMessageManager().sendMessage(player, "messages.perk-max-slots");
                return false;
            }
        }

        // Call event
        PickaxePerkToggleEvent event = new PickaxePerkToggleEvent(player, pickaxe, perk, targetState);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        if (targetState) {
            pickaxe.addPerk(perk.getId());
            plugin.getMessageManager().sendMessage(player, "messages.perk-equipped",
                    "%perk%", perk.getDisplayName());
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.3f);
        } else {
            pickaxe.removePerk(perk.getId());
            plugin.getMessageManager().sendMessage(player, "messages.perk-unequipped",
                    "%perk%", perk.getDisplayName());
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.0f);
        }

        pickaxe.saveAndSync();
        return true;
    }

    public void dispatchBlockBreak(BlockBreakEvent event, InfinityPickaxe pickaxe, Player player) {
        if (pickaxe == null || player == null) return;
        for (String perkId : pickaxe.getEquippedPerks()) {
            PickaxePerk perk = getPerk(perkId);
            if (perk != null && perk.isEnabled()) {
                try {
                    perk.onBlockBreak(event, pickaxe, player);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error executing perk " + perkId + ": " + e.getMessage());
                }
            }
        }
    }

    public void dispatchTick(Player player, InfinityPickaxe pickaxe) {
        if (player == null || pickaxe == null) return;
        for (String perkId : pickaxe.getEquippedPerks()) {
            PickaxePerk perk = getPerk(perkId);
            if (perk != null && perk.isEnabled()) {
                try {
                    perk.onTick(player, pickaxe);
                } catch (Exception ignored) {}
            }
        }
    }

    public double getActiveXpMultiplier(InfinityPickaxe pickaxe) {
        double mult = 1.0;
        if (pickaxe == null) return mult;
        if (pickaxe.hasPerk("void_siphon")) {
            PickaxePerk perk = getPerk("void_siphon");
            if (perk instanceof VoidSiphonPerk voidPerk) {
                mult *= voidPerk.getXpMultiplier();
            }
        }
        return mult;
    }
}
