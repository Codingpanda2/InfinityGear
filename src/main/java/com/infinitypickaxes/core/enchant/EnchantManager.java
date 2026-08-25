package com.infinitypickaxes.core.enchant;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.api.events.PickaxeEnchantUpgradeEvent;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import com.infinitypickaxes.utils.SoundUtil;
import com.willfp.ecoenchants.enchant.EcoEnchant;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class EnchantManager {

    private final InfinityPickaxes plugin;
    private final EcoEnchantsHook ecoHook;
    private final Map<String, EnchantSocket> socketsById = new LinkedHashMap<>();
    private final Map<String, EnchantSocket> socketsByKey = new LinkedHashMap<>();

    private Sound upgradeSound = Sound.BLOCK_ANVIL_USE;
    private float upgradeSoundVolume = 1.0f;
    private float upgradeSoundPitch = 1.1f;

    public EnchantManager(InfinityPickaxes plugin) {
        this.plugin = plugin;
        this.ecoHook = new EcoEnchantsHook(plugin);
        loadConfig();
    }

    public void loadConfig() {
        socketsById.clear();
        socketsByKey.clear();

        FileConfiguration config = plugin.getConfigManager().getEnchantsConfig();

        // Sound settings
        this.upgradeSound = SoundUtil.resolve(config.getString(
                "settings.upgrade-sound.sound", "BLOCK_ANVIL_USE"), Sound.BLOCK_ANVIL_USE);
        this.upgradeSoundVolume = (float) config.getDouble("settings.upgrade-sound.volume", 1.0);
        this.upgradeSoundPitch = (float) config.getDouble("settings.upgrade-sound.pitch", 1.1);

        // EcoEnchants is the sole source of enchantment identity and metadata.
        discoverAndRegisterEcoEnchants();

        plugin.getLogger().info("Loaded " + socketsById.size() + " compatible enchantment sockets.");
    }

    public void discoverAndRegisterEcoEnchants() {
        FileConfiguration policy = plugin.getConfigManager().getEnchantsConfig();
        int added = 0;

        for (EcoEnchant ecoEnchant : ecoHook.getPickaxeEnchants()) {
            Enchantment ench = ecoEnchant.getEnchantment();
            if (ench == null || ench.getKey() == null) continue;
            String keyStr = ench.getKey().toString().toLowerCase();
            String id = ecoEnchant.getID().toLowerCase();

            if (policy.getStringList("policy.disabled").stream().anyMatch(id::equalsIgnoreCase)) continue;

            String displayName = ecoHook.getEnchantmentDisplayName(ench);
            List<String> desc = ecoHook.getEnchantmentDescription(ench);
            int maxLevel = Math.max(1, ecoEnchant.getMaximumLevel());
            int unlockLevel = policy.getInt("policy.unlock-levels." + id,
                    policy.getInt("policy.default-unlock-level", 0));

            EnchantSocket socket = new EnchantSocket(
                    id,
                    keyStr,
                    ench.getKey(),
                    displayName,
                    Material.ENCHANTED_BOOK,
                    -1,
                    true,
                    unlockLevel,
                    maxLevel,
                    new TreeMap<>(),
                    desc,
                    null
            );

            socketsById.put(id, socket);
            socketsByKey.put(keyStr, socket);
            added++;
        }

        if (added > 0) {
            plugin.getLogger().info("Loaded " + added + " pickaxe enchantments from EcoEnchants.");
        }
    }

    public EnchantSocket getSocket(String id) {
        if (id == null) return null;
        return socketsById.get(id.toLowerCase());
    }

    public EnchantSocket getSocketByKey(String keyString) {
        if (keyString == null) return null;
        return socketsByKey.get(keyString.toLowerCase());
    }

    public Collection<EnchantSocket> getAllSockets() {
        return socketsById.values();
    }

    /**
     * Counts only managed EcoEnchants. Vanilla enchantments such as the baseline
     * Efficiency enchantment are real item enchantments, but never consume sockets.
     */
    public int countUsedSockets(InfinityPickaxe pickaxe) {
        if (pickaxe == null) return 0;
        int used = 0;
        for (String enchantmentKey : pickaxe.getEnchantments().keySet()) {
            if (socketsByKey.containsKey(enchantmentKey.toLowerCase(Locale.ROOT))) {
                used++;
            }
        }
        return used;
    }

    public EcoEnchantsHook getEcoHook() {
        return ecoHook;
    }

    /**
     * Checks if a player can apply a book to upgrade an enchantment socket.
     * Returns true if successfully upgraded, false otherwise.
     */
    public boolean handleSocketUpgrade(Player player, InfinityPickaxe pickaxe, EnchantSocket socket, ItemStack bookItem) {
        if (player == null || pickaxe == null || socket == null || bookItem == null) {
            return false;
        }
        if (!plugin.getDuplicateService().isUsable(pickaxe.getItemStack())) {
            plugin.getMessageManager().sendMessage(player, "messages.pickaxe-quarantined");
            return false;
        }

        // 1. Check if pickaxe level satisfies socket unlock
        if (!socket.isUnlocked(pickaxe.getLevel())) {
            plugin.getMessageManager().sendMessage(player, "messages.enchant-locked-pickaxe-level",
                    "%required%", String.valueOf(socket.getUnlockPickaxeLevel()));
            return false;
        }

        int currentLevelOnPickaxe = pickaxe.getEnchantmentLevel(socket.getKeyString());
        int maxAllowedForPickaxe = socket.getMaxAllowedLevel(pickaxe.getLevel());

        Enchantment liveEnchantment = getEnchantment(socket.getKeyString());
        if (currentLevelOnPickaxe == 0 && !ecoHook.canApply(pickaxe.getItemStack(), liveEnchantment)) {
            plugin.getMessageManager().sendMessage(player, "messages.enchant-conflict",
                    "%enchant%", socket.getDisplayName());
            return false;
        }

        // 2. Check if already reached maximum limit for current pickaxe level
        if (currentLevelOnPickaxe >= maxAllowedForPickaxe) {
            plugin.getMessageManager().sendMessage(player, "messages.enchant-max-reached",
                    "%max%", String.valueOf(maxAllowedForPickaxe));
            return false;
        }

        // 3. Determine required book level
        // Rule: If pickaxe has Level N (N >= 1), required book is Level N.
        //       If pickaxe has Level 0, required book is Level 1.
        int requiredBookLevel = (currentLevelOnPickaxe == 0) ? 1 : currentLevelOnPickaxe;

        // 4. Extract enchants from provided book item
        Map<String, Integer> bookEnchants = ecoHook.extractEnchantsFromBook(bookItem);
        Integer bookLevel = bookEnchants.get(socket.getKeyString().toLowerCase());

        // Fallback check by socket ID if namespace wasn't matched
        if (bookLevel == null) {
            bookLevel = bookEnchants.get("minecraft:" + socket.getId());
        }
        if (bookLevel == null && socket.getKeyString().contains(":")) {
            String sub = socket.getKeyString().substring(socket.getKeyString().indexOf(":") + 1);
            for (Map.Entry<String, Integer> entry : bookEnchants.entrySet()) {
                if (entry.getKey().endsWith(":" + sub) || entry.getKey().equalsIgnoreCase(sub)) {
                    bookLevel = entry.getValue();
                    break;
                }
            }
        }

        if (bookLevel == null || bookLevel != requiredBookLevel) {
            plugin.getMessageManager().sendMessage(player, "messages.enchant-invalid-book",
                    "%enchant%", socket.getDisplayName(),
                    "%required_book_level%", String.valueOf(requiredBookLevel));
            return false;
        }

        int nextLevel = (currentLevelOnPickaxe == 0) ? 1 : (currentLevelOnPickaxe + 1);

        // 5. Call API event
        PickaxeEnchantUpgradeEvent event = new PickaxeEnchantUpgradeEvent(player, pickaxe, socket, currentLevelOnPickaxe, nextLevel);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        // 6. Consume 1 book from cursor/stack
        if (bookItem.getAmount() > 1) {
            bookItem.setAmount(bookItem.getAmount() - 1);
        } else {
            bookItem.setAmount(0);
        }

        // 7. Apply upgrade to pickaxe
        pickaxe.setEnchantmentLevel(socket.getKeyString(), nextLevel);
        pickaxe.saveAndSync();

        // 8. Play sound & feedback
        player.playSound(player.getLocation(), upgradeSound, upgradeSoundVolume, upgradeSoundPitch);
        plugin.getMessageManager().sendMessage(player, "messages.enchant-upgraded",
                "%enchant%", socket.getDisplayName(),
                "%level%", String.valueOf(nextLevel));

        return true;
    }

    public Enchantment getEnchantment(String keyStr) {
        if (keyStr == null || keyStr.isEmpty()) return null;
        try {
            String[] parts = keyStr.split(":", 2);
            NamespacedKey key = (parts.length == 2) ? new NamespacedKey(parts[0], parts[1]) : NamespacedKey.minecraft(parts[0]);
            return Bukkit.getRegistry(Enchantment.class).get(key);
        } catch (Exception e) {
            return null;
        }
    }
}
