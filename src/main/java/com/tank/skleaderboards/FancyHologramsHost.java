package com.tank.skleaderboards;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class FancyHologramsHost implements HologramHost {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    @Override
    public Placement create(LeaderboardDefinition definition, Location location, List<String> lines) {
        Optional<Hologram> existing = manager().getHologram(definition.id());
        if (existing.isPresent()) {
            Placement linked = placementAt(definition.id(), existing.get().getData().getLocation());
            return update(linked, definition, lines);
        }

        spawn(definition, location, lines);
        return placementAt(definition.id(), location);
    }

    @Override
    public Placement update(Placement placement, LeaderboardDefinition definition, List<String> lines) {
        HologramManager manager = manager();
        if (!manager.isLoaded()) {
            return placement;
        }

        Optional<Hologram> found = manager.getHologram(placement.id());
        if (found.isEmpty()) {
            World world = Bukkit.getWorld(placement.world());
            if (world == null) {
                return placement;
            }
            spawn(definition, new Location(world, placement.x(), placement.y(), placement.z()), lines);
            return placement;
        }

        Hologram hologram = found.get();
        if (hologram.getData() instanceof TextHologramData textData) {
            boolean changed = false;

            List<String> wanted = toMiniMessage(lines);
            if (!wanted.equals(textData.getText())) {
                textData.setText(wanted);
                changed = true;
            }
            if (applyAppearance(textData, definition)) {
                changed = true;
            }

            if (changed) {
                hologram.forceUpdate();
                hologram.queueUpdate();
            }
        }

        Location current = hologram.getData().getLocation();
        if (current != null && current.getWorld() != null) {
            return placementAt(placement.id(), current);
        }
        return placement;
    }

    @Override
    public Placement move(Placement placement, LeaderboardDefinition definition, Location location,
                          List<String> lines) {
        Optional<Hologram> found = manager().getHologram(placement.id());
        if (found.isEmpty()) {
            spawn(definition, location, lines);
            return placementAt(placement.id(), location);
        }

        Hologram hologram = found.get();
        hologram.getData().setLocation(location);
        hologram.forceUpdate();
        hologram.queueUpdate();
        return placementAt(placement.id(), location);
    }

    @Override
    public void delete(Placement placement) {
        HologramManager manager = manager();
        manager.getHologram(placement.id()).ifPresent(manager::removeHologram);
    }

    private HologramManager manager() {
        return FancyHologramsPlugin.get().getHologramManager();
    }

    private void spawn(LeaderboardDefinition definition, Location location, List<String> lines) {
        TextHologramData data = new TextHologramData(definition.id(), location);
        data.setText(toMiniMessage(lines));
        applyAppearance(data, definition);

        HologramManager manager = manager();
        Hologram hologram = manager.create(data);
        manager.addHologram(hologram);
    }

    private boolean applyAppearance(TextHologramData data, LeaderboardDefinition definition) {
        boolean changed = false;

        if (definition.shadow() != null && data.hasTextShadow() != definition.shadow()) {
            data.setTextShadow(definition.shadow());
            changed = true;
        }

        if (definition.billboard() != null && data.getBillboard() != definition.billboard().toBukkit()) {
            data.setBillboard(definition.billboard().toBukkit());
            changed = true;
        }

        if (definition.background() != null) {
            Color wanted = definition.background().vanilla()
                    ? null
                    : Color.fromARGB(definition.background().argb());
            if (!Objects.equals(data.getBackground(), wanted)) {
                data.setBackground(wanted);
                changed = true;
            }
        }

        if (definition.viewDistance() != null && data.getVisibilityDistance() != definition.viewDistance()) {
            data.setVisibilityDistance(definition.viewDistance());
            changed = true;
        }

        return changed;
    }

    private List<String> toMiniMessage(List<String> lines) {
        return lines.stream()
                .map(line -> "<reset>" + MINI.serialize(LEGACY.deserialize(line)))
                .toList();
    }

    private Placement placementAt(String id, Location location) {
        return new Placement(id, HostType.FANCY_HOLOGRAMS, location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(), null);
    }
}