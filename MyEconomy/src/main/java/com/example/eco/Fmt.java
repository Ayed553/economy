package com.example.eco;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class Fmt {
    private Fmt() {}

    public static Component c(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s)
                .decoration(TextDecoration.ITALIC, false);
    }

    public static void msg(CommandSender to, String s) { to.sendMessage(c("&6[Eco] &r" + s)); }

    /** 1500 -> 1.5K, 2000000 -> 2M, up to T. */
    public static String shortMoney(double v) {
        DecimalFormat df = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.US));
        if (v >= 1e12) return df.format(v / 1e12) + "T";
        if (v >= 1e9) return df.format(v / 1e9) + "B";
        if (v >= 1e6) return df.format(v / 1e6) + "M";
        if (v >= 1e3) return df.format(v / 1e3) + "K";
        return df.format(v);
    }

    /** Parses 500, 1.5k, 2m, 3b, 1t into cents. Throws NumberFormatException when invalid. */
    public static long parseCents(String s, boolean allowZero) {
        s = s.trim().toLowerCase(Locale.ROOT).replace(",", "");
        double mult = 1;
        if (!s.isEmpty()) {
            switch (s.charAt(s.length() - 1)) {
                case 'k' -> mult = 1e3;
                case 'm' -> mult = 1e6;
                case 'b' -> mult = 1e9;
                case 't' -> mult = 1e12;
                default -> { }
            }
        }
        if (mult != 1) s = s.substring(0, s.length() - 1);
        if (!s.matches("\\d*\\.?\\d+")) throw new NumberFormatException("bad number");
        double dollars = Double.parseDouble(s) * mult;
        if (dollars > 1e15) throw new NumberFormatException("too big");
        long cents = Math.round(dollars * 100);
        if (cents <= 0 && !allowZero) throw new NumberFormatException("must be positive");
        return cents;
    }
}
