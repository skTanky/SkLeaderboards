package com.tank.skleaderboards;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

public final class RefreshScheduler {

    private final SkLeaderboards plugin;
    private final Map<String, BukkitTask> tasks = new ConcurrentHashMap<>();
    private final Set<String> warnedMismatch = ConcurrentHashMap.newKeySet();

    public RefreshScheduler(SkLeaderboards plugin) {
        this.plugin = plugin;
    }

    public void start(LeaderboardDefinition definition) {
        stop(definition.id());

        long initialDelay = 20 + Math.floorMod(definition.id().hashCode(), 60);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(
                plugin, () -> refresh(definition), initialDelay, definition.refreshTicks());
        tasks.put(definition.id(), task);
    }

    public void stop(String id) {
        BukkitTask task = tasks.remove(id);
        if (task != null) {
            task.cancel();
        }
        warnedMismatch.remove(id);
    }

    public void stopAll() {
        tasks.values().forEach(BukkitTask::cancel);
        tasks.clear();
        warnedMismatch.clear();
    }

    public List<String> renderNow(LeaderboardDefinition definition) {
        List<LeaderboardEntry> top = LeaderboardSorter.top(
                definition.source().snapshot(), definition.size(), definition.order());
        return LeaderboardRenderer.render(definition, top);
    }

    public boolean refreshNow(String id) {
        LeaderboardDefinition definition = plugin.registry().get(id);
        if (definition == null) {
            return false;
        }
        definition.source().requestFreshData();
        display(definition);
        return true;
    }

    public void refreshAllNow() {
        for (LeaderboardDefinition definition : plugin.registry().all()) {
            refreshNow(definition.id());
        }
    }

    public void redisplayAll() {
        for (LeaderboardDefinition definition : plugin.registry().all()) {
            display(definition);
        }
    }

    private void display(LeaderboardDefinition definition) {
        List<LeaderboardEntry> top = LeaderboardSorter.top(
                definition.source().snapshot(), definition.size(), definition.order());
        apply(definition, top);
    }

    private void refresh(LeaderboardDefinition definition) {
        List<LeaderboardEntry> snapshot = definition.source().snapshot();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<LeaderboardEntry> top = LeaderboardSorter.top(snapshot, definition.size(), definition.order());
            Bukkit.getScheduler().runTask(plugin, () -> apply(definition, top));
        });
    }

    private void apply(LeaderboardDefinition definition, List<LeaderboardEntry> top) {
        if (plugin.registry().get(definition.id()) != definition) {
            return;
        }

        if (!definition.source().isReady()) {
            return;
        }

        Placement placement = plugin.placements().get(definition.id());
        if (placement == null) {
            return;
        }

        List<String> lines = LeaderboardRenderer.render(definition, top);

        if (placement.host() != definition.host()) {
            migrate(definition, placement, lines);
            return;
        }

        HologramHost host = plugin.host(placement.host());
        if (host == null) {
            return;
        }

        Placement updated = host.update(placement, definition, lines);
        if (!updated.equals(placement)) {
            plugin.placements().put(updated);
        }
    }

    private void migrate(LeaderboardDefinition definition, Placement placement, List<String> lines) {
        if (Bukkit.getCurrentTick() < 200) {
            return;
        }

        HologramHost newHost = plugin.host(definition.host());
        if (newHost == null) {
            if (warnedMismatch.add(definition.id())) {
                plugin.getLogger().warning("Can't switch leaderboard '" + definition.id() + "' to the "
                        + definition.host() + " host because it isn't available. Is that plugin installed?");
            }
            return;
        }

        World world = Bukkit.getWorld(placement.world());
        if (world == null) {
            return;
        }

        HologramHost oldHost = plugin.host(placement.host());
        double y = oldHost == null
                ? placement.y()
                : convertY(placement.y(), oldHost, newHost, lines.size());

        if (oldHost != null) {
            oldHost.delete(placement);
        }

        Location location = new Location(world, placement.x(), y, placement.z());
        Placement created = newHost.create(definition, location, lines);
        plugin.placements().put(created);
        plugin.getLogger().info("Switched leaderboard '" + definition.id() + "' from "
                + placement.host() + " to " + definition.host() + ".");
    }

    private double convertY(double y, HologramHost from, HologramHost to, int lineCount) {
        double fromHeight = lineCount * from.lineHeight();
        double toHeight = lineCount * to.lineHeight();

        double centre = from.anchoredAtTop() ? y - fromHeight / 2 : y + fromHeight / 2;
        return to.anchoredAtTop() ? centre + toHeight / 2 : centre - toHeight / 2;
    }
}