package com.tank.skleaderboards;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.persistence.PersistentDataType;

public final class OrphanCleaner implements Listener {

    private final SkLeaderboards plugin;
    private final NamespacedKey key;

    public OrphanCleaner(SkLeaderboards plugin, NamespacedKey key) {
        this.plugin = plugin;
        this.key = key;
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (!(entity instanceof TextDisplay display)) {
                continue;
            }
            String id = display.getPersistentDataContainer().get(key, PersistentDataType.STRING);
            if (id == null) {
                continue;
            }
            Placement placement = plugin.placements().get(id);
            if (placement == null || !display.getUniqueId().equals(placement.entityId())) {
                display.remove();
            }
        }
    }
}