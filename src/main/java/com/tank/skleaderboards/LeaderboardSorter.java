package com.tank.skleaderboards;

import java.util.Comparator;
import java.util.List;

public final class LeaderboardSorter {

    private LeaderboardSorter() {
    }

    public static List<LeaderboardEntry> top(List<LeaderboardEntry> entries, int size, SortOrder order) {
        Comparator<LeaderboardEntry> byValue = Comparator.comparingDouble(entry -> entry.value().doubleValue());
        if (order == SortOrder.DESCENDING) {
            byValue = byValue.reversed();
        }

        return entries.stream()
                .sorted(byValue.thenComparing(LeaderboardEntry::key))
                .limit(size)
                .toList();
    }
}