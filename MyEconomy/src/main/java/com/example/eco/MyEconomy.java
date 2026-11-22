package com.example.eco;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MyEconomy extends JavaPlugin implements TabExecutor {

    private Balances balances;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        balances = new Balances(this);
        balances.load();

        getServer().getServicesManager().register(Economy.class,
                new VaultEconomy(this, balances), this, ServicePriority.Highest);

        for (String name : List.of("bal", "pay", "baltop", "eco")) {
            var cmd = getCommand(name);
            if (cmd != null) { cmd.setExecutor(this); cmd.setTabCompleter(this); }
        }

        // Save once a minute (only writes if something changed)
        getServer().getScheduler().runTaskTimer(this, balances::save, 1200L, 1200L);
        getLogger().info("MyEconomy enabled. Other plugins can now use it through Vault.");
    }

    @Override
    public void onDisable() {
        if (balances != null) balances.save();
    }

    public long startingCents() { return Math.round(getConfig().getDouble("starting-balance", 1000) * 100); }
    public String symbol() { return getConfig().getString("currency-symbol", "$"); }
    private String money(long cents) { return symbol() + Fmt.shortMoney(cents / 100.0); }

    private OfflinePlayer find(String name) {
        Player online = Bukkit.getPlayerExact(name);
        return online != null ? online : Bukkit.getOfflinePlayerIfCached(name);
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        switch (cmd.getName().toLowerCase(Locale.ROOT)) {
            case "bal" -> bal(s, a);
            case "pay" -> pay(s, a);
            case "baltop" -> baltop(s);
            case "eco" -> eco(s, a);
        }
        return true;
    }

    private void bal(CommandSender s, String[] a) {
        OfflinePlayer target;
        if (a.length > 0) {
            target = find(a[0]);
            if (target == null) { Fmt.msg(s, "&cI don't know a player called " + a[0] + "."); return; }
        } else if (s instanceof Player p) {
            target = p;
        } else { Fmt.msg(s, "&eUsage: /bal <player>"); return; }

        String who = (s instanceof Player p && p.getUniqueId().equals(target.getUniqueId())) ? "Your balance" : target.getName() + "'s balance";
        Fmt.msg(s, "&a" + who + ": &f" + money(balances.get(target)));
    }

    private void pay(CommandSender s, String[] a) {
        if (!(s instanceof Player p)) { Fmt.msg(s, "Players only."); return; }
        if (a.length < 2) { Fmt.msg(p, "&eUsage: &f/pay <player> <amount>"); return; }

        Player target = Bukkit.getPlayerExact(a[0]);
        if (target == null) { Fmt.msg(p, "&cThat player must be online."); return; }
        if (target.getUniqueId().equals(p.getUniqueId())) { Fmt.msg(p, "&cYou can't pay yourself."); return; }

        long amount;
        try { amount = Fmt.parseCents(a[1], false); }
        catch (NumberFormatException ex) { Fmt.msg(p, "&cThat's not a valid amount. Try 500, 1.5k or 2m."); return; }
        if (amount < 100) { Fmt.msg(p, "&cThe minimum payment is " + symbol() + "1."); return; }

        long mine = balances.get(p);
        long theirs = balances.get(target);
        if (mine < amount) { Fmt.msg(p, "&cYou don't have enough money."); return; }
        if (amount > Balances.MAX_CENTS - theirs) { Fmt.msg(p, "&cThat player can't hold that much money."); return; }

        balances.set(p, mine - amount);
        balances.set(target, theirs + amount);
        Fmt.msg(p, "&aYou sent &f" + money(amount) + " &ato &f" + target.getName() + "&a.");
        Fmt.msg(target, "&aYou received &f" + money(amount) + " &afrom &f" + p.getName() + "&a.");
    }

    private void baltop(CommandSender s) {
        List<Map.Entry<String, Long>> top = balances.top(10);
        Fmt.msg(s, "&e&lRichest players");
        int i = 1;
        for (Map.Entry<String, Long> e : top) {
            s.sendMessage(Fmt.c("&7" + i++ + ". &f" + e.getKey() + " &8- &a" + money(e.getValue())));
        }
        if (top.isEmpty()) s.sendMessage(Fmt.c("&7Nobody has a balance yet."));
    }

    private void eco(CommandSender s, String[] a) {
        if (!s.hasPermission("myeconomy.admin")) { Fmt.msg(s, "&cNo permission."); return; }
        if (a.length < 3) { Fmt.msg(s, "&eUsage: &f/eco <give|take|set> <player> <amount>"); return; }

        OfflinePlayer target = find(a[1]);
        if (target == null) { Fmt.msg(s, "&cI don't know a player called " + a[1] + "."); return; }

        String action = a[0].toLowerCase(Locale.ROOT);
        long amount;
        try { amount = Fmt.parseCents(a[2], action.equals("set")); }
        catch (NumberFormatException ex) { Fmt.msg(s, "&cThat's not a valid amount."); return; }

        long cur = balances.get(target);
        switch (action) {
            case "give" -> balances.set(target, Math.min(Balances.MAX_CENTS, cur + Math.min(amount, Balances.MAX_CENTS - cur)));
            case "take" -> balances.set(target, Math.max(0, cur - amount));
            case "set" -> balances.set(target, amount);
            default -> { Fmt.msg(s, "&eUsage: &f/eco <give|take|set> <player> <amount>"); return; }
        }
        Fmt.msg(s, "&a" + target.getName() + " now has &f" + money(balances.get(target)) + "&a.");
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        String name = cmd.getName().toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        if ((name.equals("bal") && a.length == 1) || (name.equals("pay") && a.length == 1)
                || (name.equals("eco") && a.length == 2)) {
            for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
        } else if (name.equals("eco") && a.length == 1) {
            out.addAll(List.of("give", "take", "set"));
        } else if ((name.equals("pay") && a.length == 2) || (name.equals("eco") && a.length == 3)) {
            out.addAll(List.of("1k", "10k", "100k", "1m"));
        }
        return out;
    }
}
