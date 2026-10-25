package com.tank.skleaderboards;

import org.bukkit.Statistic;
import org.jetbrains.annotations.Nullable;

public enum StatKind {
    KILLS("kills",
            new String[]{"PLAYER_KILLS"},
            new String[]{"minecraft:player_kills"}),
    DEATHS("deaths",
            new String[]{"DEATHS"},
            new String[]{"minecraft:deaths"}),
    PLAYTIME("playtime",
            new String[]{"PLAY_ONE_MINUTE", "PLAY_TIME", "TIME_PLAYED"},
            new String[]{"minecraft:play_time", "minecraft:play_one_minute"});

    private final String label;
    private final @Nullable Statistic statistic;
    private final String[] fileKeys;

    StatKind(String label, String[] statisticNames, String[] fileKeys) {
        this.label = label;
        this.fileKeys = fileKeys;
        Statistic found = null;
        for (String name : statisticNames) {
            try {
                found = Statistic.valueOf(name);
                break;
            } catch (IllegalArgumentException ignored) {
            }
        }
        this.statistic = found;
    }

    public String label() {
        return label;
    }

    public @Nullable Statistic statistic() {
        return statistic;
    }

    public String[] fileKeys() {
        return fileKeys;
    }
}