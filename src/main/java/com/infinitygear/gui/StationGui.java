package com.infinitygear.gui;

import com.infinitygear.station.StationType;
import com.infinitygear.station.StationSession;
import com.infinitygear.cost.BukkitCostAccount;
import com.infinitygear.cost.CostEngine;
import com.infinitygear.cost.PaymentOption;
import com.infinitygear.nexo.NexoProvider;
import com.infinitypickaxes.core.pickaxe.PickaxeData;
import com.infinitygear.enchant.EnchantmentItemTransforms;
import com.infinitygear.enchant.FusionBookService;
import com.infinitygear.gear.SocketExpansionPolicy;
import com.infinitygear.data.GearData;
import com.infinitygear.api.events.GearEnchantChangeEvent;
import com.infinitygear.inventory.InventoryTransaction;
import org.bukkit.Bukkit;
import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.gui.CustomGui;
import com.infinitypickaxes.utils.ItemBuilder;
import com.infinitypickaxes.utils.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Safe station shell: live player slots stay in place; this inventory contains
 * clones only. Concrete mutation screens can re-read selected slots on confirm.
 */
public final class StationGui extends CustomGui {
    private final StationSession session;
    private final List<Integer> selectedPlayerSlots = new ArrayList<>();
    private final java.util.Map<Integer, ItemStack> selectedSnapshots = new java.util.HashMap<>();
    private String operation;
    private int paymentIndex;
    private int selectedEnchantIndex;

    public StationGui(InfinityPickaxes plugin, Player player, StationType type) {
        this(plugin, player, new StationSession(type, null, null, true));
    }

    public StationGui(InfinityPickaxes plugin, Player player, StationSession session) {
        super(plugin, player, null, TextUtil.parse(title(session.type())), 45);
        this.session = session;
    }

    private static String title(StationType type) {
        return switch (type) {
            case RUNIC_TABLE -> "<dark_purple><b>Runic Table</b></dark_purple>";
            case FUSION_ALTAR -> "<light_purple><b>Fusion Altar</b></light_purple>";
            case GEAR_FORGE -> "<gold><b>Gear Forge</b></gold>";
        };
    }

