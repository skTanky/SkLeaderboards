package com.tank.skleaderboards;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public interface ValueSource {

    List<LeaderboardEntry> snapshot();

    String describe();

    default boolean isReady() {
        return true;
    }

    default void requestFreshData() {
    }

    default List<String> details() {
        return List.of();
    }

    default String format(Number value) {
        return new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US)).format(value);
    }
}