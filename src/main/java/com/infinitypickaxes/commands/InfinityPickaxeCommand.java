package com.infinitypickaxes.commands;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.core.enchant.EnchantSocket;
import com.infinitypickaxes.core.duplicate.DuplicateRecord;
import com.infinitypickaxes.core.duplicate.DuplicateScanResult;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import com.infinitypickaxes.gui.MainPickaxeGui;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

public class InfinityPickaxeCommand implements CommandExecutor, TabCompleter {

    private final InfinityPickaxes plugin;

    public InfinityPickaxeCommand(InfinityPickaxes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                if (!player.hasPermission("infinitypickaxes.use")) {
                    plugin.getMessageManager().sendMessage(player, "messages.no-permission");
                    return true;
                }
                InfinityPickaxe pickaxe = plugin.getPickaxeManager().getHeldPickaxe(player);
                if (pickaxe != null) {
                    new MainPickaxeGui(plugin, player, pickaxe).open();
                    return true;
                } else {
                    plugin.getMessageManager().sendMessage(player, "messages.must-hold-pickaxe");
                    return true;
                }
            }
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload" -> {
                if (!sender.hasPermission("infinitypickaxes.admin")) {
                    plugin.getMessageManager().sendMessage(sender, "messages.no-permission");
                    return true;
                }
                plugin.reloadPlugin(sender);
            }

            case "give" -> {
                if (!sender.hasPermission("infinitypickaxes.admin")) {
                    plugin.getMessageManager().sendMessage(sender, "messages.no-permission");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /" + label + " give <player> [level]");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    plugin.getMessageManager().sendMessage(sender, "messages.player-not-found");
                    return true;
                }
                int level = 0;
                if (args.length >= 3) {
                    try {
                        level = Integer.parseInt(args[2]);
                    } catch (NumberFormatException ignored) {}
                }
                ItemStack item = plugin.getPickaxeManager().createPickaxe(level);
                target.getInventory().addItem(item);

                plugin.getMessageManager().sendMessage(sender, "messages.pickaxe-given",
                        "%level%", String.valueOf(level),
                        "%player%", target.getName());
                plugin.getMessageManager().sendMessage(target, "messages.pickaxe-received",
                        "%level%", String.valueOf(level));
            }

            case "book", "limitbreak" -> {
                if (!sender.hasPermission("infinitypickaxes.admin")) {
                    plugin.getMessageManager().sendMessage(sender, "messages.no-permission");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /" + label + " book <enchant|universal> [amount] [player]");
                    return true;
                }

                String enchantArg = args[1].toLowerCase();
                int amount = 1;
                if (args.length >= 3) {
                    try {
                        amount = Math.max(1, Integer.parseInt(args[2]));
                    } catch (NumberFormatException ignored) {}
                }

                Player target = (sender instanceof Player p) ? p : null;
                if (args.length >= 4) {
                    target = Bukkit.getPlayer(args[3]);
                }

                if (target == null) {
                    plugin.getMessageManager().sendMessage(sender, "messages.player-not-found");
                    return true;
                }

                ItemStack bookItem;
                if (enchantArg.equalsIgnoreCase("universal") || enchantArg.equalsIgnoreCase("super")) {
                    bookItem = plugin.getLimitBreakManager().createUniversalBook(amount);
                    plugin.getMessageManager().sendMessage(target, "messages.limitbreak-universal-received",
                            "%amount%", String.valueOf(amount));
                    if (!target.equals(sender)) {
                        plugin.getMessageManager().sendMessage(sender, "messages.limitbreak-book-given",
                                "%amount%", String.valueOf(amount),
                                "%enchant%", "Universal Super Book",
                                "%player%", target.getName());
                    }
                } else {
                    EnchantSocket socket = plugin.getEnchantManager().getSocket(enchantArg);
                    if (socket == null) {
                        socket = plugin.getEnchantManager().getSocketByKey(enchantArg);
                    }
                    if (socket == null || !socket.isEnabled()) {
                        sender.sendMessage("§cEnchantment '" + enchantArg + "' was not found.");
                        return true;
                    }
                    bookItem = plugin.getLimitBreakManager().createSpecificBook(socket, amount);
                    plugin.getMessageManager().sendMessage(target, "messages.limitbreak-book-received",
                            "%amount%", String.valueOf(amount),
                            "%enchant%", socket.getDisplayName());
                    if (!target.equals(sender)) {
                        plugin.getMessageManager().sendMessage(sender, "messages.limitbreak-book-given",
                                "%amount%", String.valueOf(amount),
                                "%enchant%", socket.getDisplayName(),
                                "%player%", target.getName());
                    }
                }

                target.getInventory().addItem(bookItem);
            }

            case "menu", "gui" -> {
                if (!(sender instanceof Player player)) {
                    plugin.getMessageManager().sendMessage(sender, "messages.player-only");
                    return true;
                }
                if (!player.hasPermission("infinitypickaxes.use")) {
                    plugin.getMessageManager().sendMessage(player, "messages.no-permission");
                    return true;
                }
                InfinityPickaxe pickaxe = plugin.getPickaxeManager().getHeldPickaxe(player);
                if (pickaxe == null) {
                    plugin.getMessageManager().sendMessage(player, "messages.must-hold-pickaxe");
                    return true;
                }
                new MainPickaxeGui(plugin, player, pickaxe).open();
            }

            case "duplicate", "duplicates" -> handleDuplicate(sender, label, args);

            case "setlevel" -> {
                if (!sender.hasPermission("infinitypickaxes.admin")) {
                    plugin.getMessageManager().sendMessage(sender, "messages.no-permission");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /" + label + " setlevel <player> <level>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    plugin.getMessageManager().sendMessage(sender, "messages.player-not-found");
                    return true;
                }
                InfinityPickaxe pickaxe = plugin.getPickaxeManager().getHeldPickaxe(target);
                if (pickaxe == null) {
                    sender.sendMessage("§cPlayer is not holding an Infinity Pickaxe.");
                    return true;
                }
                try {
                    int targetLevel = Integer.parseInt(args[2]);
                    pickaxe.setLevel(targetLevel);
                    pickaxe.saveAndSync();
                    plugin.getMessageManager().sendMessage(sender, "messages.set-level-success",
                            "%level%", String.valueOf(targetLevel));
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cThe specified level is not a valid number.");
                }
            }

            case "addxp" -> {
                if (!sender.hasPermission("infinitypickaxes.admin")) {
                    plugin.getMessageManager().sendMessage(sender, "messages.no-permission");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /" + label + " addxp <player> <amount>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    plugin.getMessageManager().sendMessage(sender, "messages.player-not-found");
                    return true;
                }
                InfinityPickaxe pickaxe = plugin.getPickaxeManager().getHeldPickaxe(target);
                if (pickaxe == null) {
                    sender.sendMessage("§cPlayer is not holding an Infinity Pickaxe.");
                    return true;
                }
                try {
                    double amount = Double.parseDouble(args[2]);
                    plugin.getLevelManager().addXp(pickaxe, amount, target);
                    plugin.getMessageManager().sendMessage(sender, "messages.add-xp-success",
                            "%xp%", String.format("%.0f", amount),
                            "%player%", target.getName());
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cThe specified XP amount is not a valid number.");
                }
            }

            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage("§b§lInfinityPickaxes §7- Available Commands:");
        sender.sendMessage("§e/ipickaxe §7- Opens the menu for your held pickaxe.");
        if (sender.hasPermission("infinitypickaxes.admin")) {
            sender.sendMessage("§e/ipickaxe give <player> [level] §7- Gives an Infinity Pickaxe.");
            sender.sendMessage("§e/ipickaxe book <enchant|universal> [amount] [player] §7- Gives LimitBreak books.");
            sender.sendMessage("§e/ipickaxe setlevel <player> <level> §7- Sets pickaxe level.");
            sender.sendMessage("§e/ipickaxe addxp <player> <amount> §7- Adds XP to pickaxe.");
            sender.sendMessage("§e/ipickaxe reload §7- Reloads configurations and menus.");
            sender.sendMessage("§e/ipickaxe duplicate §7- Duplicate detection and quarantine administration.");
        }
        sender.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }

    private void handleDuplicate(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§e/" + label + " duplicate <list|inspect|scan|quarantine|revoke|resolve|rekey-held>");
            return;
        }

        try {
            switch (args[1].toLowerCase()) {
                case "list" -> {
                    require(sender, "infinitypickaxes.admin.duplicates.view");
                    List<DuplicateRecord> records = plugin.getDuplicateService().listRestricted();
                    sender.sendMessage("§6Restricted pickaxe UUIDs: §f" + records.size());
                    records.stream().limit(20).forEach(record -> sender.sendMessage(
                            "§8- §f" + record.uuid() + " §7[§c" + record.status() + "§7] §8" + record.reason()));
                }
                case "inspect" -> {
                    require(sender, "infinitypickaxes.admin.duplicates.view");
                    UUID uuid = requireUuid(args, 2);
                    DuplicateRecord record = plugin.getDuplicateService().find(uuid).orElse(null);
                    if (record == null) {
                        sender.sendMessage("§aThat UUID has no duplicate restriction.");
                    } else {
                        sender.sendMessage("§6UUID: §f" + record.uuid());
                        sender.sendMessage("§6Status: §f" + record.status());
                        sender.sendMessage("§6Reason: §f" + record.reason());
                        sender.sendMessage("§6Last update: §f" + record.lastUpdated());
                        sender.sendMessage("§6Replacement: §f" + (record.replacementUuid() == null ? "none" : record.replacementUuid()));
                    }
                }
                case "scan" -> {
                    require(sender, "infinitypickaxes.admin.duplicates.scan");
                    DuplicateScanResult result;
                    if (args.length >= 3 && !args[2].equalsIgnoreCase("online")) {
                        Player target = Bukkit.getPlayer(args[2]);
                        if (target == null) throw new IllegalArgumentException("Player is not online.");
                        result = plugin.getDuplicateService().scanPlayer(target, sender.getName());
                    } else {
                        result = plugin.getDuplicateService().scanOnline(sender.getName());
                    }
                    sender.sendMessage("§aScanned §f" + result.itemsScanned() + "§a pickaxes; detected §f"
                            + result.duplicatesDetected().size() + "§a compromised UUID(s).");
                }
                case "quarantine" -> {
                    require(sender, "infinitypickaxes.admin.duplicates.quarantine");
                    UUID uuid = requireUuid(args, 2);
                    plugin.getDuplicateService().quarantine(uuid, "Manual administrator quarantine", sender.getName());
                    sender.sendMessage("§eQuarantined pickaxe UUID §f" + uuid);
                }
                case "revoke" -> {
                    require(sender, "infinitypickaxes.admin.duplicates.resolve");
                    UUID uuid = requireUuid(args, 2);
                    plugin.getDuplicateService().revoke(uuid, "Manual administrator revocation", sender.getName());
                    sender.sendMessage("§cPermanently revoked pickaxe UUID §f" + uuid);
                }
                case "resolve", "rekey-held" -> {
                    require(sender, "infinitypickaxes.admin.duplicates.resolve");
                    if (!(sender instanceof Player player)) throw new IllegalArgumentException("A player must hold the canonical pickaxe.");
                    if (args[1].equalsIgnoreCase("resolve")
                            && (args.length < 3 || !args[2].equalsIgnoreCase("keep-held"))) {
                        throw new IllegalArgumentException("Use /" + label + " duplicate resolve keep-held while holding the canonical item.");
                    }
                    UUID replacement = plugin.getDuplicateService().rekeyHeld(player);
                    sender.sendMessage("§aThe held pickaxe is now canonical with UUID §f" + replacement
                            + "§a. Its previous UUID is permanently revoked.");
                }
                default -> sender.sendMessage("§cUnknown duplicate subcommand.");
            }
        } catch (SecurityException exception) {
            plugin.getMessageManager().sendMessage(sender, "messages.no-permission");
        } catch (Exception exception) {
            sender.sendMessage("§c" + exception.getMessage());
        }
    }

    private void require(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) throw new SecurityException(permission);
    }

    private UUID requireUuid(String[] args, int index) {
        if (args.length <= index) throw new IllegalArgumentException("A pickaxe UUID is required.");
        try {
            return UUID.fromString(args[index]);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid pickaxe UUID.");
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>(Arrays.asList("menu", "gui"));
            if (sender.hasPermission("infinitypickaxes.admin")) {
                list.addAll(Arrays.asList("give", "book", "reload", "setlevel", "addxp"));
                list.add("duplicate");
            }
            return list.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("duplicate")) {
            return Arrays.asList("list", "inspect", "scan", "quarantine", "revoke", "resolve", "rekey-held");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("duplicate") && args[1].equalsIgnoreCase("scan")) {
            List<String> targets = new ArrayList<>();
            targets.add("online");
            Bukkit.getOnlinePlayers().forEach(player -> targets.add(player.getName()));
            return targets;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("duplicate") && args[1].equalsIgnoreCase("resolve")) {
            return List.of("keep-held");
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("setlevel") || args[0].equalsIgnoreCase("addxp")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(p -> p.getName())
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("book") || args[0].equalsIgnoreCase("limitbreak")) {
                List<String> enchants = new ArrayList<>();
                enchants.add("universal");
                for (EnchantSocket s : plugin.getEnchantManager().getAllSockets()) {
                    if (s.isEnabled()) enchants.add(s.getId());
                }
                return enchants.stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                return Arrays.asList("0", "10", "25", "50", "100");
            }
            if (args[0].equalsIgnoreCase("book") || args[0].equalsIgnoreCase("limitbreak")) {
                return Arrays.asList("1", "5", "10", "32", "64");
            }
        }
        if (args.length == 4 && (args[0].equalsIgnoreCase("book") || args[0].equalsIgnoreCase("limitbreak"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(p -> p.getName())
                    .filter(n -> n.toLowerCase().startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
