package com.tank.skleaderboards;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public final class StatScanner {

    public record FileStats(long modified, long size, long[] values) {
    }

    private static final int BATCH = 200;
    private static final long BATCH_PAUSE_MS = 20;
    private static final long RESCAN_MINUTES = 5;

    private final Logger logger;
    private final List<Path> folders;
    private final Map<UUID, FileStats> cache = new ConcurrentHashMap<>();
    private final Map<String, Integer> keyToKind = new HashMap<>();

    private ScheduledExecutorService executor;
    private boolean warnedMissing;
    private boolean firstScanLogged;
    private volatile boolean scanned;

    public StatScanner(Logger logger) {
        this.logger = logger;

        World first = Bukkit.getWorlds().get(0);
        List<Path> roots = new ArrayList<>();

        Path path = first.getWorldFolder().toPath().toAbsolutePath();
        for (int depth = 0; depth < 5 && path != null; depth++) {
            roots.add(path);
            path = path.getParent();
        }

        Path container = Bukkit.getWorldContainer().toPath().toAbsolutePath();
        roots.add(container.resolve(first.getName()));
        roots.add(container.resolve("world"));

        List<Path> candidates = new ArrayList<>();
        for (Path root : roots) {
            for (Path candidate : List.of(root.resolve("players").resolve("stats"), root.resolve("stats"))) {
                if (!candidates.contains(candidate)) {
                    candidates.add(candidate);
                }
            }
        }
        this.folders = candidates;

        for (StatKind kind : StatKind.values()) {
            for (String key : kind.fileKeys()) {
                keyToKind.put(key, kind.ordinal());
            }
        }

        Path found = findFolder();
        if (found != null) {
            logger.info("Reading player stats from " + found);
        } else {
            logger.warning("No player stats folder found. Paper reports the main world folder as "
                    + first.getWorldFolder().getAbsolutePath());
        }
    }

    public void start() {
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "SkLeaderboards-StatScanner");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(this::scanSafely, 5, RESCAN_MINUTES * 60, TimeUnit.SECONDS);
    }

    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    public boolean hasScanned() {
        return scanned;
    }

    public Map<UUID, Long> values(StatKind kind) {
        int index = kind.ordinal();
        Map<UUID, Long> result = new HashMap<>();
        for (Map.Entry<UUID, FileStats> entry : cache.entrySet()) {
            long value = entry.getValue().values()[index];
            if (value > 0) {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    public void recordLive(Player player) {
        long[] values = new long[StatKind.values().length];
        for (StatKind kind : StatKind.values()) {
            Statistic statistic = kind.statistic();
            if (statistic != null) {
                values[kind.ordinal()] = player.getStatistic(statistic);
            }
        }
        cache.put(player.getUniqueId(), new FileStats(-1, -1, values));
    }

    private void scanSafely() {
        try {
            scan();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.warning("Stat scan failed: " + e.getMessage());
        }
    }

    private void scan() throws InterruptedException {
        Path folder = findFolder();
        if (folder == null) {
            if (!warnedMissing) {
                warnedMissing = true;
                logger.warning("Couldn't find the player stats folder, so offline players won't appear "
                        + "on kills, deaths or playtime boards.");
            }
            scanned = true;
            return;
        }

        long started = System.currentTimeMillis();
        int total = 0;
        int read = 0;

        try (DirectoryStream<Path> files = Files.newDirectoryStream(folder, "*.json")) {
            for (Path file : files) {
                UUID uuid = parseUuid(file);
                if (uuid == null) {
                    continue;
                }
                total++;

                BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
                long modified = attributes.lastModifiedTime().toMillis();
                long size = attributes.size();

                FileStats known = cache.get(uuid);
                if (known != null && known.modified() == modified && known.size() == size) {
                    continue;
                }

                long[] values = readValues(file);
                if (values != null) {
                    cache.put(uuid, new FileStats(modified, size, values));
                }

                read++;
                if (read % BATCH == 0) {
                    Thread.sleep(BATCH_PAUSE_MS);
                }
            }
        } catch (IOException e) {
            logger.warning("Stat scan failed: " + e.getMessage());
            scanned = true;
            return;
        }

        if (read >= 50 || !firstScanLogged) {
            firstScanLogged = true;
            logger.info("Stat scan: read " + read + " of " + total + " player files in "
                    + (System.currentTimeMillis() - started) + " ms.");
        }

        scanned = true;
        if (read > 0) {
            requestRedisplay();
        }
    }

    private void requestRedisplay() {
        SkLeaderboards plugin = SkLeaderboards.instance();
        if (plugin == null || !plugin.isEnabled()) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> plugin.scheduler().redisplayAll());
    }

    private @Nullable Path findFolder() {
        for (Path folder : folders) {
            if (Files.isDirectory(folder)) {
                return folder;
            }
        }
        return null;
    }

    private @Nullable UUID parseUuid(Path file) {
        String name = file.getFileName().toString();
        if (!name.endsWith(".json")) {
            return null;
        }
        try {
            return UUID.fromString(name.substring(0, name.length() - 5));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private @Nullable long[] readValues(Path file) {
        long[] values = new long[StatKind.values().length];
        try (JsonReader reader = new JsonReader(Files.newBufferedReader(file))) {
            reader.beginObject();
            while (reader.hasNext()) {
                if (!reader.nextName().equals("stats")) {
                    reader.skipValue();
                    continue;
                }
                reader.beginObject();
                while (reader.hasNext()) {
                    if (!reader.nextName().equals("minecraft:custom")) {
                        reader.skipValue();
                        continue;
                    }
                    reader.beginObject();
                    while (reader.hasNext()) {
                        Integer kind = keyToKind.get(reader.nextName());
                        if (kind != null && reader.peek() == JsonToken.NUMBER) {
                            values[kind] = reader.nextLong();
                        } else {
                            reader.skipValue();
                        }
                    }
                    return values;
                }
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
        return values;
    }
}