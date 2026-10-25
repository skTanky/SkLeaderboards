package com.tank.skleaderboards.skript;

import java.util.Locale;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import com.tank.skleaderboards.EssentialsBalanceSource;
import com.tank.skleaderboards.StatKind;
import com.tank.skleaderboards.StatisticSource;
import com.tank.skleaderboards.ValueSource;
import com.tank.skleaderboards.VariableSource;

import ch.njol.skript.Skript;

public final class ValueSourceParser {

    private ValueSourceParser() {
    }

    public static @Nullable ValueSource parse(String input) {
        String text = input.trim();
        if (text.startsWith("{")) {
            return parseVariable(text);
        }

        String keyword = text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        return switch (keyword) {
            case "kills", "player kills" -> statistic(StatKind.KILLS);
            case "deaths" -> statistic(StatKind.DEATHS);
            case "playtime", "play time", "time played" -> statistic(StatKind.PLAYTIME);
            case "balance", "money" -> balance();
            default -> {
                Skript.error("Unknown value '" + text + "'. Use a list variable like {balance::*}, "
                        + "or one of: kills, deaths, playtime, balance.");
                yield null;
            }
        };
    }

    private static @Nullable ValueSource statistic(StatKind kind) {
        if (kind.statistic() == null) {
            Skript.error("This server version has no '" + kind.label() + "' statistic.");
            return null;
        }
        return new StatisticSource(kind);
    }

    private static @Nullable ValueSource balance() {
        if (Bukkit.getPluginManager().getPlugin("Essentials") == null) {
            Skript.error("The balance source needs EssentialsX, which isn't installed.");
            return null;
        }
        return new EssentialsBalanceSource();
    }

    private static @Nullable ValueSource parseVariable(String text) {
        if (!text.endsWith("}") || text.length() < 4) {
            Skript.error("A variable value must look like {name::*}.");
            return null;
        }
        String path = text.substring(1, text.length() - 1).trim();
        if (path.startsWith("_")) {
            Skript.error("Local variables can't be used, use a global one.");
            return null;
        }
        if (path.contains("%") || path.contains("{") || path.contains("}")) {
            Skript.error("The variable name can't contain expressions.");
            return null;
        }
        if (!path.endsWith("::*")) {
            Skript.error("The value must be a list variable ending in ::*, such as {balance::*}.");
            return null;
        }
        return new VariableSource(path);
    }
}