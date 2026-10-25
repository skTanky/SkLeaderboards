package com.tank.skleaderboards;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LeaderboardRegistry {

    private final Map<String, LeaderboardDefinition> definitions = new ConcurrentHashMap<>();

    public boolean register(LeaderboardDefinition definition) {
        return definitions.putIfAbsent(definition.id(), definition) == null;
    }

    public void unregister(String id) {
        definitions.remove(id);
    }

    public @Nullable LeaderboardDefinition get(String id) {
        return definitions.get(id);
    }

    public Collection<LeaderboardDefinition> all() {
        return definitions.values();
    }

    public void clear() {
        definitions.clear();
    }
}