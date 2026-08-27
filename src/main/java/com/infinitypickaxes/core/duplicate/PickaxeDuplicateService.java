package com.infinitypickaxes.core.duplicate;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.api.events.PickaxeDuplicateDetectedEvent;
import com.infinitypickaxes.api.events.PickaxeRekeyedEvent;
import com.infinitypickaxes.api.events.PickaxeQuarantinedEvent;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import com.infinitypickaxes.core.pickaxe.PickaxeData;
import com.infinitygear.data.GearData;
import com.infinitygear.data.TrackedItemData;
import com.infinitygear.data.TrackedKind;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import io.papermc.paper.block.TileStateInventoryHolder;
import org.bukkit.entity.Player;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/** @deprecated Use GearDuplicateService; retained as a source/binary compatibility facade. */
@Deprecated
public class PickaxeDuplicateService implements AutoCloseable {

    private final InfinityPickaxes plugin;
    private final DuplicateStore store;
    private final Set<UUID> restricted = new HashSet<>();

    public PickaxeDuplicateService(InfinityPickaxes plugin) throws Exception {
        this.plugin = plugin;
        Path database = plugin.getDataFolder().toPath().resolve("duplicates.db");
        this.store = new DuplicateStore(database);
        this.restricted.addAll(store.loadRestrictedUuids());
    }

    PickaxeDuplicateService(InfinityPickaxes plugin, DuplicateStore store) throws SQLException {
        this.plugin = plugin;
        this.store = store;
        this.restricted.addAll(store.loadRestrictedUuids());
    }

    public boolean isRestricted(UUID uuid) {
        return uuid != null && restricted.contains(uuid);
    }

    public boolean isUsable(ItemStack item) {
        TrackedItemData.Identity identity = trackedIdentity(item);
        if (identity != null && item.getAmount() != 1) {
            try {
                quarantine(identity.uuid(), "Tracked singleton item was stacked", "system:stack-validation");
            } catch (SQLException exception) {
                plugin.getLogger().log(Level.SEVERE, "Could not quarantine stacked tracked item", exception);
            }
            return false;
        }
        UUID uuid = identity == null ? null : identity.uuid();
        if (isItemQuarantined(item)) {
            if (uuid != null && restricted.add(uuid)) {
                try {
                    store.quarantine(uuid, "Recovered quarantine from item metadata",
                            "system:pdc-recovery", List.of("item-pdc"));
                } catch (SQLException exception) {
                    plugin.getLogger().log(Level.SEVERE,
                            "Could not restore item-local quarantine for " + uuid, exception);
                }
            }
            markRestricted(item);
            return false;
        }
        if (!isRestricted(uuid)) return true;
        markRestricted(item);
        return false;
    }

    public DuplicateScanResult scanOnline(String actor) {
        return scanOnline(actor, List.of());
    }

