package com.infinitygear.cost;

import com.infinitygear.data.TrackedItemData;
import com.infinitygear.nexo.NexoProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Live Bukkit/Vault/Nexo payment adapter. One instance is used for one attempted transaction. */
public final class BukkitCostAccount implements CostAccount {
    private final Player player;
    private final MoneyGateway money;
    private final NexoProvider nexo;
    private final java.util.function.Function<String, Boolean> trackedConsumption;
    private final java.util.function.Predicate<ItemStack> trackedUsable;
    private final Map<CostComponent, Deque<List<ItemStack>>> withdrawnItems = new HashMap<>();

    public BukkitCostAccount(Player player, MoneyGateway money, NexoProvider nexo) {
        this(player, money, nexo, ignored -> true, item -> {
            var identity = TrackedItemData.read(item);
            return identity != null && !identity.quarantined();
        });
    }

    public BukkitCostAccount(Player player, MoneyGateway money, NexoProvider nexo,
                             java.util.function.Function<String, Boolean> trackedConsumption) {
        this(player, money, nexo, trackedConsumption, item -> {
            var identity = TrackedItemData.read(item);
            return identity != null && !identity.quarantined();
        });
    }

    public BukkitCostAccount(Player player, MoneyGateway money, NexoProvider nexo,
                             java.util.function.Function<String, Boolean> trackedConsumption,
                             java.util.function.Predicate<ItemStack> trackedUsable) {
        this.player = player;
        this.money = money == null ? new UnavailableMoneyGateway() : money;
        this.nexo = nexo;
        this.trackedConsumption = trackedConsumption == null ? ignored -> true : trackedConsumption;
        this.trackedUsable = trackedUsable == null ? ignored -> false : trackedUsable;
    }

    public Check check(CostComponent component) {
        return switch (component.type()) {
            case MONEY -> !money.available() ? Check.PROVIDER_UNAVAILABLE
                    : money.has(player, component.amount()) ? Check.AVAILABLE : Check.INSUFFICIENT;
            case XP_POINTS -> player.calculateTotalExperiencePoints() >= component.wholeAmount()
                    ? Check.AVAILABLE : Check.INSUFFICIENT;
            case XP_LEVELS -> player.getLevel() >= component.wholeAmount() ? Check.AVAILABLE : Check.INSUFFICIENT;
            case VANILLA_ITEM -> {
                Material material = Material.matchMaterial(component.itemId());
                if (material == null) yield Check.INVALID_CONFIGURATION;
                yield count(item -> item.getType() == material) >= component.wholeAmount()
                        ? Check.AVAILABLE : Check.INSUFFICIENT;
            }
            case EXTERNAL_ITEM -> {
                if (!"nexo".equals(component.provider()) || nexo == null || !nexo.itemExists(component.itemId())) {
                    yield Check.PROVIDER_UNAVAILABLE;
                }
                yield count(item -> component.itemId().equalsIgnoreCase(nexo.itemId(item))) >= component.wholeAmount()
                        ? Check.AVAILABLE : Check.INSUFFICIENT;
            }
            case TRACKED_ARTIFACT -> count(item -> {
                TrackedItemData.Identity identity = TrackedItemData.read(item);
                return identity != null && trackedUsable.test(item)
                        && component.itemId().equalsIgnoreCase(identity.type());
            }) >= component.wholeAmount() ? Check.AVAILABLE : Check.INSUFFICIENT;
        };
    }

    public boolean withdraw(CostComponent component) {
        if (check(component) != Check.AVAILABLE) return false;
        if (!shouldConsume(component)) return true;
        return switch (component.type()) {
            case MONEY -> money.withdraw(player, component.amount());
            case XP_POINTS -> { player.giveExp(-Math.toIntExact(component.wholeAmount())); yield true; }
            case XP_LEVELS -> { player.setLevel(player.getLevel() - Math.toIntExact(component.wholeAmount())); yield true; }
            case VANILLA_ITEM -> removeItems(component,
                    item -> item.getType() == Material.matchMaterial(component.itemId()));
            case EXTERNAL_ITEM -> removeItems(component,
                    item -> component.itemId().equalsIgnoreCase(nexo.itemId(item)));
            case TRACKED_ARTIFACT -> removeItems(component, item -> {
                var identity = TrackedItemData.read(item);
                return identity != null && trackedUsable.test(item)
                        && component.itemId().equalsIgnoreCase(identity.type());
            });
        };
    }

    public void refund(CostComponent component) {
        if (!shouldConsume(component)) return;
        switch (component.type()) {
            case MONEY -> { if (!money.deposit(player, component.amount())) throw new IllegalStateException("Vault refund failed."); }
            case XP_POINTS -> player.giveExp(Math.toIntExact(component.wholeAmount()));
            case XP_LEVELS -> player.setLevel(player.getLevel() + Math.toIntExact(component.wholeAmount()));
            case VANILLA_ITEM, EXTERNAL_ITEM, TRACKED_ARTIFACT -> {
                Deque<List<ItemStack>> history = withdrawnItems.get(component);
                if (history == null || history.isEmpty()) throw new IllegalStateException("Missing item refund journal.");
                Map<Integer, ItemStack> leftovers = player.getInventory().addItem(
                        history.removeLast().stream().map(ItemStack::clone).toArray(ItemStack[]::new));
                if (!leftovers.isEmpty()) throw new IllegalStateException("Inventory became full during compensation.");
            }
        }
    }

    private boolean shouldConsume(CostComponent component) {
        return component.consumed() && (component.type() != CostComponent.Type.TRACKED_ARTIFACT
                || trackedConsumption.apply(component.itemId()));
    }

    private long count(java.util.function.Predicate<ItemStack> predicate) {
        long count = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && !item.getType().isAir() && predicate.test(item)) count += item.getAmount();
        }
        return count;
    }

    private boolean removeItems(CostComponent component, java.util.function.Predicate<ItemStack> predicate) {
        long remaining = component.wholeAmount();
        List<ItemStack> removed = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getStorageContents().length && remaining > 0; slot++) {
            ItemStack live = player.getInventory().getItem(slot);
            if (live == null || !predicate.test(live)) continue;
            int take = (int) Math.min(remaining, live.getAmount());
            ItemStack journal = live.clone();
            journal.setAmount(take);
            removed.add(journal);
            if (take == live.getAmount()) player.getInventory().setItem(slot, null);
            else live.setAmount(live.getAmount() - take);
            remaining -= take;
        }
        if (remaining != 0) {
            player.getInventory().addItem(removed.toArray(ItemStack[]::new));
            return false;
        }
        withdrawnItems.computeIfAbsent(component, ignored -> new ArrayDeque<>()).addLast(removed);
        return true;
    }
}
