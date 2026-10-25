package com.tank.skleaderboards;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

public enum SortOrder {
    DESCENDING,
    ASCENDING;

    public static @Nullable SortOrder parse(String input) {
        return switch (input.trim().toLowerCase(Locale.ROOT)) {
            case "descending", "desc", "highest first", "highest", "high to low" -> DESCENDING;
            case "ascending", "asc", "lowest first", "lowest", "low to high" -> ASCENDING;
            default -> null;
        };
    }
}
