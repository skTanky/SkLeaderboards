package com.tank.skleaderboards;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public record BackgroundSetting(boolean vanilla, int argb) {

    public static final BackgroundSetting VANILLA = new BackgroundSetting(true, 0);
    public static final BackgroundSetting NONE = new BackgroundSetting(false, 0);

    public static @Nullable BackgroundSetting parse(String input) {
        String text = input.trim().toLowerCase(Locale.ROOT);
        if (text.equals("none") || text.equals("transparent") || text.equals("off")) {
            return NONE;
        }
        if (text.equals("default") || text.equals("vanilla")) {
            return VANILLA;
        }
        if (text.startsWith("#")) {
            text = text.substring(1);
        }
        if (text.length() != 6 && text.length() != 8) {
            return null;
        }
        try {
            long value = Long.parseLong(text, 16);
            if (text.length() == 6) {
                return new BackgroundSetting(false, 0xFF000000 | (int) value);
            }
            int rgb = (int) (value >> 8);
            int alpha = (int) (value & 0xFF);
            return new BackgroundSetting(false, (alpha << 24) | rgb);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}