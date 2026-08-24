package com.infinitypickaxes.core.enchant;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.api.events.PickaxeEnchantUpgradeEvent;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
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
        try {
            this.upgradeSound = Sound.valueOf(config.getString("settings.upgrade-sound.sound", "BLOCK_ANVIL_USE"));
        } catch (Exception e) {
            this.upgradeSound = Sound.BLOCK_ANVIL_USE;
        }
        this.upgradeSoundVolume = (float) config.getDouble("settings.upgrade-sound.volume", 1.0);
        this.upgradeSoundPitch = (float) config.getDouble("settings.upgrade-sound.pitch", 1.1);

        // Load sockets
        ConfigurationSection enchantsSec = config.getConfigurationSection("enchants");
        if (enchantsSec != null) {
            for (String id : enchantsSec.getKeys(false)) {
                ConfigurationSection sec = enchantsSec.getConfigurationSection(id);
                if (sec == null) continue;

                boolean enabled = sec.getBoolean("enabled", true);
                String keyStr = sec.getString("key", "minecraft:" + id).toLowerCase();
                String displayName = sec.getString("display-name", id);
                Material icon = Material.matchMaterial(sec.getString("icon", "ENCHANTED_BOOK"));
                if (icon == null) icon = Material.ENCHANTED_BOOK;
                int slot = sec.getInt("slot", -1);
                int unlockLevel = sec.getInt("unlock-pickaxe-level", 0);
                int maxLevel = sec.getInt("max-level", 1);
                List<String> desc = sec.getStringList("description");
                Integer customModelData = sec.contains("custom-model-data") ? sec.getInt("custom-model-data") : null;

                NavigableMap<Integer, Integer> scaling = new TreeMap<>();
                if (sec.isConfigurationSection("level-scaling")) {
                    for (String lvlKey : sec.getConfigurationSection("level-scaling").getKeys(false)) {
                        try {
                            int pickaxeLvl = Integer.parseInt(lvlKey);
                            int enchantLvl = sec.getInt("level-scaling." + lvlKey);
                            scaling.put(pickaxeLvl, enchantLvl);
                        } catch (NumberFormatException ignored) {}
                    }
                }

                NamespacedKey namespacedKey;
                try {
                    String[] parts = keyStr.split(":", 2);
                    namespacedKey = (parts.length == 2) ? new NamespacedKey(parts[0], parts[1]) : NamespacedKey.minecraft(parts[0]);
                } catch (Exception e) {
                    namespacedKey = NamespacedKey.minecraft(id);
                }

                // Verify that the enchantment actually exists on the server
                Enchantment realEnchant = Bukkit.getRegistry(Enchantment.class).get(namespacedKey);
                if (realEnchant == null) {
                    realEnchant = getEnchantment(keyStr);
                }
                if (realEnchant == null) {
                    plugin.getLogger().info("El encantamiento '" + keyStr + "' no está instalado en el servidor. Omitiendo socket.");
                    continue;
                }

                EnchantSocket socket = new EnchantSocket(id, keyStr, namespacedKey, displayName, icon, slot, enabled, unlockLevel, maxLevel, scaling, desc, customModelData);
                socketsById.put(id.toLowerCase(), socket);
                socketsByKey.put(keyStr.toLowerCase(), socket);
            }
        }

        // 2. Discover and dynamically register any compatible EcoEnchants
        discoverAndRegisterEcoEnchants();

        plugin.getLogger().info("Cargados " + socketsById.size() + " sockets de encantamientos compatibles.");
    }

    public void discoverAndRegisterEcoEnchants() {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        if (!config.getBoolean("settings.auto-register-ecoenchants", true)) {
            return;
        }

        List<Enchantment> discovered = ecoHook.discoverPickaxeEnchants();
        int added = 0;

        for (Enchantment ench : discovered) {
            if (ench == null || ench.getKey() == null) continue;
            String keyStr = ench.getKey().toString().toLowerCase();
            String id = ench.getKey().getKey().toLowerCase();

            if (socketsByKey.containsKey(keyStr) || socketsById.containsKey(id)) {
                continue;
            }

            // Derive real display name & description from EcoEnchants / Paper
            String displayName = ecoHook.getEnchantmentDisplayName(ench);
            List<String> desc = ecoHook.getEnchantmentDescription(ench);

            int maxLevel = Math.max(1, ench.getMaxLevel());
            int unlockLevel = 10;
            if (maxLevel == 1) unlockLevel = 15;
            else if (maxLevel >= 5) unlockLevel = 25;

            NavigableMap<Integer, Integer> scaling = new TreeMap<>();
            scaling.put(unlockLevel, 1);
            if (maxLevel > 1) {
                scaling.put(50, Math.max(2, maxLevel / 2));
                scaling.put(75, maxLevel);
            }

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
                    scaling,
                    desc,
                    null
            );

            socketsById.put(id, socket);
            socketsByKey.put(keyStr, socket);
            added++;
        }

        if (added > 0) {
            plugin.getLogger().info("Se detectaron y agregaron " + added + " encantamientos de EcoEnchants al menú dinámico.");
        }
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String part : text.split(" ")) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase()).append(" ");
            }
        }
        return sb.toString().trim();
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

        // 1. Check if pickaxe level satisfies socket unlock
        if (!socket.isUnlocked(pickaxe.getLevel())) {
            plugin.getMessageManager().sendMessage(player, "messages.enchant-locked-pickaxe-level",
                    "%required%", String.valueOf(socket.getUnlockPickaxeLevel()));
            return false;
        }

        int currentLevelOnPickaxe = pickaxe.getEnchantmentLevel(socket.getKeyString());
        int maxAllowedForPickaxe = socket.getMaxAllowedLevel(pickaxe.getLevel());

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
