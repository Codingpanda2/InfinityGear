package com.infinitygear.cost;

import org.bukkit.OfflinePlayer;

public interface MoneyGateway {
    boolean available();
    boolean has(OfflinePlayer player, double amount);
    boolean withdraw(OfflinePlayer player, double amount);
    boolean deposit(OfflinePlayer player, double amount);
}
