package com.tank.skleaderboards;

import ch.njol.skript.variables.Variables;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class VariableReader {
    
    private VariableReader() {
    }

    public static List<LeaderboardEntry> read(String variablePath) {
        List<LeaderboardEntry> entries = new ArrayList<>();

        Object raw = Variables.getVariable(variablePath, null, false);
        if (!(raw instanceof Map<?, ?> map)) {
            return entries;
        }

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            if (entry.getValue() instanceof Number number) {
                entries.add(new LeaderboardEntry(entry.getKey().toString(), number));
            }
        }
        return entries;
    }
}
