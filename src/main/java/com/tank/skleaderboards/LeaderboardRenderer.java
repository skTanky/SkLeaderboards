package com.tank.skleaderboards;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class LeaderboardRenderer {

    private static final Pattern HEX_TAG = Pattern.compile("<#([0-9a-fA-F]{6})>");

    private LeaderboardRenderer() {
    }

    public static List<String> render(LeaderboardDefinition definition, List<LeaderboardEntry> top) {
        List<String> lines = new ArrayList<>();
        addMultiline(lines, definition.title());
        lines.add("");

        for (int i = 0; i < top.size(); i++) {
            LeaderboardEntry entry = top.get(i);
            String template = definition.rankFormats().getOrDefault(i + 1, definition.format());
            lines.add(template
                    .replace("%position%", String.valueOf(i + 1))
                    .replace("%name%", NameResolver.resolve(entry.key()))
                    .replace("%value%", definition.source().format(entry.value())));
        }

        if (definition.footer() != null) {
            lines.add("");
            addMultiline(lines, definition.footer()
                    .replace("%refresh%", formatInterval(definition.refreshTicks())));
        }

        return lines.stream().map(LeaderboardRenderer::normaliseColours).toList();
    }

    private static String normaliseColours(String line) {
        return HEX_TAG.matcher(line).replaceAll("&#$1");
    }

    private static void addMultiline(List<String> lines, String text) {
        for (String part : text.split("\\\\n", -1)) {
            lines.add(part);
        }
    }

    public static String formatInterval(long ticks) {
        long seconds = ticks / 20;
        if (seconds % 3600 == 0) {
            return (seconds / 3600) + "h";
        }
        if (seconds % 60 == 0) {
            return (seconds / 60) + "m";
        }
        return seconds + "s";
    }
}