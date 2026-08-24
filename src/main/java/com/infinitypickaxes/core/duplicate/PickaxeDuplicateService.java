package com.infinitypickaxes.core.duplicate;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.api.events.PickaxeDuplicateDetectedEvent;
import com.infinitypickaxes.api.events.PickaxeRekeyedEvent;
import com.infinitypickaxes.api.events.PickaxeQuarantinedEvent;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import com.infinitypickaxes.core.pickaxe.PickaxeData;
import com.infinitypickaxes.gui.CustomGui;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Item;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class PickaxeDuplicateService implements AutoCloseable {

    private final InfinityPickaxes plugin;
    private final DuplicateStore store;
    private final Set<UUID> restricted = new HashSet<>();

    public PickaxeDuplicateService(InfinityPickaxes plugin) throws Exception {
        this.plugin = plugin;
        Path database = plugin.getDataFolder().toPath().resolve("duplicates.db");
        this.store = new DuplicateStore(database);
        this.restricted.addAll(store.loadRestrictedUuids());
    }

    public boolean isRestricted(UUID uuid) {
        return uuid != null && restricted.contains(uuid);
    }

    public boolean isUsable(ItemStack item) {
        UUID uuid = PickaxeData.getPickaxeUuid(item);
        if (!isRestricted(uuid)) return true;
        markRestricted(item);
        return false;
    }

    public DuplicateScanResult scanOnline(String actor) {
        Map<UUID, List<Sighting>> sightings = new HashMap<>();
        Set<Inventory> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        int scanned = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            scanned += collectInventory(player.getInventory(), "player:" + player.getName(), visited, sightings);
            scanned += collectInventory(player.getEnderChest(), "enderchest:" + player.getName(), visited, sightings);
            Inventory top = player.getOpenInventory().getTopInventory();
            if (!(top.getHolder() instanceof CustomGui)) {
                scanned += collectInventory(top, "open-container:" + player.getName(), visited, sightings);
            }
        }
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Item entity : world.getEntitiesByClass(Item.class)) {
                scanned += collectItem(entity.getItemStack(), "dropped-item:" + entity.getUniqueId(), 0, sightings);
            }
        }

        return quarantineDuplicates(sightings, scanned, actor);
    }

    public DuplicateScanResult scanPlayer(Player player, String actor) {
        Map<UUID, List<Sighting>> sightings = new HashMap<>();
        Set<Inventory> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        int scanned = collectInventory(player.getInventory(), "player:" + player.getName(), visited, sightings);
        scanned += collectInventory(player.getEnderChest(), "enderchest:" + player.getName(), visited, sightings);
        return quarantineDuplicates(sightings, scanned, actor);
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
        UUID oldUuid = PickaxeData.getPickaxeUuid(held);
        if (oldUuid == null) throw new IllegalArgumentException("Hold an Infinity Pickaxe first.");

        UUID replacement = UUID.randomUUID();
        store.revoke(oldUuid, "Administrator selected a canonical replacement", administrator.getName(), replacement);
        restricted.add(oldUuid);
        PickaxeData.setPickaxeUuid(held, replacement);
        PickaxeData.setQuarantined(held, false);
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

    private int collectInventory(Inventory inventory, String label, Set<Inventory> visited,
                                 Map<UUID, List<Sighting>> sightings) {
        if (inventory == null || !visited.add(inventory)) return 0;
        int scanned = 0;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            scanned += collectItem(item, label + ":slot=" + slot, 0, sightings);
        }
        return scanned;
    }

    private int collectItem(ItemStack item, String location, int depth,
                            Map<UUID, List<Sighting>> sightings) {
        if (item == null || item.getType().isAir()) return 0;
        int scanned = 0;
        UUID uuid = PickaxeData.getPickaxeUuid(item);
        if (uuid != null) {
            int physicalCopies = Math.max(1, item.getAmount());
            scanned += physicalCopies;
            List<Sighting> uuidSightings = sightings.computeIfAbsent(uuid, ignored -> new ArrayList<>());
            for (int copy = 0; copy < physicalCopies; copy++) {
                uuidSightings.add(new Sighting(item, physicalCopies == 1 ? location : location + ":stack-copy=" + copy));
            }
            if (isRestricted(uuid)) markRestricted(item);
        }

        int maxDepth = Math.max(0, plugin.getConfigManager().getConfig()
                .getInt("duplicate-protection.container-recursion-depth", 3));
        if (depth >= maxDepth || !(item.getItemMeta() instanceof BlockStateMeta blockMeta)
                || !(blockMeta.getBlockState() instanceof Container container)) {
            return scanned;
        }

        ItemStack[] nested = container.getInventory().getContents();
        for (int slot = 0; slot < nested.length; slot++) {
            scanned += collectItem(nested[slot], location + "/container-slot=" + slot, depth + 1, sightings);
        }
        return scanned;
    }

    private DuplicateScanResult quarantineDuplicates(Map<UUID, List<Sighting>> sightings, int scanned, String actor) {
        Set<UUID> detected = new HashSet<>();
        for (Map.Entry<UUID, List<Sighting>> entry : sightings.entrySet()) {
            if (entry.getValue().size() < 2) continue;
            UUID uuid = entry.getKey();
            boolean newlyDetected = !restricted.contains(uuid);
            List<String> locations = entry.getValue().stream().map(Sighting::location).toList();
            try {
                store.quarantine(uuid, "Multiple physical pickaxes observed in one scan", actor, locations);
                restricted.add(uuid);
                entry.getValue().forEach(sighting -> markRestricted(sighting.item()));
                detected.add(uuid);
                if (newlyDetected) {
                    Bukkit.getPluginManager().callEvent(new PickaxeDuplicateDetectedEvent(uuid, locations));
                    Bukkit.getPluginManager().callEvent(new PickaxeQuarantinedEvent(
                            uuid, DuplicateStatus.QUARANTINED,
                            "Multiple physical pickaxes observed in one scan", actor));
                }
            } catch (SQLException exception) {
                plugin.getLogger().log(Level.SEVERE, "Could not quarantine duplicate pickaxe " + uuid, exception);
            }
        }
        return new DuplicateScanResult(scanned, detected);
    }

    private void markVisibleCopies(UUID uuid) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            markInventoryCopies(player.getInventory(), uuid);
            markInventoryCopies(player.getEnderChest(), uuid);
            Inventory top = player.getOpenInventory().getTopInventory();
            if (!(top.getHolder() instanceof CustomGui)) markInventoryCopies(top, uuid);
        }
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Item entity : world.getEntitiesByClass(Item.class)) {
                if (uuid.equals(PickaxeData.getPickaxeUuid(entity.getItemStack()))) {
                    markRestricted(entity.getItemStack());
                }
            }
        }
    }

    private void markInventoryCopies(Inventory inventory, UUID uuid) {
        for (ItemStack item : inventory.getContents()) {
            if (uuid.equals(PickaxeData.getPickaxeUuid(item))) markRestricted(item);
        }
    }

    private void markRestricted(ItemStack item) {
        PickaxeData.setQuarantined(item, true);
        InfinityPickaxe pickaxe = PickaxeData.fromItemStack(item);
        if (pickaxe != null && plugin.getPickaxeManager() != null) {
            plugin.getPickaxeManager().syncPickaxe(pickaxe);
        }
    }

    @Override
    public void close() throws Exception {
        store.close();
    }

    private record Sighting(ItemStack item, String location) {}
}