    @Override public void setupItems() {
        inventory.clear();
        ItemStack filler = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);
        List<String> operations = switch (session.type()) {
            case RUNIC_TABLE -> List.of("Apply Enchantment", "Remove Enchantment", "Transfer Enchantment", "View Policy");
            case FUSION_ALTAR -> List.of("Fuse Pair", "Fuse All Matching");
            case GEAR_FORGE -> List.of("Expand Socket Capacity");
        };
        Material[] icons = {Material.ENCHANTED_BOOK, Material.GRINDSTONE, Material.BOOK, Material.KNOWLEDGE_BOOK};
        for (int i = 0; i < operations.size(); i++) {
            inventory.setItem(10 + i * 2, new ItemBuilder(icons[Math.min(i, icons.length - 1)])
                    .name("<aqua><b>" + operations.get(i) + "</b></aqua>")
                    .lore(List.of("<gray>Select live input slots from your inventory below.</gray>",
                            "<dark_gray>Inputs remain in place until a validated confirmation.</dark_gray>"))
                    .build());
        }
        for (int i = 0; i < selectedPlayerSlots.size() && i < 5; i++) {
            ItemStack live = player.getInventory().getItem(selectedPlayerSlots.get(i));
            if (live != null) inventory.setItem(28 + i, live.clone());
        }
        if (operation != null) {
            if (!"view".equals(operation)) {
                List<PaymentOption> options = resolvedPaymentOptions();
                String cost = options.isEmpty() ? "<red>No usable cost option</red>"
                        : describe(options.get(Math.floorMod(paymentIndex, options.size())));
                inventory.setItem(36, new ItemBuilder(Material.GOLD_INGOT).name("<gold>Payment Option</gold>")
                        .lore(List.of(cost, "<gray>Click to select another configured option.</gray>")).build());
            }
            if (operation.equals("removal") || operation.equals("transfer") || operation.equals("view")
                    || operation.equals("fusion-all")) {
                List<String> keys = selectedManagedEnchantments();
                String selected = keys.isEmpty() ? "<red>No managed enchantment selected</red>"
                        : "<white>" + keys.get(Math.floorMod(selectedEnchantIndex, keys.size())) + "</white>";
                inventory.setItem(37, new ItemBuilder(Material.ENCHANTED_BOOK)
                        .name("<light_purple>Selected Enchantment</light_purple>")
                        .lore(List.of(selected, "<gray>Click to cycle.</gray>")).build());
            }
            inventory.setItem(40, new ItemBuilder(Material.LIME_CONCRETE).name("<green><b>Confirm</b></green>")
                    .lore(List.of("<yellow>All live inputs, station, identity, policy and cost are revalidated.</yellow>"))
                    .build());
            if ("application".equals(operation)) inventory.setItem(38, applicationPreview());
            if (operation.startsWith("fusion-")) inventory.setItem(38, fusionPreview());
            if ("socket-expansion".equals(operation)) inventory.setItem(38, forgePreview());
            if ("view".equals(operation)) inventory.setItem(38, viewPreview());
        }
        inventory.setItem(44, new ItemBuilder(Material.BARRIER).name("<red>Close</red>").build());
    }

    @Override public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getRawSlot() == 44) { player.closeInventory(); return; }
        int operationIndex = (event.getRawSlot() - 10) / 2;
        if (event.getRawSlot() >= 10 && event.getRawSlot() <= 16 && event.getRawSlot() % 2 == 0) {
            operation = switch (session.type()) {
                case RUNIC_TABLE -> List.of("application", "removal", "transfer", "view").get(Math.min(3, operationIndex));
                case FUSION_ALTAR -> List.of("fusion-pair", "fusion-all").get(Math.min(1, operationIndex));
                case GEAR_FORGE -> "socket-expansion";
            };
            setupItems();
            return;
        }
        if (event.getRawSlot() == 36 && operation != null && !"view".equals(operation)) {
            paymentIndex++; setupItems(); return;
        }
        if (event.getRawSlot() == 37 && operation != null) { selectedEnchantIndex++; setupItems(); return; }
        if (event.getRawSlot() == 40 && operation != null) { confirm(); return; }
        if (event.getClickedInventory() == event.getView().getBottomInventory()) {
            int slot = event.getSlot();
            ItemStack live = player.getInventory().getItem(slot);
            if (live == null || live.getType().isAir()) return;
            if (selectedPlayerSlots.contains(slot)) {
                selectedPlayerSlots.remove(Integer.valueOf(slot)); selectedSnapshots.remove(slot);
            } else if (selectedPlayerSlots.size() < 5) {
                selectedPlayerSlots.add(slot); selectedSnapshots.put(slot, live.clone());
            }
            setupItems();
        }
    }

    private void confirm() {
        if (!session.revalidate(plugin.getStationManager(), player)) {
            message("station.session-invalid");
            player.closeInventory(); return;
        }
        for (int slot : selectedPlayerSlots) {
            ItemStack live = player.getInventory().getItem(slot), expected = selectedSnapshots.get(slot);
            if (live == null || expected == null || live.getAmount() != expected.getAmount() || !live.isSimilar(expected)) {
                message("station.stale-input", "%slot%", String.valueOf(slot));
                player.closeInventory(); return;
            }
        }
        switch (operation) {
            case "application" -> confirmApplication();
            case "removal" -> confirmRemoval();
            case "transfer" -> confirmTransfer();
            case "fusion-pair" -> confirmFusionPair();
            case "fusion-all" -> confirmFusionAll();
            case "socket-expansion" -> confirmSocketExpansion();
            case "view" -> player.sendMessage("§bInstalled managed enchantments: §f" + selectedManagedEnchantments());
            default -> message("station.unknown-operation");
        }
    }

    private void confirmApplication() {
        int gearSlot = -1, bookSlot = -1;
        for (int slot : selectedPlayerSlots) {
            ItemStack item = player.getInventory().getItem(slot);
            if (GearData.isGear(item)) gearSlot = slot;
            else if (!plugin.getEnchantManager().getManagedBookEnchants(item).isEmpty()
                    || plugin.getLimitBreakManager().isLimitBreakBook(item)) bookSlot = slot;
        }
        if (gearSlot < 0) { message("station.application.missing-gear"); return; }
        if (bookSlot < 0) { message("station.application.missing-book"); return; }
        ItemStack gear = player.getInventory().getItem(gearSlot);
        ItemStack book = player.getInventory().getItem(bookSlot);
        if (plugin.getLimitBreakManager().isLimitBreakBook(book)) {
            if (!PickaxeData.isInfinityPickaxe(gear)) {
                message("station.limitbreak.pickaxe-profile-required");
                return;
            }
            confirmLimitBreak(gearSlot, bookSlot, gear, book);
            return;
        }
        var managed = plugin.getEnchantManager().getManagedBookEnchants(book);
        if (managed.isEmpty()) { message("station.application.unmanaged-book"); return; }
        if (managed.size() != 1) { message("station.application.multiple-enchantments"); return; }
        if (!plugin.getDuplicateService().isUsable(gear)) { message("station.gear-restricted"); return; }
        if (!PickaxeData.isInfinityPickaxe(gear)) {
            var validation = plugin.getGearService().validateEnchantmentApplication(gear, book,
                    managed.getFirst().socket().getKeyString());
            if (!validation.success()) {
                message(validation.messageKey());
                return;
            }
            List<PaymentOption> options = plugin.getCostRegistry().options("application");
            if (options.isEmpty()) { message("station.payment.no-option", "%operation%", "application"); return; }
            PaymentOption selected = options.get(Math.floorMod(paymentIndex, options.size()));
            NexoProvider nexo = plugin.getServer().getPluginManager().isPluginEnabled("Nexo") ? new NexoProvider() : null;
            BukkitCostAccount account = costAccount(nexo);
            if (!paymentReady(selected, account)) return;
            ItemStack gearBefore = gear.clone(), bookBefore = book.clone();
            try (CostEngine.Payment payment = new CostEngine().charge(selected, account)) {
                var applied = plugin.getGearService().applyEnchantment(gear, book,
                        managed.getFirst().socket().getKeyString());
                if (!applied.success()) { message(applied.messageKey()); return; }
                payment.commit(); selectedSnapshots.clear(); selectedPlayerSlots.clear(); player.closeInventory();
            } catch (CostEngine.PaymentException changed) {
                message("station.payment.changed");
            } catch (RuntimeException failure) {
                player.getInventory().setItem(gearSlot, gearBefore);
                player.getInventory().setItem(bookSlot, bookBefore);
                message(operationFailureMessage(failure));
            }
            return;
        }
        var pickaxe = PickaxeData.fromItemStack(gear);
        if (pickaxe == null) { message("station.gear-malformed"); return; }
        var target = managed.getFirst();
        int currentLevel = pickaxe.getEnchantmentLevel(target.socket().getKeyString());
        int standardMaximum = target.socket().getMaxAllowedLevel(pickaxe.getLevel());
        if (!target.socket().isEnabled()) { message("enchant.application.disabled", "%enchant%", target.socket().getDisplayName()); return; }
        if (!target.socket().isUnlocked(pickaxe.getLevel())) {
            message("enchant.application.locked", "%enchant%", target.socket().getDisplayName(),
                    "%required%", String.valueOf(target.socket().getUnlockPickaxeLevel())); return;
        }
        if (target.level() <= currentLevel) {
            message("enchant.application.equal_or_lower_level", "%book_level%", String.valueOf(target.level()),
                    "%current_level%", String.valueOf(currentLevel)); return;
        }
        if (target.level() > standardMaximum) {
            message("enchant.application.above_standard_maximum", "%book_level%", String.valueOf(target.level()),
                    "%maximum%", String.valueOf(standardMaximum)); return;
        }
        if (currentLevel == 0 && !plugin.getEnchantManager().canIntroduceEnchantment(player, pickaxe, target.socket())) return;
        List<PaymentOption> options = plugin.getCostRegistry().options("application");
        if (options.isEmpty()) { message("station.payment.no-option", "%operation%", "application"); return; }
        PaymentOption selected = options.get(Math.floorMod(paymentIndex, options.size()));
        NexoProvider nexo = plugin.getServer().getPluginManager().isPluginEnabled("Nexo") ? new NexoProvider() : null;
        BukkitCostAccount account = costAccount(nexo);
        if (!paymentReady(selected, account)) return;
        try (CostEngine.Payment payment = new CostEngine().charge(selected, account)) {
            ItemStack gearBefore = gear.clone(), bookBefore = book.clone();
            try {
                if (!plugin.getEnchantManager().handleSocketUpgrade(player, pickaxe, managed.getFirst().socket(), book)) {
                    return; // Payment closes uncommitted and compensates.
                }
                payment.commit();
                selectedSnapshots.clear(); selectedPlayerSlots.clear();
                player.closeInventory();
            } catch (RuntimeException mutationFailure) {
                player.getInventory().setItem(gearSlot, gearBefore);
                player.getInventory().setItem(bookSlot, bookBefore);
                throw mutationFailure;
            }
        } catch (CostEngine.PaymentException changed) {
            message("station.payment.changed");
        } catch (RuntimeException failure) {
            message(operationFailureMessage(failure));
        }
    }

    private void confirmLimitBreak(int gearSlot, int bookSlot, ItemStack gear, ItemStack book) {
        var pickaxe = PickaxeData.fromItemStack(gear);
        if (pickaxe == null) { message("station.gear-malformed"); return; }
        if (!plugin.getDuplicateService().isUsable(gear)) { message("station.gear-restricted"); return; }
        com.infinitypickaxes.core.enchant.EnchantSocket socket;
        if (plugin.getLimitBreakManager().isUniversalBook(book)) {
            var installed = managedOn(gear);
            if (installed.isEmpty()) { message("station.limitbreak.no-installed-enchantment"); return; }
            socket = installed.get(Math.floorMod(selectedEnchantIndex, installed.size())).socket();
        } else {
            socket = plugin.getEnchantManager().getSocketByKey(plugin.getLimitBreakManager().getTargetEnchantKey(book));
        }
        if (socket == null) { message("station.limitbreak.target-unavailable"); return; }
        int current = pickaxe.getEnchantmentLevel(socket.getKeyString());
        int standard = plugin.getLimitBreakManager().getStandardMaximum(pickaxe, socket);
        int absolute = plugin.getLimitBreakManager().getAbsoluteMaximum(pickaxe, socket);
        if (!socket.isEnabled()) { message("station.limitbreak.disabled", "%enchant%", socket.getDisplayName()); return; }
        if (!socket.supportsLimitBreak()) { message("messages.limitbreak-not-supported", "%enchant%", socket.getDisplayName()); return; }
        if (pickaxe.getLevel() < plugin.getLimitBreakManager().getUnlockLevel()) {
            message("messages.limitbreak-locked-pickaxe-level", "%required%",
                    String.valueOf(plugin.getLimitBreakManager().getUnlockLevel()), "%enchant%", socket.getDisplayName()); return;
        }
        if (!socket.isUnlocked(pickaxe.getLevel())) {
            message("station.limitbreak.enchantment-locked", "%enchant%", socket.getDisplayName(),
                    "%required%", String.valueOf(socket.getUnlockPickaxeLevel())); return;
        }
        if (current == 0) { message("messages.limitbreak-missing-enchantment", "%enchant%", socket.getDisplayName()); return; }
        if (current < standard) {
            message("messages.limitbreak-premature", "%enchant%", socket.getDisplayName(),
                    "%max%", String.valueOf(standard), "%current%", String.valueOf(current)); return;
        }
        if (current >= absolute) {
            message("messages.limitbreak-max-reached", "%enchant%", socket.getDisplayName(),
                    "%max%", String.valueOf(absolute), "%extra%", String.valueOf(Math.max(0, absolute - standard))); return;
        }
        ItemStack gearBefore = gear.clone(), bookBefore = book.clone();
        List<PaymentOption> options = plugin.getCostRegistry().options("application");
        if (options.isEmpty()) { message("station.payment.no-option", "%operation%", "LimitBreak"); return; }
        PaymentOption option = options.get(Math.floorMod(paymentIndex, options.size()));
        NexoProvider nexo = plugin.getServer().getPluginManager().isPluginEnabled("Nexo") ? new NexoProvider() : null;
        BukkitCostAccount account = costAccount(nexo);
        if (!paymentReady(option, account)) return;
        try (CostEngine.Payment payment = new CostEngine().charge(option,
                account)) {
            if (!plugin.getLimitBreakManager().applyLimitBreak(player, pickaxe, socket, book)) return;
            payment.commit(); selectedSnapshots.clear(); selectedPlayerSlots.clear(); player.closeInventory();
        } catch (CostEngine.PaymentException changed) {
            message("station.payment.changed");
        } catch (RuntimeException failure) {
            player.getInventory().setItem(gearSlot, gearBefore);
            player.getInventory().setItem(bookSlot, bookBefore);
            message(operationFailureMessage(failure));
        }
    }

    private void confirmRemoval() {
        int sourceSlot = selectedSourceSlot();
        if (sourceSlot < 0) { message("station.removal.missing-source"); return; }
        ItemStack source = player.getInventory().getItem(sourceSlot);
        if (GearData.isGear(source) && !plugin.getDuplicateService().isUsable(source)) {
            message("station.gear-restricted"); return;
        }
        var managed = managedOn(source);
        if (managed.isEmpty()) { message("station.source-no-managed-enchantment"); return; }
        var selected = managed.get(Math.floorMod(selectedEnchantIndex, managed.size()));
        if (plugin.getConfigManager().getEnchantsConfig().getBoolean(
                "enchants." + selected.socket().getId() + ".non-removable", false)) {
            message("station.removal.non-removable", "%enchant%", selected.socket().getDisplayName()); return;
        }
        final ItemStack result;
        try {
            ItemStack sourceUnit = source.clone();
            sourceUnit.setAmount(1);
            result = new EnchantmentItemTransforms().remove(sourceUnit,
                    plugin.getEnchantManager().getEnchantment(selected.socket().getKeyString()), true).sourceResult();
            synchronizeLegacyClone(result);
        } catch (IllegalArgumentException invalid) { message(transformFailureMessage(invalid)); return; }
        GearEnchantChangeEvent event = gearChange(source, selected.socket().getKeyString(), selected.level(), 0,
                GearEnchantChangeEvent.Operation.REMOVE);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) { message("station.cancelled-by-plugin"); return; }
        payAndMutateOptions(removalOptions(source, selected), () -> {
            ItemStack remainder = source.getAmount() == 1 ? result : source.clone();
            List<ItemStack> outputs = List.of();
            if (source.getAmount() > 1) {
                remainder.setAmount(source.getAmount() - 1);
                outputs = List.of(result);
            }
            var transaction = new InventoryTransaction().execute(player.getInventory(),
                    java.util.Map.of(sourceSlot, source.clone()),
                    java.util.Map.of(sourceSlot, remainder), outputs);
            if (!transaction.success()) throw new IllegalStateException("Inventory transaction: " + transaction.failure());
        });
    }

    private void confirmTransfer() {
        int sourceSlot = selectedSourceSlot(), blankSlot = -1;
        for (int slot : selectedPlayerSlots) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item != null && item.getType() == Material.BOOK) blankSlot = slot;
        }
        if (sourceSlot < 0) { message("station.transfer.missing-source"); return; }
        if (blankSlot < 0) { message("station.transfer.blank-book-required"); return; }
        ItemStack source = player.getInventory().getItem(sourceSlot), blank = player.getInventory().getItem(blankSlot);
        if (GearData.isGear(source) && !plugin.getDuplicateService().isUsable(source)) {
            message("station.gear-restricted"); return;
        }
        var managed = managedOn(source);
        if (managed.isEmpty()) { message("station.source-no-managed-enchantment"); return; }
        var selected = managed.get(Math.floorMod(selectedEnchantIndex, managed.size()));
        final EnchantmentItemTransforms.Transfer transfer;
        try {
            ItemStack sourceUnit = source.clone();
            sourceUnit.setAmount(1);
            transfer = new EnchantmentItemTransforms().transfer(sourceUnit,
                    plugin.getEnchantManager().getEnchantment(selected.socket().getKeyString()),
                    selected.socket().getMaxLevel(), blank);
            synchronizeLegacyClone(transfer.sourceResult());
        } catch (IllegalArgumentException invalid) { message(transformFailureMessage(invalid)); return; }
        GearEnchantChangeEvent event = gearChange(source, selected.socket().getKeyString(), selected.level(), 0,
                GearEnchantChangeEvent.Operation.TRANSFER);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) { message("station.cancelled-by-plugin"); return; }
        int finalBlankSlot = blankSlot;
        payAndMutate("transfer", () -> {
            java.util.Map<Integer, ItemStack> expected = java.util.Map.of(sourceSlot, source.clone(), finalBlankSlot, blank.clone());
            ItemStack blankRemainder = blank.getAmount() == 1 ? null : blank.clone();
            if (blankRemainder != null) blankRemainder.setAmount(blank.getAmount() - 1);
            java.util.Map<Integer, ItemStack> replacements = new java.util.LinkedHashMap<>();
            ItemStack sourceRemainder = source.getAmount() == 1 ? transfer.sourceResult() : source.clone();
            if (source.getAmount() > 1) sourceRemainder.setAmount(source.getAmount() - 1);
            replacements.put(sourceSlot, sourceRemainder);
            replacements.put(finalBlankSlot, blankRemainder);
            List<ItemStack> outputs = source.getAmount() == 1
                    ? List.of(transfer.bookResult())
                    : List.of(transfer.sourceResult(), transfer.bookResult());
            var result = new InventoryTransaction().execute(player.getInventory(), expected,
                    replacements, outputs);
            if (!result.success()) throw new IllegalStateException("Inventory transaction: " + result.failure());
        });
    }

    private void confirmFusionPair() {
        List<Integer> bookSlots = selectedPlayerSlots.stream().filter(slot -> {
            ItemStack item = player.getInventory().getItem(slot);
            return item != null && item.getType() == Material.ENCHANTED_BOOK;
        }).toList();
        if (bookSlots.size() != 2) {
            message("station.fusion.exactly-two-books", "%selected%", String.valueOf(bookSlots.size())); return;
        }
        ItemStack first = player.getInventory().getItem(bookSlots.get(0));
        ItemStack second = player.getInventory().getItem(bookSlots.get(1));
        FusionBookService.Result fusion = new FusionBookService(plugin.getEnchantManager()).pair(first, second);
        if (!fusion.allowed()) { message(fusionMessage(fusion.failure())); return; }
        payAndMutateOptions(scaledFusionOptions(fusion.plan()),
                () -> consumeBooksAndOutput(bookSlots, List.of(1, 1), fusion.outputs()));
    }

    private void confirmFusionAll() {
        String selectedKey = selectedManagedEnchantments().stream().findFirst().orElse(null);
        if (selectedKey == null) { message("station.fusion.missing-selection"); return; }
        List<ItemStack> expanded = new ArrayList<>();
        List<Integer> originSlots = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getStorageContents().length; slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null || item.getType() != Material.ENCHANTED_BOOK) continue;
            var managed = plugin.getEnchantManager().getManagedBookEnchants(item);
            if (managed.size() != 1 || !managed.getFirst().socket().getKeyString().equalsIgnoreCase(selectedKey)) continue;
            for (int count = 0; count < item.getAmount(); count++) { expanded.add(item); originSlots.add(slot); }
        }
        FusionBookService.Result fusion = new FusionBookService(plugin.getEnchantManager()).bulk(expanded, selectedKey);
        if (!fusion.allowed()) { message(fusionMessage(fusion.failure())); return; }
        java.util.Map<Integer, Integer> consume = new java.util.HashMap<>();
        for (int inputIndex : fusion.plan().consumedInputIndices()) consume.merge(originSlots.get(inputIndex), 1, Integer::sum);
        payAndMutateOptions(scaledFusionOptions(fusion.plan()), () -> {
            List<Integer> slots = new ArrayList<>(), quantities = new ArrayList<>();
            consume.forEach((slot, quantity) -> { slots.add(slot); quantities.add(quantity); });
            consumeBooksAndOutput(slots, quantities, fusion.outputs());
        });
    }

    private void confirmSocketExpansion() {
        int sourceSlot = selectedSourceSlot();
        if (sourceSlot < 0) { message("station.forge.missing-gear"); return; }
        ItemStack item = player.getInventory().getItem(sourceSlot);
        var gear = plugin.getGearManager().inspect(item, true).orElse(null);
        var profile = gear == null ? null : plugin.getGearProfiles().find(gear.profileId()).orElse(null);
        if (gear == null) { message("station.gear-malformed"); return; }
        if (profile == null) { message("station.gear-unknown-profile", "%profile%", gear.profileId()); return; }
        if (!plugin.getDuplicateService().isUsable(item)) { message("station.gear-restricted"); return; }
        var decision = SocketExpansionPolicy.evaluate(gear.socketCapacity(), profile.maximumExpandedSockets(), true);
        if (!decision.allowed()) {
            message(socketMessage(decision.failure()), "%current%", String.valueOf(decision.current()),
                    "%maximum%", String.valueOf(decision.maximum())); return;
        }
        var event = new com.infinitygear.api.events.GearSocketExpansionEvent(
                player, item, decision.current(), decision.resulting());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) { message("station.cancelled-by-plugin"); return; }
        payAndMutateOptions(socketExpansionOptions(decision.resulting()), () -> {
            var live = plugin.getGearManager().inspect(player.getInventory().getItem(sourceSlot), true).orElseThrow();
            if (!live.uuid().equals(gear.uuid())) throw new IllegalStateException("Gear UUID changed.");
            live.socketCapacity(decision.resulting());
            GearData.save(live, plugin.getDuplicateService().isRestricted(live.uuid()),
                    GearData.LEGACY_PICKAXE_PROFILE.equals(live.profileId()));
            if (GearData.LEGACY_PICKAXE_PROFILE.equals(live.profileId())) {
                var legacy = PickaxeData.fromItemStack(live.item());
                if (legacy != null) plugin.getPickaxeManager().syncPickaxe(legacy);
            }
        });
    }

    private void consumeBooksAndOutput(List<Integer> slots, List<Integer> quantities, List<ItemStack> outputs) {
        java.util.Map<Integer, ItemStack> expected = new java.util.LinkedHashMap<>();
        java.util.Map<Integer, ItemStack> replacements = new java.util.LinkedHashMap<>();
        for (int i = 0; i < slots.size(); i++) {
            int slot = slots.get(i), quantity = quantities.get(i);
            ItemStack live = player.getInventory().getItem(slot);
            expected.put(slot, live.clone());
            ItemStack replacement = live.getAmount() == quantity ? null : live.clone();
            if (replacement != null) replacement.setAmount(live.getAmount() - quantity);
            replacements.put(slot, replacement);
        }
        var result = new InventoryTransaction().execute(player.getInventory(), expected, replacements, outputs);
        if (!result.success()) throw new IllegalStateException("Inventory transaction: " + result.failure());
    }

    private void payAndMutate(String costKey, Runnable mutation) {
        payAndMutateOptions(plugin.getCostRegistry().options(costKey), mutation);
    }

    private void payAndMutateOptions(List<PaymentOption> options, Runnable mutation) {
        if (options.isEmpty()) { message("station.payment.no-option", "%operation%", operationCostKey()); return; }
        PaymentOption selected = options.get(Math.floorMod(paymentIndex, options.size()));
        NexoProvider nexo = plugin.getServer().getPluginManager().isPluginEnabled("Nexo") ? new NexoProvider() : null;
        BukkitCostAccount account = costAccount(nexo);
        CostEngine engine = new CostEngine();
        CostEngine.OptionQuote quote = engine.quote(List.of(selected), account).getFirst();
        if (!quote.providerUsable()) {
            message("station.payment.provider-unavailable", "%option%", selected.id()); return;
        }
        if (!quote.affordable()) {
            message("station.payment.insufficient", "%option%", selected.id(),
                    "%cost%", describe(selected).replaceAll("<[^>]+>", "")); return;
        }
        try (CostEngine.Payment payment = engine.charge(selected, account)) {
            mutation.run();
            payment.commit();
            selectedSnapshots.clear(); selectedPlayerSlots.clear(); player.closeInventory();
        } catch (CostEngine.PaymentException changed) {
            message("station.payment.changed");
        } catch (RuntimeException failure) {
            message(operationFailureMessage(failure));
        }
    }

    private boolean paymentReady(PaymentOption selected, BukkitCostAccount account) {
        CostEngine.OptionQuote quote = new CostEngine().quote(List.of(selected), account).getFirst();
        if (!quote.providerUsable()) {
            message("station.payment.provider-unavailable", "%option%", selected.id());
            return false;
        }
        if (!quote.affordable()) {
            message("station.payment.insufficient", "%option%", selected.id(),
                    "%cost%", describe(selected).replaceAll("<[^>]+>", ""));
            return false;
        }
        return true;
    }

    private String fusionMessage(FusionBookService.Failure failure) {
        return "station.fusion." + switch (failure) {
            case INVALID_BOOK -> "invalid-book";
            case MULTIPLE_ENCHANTMENTS -> "multiple-enchantments";
            case DIFFERENT_ENCHANTMENTS -> "different-enchantments";
            case DIFFERENT_LEVELS -> "different-levels";
            case STANDARD_MAXIMUM -> "standard-maximum";
            case NO_MATCHING_PAIR -> "no-matching-pair";
            case NONE -> "unknown";
        };
    }

    private String socketMessage(SocketExpansionPolicy.Failure failure) {
        return "station.forge." + switch (failure) {
            case CATALYST_MISSING -> "catalyst-missing";
            case AT_MAXIMUM -> "at-maximum";
            case GRANDFATHERED_OVER_MAXIMUM -> "grandfathered-over-maximum";
            case NONE -> "unknown";
        };
    }

    private String operationFailureMessage(RuntimeException failure) {
        String detail = failure.getMessage() == null ? "" : failure.getMessage();
        if (detail.contains("STALE_INPUT") || detail.contains("Gear UUID changed")) return "station.stale-confirmation";
        if (detail.contains("OUTPUTS_DO_NOT_FIT")) return "station.inventory-full";
        if (detail.contains("MUTATION_FAILED")) return "station.mutation-failed";
        return "station.operation-failed";
    }

    private String transformFailureMessage(IllegalArgumentException failure) {
        String detail = failure.getMessage() == null ? "" : failure.getMessage();
        if (detail.contains("blank ordinary book")) return "station.transfer.blank-book-required";
        if (detail.contains("LimitBroken")) return "station.transfer.overcap-not-supported";
        if (detail.contains("not present")) return "station.selected-enchantment-missing";
        if (detail.contains("non-removable")) return "station.removal.non-removable";
        return "station.invalid-input";
    }

    private void message(String key, String... placeholders) {
        plugin.getMessageManager().sendMessage(player,
                key.startsWith("messages.") ? key : "messages." + key, placeholders);
    }

    private List<PaymentOption> scaledFusionOptions(com.infinitygear.enchant.FusionCalculator.Plan plan) {
        var config = plugin.getConfigManager().getCostsConfig();
        var section = config.getConfigurationSection("operations.fusion.result-level-weights");
        java.util.Map<Integer, Long> weights = new java.util.HashMap<>();
        if (section != null) for (String key : section.getKeys(false)) {
            try { weights.put(Integer.parseInt(key), Math.max(0, section.getLong(key))); }
            catch (NumberFormatException ignored) { }
        }
        long multiplier = plan.weightedCost(weights,
                Math.max(0, config.getLong("operations.fusion.fallback-weight", 1)));
        return plugin.getCostRegistry().options("fusion").stream().map(option -> new PaymentOption(option.id(),
                option.components().stream().filter(component -> multiplier > 0)
                        .map(component -> new com.infinitygear.cost.CostComponent(
                                component.type(), component.provider(), component.itemId(),
                                component.amount() * multiplier, component.consumed())).toList())).toList();
    }

    private List<PaymentOption> removalOptions(ItemStack source,
            com.infinitypickaxes.core.enchant.EnchantManager.ManagedBookEnchant selected) {
        var config = plugin.getConfigManager().getCostsConfig();
        String root = "operations.removal.";
        java.util.NavigableMap<Integer, Double> levelValues = new java.util.TreeMap<>();
        var levels = config.getConfigurationSection(root + "level-weights");
        if (levels != null) for (String key : levels.getKeys(false)) {
            try { levelValues.put(Integer.parseInt(key), levels.getDouble(key)); }
            catch (NumberFormatException ignored) { }
        }
        java.util.Map<String, Double> overrides = new java.util.HashMap<>();
        var enchantWeights = config.getConfigurationSection(root + "enchantment-weights");
        if (enchantWeights != null) for (String key : enchantWeights.getKeys(false)) {
            overrides.put(key.toLowerCase(java.util.Locale.ROOT), enchantWeights.getDouble(key));
        }
        var profile = plugin.getGearManager().inspect(source, true)
                .flatMap(gear -> plugin.getGearProfiles().find(gear.profileId())).orElse(null);
        double profileMultiplier = profile == null ? 1.0 : profile.costMultiplier();
        if (profile != null) {
            var profileOverride = profile.enchantmentOverrides().get(
                    selected.socket().getKeyString().toLowerCase(java.util.Locale.ROOT));
            if (profileOverride != null && profileOverride.costWeight() != null) {
                overrides.put(selected.socket().getKeyString().toLowerCase(java.util.Locale.ROOT),
                        profileOverride.costWeight());
            }
        }
        double moneyCost = com.infinitygear.cost.RemovalCostFormula.calculate(
                config.getDouble(root + "base", 0), selected.socket().getKeyString(), selected.level(),
                selected.socket().getMaxLevel(), config.getDouble(root + "default-enchantment-weight", 1),
                overrides, new com.infinitygear.cost.WeightTable(levelValues,
                        config.getDouble(root + "level-weight-fallback", 1)), profileMultiplier,
                config.getDouble(root + "overcap-multiplier-per-level", 0));
        return plugin.getCostRegistry().options("removal").stream().map(option -> new PaymentOption(option.id(),
                option.components().stream()
                        .filter(component -> component.type() != com.infinitygear.cost.CostComponent.Type.MONEY || moneyCost > 0)
                        .map(component -> component.type() == com.infinitygear.cost.CostComponent.Type.MONEY
                                ? new com.infinitygear.cost.CostComponent(component.type(), component.provider(), component.itemId(), moneyCost, component.consumed())
                                : component).toList())).toList();
    }

    private int selectedSourceSlot() {
        for (int slot : selectedPlayerSlots) {
            ItemStack item = player.getInventory().getItem(slot);
            if (GearData.isGear(item) || item != null && item.getType() == Material.ENCHANTED_BOOK) return slot;
        }
        return -1;
    }

    private List<com.infinitypickaxes.core.enchant.EnchantManager.ManagedBookEnchant> managedOn(ItemStack item) {
        if (item == null) return List.of();
        if (item.getType() == Material.ENCHANTED_BOOK) return plugin.getEnchantManager().getManagedBookEnchants(item);
        List<com.infinitypickaxes.core.enchant.EnchantManager.ManagedBookEnchant> result = new ArrayList<>();
        for (var socket : plugin.getEnchantManager().getAllSockets()) {
            var enchantment = plugin.getEnchantManager().getEnchantment(socket.getKeyString());
            int level = enchantment == null ? 0 : item.getEnchantmentLevel(enchantment);
            if (level > 0) result.add(new com.infinitypickaxes.core.enchant.EnchantManager.ManagedBookEnchant(socket, level));
        }
        return result;
    }

    private void synchronizeLegacyClone(ItemStack item) {
        if (!PickaxeData.isInfinityPickaxe(item)) return;
        var legacy = PickaxeData.fromItemStack(item);
        if (legacy != null) plugin.getPickaxeManager().syncPickaxe(legacy);
    }

    private List<String> selectedManagedEnchantments() {
        List<String> result = new ArrayList<>();
        for (int slot : selectedPlayerSlots) for (var enchant : managedOn(player.getInventory().getItem(slot))) {
            if (!result.contains(enchant.socket().getKeyString())) result.add(enchant.socket().getKeyString());
        }
        return result;
    }

    private GearEnchantChangeEvent gearChange(ItemStack source, String key, int oldLevel, int newLevel,
                                               GearEnchantChangeEvent.Operation operation) {
        String profile = plugin.getGearManager().inspect(source, true).map(com.infinitygear.gear.GearInstance::profileId)
                .orElse(source.getType() == Material.ENCHANTED_BOOK ? "book" : "unknown");
        return new GearEnchantChangeEvent(player, source, profile, key, oldLevel, newLevel, operation);
    }

    private BukkitCostAccount costAccount(NexoProvider nexo) {
        return new BukkitCostAccount(player, plugin.getMoneyGateway(), nexo, artifact ->
                plugin.getConfigManager().getItemsConfig().getBoolean(
                        "artifacts." + artifact + ".consumed", true),
                plugin.getDuplicateService()::isUsable);
    }

    private String operationCostKey() {
        return switch (operation) {
            case "application" -> "application";
            case "removal" -> "removal";
            case "transfer" -> "transfer";
            case "fusion-pair", "fusion-all" -> "fusion";
            case "socket-expansion" -> "socket-expansion";
            default -> "application";
        };
    }

    private List<PaymentOption> resolvedPaymentOptions() {
        if ("removal".equals(operation)) {
            int slot = selectedSourceSlot();
            if (slot >= 0) {
                ItemStack source = player.getInventory().getItem(slot);
                var managed = managedOn(source);
                if (!managed.isEmpty()) return removalOptions(source,
                        managed.get(Math.floorMod(selectedEnchantIndex, managed.size())));
            }
        }
        if ("fusion-pair".equals(operation)) {
            List<ItemStack> books = selectedPlayerSlots.stream().map(player.getInventory()::getItem)
                    .filter(item -> item != null && item.getType() == Material.ENCHANTED_BOOK).toList();
            if (books.size() == 2) {
                var fusion = new FusionBookService(plugin.getEnchantManager()).pair(books.get(0), books.get(1));
                if (fusion.allowed()) return scaledFusionOptions(fusion.plan());
            }
        }
        if ("fusion-all".equals(operation)) {
            var plan = bulkPlan();
            if (plan != null) return scaledFusionOptions(plan);
        }
        if ("socket-expansion".equals(operation)) {
            int slot = selectedSourceSlot();
            var gear = slot < 0 ? null : plugin.getGearManager()
                    .inspect(player.getInventory().getItem(slot), true).orElse(null);
            if (gear != null) return socketExpansionOptions(gear.socketCapacity() + 1);
        }
        return plugin.getCostRegistry().options(operationCostKey());
    }

    private List<PaymentOption> socketExpansionOptions(int resultingCapacity) {
        var config = plugin.getConfigManager().getCostsConfig();
        String root = "operations.socket-expansion.";
        var section = config.getConfigurationSection(root + "result-capacity-weights");
        java.util.NavigableMap<Integer, Long> weights = new java.util.TreeMap<>();
        if (section != null) for (String key : section.getKeys(false)) {
            try { weights.put(Integer.parseInt(key), Math.max(0, section.getLong(key))); }
            catch (NumberFormatException ignored) { }
        }
        var entry = weights.floorEntry(resultingCapacity);
        long multiplier = entry == null ? Math.max(0, config.getLong(root + "fallback-weight", 1)) : entry.getValue();
        return plugin.getCostRegistry().options("socket-expansion").stream().map(option ->
                new PaymentOption(option.id(), option.components().stream().filter(component -> multiplier > 0)
                        .map(component -> new com.infinitygear.cost.CostComponent(component.type(), component.provider(),
                                component.itemId(), component.amount() * multiplier, component.consumed())).toList()))
                .toList();
    }

    private com.infinitygear.enchant.FusionCalculator.Plan bulkPlan() {
        String selectedKey = selectedManagedEnchantments().stream().findFirst().orElse(null);
        if (selectedKey == null) return null;
        List<Integer> levels = new ArrayList<>();
        int maximum = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType() != Material.ENCHANTED_BOOK) continue;
            var managed = plugin.getEnchantManager().getManagedBookEnchants(item);
            if (managed.size() != 1 || !managed.getFirst().socket().getKeyString().equalsIgnoreCase(selectedKey)) continue;
            maximum = managed.getFirst().socket().getMaxLevel();
            for (int i = 0; i < item.getAmount(); i++) levels.add(managed.getFirst().level());
        }
        try { return com.infinitygear.enchant.FusionCalculator.fuseAll(levels, maximum); }
        catch (IllegalArgumentException invalid) { return null; }
    }

    private ItemStack fusionPreview() {
        com.infinitygear.enchant.FusionCalculator.Plan plan = null;
        if ("fusion-pair".equals(operation)) {
            List<ItemStack> books = selectedPlayerSlots.stream().map(player.getInventory()::getItem)
                    .filter(item -> item != null && item.getType() == Material.ENCHANTED_BOOK).toList();
            if (books.size() == 2) {
                var result = new FusionBookService(plugin.getEnchantManager()).pair(books.get(0), books.get(1));
                if (result.allowed()) plan = result.plan();
            }
        } else plan = bulkPlan();
        if (plan == null) return new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .name("<red>No valid fusion plan</red>").build();
        List<String> lore = new ArrayList<>();
        for (int index : plan.consumedInputIndices()) {
            lore.add("<red>Consume input: level " + plan.inputs().get(index) + "</red>");
        }
        for (int output : plan.createdOutputs()) lore.add("<green>Create output: level " + output + "</green>");
        lore.add("<gray>Pairwise fusions charged: <white>" + plan.fusionCount() + "</white></gray>");
        return new ItemBuilder(Material.PAPER).name("<light_purple><b>Fusion Preview</b></light_purple>")
                .lore(lore).build();
    }

    private ItemStack forgePreview() {
        int slot = selectedSourceSlot();
        if (slot < 0) return new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .name("<red>Select one InfinityGear item</red>").build();
        var gear = plugin.getGearManager().inspect(player.getInventory().getItem(slot), true).orElse(null);
        var profile = gear == null ? null : plugin.getGearProfiles().find(gear.profileId()).orElse(null);
        if (gear == null || profile == null) return new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .name("<red>Invalid gear/profile</red>").build();
        var decision = SocketExpansionPolicy.evaluate(gear.socketCapacity(), profile.maximumExpandedSockets(), true);
        return new ItemBuilder(Material.SMITHING_TABLE).name("<gold><b>Socket Expansion Preview</b></gold>")
                .lore(List.of("<gray>Gear UUID: <white>" + gear.uuid() + "</white></gray>",
                        "<gray>Capacity: <white>" + decision.current() + " → " + decision.resulting() + "</white></gray>",
                        "<gray>Profile maximum: <white>" + decision.maximum() + "</white></gray>",
                        "<gray>Status: <white>" + decision.failure() + "</white></gray>"))
                .build();
    }

    private ItemStack viewPreview() {
        int slot = selectedSourceSlot();
        if (slot < 0) return new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .name("<red>Select gear or an enchanted book</red>").build();
        ItemStack source = player.getInventory().getItem(slot);
        var installed = managedOn(source);
        List<String> lore = new ArrayList<>();
        plugin.getGearManager().inspect(source, true).ifPresentOrElse(gear -> {
            var profile = plugin.getGearProfiles().find(gear.profileId()).orElse(null);
            lore.add("<gray>Profile: <white>" + gear.profileId() + "</white></gray>");
            lore.add("<gray>UUID: <white>" + gear.uuid() + "</white></gray>");
            lore.add("<gray>Level/mode: <white>" + gear.level() + " / "
                    + (profile == null ? "UNKNOWN" : profile.progressionMode()) + "</white></gray>");
            lore.add("<gray>Sockets: <white>" + plugin.getGearService().usedSockets(source) + " / "
                    + gear.socketCapacity() + "</white></gray>");
            if (profile != null) lore.add("<gray>Expanded maximum: <white>"
                    + profile.maximumExpandedSockets() + "</white></gray>");
        }, () -> lore.add("<gray>Source: <white>ordinary enchanted book</white></gray>"));
        lore.add("");
        if (installed.isEmpty()) lore.add("<red>No installed managed enchantments.</red>");
        for (var entry : installed) {
            boolean removable = !plugin.getConfigManager().getEnchantsConfig().getBoolean(
                    "enchants." + entry.socket().getId() + ".non-removable", false);
            lore.add("<white>" + entry.socket().getKeyString() + " " + entry.level()
                    + "</white> <dark_gray>(standard " + entry.socket().getMaxLevel()
                    + ", " + (entry.socket().isEnabled() ? "enabled" : "grandfathered disabled")
                    + ", " + (removable ? "removable" : "protected") + ")</dark_gray>");
        }
        return new ItemBuilder(Material.KNOWLEDGE_BOOK).name("<aqua><b>Installed Enchantments & Policy</b></aqua>")
                .lore(lore).build();
    }

    private String describe(PaymentOption option) {
        if (option.free()) return "<green>Free</green>";
        return "<white>" + option.id() + ": " + option.components().stream()
                .map(component -> component.amount() + " " + component.type()
                        + (component.itemId().isBlank() ? "" : " " + component.itemId()))
                .collect(java.util.stream.Collectors.joining(" + ")) + "</white>";
    }

    private ItemStack applicationPreview() {
        ItemStack gear = null, book = null;
        for (int slot : selectedPlayerSlots) {
            ItemStack live = player.getInventory().getItem(slot);
            if (GearData.isGear(live)) gear = live;
            else if (!plugin.getEnchantManager().getManagedBookEnchants(live).isEmpty()
                    || plugin.getLimitBreakManager().isLimitBreakBook(live)) book = live;
        }
        if (gear == null || book == null) return new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .name("<red>Invalid: select gear and one book</red>").build();
        if (plugin.getLimitBreakManager().isLimitBreakBook(book)) {
            return new ItemBuilder(Material.PAPER).name("<light_purple><b>LimitBreak Preview</b></light_purple>")
                    .lore(List.of("<gray>Target must already exist at its standard maximum.</gray>",
                            "<gray>Result: exactly +1, capped by the absolute maximum.</gray>",
                            "<gray>Universal books use the selected installed enchantment.</gray>"))
                    .build();
        }
        var found = plugin.getEnchantManager().getManagedBookEnchants(book);
        if (found.size() != 1) return new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .name("<red>Invalid: book needs exactly one managed enchantment</red>").build();
        var enchant = found.getFirst();
        var gearInstance = plugin.getGearManager().inspect(gear, true).orElse(null);
        if (gearInstance == null) return new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .name("<red>Invalid: malformed gear identity</red>").build();
        var bukkitEnchant = plugin.getEnchantManager().getEnchantment(enchant.socket().getKeyString());
        int current = bukkitEnchant == null ? 0 : gear.getEnchantmentLevel(bukkitEnchant);
        int used = plugin.getGearService().usedSockets(gear);
        int capacity = gearInstance.socketCapacity();
        var profile = plugin.getGearProfiles().find(gearInstance.profileId()).orElse(null);
        var override = profile == null ? null : profile.enchantmentOverrides().get(
                enchant.socket().getKeyString().toLowerCase(java.util.Locale.ROOT));
        int standard = override != null && override.standardMaximum() != null
                ? Math.min(enchant.socket().getMaxLevel(), Math.max(1, override.standardMaximum()))
                : enchant.socket().getMaxAllowedLevel(gearInstance.level());
        int absolute = override != null && override.absoluteMaximum() != null
                ? Math.max(standard, override.absoluteMaximum())
                : enchant.socket().getMaxLevel()
                + plugin.getEnchantManager().getProgressionPolicy().getMaximumLimitBreakExtraLevels();
        var validation = plugin.getGearService().validateEnchantmentApplication(gear, book,
                enchant.socket().getKeyString());
        String invalid = validation.success() ? "<green>None</green>"
                : plugin.getMessageManager().getMessage(player, "messages." + validation.messageKey());
        if (invalid == null) invalid = "<red>" + validation.messageKey() + "</red>";
        return new ItemBuilder(Material.PAPER).name("<aqua><b>Application Preview</b></aqua>")
                .lore(List.of(
                        "<gray>Gear UUID: <white>" + gearInstance.uuid() + "</white></gray>",
                        "<gray>Enchantment: <white>" + enchant.socket().getKeyString() + "</white></gray>",
                        "<gray>Level: <white>" + current + " → " + enchant.level() + "</white></gray>",
                        "<gray>Sockets: <white>" + used + " → " + (used + (current == 0 ? 1 : 0)) + " / " + capacity + "</white></gray>",
                        "<gray>Standard / absolute max: <white>" + standard + " / " + absolute + "</white></gray>",
                        "<gray>Compatibility/conflicts: <white>EcoEnchants + configured policy</white></gray>",
                        "<gray>Invalid reason: <white>" + invalid + "</white></gray>"))
                .build();
    }
}
