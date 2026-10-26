package com.tank.skleaderboards;

import java.util.Map;

public record LeaderboardDefinition(
        String id,
        String title,
        ValueSource source,
        HostType host,
        int size,
        SortOrder order,
        long refreshTicks,
        String format,
        Map<Integer, String> rankFormats,
        String footer,
        Boolean shadow,
        BillboardMode billboard,
        BackgroundSetting background,
        Integer viewDistance
) {
}