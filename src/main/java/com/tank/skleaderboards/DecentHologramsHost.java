package com.tank.skleaderboards;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import eu.decentsoftware.holograms.api.holograms.HologramLine;
import eu.decentsoftware.holograms.api.holograms.HologramPage;

public final class DecentHologramsHost implements HologramHost {

    private static final Pattern HEX = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final int GRACE_TICKS = 200;

    private final int startedAtTick = Bukkit.getCurrentTick();

    @Override
    public boolean anchoredAtTop() {
        return true;
    }

    @Override
    public double lineHeight() {
        return 0.3;
    }

    @Override
    public Placement create(LeaderboardDefinition definition, Location location, List<String> lines) {
        Hologram existing = DHAPI.getHologram(definition.id());
        if (existing != null) {
            Placement linked = placementAt(definition.id(), existing.getLocation());
            return update(linked, definition, lines);
        }

        spawn(definition, location, lines);
        return placementAt(definition.id(), location);
    }

    @Override
    public Placement update(Placement placement, LeaderboardDefinition definition, List<String> lines) {
        Hologram hologram = DHAPI.getHologram(placement.id());
        if (hologram == null) {
            if (Bukkit.getCurrentTick() - startedAtTick < GRACE_TICKS) {
                return placement;
            }
            World world = Bukkit.getWorld(placement.world());
            if (world == null) {
                return placement;
            }
            spawn(definition, new Location(world, placement.x(), placement.y(), placement.z()), lines);
            return placement;
        }

        List<String> wanted = toDecentLines(lines);
        if (!wanted.equals(currentLines(hologram))) {
            DHAPI.setHologramLines(hologram, wanted);
        }
        applyRange(hologram, definition);

        Location current = hologram.getLocation();
        if (current != null && current.getWorld() != null) {
            return placementAt(placement.id(), current);
        }
        return placement;
    }

    @Override
    public Placement move(Placement placement, LeaderboardDefinition definition, Location location,
                          List<String> lines) {
        Hologram hologram = DHAPI.getHologram(placement.id());
        if (hologram == null) {
            spawn(definition, location, lines);
            return placementAt(placement.id(), location);
        }

        DHAPI.moveHologram(hologram, location);
        return placementAt(placement.id(), location);
    }

    @Override
    public void delete(Placement placement) {
        if (DHAPI.getHologram(placement.id()) != null) {
            DHAPI.removeHologram(placement.id());
        }
    }

    private void spawn(LeaderboardDefinition definition, Location location, List<String> lines) {
        if (DHAPI.getHologram(definition.id()) != null) {
            return;
        }
        Hologram hologram = DHAPI.createHologram(definition.id(), location, true, toDecentLines(lines));
        applyRange(hologram, definition);
    }

    private void applyRange(Hologram hologram, LeaderboardDefinition definition) {
        Integer distance = definition.viewDistance();
        if (distance != null && hologram.getDisplayRange() != distance) {
            hologram.setDisplayRange(distance);
        }
    }

    private List<String> currentLines(Hologram hologram) {
        HologramPage page = hologram.getPage(0);
        if (page == null) {
            return List.of();
        }
        return page.getLines().stream().map(HologramLine::getContent).toList();
    }

    private List<String> toDecentLines(List<String> lines) {
        return lines.stream().map(this::toDecentLine).toList();
    }

    private String toDecentLine(String line) {
        if (line.isEmpty()) {
            return " ";
        }
        return HEX.matcher(line).replaceAll(match -> {
            StringBuilder out = new StringBuilder("&x");
            for (char digit : match.group(1).toCharArray()) {
                out.append('&').append(digit);
            }
            return Matcher.quoteReplacement(out.toString());
        });
    }

    private Placement placementAt(String id, Location location) {
        return new Placement(id, HostType.DECENT_HOLOGRAMS, location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(), null);
    }
}