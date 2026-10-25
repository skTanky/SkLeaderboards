package com.tank.skleaderboards;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class NameListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        NameResolver.forget(event.getPlayer().getUniqueId());
    }
}