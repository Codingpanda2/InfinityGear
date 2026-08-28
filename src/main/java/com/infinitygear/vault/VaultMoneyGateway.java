package com.infinitygear.vault;

import com.infinitygear.cost.MoneyGateway;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;

public final class VaultMoneyGateway implements MoneyGateway {
    private final Economy economy;
    public VaultMoneyGateway(Economy economy) { this.economy = economy; }
    public boolean available() { return economy != null; }
    public boolean has(OfflinePlayer player, double amount) { return economy != null && economy.has(player, amount); }
    public boolean withdraw(OfflinePlayer player, double amount) {
        return economy != null && economy.withdrawPlayer(player, amount).transactionSuccess();
    }
    public boolean deposit(OfflinePlayer player, double amount) {
        return economy != null && economy.depositPlayer(player, amount).transactionSuccess();
    }
}
