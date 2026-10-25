package com.tank.skleaderboards;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record Placement(
        String id,
        HostType host,
        String world,
        double x,
        double y,
        double z,
        @Nullable UUID entityId
) {
}