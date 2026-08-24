package com.infinitypickaxes.commands;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import com.infinitypickaxes.core.pickaxe.PickaxeData;
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
                    sender.sendMessage("§cUso: /" + label + " give <jugador> [nivel]");
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
                    sender.sendMessage("§cUso: /" + label + " setlevel <jugador> <nivel>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    plugin.getMessageManager().sendMessage(sender, "messages.player-not-found");
                    return true;
                }
                InfinityPickaxe pickaxe = plugin.getPickaxeManager().getHeldPickaxe(target);
                if (pickaxe == null) {
                    sender.sendMessage("§cEl jugador no sostiene un Infinity Pickaxe.");
                    return true;
                }
                try {
                    int targetLevel = Integer.parseInt(args[2]);
                    pickaxe.setLevel(targetLevel);
                    pickaxe.saveAndSync();
                    plugin.getMessageManager().sendMessage(sender, "messages.set-level-success",
                            "%level%", String.valueOf(targetLevel));
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cEl nivel especificado no es un número válido.");
                }
            }

            case "addxp" -> {
                if (!sender.hasPermission("infinitypickaxes.admin")) {
                    plugin.getMessageManager().sendMessage(sender, "messages.no-permission");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /" + label + " addxp <jugador> <cantidad>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    plugin.getMessageManager().sendMessage(sender, "messages.player-not-found");
                    return true;
                }
                InfinityPickaxe pickaxe = plugin.getPickaxeManager().getHeldPickaxe(target);
                if (pickaxe == null) {
                    sender.sendMessage("§cEl jugador no sostiene un Infinity Pickaxe.");
                    return true;
                }
                try {
                    double amount = Double.parseDouble(args[2]);
                    plugin.getLevelManager().addXp(pickaxe, amount, target);
                    plugin.getMessageManager().sendMessage(sender, "messages.add-xp-success",
                            "%xp%", String.format("%.0f", amount),
                            "%player%", target.getName());
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cLa cantidad de XP especificada no es un número válido.");
                }
            }

            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage("§b§lInfinityPickaxes §7- Comandos Disponibles:");
        sender.sendMessage("§e/pickaxe §7- Abre el menú del pico que sostienes.");
        if (sender.hasPermission("infinitypickaxes.admin")) {
            sender.sendMessage("§e/pickaxe give <jugador> [nivel] §7- Entrega un pico.");
            sender.sendMessage("§e/pickaxe setlevel <jugador> <nivel> §7- Modifica el nivel del pico.");
            sender.sendMessage("§e/pickaxe addxp <jugador> <xp> §7- Añade XP al pico.");
            sender.sendMessage("§e/pickaxe reload §7- Recarga configuraciones y menús.");
        }
        sender.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>(Arrays.asList("menu", "gui"));
            if (sender.hasPermission("infinitypickaxes.admin")) {
                list.addAll(Arrays.asList("give", "reload", "setlevel", "addxp"));
            }
            return list.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("setlevel") || args[0].equalsIgnoreCase("addxp"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return Arrays.asList("0", "10", "25", "50", "100");
        }
        return Collections.emptyList();
    }
}
