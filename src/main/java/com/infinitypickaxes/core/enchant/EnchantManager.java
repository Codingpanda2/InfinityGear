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
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

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

        // EcoEnchants supplies custom enchantments. Fortune and Silk Touch are
        // deliberately managed alongside them as vanilla socket exceptions.
        discoverAndRegisterEcoEnchants();

        long enabledSockets = socketsById.values().stream().filter(EnchantSocket::isEnabled).count();
        plugin.getLogger().info("Loaded " + socketsById.size() + " compatible enchantment sockets ("
                + enabledSockets + " enabled).");
        if (socketsById.isEmpty() && ecoHook.isEcoEnchantsPresent()) {
            plugin.getLogger().warning("EcoEnchants is enabled, but no pickaxe-compatible enchantments "
                    + "were discovered. The enchantment menu will remain unavailable until EcoEnchants "
                    + "has loaded its registry; run /ipickaxe reload afterward.");
        } else if (!socketsById.isEmpty() && enabledSockets == 0) {
            plugin.getLogger().warning("Every discovered managed enchantment is disabled in enchants.yml; "
                    + "the enchantment menu has no available sockets.");
        }
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

            String displayName = ecoHook.getEnchantmentDisplayName(ench,
                    policy.getString(path + ".display-color", "<gray>"));
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
        registerVanillaSocket(policy, "fortune", "minecraft:fortune", "Fortune",
                List.of("<gray>Increases the drops from certain blocks.</gray>"), true);
        registerVanillaSocket(policy, "silk_touch", "minecraft:silk_touch", "Silk Touch",
                List.of("<gray>Allows compatible blocks to drop themselves.</gray>"), false);
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

    /** Counts every managed socket enchantment, including Fortune and Silk Touch. */
    public int countUsedSockets(InfinityPickaxe pickaxe) {
        if (pickaxe == null) return 0;
        return countRecognizedSockets(pickaxe.getEnchantments().keySet(), socketsByKey);
    }

    static int countRecognizedSockets(Collection<String> enchantmentKeys,
                                      Map<String, EnchantSocket> socketsByKey) {
        if (enchantmentKeys == null || socketsByKey == null) return 0;
        int used = 0;
        for (String enchantmentKey : enchantmentKeys) {
            if (enchantmentKey == null) continue;
            EnchantSocket socket = socketsByKey.get(enchantmentKey.toLowerCase(Locale.ROOT));
            if (socket != null) {
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
            boolean nativeConflict = candidate != null && existing != null
                    && (candidate.conflictsWith(existing) || existing.conflictsWith(candidate));
            if (adminConflict || nativeConflict || ecoHook.conflictsWith(candidate, existing)) {
                plugin.getMessageManager().sendMessage(player, "messages.enchant-conflict",
                        "%enchant%", socket.getDisplayName());
                return false;
            }
        }

        boolean ecoManaged = ecoHook.findEcoEnchant(candidate) != null;
        boolean canApply = ecoManaged
                ? ecoHook.canApply(pickaxe.getItemStack(), candidate)
                : candidate != null && candidate.canEnchantItem(pickaxe.getItemStack());
        if (!canApply) {
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
        // Regular socket progression consumes actual enchanted books only.
        // LimitBreak validates its independently configurable PDC item.
        if (bookItem.getType() != Material.ENCHANTED_BOOK) return false;
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
        Integer bookLevel = getVanillaOrStoredBookLevel(bookItem, socket);
        if (bookLevel == null) {
            bookLevel = bookEnchants.get(socket.getKeyString().toLowerCase());
        }

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

    public Integer getBookLevel(ItemStack bookItem, EnchantSocket socket) {
        if (bookItem == null || socket == null) return null;
        if (bookItem.getType() != Material.ENCHANTED_BOOK) return null;
        Integer direct = getVanillaOrStoredBookLevel(bookItem, socket);
        if (direct != null) return direct;
        return ecoHook.extractEnchantsFromBook(bookItem).get(socket.getKeyString().toLowerCase(Locale.ROOT));
    }

    public boolean containsManagedEnchantBook(ItemStack item) {
        if (item == null || item.getType() != Material.ENCHANTED_BOOK) return false;
        for (EnchantSocket socket : socketsById.values()) {
            if (socket.isEnabled() && getBookLevel(item, socket) != null) return true;
        }
        return false;
    }

    /** Returns true when an anvil result introduces or raises a managed enchantment. */
    public boolean hasManagedEnchantIncrease(ItemStack original, ItemStack result) {
        if (original == null || result == null) return false;
        Map<String, Integer> originalLevels = new HashMap<>();
        Map<String, Integer> resultLevels = new HashMap<>();
        for (EnchantSocket socket : socketsById.values()) {
            Enchantment enchantment = getEnchantment(socket.getKeyString());
            if (enchantment == null) continue;
            originalLevels.put(socket.getKeyString(), original.getEnchantmentLevel(enchantment));
            resultLevels.put(socket.getKeyString(), result.getEnchantmentLevel(enchantment));
        }
        return hasAnyManagedLevelIncrease(originalLevels, resultLevels, socketsByKey.keySet());
    }

    static boolean hasAnyManagedLevelIncrease(Map<String, Integer> originalLevels,
                                              Map<String, Integer> resultLevels,
                                              Collection<String> managedKeys) {
        if (originalLevels == null || resultLevels == null || managedKeys == null) return false;
        for (String key : managedKeys) {
            if (key != null && resultLevels.getOrDefault(key, 0) > originalLevels.getOrDefault(key, 0)) {
                return true;
            }
        }
        return false;
    }

    private Integer getVanillaOrStoredBookLevel(ItemStack bookItem, EnchantSocket socket) {
        if (bookItem == null || socket == null || !bookItem.hasItemMeta()) return null;
        Enchantment enchantment = getEnchantment(socket.getKeyString());
        if (enchantment == null) return null;
        ItemMeta meta = bookItem.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            Integer stored = storageMeta.getStoredEnchants().get(enchantment);
            if (stored != null) return stored;
        }
        return meta.getEnchants().get(enchantment);
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
        List<EnchantPolicySynchronizer.EnchantDescriptor> descriptors = new ArrayList<>(liveEnchants.stream()
                .filter(enchant -> enchant != null && enchant.getEnchantment() != null
                        && enchant.getEnchantment().getKey() != null)
                .map(enchant -> new EnchantPolicySynchronizer.EnchantDescriptor(
                        enchant.getID().toLowerCase(Locale.ROOT),
                        enchant.getEnchantment().getKey().toString().toLowerCase(Locale.ROOT),
                        EcoEnchantsHook.getDefaultDisplayColor(enchant)))
                .toList());
        descriptors.add(new EnchantPolicySynchronizer.EnchantDescriptor("fortune", "minecraft:fortune"));
        descriptors.add(new EnchantPolicySynchronizer.EnchantDescriptor("silk_touch", "minecraft:silk_touch"));
        EnchantPolicySynchronizer.SyncResult result = EnchantPolicySynchronizer.synchronize(policy, descriptors);
        result.added().forEach(id -> plugin.getLogger().info(
                "Added enchantment policy entry for '" + id + "' to enchants.yml."));
        result.updated().forEach(id -> plugin.getLogger().info(
                "Added missing display-color for '" + id + "' to enchants.yml."));
        result.migrated().forEach(id -> plugin.getLogger().warning(
                "Disabled legacy no-op enchantment '" + id
                        + "' because Infinity Pickaxes are unbreakable; administrators may re-enable it explicitly."));
        result.orphaned().forEach(id -> plugin.getLogger().warning(
                "Orphaned enchants.yml entry '" + id
                        + "' has no matching live managed pickaxe enchantment; it was preserved."));
        if (result.changed()) plugin.getConfigManager().saveEnchantsConfig();
    }

    private void registerVanillaSocket(FileConfiguration policy, String id, String key,
                                       String rawDisplayName, List<String> description,
                                       boolean supportsLimitBreak) {
        Enchantment enchantment = getEnchantment(key);
        if (enchantment == null || enchantment.getKey() == null) {
            plugin.getLogger().warning("Could not resolve vanilla enchantment '" + key + "'.");
            return;
        }
        String path = "enchants." + id;
        String canonicalKey = enchantment.getKey().toString().toLowerCase(Locale.ROOT);
        String configuredKey = policy.getString(path + ".key", canonicalKey);
        if (!canonicalKey.equalsIgnoreCase(configuredKey)) {
            plugin.getLogger().warning("Ignoring non-canonical key for vanilla enchantment '" + id
                    + "' in enchants.yml; live key is " + canonicalKey + ".");
        }
        EnchantSocket socket = new EnchantSocket(
                id,
                canonicalKey,
                enchantment.getKey(),
                configuredDisplayName(policy, path, rawDisplayName),
                Material.ENCHANTED_BOOK,
                -1,
                policy.getBoolean(path + ".enabled", true),
                Math.max(0, policy.getInt(path + ".unlock-pickaxe-level", 0)),
                effectiveMaximum(policy.get(path + ".max-level"), enchantment.getMaxLevel(), id),
                new TreeMap<>(),
                description,
                null,
                new LinkedHashSet<>(policy.getStringList(path + ".additional-conflicts")),
                supportsLimitBreak
        );
        socketsById.put(id, socket);
        socketsByKey.put(canonicalKey, socket);
    }

    static String configuredDisplayName(FileConfiguration policy, String path, String rawDisplayName) {
        String color = policy == null ? "<gray>"
                : policy.getString(path + ".display-color", "<gray>");
        return EcoEnchantsHook.formatDisplayName(rawDisplayName, color);
    }

    private int effectiveMaximum(Object configured, int nativeMaximum, String id) {
        int effective = EnchantPolicySynchronizer.effectiveMaximum(configured, nativeMaximum);
        if (configured != null
                && !"inherit".equalsIgnoreCase(String.valueOf(configured))) {
            try {
                if (Integer.parseInt(String.valueOf(configured)) < 1) throw new NumberFormatException();
            } catch (NumberFormatException exception) {
                plugin.getLogger().warning("Invalid max-level for managed enchantment '" + id
                        + "'; inheriting its native maximum " + nativeMaximum + ".");
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
                            + "' for managed enchantment '" + socket.getId() + "'; policy was preserved.");
                }
            }
        }
    }
}
