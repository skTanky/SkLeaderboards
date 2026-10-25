package com.tank.skleaderboards;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class StatQuitListener implements Listener {

    private final StatScanner scanner;

    public StatQuitListener(StatScanner scanner) {
        this.scanner = scanner;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        scanner.recordLive(event.getPlayer());
    }
}