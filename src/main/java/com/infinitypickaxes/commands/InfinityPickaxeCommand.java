package com.infinitypickaxes.commands;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.core.enchant.EnchantSocket;
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

public class InfinityPickaxeCommand implements CommandExecutor, TabCompleter {

    private final InfinityPickaxes plugin;

    public InfinityPickaxeCommand(InfinityPickaxes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
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
                ItemStack item = plugin.getPickaxeManager().createPickaxe(target.getUniqueId(), target.getName(), level);
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
                    if (socket == null) {
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
                InfinityPickaxe pickaxe = plugin.getPickaxeManager().getHeldPickaxe(player);
                if (pickaxe == null) {
                    plugin.getMessageManager().sendMessage(player, "messages.must-hold-pickaxe");
                    return true;
                }
                new MainPickaxeGui(plugin, player, pickaxe).open();
            }

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
        sender.sendMessage("§e/pickaxe §7- Opens the menu for your held pickaxe.");
        if (sender.hasPermission("infinitypickaxes.admin")) {
            sender.sendMessage("§e/pickaxe give <player> [level] §7- Gives an Infinity Pickaxe.");
            sender.sendMessage("§e/pickaxe book <enchant|universal> [amount] [player] §7- Gives LimitBreak books.");
            sender.sendMessage("§e/pickaxe setlevel <player> <level> §7- Sets pickaxe level.");
            sender.sendMessage("§e/pickaxe addxp <player> <amount> §7- Adds XP to pickaxe.");
            sender.sendMessage("§e/pickaxe reload §7- Reloads configurations and menus.");
        }
        sender.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>(Arrays.asList("menu", "gui"));
            if (sender.hasPermission("infinitypickaxes.admin")) {
                list.addAll(Arrays.asList("give", "book", "reload", "setlevel", "addxp"));
            }
            return list.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("setlevel") || args[0].equalsIgnoreCase("addxp")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("book") || args[0].equalsIgnoreCase("limitbreak")) {
                List<String> enchants = new ArrayList<>();
                enchants.add("universal");
                for (EnchantSocket s : plugin.getEnchantManager().getAllSockets()) {
                    enchants.add(s.getId());
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
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
