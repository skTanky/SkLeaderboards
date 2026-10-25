package com.tank.skleaderboards;

import org.bukkit.Bukkit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class NameResolver {

    private record Cached(String name, long expiresAt) {
    }

    private static final String UNKNOWN = "Unknown";
    private static final long NAME_TTL_MS = TimeUnit.MINUTES.toMillis(10);
    private static final long UNKNOWN_TTL_MS = TimeUnit.MINUTES.toMillis(1);

    private static final Map<String, Cached> CACHE = new ConcurrentHashMap<>();

    private NameResolver() {
    }

    public static String resolve(String key) {
        long now = System.currentTimeMillis();

        Cached cached = CACHE.get(key);
        if (cached != null && cached.expiresAt() > now) {
            return cached.name();
        }

        String name = lookup(key);
        long ttl = name.equals(UNKNOWN) ? UNKNOWN_TTL_MS : NAME_TTL_MS;
        CACHE.put(key, new Cached(name, now + ttl));
        return name;
    }

    public static void forget(UUID uuid) {
        CACHE.remove(uuid.toString());
    }

    public static void clearCache() {
        CACHE.clear();
    }

    private static String lookup(String key) {
        UUID uuid;
        try {
            uuid = UUID.fromString(key);
        } catch (IllegalArgumentException e) {
            return key;
        }
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name != null ? name : UNKNOWN;
    }
}