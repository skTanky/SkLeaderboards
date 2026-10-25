package com.tank.skleaderboards;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import net.essentialsx.api.v2.services.BalanceTop;

public final class EssentialsBalanceSource implements ValueSource {

    private static final long RECALCULATE_AFTER_MS = TimeUnit.MINUTES.toMillis(5);
    private static final long MANUAL_COOLDOWN_MS = TimeUnit.SECONDS.toMillis(10);

    private volatile long lastAutomaticMs;
    private volatile long lastManualMs;
    private volatile long lastFinishedMs;
    private volatile boolean calculated;
    private volatile boolean pendingManual;
    private boolean warnedMissing;

    @Override
    public List<LeaderboardEntry> snapshot() {
        BalanceTop balanceTop = service();
        if (balanceTop == null) {
            if (!warnedMissing) {
                warnedMissing = true;
                SkLeaderboards.instance().getLogger().warning(
                        "EssentialsX's balance top service wasn't found, so the balance source has no data.");
            }
            return List.of();
        }

        Map<UUID, BalanceTop.Entry> cache = balanceTop.getBalanceTopCache();

        long now = System.currentTimeMillis();
        if (isStale(balanceTop, cache.isEmpty())
                && !balanceTop.isCacheLocked()
                && now - lastAutomaticMs >= RECALCULATE_AFTER_MS) {
            lastAutomaticMs = now;
            startCalculation(balanceTop);
        }

        List<LeaderboardEntry> entries = new ArrayList<>(cache.size());
        for (BalanceTop.Entry entry : cache.values()) {
            entries.add(new LeaderboardEntry(entry.getUuid().toString(), entry.getBalance()));
        }
        return entries;
    }

    @Override
    public boolean isReady() {
        BalanceTop balanceTop = service();
        if (balanceTop == null) {
            return false;
        }
        return calculated || !balanceTop.getBalanceTopCache().isEmpty();
    }

    @Override
    public void requestFreshData() {
        BalanceTop balanceTop = service();
        if (balanceTop == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastManualMs < MANUAL_COOLDOWN_MS) {
            return;
        }
        lastManualMs = now;

        if (balanceTop.isCacheLocked()) {
            pendingManual = true;
            return;
        }
        startCalculation(balanceTop);
    }

    @Override
    public String describe() {
        return "balance (EssentialsX balance top, all players)";
    }

    @Override
    public List<String> details() {
        BalanceTop balanceTop = service();
        if (balanceTop == null) {
            return List.of("EssentialsX balance top service: not found");
        }

        return List.of(
                "EssentialsX cache: " + balanceTop.getBalanceTopCache().size() + " players, total "
                        + format(balanceTop.getBalanceTopTotal())
                        + ", recalculating now: " + (balanceTop.isCacheLocked() ? "yes" : "no"),
                "Cache age value from EssentialsX: " + balanceTop.getCacheAge(),
                "Last automatic request: " + ago(lastAutomaticMs)
                        + ", last manual: " + ago(lastManualMs)
                        + ", last finished: " + ago(lastFinishedMs));
    }

    private @Nullable BalanceTop service() {
        return Bukkit.getServicesManager().load(BalanceTop.class);
    }

    private boolean isStale(BalanceTop balanceTop, boolean empty) {
        long cacheAge = balanceTop.getCacheAge();
        long ageMs = cacheAge > 1_000_000_000_000L ? System.currentTimeMillis() - cacheAge : cacheAge;
        return empty || cacheAge <= 0 || ageMs > RECALCULATE_AFTER_MS;
    }

    private void startCalculation(BalanceTop balanceTop) {
        CompletableFuture<Void> future = balanceTop.calculateBalanceTopMapAsync();
        if (future != null) {
            future.thenRun(this::onCalculated);
        }
    }

    private void onCalculated() {
        lastFinishedMs = System.currentTimeMillis();
        calculated = true;

        SkLeaderboards plugin = SkLeaderboards.instance();
        if (plugin == null || !plugin.isEnabled()) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (pendingManual) {
                pendingManual = false;
                BalanceTop balanceTop = service();
                if (balanceTop != null && !balanceTop.isCacheLocked()) {
                    startCalculation(balanceTop);
                }
            }
            plugin.scheduler().redisplayAll();
        });
    }

    private static String ago(long timestampMs) {
        if (timestampMs == 0) {
            return "never";
        }
        return ((System.currentTimeMillis() - timestampMs) / 1000) + "s ago";
    }
}