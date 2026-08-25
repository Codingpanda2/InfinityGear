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
    private PickaxeProgressionPolicy progressionPolicy;

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
        this.progressionPolicy = PickaxeProgressionPolicy.from(config);

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
        Collection<EcoEnchant> liveEnchants = ecoHook.getPickaxeEnchants();
        synchronizePolicy(policy, liveEnchants);
        int added = 0;

        for (EcoEnchant ecoEnchant : liveEnchants) {
            Enchantment ench = ecoEnchant.getEnchantment();
            if (ench == null || ench.getKey() == null) continue;
            String keyStr = ench.getKey().toString().toLowerCase(Locale.ROOT);
            String id = ecoEnchant.getID().toLowerCase(Locale.ROOT);
            String path = "enchants." + id;

            String displayName = ecoHook.getEnchantmentDisplayName(ench);
            List<String> desc = ecoHook.getEnchantmentDescription(ench);
            int nativeMax = Math.max(1, ecoEnchant.getMaximumLevel());
            int maxLevel = effectiveMaximum(policy.get(path + ".max-level"), nativeMax, id);
            int unlockLevel = Math.max(0, policy.getInt(path + ".unlock-pickaxe-level", 0));
            boolean enabled = policy.getBoolean(path + ".enabled", true);
            Set<String> additionalConflicts = new LinkedHashSet<>(
                    policy.getStringList(path + ".additional-conflicts"));

            String configuredKey = policy.getString(path + ".key", keyStr);
            if (!keyStr.equalsIgnoreCase(configuredKey)) {
                plugin.getLogger().warning("Ignoring non-canonical key for EcoEnchant '" + id
                        + "' in enchants.yml; live key is " + keyStr + ".");
            }

            EnchantSocket socket = new EnchantSocket(
                    id,
                    keyStr,
                    ench.getKey(),
                    displayName,
                    Material.ENCHANTED_BOOK,
                    -1,
                    enabled,
                    unlockLevel,
                    maxLevel,
                    new TreeMap<>(),
                    desc,
                    null,
                    additionalConflicts
            );

            socketsById.put(id, socket);
            socketsByKey.put(keyStr, socket);
            added++;
        }

        if (added > 0) {
            plugin.getLogger().info("Loaded " + added + " pickaxe enchantments from EcoEnchants.");
        }
        validateAdditionalConflicts();
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
        return Collections.unmodifiableCollection(socketsById.values());
    }

    public PickaxeProgressionPolicy getProgressionPolicy() {
        return progressionPolicy;
    }

    public int getSocketLimit(int pickaxeLevel) {
        return progressionPolicy == null ? 0 : progressionPolicy.getSocketLimit(pickaxeLevel);
    }

    /**
     * Counts only managed EcoEnchants. Vanilla enchantments such as the baseline
     * Efficiency enchantment are real item enchantments, but never consume sockets.
     */
    public int countUsedSockets(InfinityPickaxe pickaxe) {
        if (pickaxe == null) return 0;
        int used = 0;
        for (String enchantmentKey : pickaxe.getEnchantments().keySet()) {
            EnchantSocket socket = socketsByKey.get(enchantmentKey.toLowerCase(Locale.ROOT));
            if (socket != null && socket.isEnabled()) {
                used++;
            }
        }
        return used;
    }

    public EcoEnchantsHook getEcoHook() {
        return ecoHook;
    }

    /**
     * Applies all rules for introducing a new managed enchantment. Existing
     * enchantments are grandfathered and can still be upgraded while over cap.
     */
    public boolean canIntroduceEnchantment(Player player, InfinityPickaxe pickaxe, EnchantSocket socket) {
        if (player == null || pickaxe == null || socket == null || !socket.isEnabled()) return false;

        int used = countUsedSockets(pickaxe);
        int limit = getSocketLimit(pickaxe.getLevel());
        if (used >= limit) {
            plugin.getMessageManager().sendMessage(player, "messages.enchant-sockets-full",
                    "%used%", String.valueOf(used),
                    "%max%", String.valueOf(limit));
            return false;
        }

        Enchantment candidate = getEnchantment(socket.getKeyString());
        for (String existingKey : pickaxe.getEnchantments().keySet()) {
            Enchantment existing = getEnchantment(existingKey);
            EnchantSocket existingSocket = getSocketByKey(existingKey);
            boolean adminConflict = socket.additionallyConflictsWith(existingKey)
                    || (existingSocket != null
                    && existingSocket.additionallyConflictsWith(socket.getKeyString()));
            if (adminConflict || ecoHook.conflictsWith(candidate, existing)) {
                plugin.getMessageManager().sendMessage(player, "messages.enchant-conflict",
                        "%enchant%", socket.getDisplayName());
                return false;
            }
        }

        if (!ecoHook.canApply(pickaxe.getItemStack(), candidate)) {
            plugin.getMessageManager().sendMessage(player, "messages.enchant-conflict",
                    "%enchant%", socket.getDisplayName());
            return false;
        }
        return true;
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
        if (!socket.isEnabled()) return false;

        // 1. Check if pickaxe level satisfies socket unlock
        if (!socket.isUnlocked(pickaxe.getLevel())) {
            plugin.getMessageManager().sendMessage(player, "messages.enchant-locked-pickaxe-level",
                    "%required%", String.valueOf(socket.getUnlockPickaxeLevel()));
            return false;
        }

        int currentLevelOnPickaxe = pickaxe.getEnchantmentLevel(socket.getKeyString());
        int maxAllowedForPickaxe = socket.getMaxAllowedLevel(pickaxe.getLevel());

        if (currentLevelOnPickaxe == 0 && !canIntroduceEnchantment(player, pickaxe, socket)) {
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

    private void synchronizePolicy(FileConfiguration policy, Collection<EcoEnchant> liveEnchants) {
        List<EnchantPolicySynchronizer.EnchantDescriptor> descriptors = liveEnchants.stream()
                .filter(enchant -> enchant != null && enchant.getEnchantment() != null
                        && enchant.getEnchantment().getKey() != null)
                .map(enchant -> new EnchantPolicySynchronizer.EnchantDescriptor(
                        enchant.getID().toLowerCase(Locale.ROOT),
                        enchant.getEnchantment().getKey().toString().toLowerCase(Locale.ROOT)))
                .toList();
        EnchantPolicySynchronizer.SyncResult result = EnchantPolicySynchronizer.synchronize(policy, descriptors);
        result.added().forEach(id -> plugin.getLogger().info(
                "Added EcoEnchant policy entry for '" + id + "' to enchants.yml."));
        result.orphaned().forEach(id -> plugin.getLogger().warning(
                "Orphaned enchants.yml entry '" + id
                        + "' has no matching live pickaxe EcoEnchant; it was preserved."));
        if (result.changed()) plugin.getConfigManager().saveEnchantsConfig();
    }

    private int effectiveMaximum(Object configured, int nativeMaximum, String id) {
        int effective = EnchantPolicySynchronizer.effectiveMaximum(configured, nativeMaximum);
        if (configured != null
                && !"inherit".equalsIgnoreCase(String.valueOf(configured))) {
            try {
                if (Integer.parseInt(String.valueOf(configured)) < 1) throw new NumberFormatException();
            } catch (NumberFormatException exception) {
                plugin.getLogger().warning("Invalid max-level for EcoEnchant '" + id
                        + "'; inheriting EcoEnchants maximum " + nativeMaximum + ".");
            }
        }
        return effective;
    }

    private void validateAdditionalConflicts() {
        for (EnchantSocket socket : socketsById.values()) {
            for (String configuredConflict : socket.getAdditionalConflicts()) {
                boolean knownSocket = getSocket(configuredConflict) != null
                        || getSocketByKey(configuredConflict) != null;
                boolean knownLiveEnchant = configuredConflict.contains(":")
                        && getEnchantment(configuredConflict) != null;
                if (!knownSocket && !knownLiveEnchant) {
                    plugin.getLogger().warning("Unknown additional-conflict '" + configuredConflict
                            + "' for EcoEnchant '" + socket.getId() + "'; policy was preserved.");
                }
            }
        }
    }
}
