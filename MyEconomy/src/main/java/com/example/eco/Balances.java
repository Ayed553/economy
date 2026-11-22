package com.example.eco;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** Stores every balance as whole cents (a long) so big numbers stay exact. */
public class Balances {
    /** 1 quadrillion dollars, in cents. */
    public static final long MAX_CENTS = 100_000_000_000_000_000L;

    private final MyEconomy plugin;
    private final File file;
    private final Map<UUID, Long> cents = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();
    private boolean dirty;

    public Balances(MyEconomy plugin) {
        this.plugin = plugin;
        plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "balances.yml");
    }

    public synchronized void load() {
        cents.clear();
        names.clear();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection b = y.getConfigurationSection("balances");
        if (b != null) for (String k : b.getKeys(false)) {
            try { cents.put(UUID.fromString(k), b.getLong(k)); }
            catch (IllegalArgumentException ex) { plugin.getLogger().warning("Skipping bad balance key " + k); }
        }
        ConfigurationSection n = y.getConfigurationSection("names");
        if (n != null) for (String k : n.getKeys(false)) {
            try { names.put(UUID.fromString(k), n.getString(k)); }
            catch (IllegalArgumentException ignored) { }
        }
    }

    public synchronized void save() {
        if (!dirty) return;
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, Long> e : cents.entrySet()) y.set("balances." + e.getKey(), e.getValue());
        for (Map.Entry<UUID, String> e : names.entrySet()) y.set("names." + e.getKey(), e.getValue());
        try { y.save(file); dirty = false; }
        catch (IOException ex) { plugin.getLogger().severe("Could not save balances.yml: " + ex.getMessage()); }
    }

    /** Gets the balance in cents. New players get the starting balance. */
    public synchronized long get(OfflinePlayer p) {
        UUID id = p.getUniqueId();
        String name = p.getName();
        if (name != null && !name.equals(names.get(id))) { names.put(id, name); dirty = true; }
        Long v = cents.get(id);
        if (v == null) {
            v = plugin.startingCents();
            cents.put(id, v);
            dirty = true;
        }
        return v;
    }

    public synchronized void set(OfflinePlayer p, long value) {
        get(p);
        cents.put(p.getUniqueId(), Math.max(0, Math.min(MAX_CENTS, value)));
        dirty = true;
    }

    public synchronized List<Map.Entry<String, Long>> top(int n) {
        List<Map.Entry<UUID, Long>> all = new ArrayList<>(cents.entrySet());
        all.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        List<Map.Entry<String, Long>> out = new ArrayList<>();
        for (int i = 0; i < Math.min(n, all.size()); i++) {
            UUID id = all.get(i).getKey();
            String name = names.getOrDefault(id, id.toString().substring(0, 8));
            out.add(new AbstractMap.SimpleEntry<>(name, all.get(i).getValue()));
        }
        return out;
    }
}
