package com.infinitypickaxes.hooks;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.core.enchant.EnchantSocket;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import com.infinitypickaxes.core.pickaxe.PickaxeData;
import com.infinitypickaxes.utils.ProgressBarUtil;
import com.infinitypickaxes.utils.TextUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceholderAPIHook extends PlaceholderExpansion {

    private final InfinityPickaxes plugin;

    public PlaceholderAPIHook(InfinityPickaxes plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "infinitypickaxes";
    }

    @Override
    public @NotNull String getAuthor() {
        return "aland";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) return "";
        Player player = offlinePlayer.getPlayer();
        if (player == null) return "";

        // Placeholder resolution must be read-only; never auto-convert a vanilla item.
        InfinityPickaxe pickaxe = PickaxeData.fromItemStack(player.getInventory().getItemInMainHand());
        if (pickaxe == null) {
            if (params.equalsIgnoreCase("is_holding")) return "false";
            return "0";
        }

        FileConfiguration config = plugin.getConfigManager().getConfig();
        String lower = params.toLowerCase();

        switch (lower) {
            case "is_holding" -> {
                return "true";
            }
            case "level" -> {
                return String.valueOf(pickaxe.getLevel());
            }
            case "max_level" -> {
                return String.valueOf(plugin.getLevelManager().getMaxLevel());
            }
            case "xp" -> {
                return String.format("%.0f", pickaxe.getXp());
            }
            case "required_xp" -> {
                return String.format("%.0f", plugin.getLevelManager().getRequiredXp(pickaxe.getLevel()));
            }
            case "xp_percent" -> {
                double req = plugin.getLevelManager().getRequiredXp(pickaxe.getLevel());
                if (req <= 0) return "100.0";
                double pct = Math.min(100.0, (pickaxe.getXp() / req) * 100.0);
                return String.format("%.1f", pct);
            }
            case "xp_bar" -> {
                double req = plugin.getLevelManager().getRequiredXp(pickaxe.getLevel());
                return ProgressBarUtil.getProgressBar(
                        pickaxe.getXp(),
                        req,
                        config.getInt("progress-bar.total-bars", 20),
                        config.getString("progress-bar.completed-symbol", "■"),
                        config.getString("progress-bar.uncompleted-symbol", "□"),
                        config.getString("progress-bar.completed-color", "<#00FF88>"),
                        config.getString("progress-bar.uncompleted-color", "<#555555>")
                );
            }
            case "blocks_mined" -> {
                return String.valueOf(pickaxe.getBlocksMined());
            }
            case "blocks_mined_formatted" -> {
                return String.format("%,d", pickaxe.getBlocksMined());
            }
            case "enchant_count" -> {
                return String.valueOf(plugin.getEnchantManager().countUsedSockets(pickaxe));
            }
            case "max_sockets" -> {
                return String.valueOf(plugin.getEnchantManager().getSocketLimit(pickaxe));
            }
        }

        // Check dynamic enchant level placeholder: %infinitypickaxes_enchant_level_<enchant_id>%
        if (lower.startsWith("enchant_level_")) {
            String enchantId = lower.substring("enchant_level_".length());
            EnchantSocket socket = plugin.getEnchantManager().getSocket(enchantId);
            String key = (socket != null) ? socket.getKeyString() : "minecraft:" + enchantId;
            return String.valueOf(pickaxe.getEnchantmentLevel(key));
        }

        // Check dynamic enchant roman numeral: %infinitypickaxes_enchant_roman_<enchant_id>%
        if (lower.startsWith("enchant_roman_")) {
            String enchantId = lower.substring("enchant_roman_".length());
            EnchantSocket socket = plugin.getEnchantManager().getSocket(enchantId);
            String key = (socket != null) ? socket.getKeyString() : "minecraft:" + enchantId;
            int lvl = pickaxe.getEnchantmentLevel(key);
            return (lvl > 0) ? TextUtil.toRoman(lvl) : "0";
        }

        return null;
    }
}