    public DuplicateScanResult scanOnline(String actor, Collection<PhysicalStorageKey> retainedStorages) {
        DuplicateObservations<ItemStack> sightings = new DuplicateObservations<>();
        Set<PhysicalStorageKey> visitedStorages = new HashSet<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            collectInventory(player.getInventory(), "player:" + player.getName(), sightings);
            collectInventory(player.getEnderChest(), "enderchest:" + player.getName(), sightings);
            Inventory top = player.getOpenInventory().getTopInventory();
            collectPhysicalInventory(top, "open-container:" + player.getName(), visitedStorages, sightings);
        }
        for (PhysicalStorageKey retained : retainedStorages) {
            if (!visitedStorages.add(retained)) continue;
            retained.resolveInventory().ifPresent(inventory ->
                    collectInventory(inventory, "retained-storage:" + retained.value(), sightings));
        }
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Item entity : world.getEntitiesByClass(Item.class)) {
                collectItem(entity.getItemStack(), "dropped-item:" + entity.getUniqueId(), 0, sightings);
            }
        }

        return quarantineDuplicates(sightings, actor);
    }

    public DuplicateScanResult scanPlayer(Player player, String actor) {
        DuplicateObservations<ItemStack> sightings = new DuplicateObservations<>();
        collectInventory(player.getInventory(), "player:" + player.getName(), sightings);
        collectInventory(player.getEnderChest(), "enderchest:" + player.getName(), sightings);
        return quarantineDuplicates(sightings, actor);
    }

    public void quarantine(UUID uuid, String reason, String actor) throws SQLException {
        store.quarantine(uuid, reason, actor, List.of("manual"));
        restricted.add(uuid);
        markVisibleCopies(uuid);
        Bukkit.getPluginManager().callEvent(new PickaxeQuarantinedEvent(
                uuid, DuplicateStatus.QUARANTINED, reason, actor));
    }

    public void revoke(UUID uuid, String reason, String actor) throws SQLException {
        store.revoke(uuid, reason, actor, null);
        restricted.add(uuid);
        markVisibleCopies(uuid);
        Bukkit.getPluginManager().callEvent(new PickaxeQuarantinedEvent(
                uuid, DuplicateStatus.REVOKED, reason, actor));
    }

    public UUID rekeyHeld(Player administrator) throws SQLException {
        ItemStack held = administrator.getInventory().getItemInMainHand();
        if (plugin.getGearManager() != null) plugin.getGearManager().inspect(held, true);
        TrackedItemData.Identity identity = trackedIdentity(held);
        UUID oldUuid = identity == null ? null : identity.uuid();
        if (oldUuid == null) throw new IllegalArgumentException("Hold one tracked InfinityGear item first.");
        validateRekeyAmount(held.getAmount());

        UUID replacement = UUID.randomUUID();
        store.revoke(oldUuid, "Administrator selected a canonical replacement", administrator.getName(), replacement);
        restricted.add(oldUuid);
        PickaxeData.setPickaxeUuid(held, replacement);
        PickaxeData.setQuarantined(held, false);
        if (held.hasItemMeta()) {
            var meta = held.getItemMeta();
            var pdc = meta.getPersistentDataContainer();
            pdc.set(GearData.KEY_UUID, org.bukkit.persistence.PersistentDataType.STRING, replacement.toString());
            pdc.remove(GearData.KEY_QUARANTINED);
            held.setItemMeta(meta);
        }
        markVisibleCopies(oldUuid);
        InfinityPickaxe pickaxe = PickaxeData.fromItemStack(held);
        if (pickaxe != null) plugin.getPickaxeManager().syncPickaxe(pickaxe);
        Bukkit.getPluginManager().callEvent(new PickaxeRekeyedEvent(administrator, held, oldUuid, replacement));
        return replacement;
    }

    public Optional<DuplicateRecord> find(UUID uuid) throws SQLException {
        return store.find(uuid);
    }

    public List<DuplicateRecord> listRestricted() throws SQLException {
        return store.listRestricted();
    }

    public boolean isPhysicalStorageInventory(Inventory inventory) {
        return PhysicalStorageKey.from(inventory).isPresent();
    }

    public boolean containsInfinityPickaxe(Inventory inventory) {
        if (inventory == null) return false;
        for (ItemStack item : inventory.getContents()) {
            if (containsInfinityPickaxe(item, 0)) return true;
        }
        return false;
    }

    static boolean isPhysicalStorageHolder(InventoryHolder holder) {
        return PhysicalStorageKey.from(holder).isPresent();
    }

    static void validateRekeyAmount(int amount) {
        if (amount != 1) {
            throw new IllegalArgumentException("Canonical rekeying requires exactly one unstacked pickaxe.");
        }
    }

    private boolean containsInfinityPickaxe(ItemStack item, int depth) {
        if (isEmpty(item)) return false;
        if (trackedIdentity(item) != null || PickaxeData.isInfinityPickaxe(item)) return true;

        if (!(item.getItemMeta() instanceof BlockStateMeta blockMeta)
                || !(blockMeta.getBlockState() instanceof TileStateInventoryHolder container)) {
            return false;
        }
        int maxDepth = Math.max(0, plugin.getConfigManager().getConfig()
                .getInt("duplicate-protection.container-recursion-depth", 3));
        if (depth >= maxDepth) return false;
        for (ItemStack nested : container.getInventory().getContents()) {
            if (containsInfinityPickaxe(nested, depth + 1)) return true;
        }
        return false;
    }

    private void collectPhysicalInventory(Inventory inventory, String label,
                                          Set<PhysicalStorageKey> visitedStorages,
                                          DuplicateObservations<ItemStack> sightings) {
        Optional<PhysicalStorageKey> key = PhysicalStorageKey.from(inventory);
        if (key.isEmpty() || !visitedStorages.add(key.get())) return;
        collectInventory(inventory, label + ":storage=" + key.get().value(), sightings);
    }

    private void collectInventory(Inventory inventory, String label,
                                  DuplicateObservations<ItemStack> sightings) {
        if (inventory == null) return;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            collectItem(item, label + ":slot=" + slot, 0, sightings);
        }
    }

    private void collectItem(ItemStack item, String location, int depth,
                             DuplicateObservations<ItemStack> sightings) {
        if (isEmpty(item)) return;
        TrackedItemData.Identity identity = trackedIdentity(item);
        UUID uuid = identity == null ? null : identity.uuid();
        if (uuid != null) {
            int physicalCopies = Math.max(1, item.getAmount());
            sightings.observe(uuid, item, location, physicalCopies);
            if (isRestricted(uuid)) markRestricted(item);
        }

        if (!(item.getItemMeta() instanceof BlockStateMeta blockMeta)
                || !(blockMeta.getBlockState() instanceof TileStateInventoryHolder container)) {
            return;
        }
        int maxDepth = Math.max(0, plugin.getConfigManager().getConfig()
                .getInt("duplicate-protection.container-recursion-depth", 3));
        if (depth >= maxDepth) return;

        ItemStack[] nested = container.getInventory().getContents();
        for (int slot = 0; slot < nested.length; slot++) {
            collectItem(nested[slot], location + "/container-slot=" + slot, depth + 1, sightings);
        }
    }

    private DuplicateScanResult quarantineDuplicates(DuplicateObservations<ItemStack> sightings, String actor) {
        Set<UUID> detected = new HashSet<>();
        for (Map.Entry<UUID, List<DuplicateObservations.Observation<ItemStack>>> entry
                : sightings.entries().entrySet()) {
            if (entry.getValue().size() < 2) continue;
            UUID uuid = entry.getKey();
            boolean newlyDetected = !restricted.contains(uuid);
            List<String> locations = entry.getValue().stream()
                    .map(DuplicateObservations.Observation::location).toList();
            try {
                TrackedItemData.Identity identity = trackedIdentity(entry.getValue().getFirst().value());
                String kind = identity == null ? TrackedKind.GEAR.name() : identity.kind().name();
                String type = identity == null ? GearData.LEGACY_PICKAXE_PROFILE : identity.type();
                store.quarantine(uuid, kind, type,
                        "Multiple physical tracked items observed in one scan", actor, locations);
                restricted.add(uuid);
                entry.getValue().forEach(sighting -> markRestricted(sighting.value()));
                detected.add(uuid);
                if (newlyDetected) {
                    Bukkit.getPluginManager().callEvent(new PickaxeDuplicateDetectedEvent(uuid, locations));
                    Bukkit.getPluginManager().callEvent(new PickaxeQuarantinedEvent(
                            uuid, DuplicateStatus.QUARANTINED,
                            "Multiple physical tracked items observed in one scan", actor));
                }
            } catch (SQLException exception) {
                plugin.getLogger().log(Level.SEVERE, "Could not quarantine duplicate pickaxe " + uuid, exception);
            }
        }
        return new DuplicateScanResult(sightings.observedCopies(), detected);
    }

    private void markVisibleCopies(UUID uuid) {
        Set<PhysicalStorageKey> visitedStorages = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            markInventoryCopies(player.getInventory(), uuid);
            markInventoryCopies(player.getEnderChest(), uuid);
            Inventory top = player.getOpenInventory().getTopInventory();
            PhysicalStorageKey.from(top).filter(visitedStorages::add)
                    .ifPresent(ignored -> markInventoryCopies(top, uuid));
        }
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Item entity : world.getEntitiesByClass(Item.class)) {
                TrackedItemData.Identity identity = trackedIdentity(entity.getItemStack());
                if (identity != null && uuid.equals(identity.uuid())) {
                    markRestricted(entity.getItemStack());
                }
            }
        }
    }

    private void markInventoryCopies(Inventory inventory, UUID uuid) {
        for (ItemStack item : inventory.getContents()) {
            TrackedItemData.Identity identity = trackedIdentity(item);
            if (identity != null && uuid.equals(identity.uuid())) markRestricted(item);
        }
    }

    private void markRestricted(ItemStack item) {
        PickaxeData.setQuarantined(item, true);
        if (item != null && item.hasItemMeta()) {
            var meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(GearData.KEY_QUARANTINED,
                    org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        InfinityPickaxe pickaxe = PickaxeData.fromItemStack(item);
        if (pickaxe != null && plugin.getPickaxeManager() != null) {
            plugin.getPickaxeManager().syncPickaxe(pickaxe);
        }
    }

    private static boolean isEmpty(ItemStack item) {
        if (item == null || item.getAmount() <= 0) return true;
        Material type = item.getType();
        return type == Material.AIR || type == Material.CAVE_AIR || type == Material.VOID_AIR;
    }

    private static TrackedItemData.Identity trackedIdentity(ItemStack item) {
        TrackedItemData.Identity identity = TrackedItemData.readRaw(item);
        if (identity != null) return identity;
        UUID legacy = PickaxeData.getPickaxeUuid(item);
        return legacy == null ? null : new TrackedItemData.Identity(legacy, TrackedKind.GEAR,
                GearData.LEGACY_PICKAXE_PROFILE, 0, PickaxeData.isQuarantined(item));
    }

    private static boolean isItemQuarantined(ItemStack item) {
        TrackedItemData.Identity identity = trackedIdentity(item);
        return identity != null && identity.quarantined();
    }

    @Override
    public void close() throws Exception {
        store.close();
    }
}
