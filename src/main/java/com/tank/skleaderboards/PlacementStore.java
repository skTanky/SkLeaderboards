package com.tank.skleaderboards;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class PlacementStore {

    private final File file;
    private final Logger logger;
    private final Map<String, Placement> placements = new ConcurrentHashMap<>();

    public PlacementStore(File dataFolder, Logger logger) {
        this.file = new File(dataFolder, "placements.yml");
        this.logger = logger;
        load();
    }

    public @Nullable Placement get(String id) {
        return placements.get(id);
    }

    public Collection<Placement> all() {
        return placements.values();
    }

    public void put(Placement placement) {
        placements.put(placement.id(), placement);
        save();
    }

    public void remove(String id) {
        placements.remove(id);
        save();
    }

    private void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String id : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            HostType host = HostType.parse(section.getString("host", ""));
            String world = section.getString("world");
            if (host == null || world == null) {
                logger.warning("Skipping invalid placement '" + id + "' in placements.yml");
                continue;
            }
            UUID entityId = null;
            String entity = section.getString("entity");
            if (entity != null) {
                try {
                    entityId = UUID.fromString(entity);
                } catch (IllegalArgumentException ignored) {
                }
            }
            placements.put(id, new Placement(id, host, world,
                    section.getDouble("x"), section.getDouble("y"), section.getDouble("z"), entityId));
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Placement placement : placements.values()) {
            ConfigurationSection section = yaml.createSection(placement.id());
            section.set("host", placement.host().name());
            section.set("world", placement.world());
            section.set("x", placement.x());
            section.set("y", placement.y());
            section.set("z", placement.z());
            if (placement.entityId() != null) {
                section.set("entity", placement.entityId().toString());
            }
        }
        try {
            file.getParentFile().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            logger.severe("Could not save placements.yml: " + e.getMessage());
        }
    }
}