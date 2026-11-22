package com.example.eco;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;

/** Lets other plugins (like the auction house) use this economy through Vault. */
@SuppressWarnings("deprecation")
public class VaultEconomy implements Economy {
    private final MyEconomy plugin;
    private final Balances b;

    public VaultEconomy(MyEconomy plugin, Balances b) { this.plugin = plugin; this.b = b; }

    private static long toCents(double d) { return Math.round(d * 100); }
    private static OfflinePlayer op(String name) { return Bukkit.getOfflinePlayer(name); }

    // ----- info -----
    @Override public boolean isEnabled() { return true; }
    @Override public String getName() { return "MyEconomy"; }
    @Override public boolean hasBankSupport() { return false; }
    @Override public int fractionalDigits() { return 2; }
    @Override public String format(double amount) { return plugin.symbol() + Fmt.shortMoney(amount); }
    @Override public String currencyNamePlural() { return plugin.getConfig().getString("currency-plural", "Dollars"); }
    @Override public String currencyNameSingular() { return plugin.getConfig().getString("currency-singular", "Dollar"); }

    // ----- accounts (created automatically on first use) -----
    @Override public boolean hasAccount(String name) { return true; }
    @Override public boolean hasAccount(OfflinePlayer p) { return true; }
    @Override public boolean hasAccount(String name, String world) { return true; }
    @Override public boolean hasAccount(OfflinePlayer p, String world) { return true; }
    @Override public boolean createPlayerAccount(String name) { b.get(op(name)); return true; }
    @Override public boolean createPlayerAccount(OfflinePlayer p) { b.get(p); return true; }
    @Override public boolean createPlayerAccount(String name, String world) { return createPlayerAccount(name); }
    @Override public boolean createPlayerAccount(OfflinePlayer p, String world) { return createPlayerAccount(p); }

    // ----- balance -----
    @Override public double getBalance(String name) { return getBalance(op(name)); }
    @Override public double getBalance(OfflinePlayer p) { return b.get(p) / 100.0; }
    @Override public double getBalance(String name, String world) { return getBalance(op(name)); }
    @Override public double getBalance(OfflinePlayer p, String world) { return getBalance(p); }

    @Override public boolean has(String name, double amount) { return has(op(name), amount); }
    @Override public boolean has(OfflinePlayer p, double amount) { return b.get(p) >= toCents(amount); }
    @Override public boolean has(String name, String world, double amount) { return has(op(name), amount); }
    @Override public boolean has(OfflinePlayer p, String world, double amount) { return has(p, amount); }

    // ----- withdraw -----
    @Override public EconomyResponse withdrawPlayer(String name, double amount) { return withdrawPlayer(op(name), amount); }
    @Override public EconomyResponse withdrawPlayer(String name, String world, double amount) { return withdrawPlayer(op(name), amount); }
    @Override public EconomyResponse withdrawPlayer(OfflinePlayer p, String world, double amount) { return withdrawPlayer(p, amount); }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer p, double amount) {
        long cur = b.get(p);
        if (Double.isNaN(amount) || amount < 0)
            return new EconomyResponse(0, cur / 100.0, ResponseType.FAILURE, "Cannot withdraw negative funds");
        long c = toCents(amount);
        if (cur < c)
            return new EconomyResponse(0, cur / 100.0, ResponseType.FAILURE, "Insufficient funds");
        b.set(p, cur - c);
        return new EconomyResponse(amount, (cur - c) / 100.0, ResponseType.SUCCESS, null);
    }

    // ----- deposit -----
    @Override public EconomyResponse depositPlayer(String name, double amount) { return depositPlayer(op(name), amount); }
    @Override public EconomyResponse depositPlayer(String name, String world, double amount) { return depositPlayer(op(name), amount); }
    @Override public EconomyResponse depositPlayer(OfflinePlayer p, String world, double amount) { return depositPlayer(p, amount); }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer p, double amount) {
        long cur = b.get(p);
        if (Double.isNaN(amount) || amount < 0)
            return new EconomyResponse(0, cur / 100.0, ResponseType.FAILURE, "Cannot deposit negative funds");
        long c = toCents(amount);
        if (c > Balances.MAX_CENTS - cur)
            return new EconomyResponse(0, cur / 100.0, ResponseType.FAILURE, "Balance limit reached");
        b.set(p, cur + c);
        return new EconomyResponse(amount, (cur + c) / 100.0, ResponseType.SUCCESS, null);
    }

    // ----- banks (not supported) -----
    private static EconomyResponse noBank() {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Banks are not supported");
    }
    @Override public EconomyResponse createBank(String name, String player) { return noBank(); }
    @Override public EconomyResponse createBank(String name, OfflinePlayer player) { return noBank(); }
    @Override public EconomyResponse deleteBank(String name) { return noBank(); }
    @Override public EconomyResponse bankBalance(String name) { return noBank(); }
    @Override public EconomyResponse bankHas(String name, double amount) { return noBank(); }
    @Override public EconomyResponse bankWithdraw(String name, double amount) { return noBank(); }
    @Override public EconomyResponse bankDeposit(String name, double amount) { return noBank(); }
    @Override public EconomyResponse isBankOwner(String name, String playerName) { return noBank(); }
    @Override public EconomyResponse isBankOwner(String name, OfflinePlayer player) { return noBank(); }
    @Override public EconomyResponse isBankMember(String name, String playerName) { return noBank(); }
    @Override public EconomyResponse isBankMember(String name, OfflinePlayer player) { return noBank(); }
    @Override public List<String> getBanks() { return List.of(); }
}
