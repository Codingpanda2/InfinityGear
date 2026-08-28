package com.infinitygear.cost;

import org.bukkit.OfflinePlayer;

public final class UnavailableMoneyGateway implements MoneyGateway {
    public boolean available() { return false; }
    public boolean has(OfflinePlayer player, double amount) { return false; }
    public boolean withdraw(OfflinePlayer player, double amount) { return false; }
    public boolean deposit(OfflinePlayer player, double amount) { return false; }
}
