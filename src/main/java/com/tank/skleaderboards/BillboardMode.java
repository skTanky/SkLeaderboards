package com.tank.skleaderboards;

import org.bukkit.entity.Display;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum BillboardMode {
    FIXED(Display.Billboard.FIXED),
    VERTICAL(Display.Billboard.VERTICAL),
    HORIZONTAL(Display.Billboard.HORIZONTAL),
    CENTER(Display.Billboard.CENTER);

    private final Display.Billboard bukkit;

    BillboardMode(Display.Billboard bukkit) {
        this.bukkit = bukkit;
    }

    public Display.Billboard toBukkit() {
        return bukkit;
    }

    public static @Nullable BillboardMode parse(String input) {
        return switch (input.trim().toLowerCase(Locale.ROOT)) {
            case "fixed", "none", "off" -> FIXED;
            case "vertical" -> VERTICAL;
            case "horizontal" -> HORIZONTAL;
            case "center", "centre", "both" -> CENTER;
            default -> null;
        };
    }
}