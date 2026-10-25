package com.tank.skleaderboards;

import org.jetbrains.annotations.Nullable;

public enum HostType {
    TEXT_DISPLAY,
    FANCY_HOLOGRAMS,
    DECENT_HOLOGRAMS;

    public static @Nullable HostType parse(String input) {
        String normalised = input.toLowerCase().replaceAll("[\\s_-]", "");
        return switch (normalised) {
            case "textdisplay" -> TEXT_DISPLAY;
            case "fancyholograms" -> FANCY_HOLOGRAMS;
            case "decentholograms" -> DECENT_HOLOGRAMS;
            default -> null;
        };
    }
}
