package com.infinitypickaxes.core.limitbreak;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.api.events.LimitBreakApplyEvent;
import com.infinitypickaxes.core.enchant.EnchantSocket;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import com.infinitypickaxes.utils.ItemBuilder;
import com.infinitypickaxes.utils.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class LimitBreakManager {

    private final InfinityPickaxes plugin;

    private final NamespacedKey KEY_TYPE;
    private final NamespacedKey KEY_TARGET;
    private final NamespacedKey KEY_BONUS;

    public LimitBreakManager(InfinityPickaxes plugin) {
        this.plugin = plugin;
        this.KEY_TYPE = new NamespacedKey(plugin, "limitbreak_type");
        this.KEY_TARGET = new NamespacedKey(plugin, "limitbreak_target");
        this.KEY_BONUS = new NamespacedKey(plugin, "limitbreak_bonus");
    }

    public FileConfiguration getConfig() {
        return plugin.getConfigManager().getLimitBreakConfig();
    }

    public int getMaxExtraLevels() {
        return getConfig().getInt("settings.max-extra-levels", 5);
    }

    /**
     * Creates a Specific (+1) LimitBreak Book for a designated enchantment socket.
     */
    public ItemStack createSpecificBook(EnchantSocket socket, int amount) {
        if (socket == null) return null;
        FileConfiguration config = getConfig();

        Material mat = Material.matchMaterial(config.getString("specific-book.material", "ENCHANTED_BOOK"));
        if (mat == null) mat = Material.ENCHANTED_BOOK;

        int cmd = config.getInt("specific-book.custom-model-data", 10001);
        boolean glowing = config.getBoolean("specific-book.glowing", true);

        String name = config.getString("specific-book.display-name", "<gradient:#FF0055:#FFAA00><b>LimitBreak: %enchant_name% +1</b></gradient>")
                .replace("%enchant_name%", socket.getCleanName())
                .replace("%enchant_display_name%", socket.getDisplayName());

        List<String> rawLore = config.getStringList("specific-book.lore");
        List<String> lore = new ArrayList<>();
        int maxExtra = getMaxExtraLevels();

        for (String line : rawLore) {
            lore.add(line
                    .replace("%enchant_name%", socket.getCleanName())
                    .replace("%enchant_display_name%", socket.getDisplayName())
                    .replace("%max_extra%", String.valueOf(maxExtra))
            );
        }

        ItemBuilder builder = new ItemBuilder(mat, Math.max(1, amount))
                .name(name)
                .lore(lore)
                .customModelData(cmd);

        if (glowing) {
            builder.glow();
        }

        ItemStack item = builder.build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(KEY_TYPE, PersistentDataType.STRING, "SPECIFIC");
            pdc.set(KEY_TARGET, PersistentDataType.STRING, socket.getKeyString().toLowerCase());
            pdc.set(KEY_BONUS, PersistentDataType.INTEGER, 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Creates a Universal Super Book (+1) applicable to any pickaxe enchantment.
     */
    public ItemStack createUniversalBook(int amount) {
        FileConfiguration config = getConfig();

        Material mat = Material.matchMaterial(config.getString("universal-book.material", "ENCHANTED_BOOK"));
        if (mat == null) mat = Material.ENCHANTED_BOOK;

        int cmd = config.getInt("universal-book.custom-model-data", 10002);
        boolean glowing = config.getBoolean("universal-book.glowing", true);

        String name = config.getString("universal-book.display-name", "<gradient:#FF00FF:#00FFFF><b>✦ SÚPER LIBRO UNIVERSAL +1 ✦</b></gradient>");
        List<String> rawLore = config.getStringList("universal-book.lore");
        List<String> lore = new ArrayList<>();
        int maxExtra = getMaxExtraLevels();

        for (String line : rawLore) {
            lore.add(line.replace("%max_extra%", String.valueOf(maxExtra)));
        }

        ItemBuilder builder = new ItemBuilder(mat, Math.max(1, amount))
                .name(name)
                .lore(lore)
                .customModelData(cmd);

        if (glowing) {
            builder.glow();
        }

        ItemStack item = builder.build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(KEY_TYPE, PersistentDataType.STRING, "UNIVERSAL");
            pdc.set(KEY_TARGET, PersistentDataType.STRING, "UNIVERSAL");
            pdc.set(KEY_BONUS, PersistentDataType.INTEGER, 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Checks if the item is any LimitBreak book (Specific or Universal).
     */
    public boolean isLimitBreakBook(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(KEY_TYPE, PersistentDataType.STRING);
    }

    public boolean isUniversalBook(ItemStack item) {
        if (!isLimitBreakBook(item)) return false;
        String type = item.getItemMeta().getPersistentDataContainer().get(KEY_TYPE, PersistentDataType.STRING);
        return "UNIVERSAL".equalsIgnoreCase(type);
    }

    public String getTargetEnchantKey(ItemStack item) {
        if (!isLimitBreakBook(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(KEY_TARGET, PersistentDataType.STRING);
    }

    /**
     * Applies a LimitBreak book to a pickaxe socket.
     */
    public boolean applyLimitBreak(Player player, InfinityPickaxe pickaxe, EnchantSocket socket, ItemStack bookItem) {
        if (player == null || pickaxe == null || socket == null || bookItem == null) {
            return false;
        }
        if (plugin.getDuplicateService().isRestricted(pickaxe.getUuid())) {
            plugin.getMessageManager().sendMessage(player, "messages.pickaxe-quarantined");
            return false;
        }

        if (!isLimitBreakBook(bookItem)) {
            return false;
        }

        boolean universal = isUniversalBook(bookItem);
        String targetKey = getTargetEnchantKey(bookItem);

        // 1. Target validation for specific books
        if (!universal && targetKey != null) {
            String socketKey = socket.getKeyString().toLowerCase();
            String socketId = socket.getId().toLowerCase();
            if (!targetKey.equalsIgnoreCase(socketKey) && !targetKey.endsWith(":" + socketId) && !targetKey.equalsIgnoreCase(socketId)) {
                plugin.getMessageManager().sendMessage(player, "messages.limitbreak-invalid-target",
                        "%enchant%", socket.getDisplayName());
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                return false;
            }
        }

        // 2. Check if socket is unlocked by pickaxe level
        if (!socket.isUnlocked(pickaxe.getLevel())) {
            plugin.getMessageManager().sendMessage(player, "messages.limitbreak-locked-pickaxe-level",
                    "%required%", String.valueOf(socket.getUnlockPickaxeLevel()),
                    "%enchant%", socket.getDisplayName());
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            return false;
        }

        int currentLvl = pickaxe.getEnchantmentLevel(socket.getKeyString());
        if (currentLvl == 0 && !plugin.getEnchantManager().getEcoHook().canApply(
                pickaxe.getItemStack(), plugin.getEnchantManager().getEnchantment(socket.getKeyString()))) {
            plugin.getMessageManager().sendMessage(player, "messages.enchant-conflict",
                    "%enchant%", socket.getDisplayName());
            return false;
        }
        int baseMax = socket.getMaxLevel();
        int maxExtra = getMaxExtraLevels();
        int absoluteMax = baseMax + maxExtra;

        // 3. Check absolute LimitBreak ceiling
        if (currentLvl >= absoluteMax) {
            plugin.getMessageManager().sendMessage(player, "messages.limitbreak-max-reached",
                    "%enchant%", socket.getDisplayName(),
                    "%max%", String.valueOf(absoluteMax),
                    "%extra%", String.valueOf(maxExtra));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            return false;
        }

        int nextLvl = currentLvl + 1;

        // 4. Call Bukkit API Event
        LimitBreakApplyEvent event = new LimitBreakApplyEvent(player, pickaxe, socket, bookItem, universal, currentLvl, nextLvl);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        // 5. Consume 1 book from cursor/stack
        if (bookItem.getAmount() > 1) {
            bookItem.setAmount(bookItem.getAmount() - 1);
        } else {
            bookItem.setAmount(0);
        }

        // 6. Update pickaxe
        pickaxe.setEnchantmentLevel(socket.getKeyString(), nextLvl);
        pickaxe.saveAndSync();

        // 7. Visual & Audio feedback
        FileConfiguration config = getConfig();
        if (config.getBoolean("settings.sound.enabled", true)) {
            String sndName = config.getString("settings.sound.sound", "BLOCK_ENCHANTMENT_TABLE_USE");
            float vol = (float) config.getDouble("settings.sound.volume", 1.0);
            float pitch = (float) config.getDouble("settings.sound.pitch", 1.4);
            Sound sound = SoundUtil.resolve(sndName, null);
            if (sound != null) player.playSound(player.getLocation(), sound, vol, pitch);
        }

        if (config.getBoolean("settings.particles.enabled", true)) {
            String partName = config.getString("settings.particles.particle", "TOTEM_OF_UNDYING");
            int count = config.getInt("settings.particles.count", 35);
            try {
                Particle particle = Particle.valueOf(partName);
                player.spawnParticle(particle, player.getLocation().add(0, 1.2, 0), count, 0.5, 0.5, 0.5, 0.1);
            } catch (IllegalArgumentException ignored) {}
        }

        // 8. Send localized message
        int extraLvl = Math.max(0, nextLvl - baseMax);
        String extraIndicator = (extraLvl > 0) ? " <light_purple>(LimitBreak +" + extraLvl + ")</light_purple>" : "";

        plugin.getMessageManager().sendMessage(player, "messages.limitbreak-upgraded",
                "%enchant%", socket.getDisplayName(),
                "%level%", String.valueOf(nextLvl),
                "%limitbreak_indicator%", extraIndicator);

        return true;
    }
}
