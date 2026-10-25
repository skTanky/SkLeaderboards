package com.tank.skleaderboards;

import java.util.List;

import org.bukkit.Location;

public interface HologramHost {

    Placement create(LeaderboardDefinition definition, Location location, List<String> lines);

    Placement update(Placement placement, LeaderboardDefinition definition, List<String> lines);

    Placement move(Placement placement, LeaderboardDefinition definition, Location location, List<String> lines);

    void delete(Placement placement);

    default boolean anchoredAtTop() {
        return false;
    }

    default double lineHeight() {
        return 0.25;
    }
}