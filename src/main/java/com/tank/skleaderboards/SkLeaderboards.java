package com.tank.skleaderboards;

import java.util.EnumMap;
import java.util.Map;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;

import com.tank.skleaderboards.skript.EffRefreshLeaderboard;
import com.tank.skleaderboards.skript.StructLeaderboard;

import ch.njol.skript.Skript;

public final class SkLeaderboards extends JavaPlugin {

    private static SkLeaderboards instance;
    private SkriptAddon addon;
    private LeaderboardRegistry registry;
    private PlacementStore placements;
    private RefreshScheduler scheduler;
    private final Map<HostType, HologramHost> hosts = new EnumMap<>(HostType.class);
    private StatScanner statScanner;

    @Override
    public void onEnable() {
        instance = this;
        registry = new LeaderboardRegistry();
        placements = new PlacementStore(getDataFolder(), getLogger());
        scheduler = new RefreshScheduler(this);

        statScanner = new StatScanner(getLogger());
        statScanner.start();
        getServer().getPluginManager().registerEvents(new StatQuitListener(statScanner), this);
        getServer().getPluginManager().registerEvents(new NameListener(), this);

        TextDisplayHost textDisplayHost = new TextDisplayHost(this);
        hosts.put(HostType.TEXT_DISPLAY, textDisplayHost);
        getServer().getPluginManager().registerEvents(new OrphanCleaner(this, textDisplayHost.key()), this);

        if (getServer().getPluginManager().isPluginEnabled("FancyHolograms")) {
            hosts.put(HostType.FANCY_HOLOGRAMS, new FancyHologramsHost());
            getLogger().info("FancyHolograms found, the fancyholograms host is available.");
        }

        if (getServer().getPluginManager().isPluginEnabled("DecentHolograms")) {
            hosts.put(HostType.DECENT_HOLOGRAMS, new DecentHologramsHost());
            getLogger().info("DecentHolograms found, the decentholograms host is available.");
        }

        LbCommand command = new LbCommand(this);
        getCommand("lb").setExecutor(command);
        getCommand("lb").setTabCompleter(command);

        addon = Skript.instance().registerAddon(getClass(), getName());

        StructLeaderboard.register(addon);
        EffRefreshLeaderboard.register(addon);

        getLogger().info("Successfully hooked with Skript.");
    }

    @Override
    public void onDisable() {
        if (statScanner != null) {
            statScanner.stop();
        }
        if (scheduler != null) {
            scheduler.stopAll();
        }
        if (registry != null) {
            registry.clear();
        }
    }

    public static SkLeaderboards instance() {
        return instance;
    }

    public SkriptAddon addon() {
        return addon;
    }

    public LeaderboardRegistry registry() {
        return registry;
    }

    public PlacementStore placements() {
        return placements;
    }

    public RefreshScheduler scheduler() {
        return scheduler;
    }

    public @Nullable HologramHost host(HostType type) {
        return hosts.get(type);
    }

    public StatScanner statScanner() {
        return statScanner;
    }
}