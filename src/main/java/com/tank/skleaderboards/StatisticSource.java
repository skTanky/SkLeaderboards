package com.tank.skleaderboards;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

public final class StatisticSource implements ValueSource {

    private final StatKind kind;

    public StatisticSource(StatKind kind) {
        this.kind = kind;
    }

    @Override
    public List<LeaderboardEntry> snapshot() {
        Statistic statistic = kind.statistic();
        if (statistic == null) {
            return List.of();
        }

        Map<UUID, Long> values = SkLeaderboards.instance().statScanner().values(kind);
        for (Player player : Bukkit.getOnlinePlayers()) {
            long live = player.getStatistic(statistic);
            if (live > 0) {
                values.put(player.getUniqueId(), live);
            } else {
                values.remove(player.getUniqueId());
            }
        }

        List<LeaderboardEntry> entries = new ArrayList<>(values.size());
        values.forEach((uuid, value) -> entries.add(new LeaderboardEntry(uuid.toString(), value)));
        return entries;
    }

    @Override
    public boolean isReady() {
        return SkLeaderboards.instance().statScanner().hasScanned();
    }

    @Override
    public String describe() {
        return kind.label() + " statistic (all players, read from the stat files)";
    }

    @Override
    public String format(Number value) {
        if (kind != StatKind.PLAYTIME) {
            return ValueSource.super.format(value);
        }

        long minutes = value.longValue() / 1200;
        long hours = minutes / 60;
        long days = hours / 24;
        if (days > 0) {
            return days + "d " + (hours % 24) + "h";
        }
        if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        }
        return minutes + "m";
    }
}