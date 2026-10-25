package com.tank.skleaderboards;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class TextDisplayHost implements HologramHost {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    private final NamespacedKey key;

    public TextDisplayHost(SkLeaderboards plugin) {
        this.key = new NamespacedKey(plugin, "leaderboard");
    }

    public NamespacedKey key() {
        return key;
    }

    @Override
    public Placement create(LeaderboardDefinition definition, Location location, List<String> lines) {
        TextDisplay display = spawn(definition, location, lines);
        return new Placement(definition.id(), HostType.TEXT_DISPLAY, location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(), display.getUniqueId());
    }

    @Override
    public Placement update(Placement placement, LeaderboardDefinition definition, List<String> lines) {
        World world = Bukkit.getWorld(placement.world());
        if (world == null) {
            return placement;
        }

        TextDisplay display = find(placement);
        if (display != null) {
            apply(display, definition, lines);
            return placement;
        }

        if (!entitiesLoaded(world, placement)) {
            return placement;
        }

        Location location = new Location(world, placement.x(), placement.y(), placement.z());
        TextDisplay respawned = spawn(definition, location, lines);
        return withEntity(placement, respawned.getUniqueId());
    }

    @Override
    public Placement move(Placement placement, LeaderboardDefinition definition, Location location,
                          List<String> lines) {
        Placement moved = new Placement(placement.id(), placement.host(), location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(), placement.entityId());

        TextDisplay display = find(placement);
        if (display != null) {
            display.teleport(location);
            return moved;
        }

        TextDisplay spawned = spawn(definition, location, lines);
        return withEntity(moved, spawned.getUniqueId());
    }

    @Override
    public void delete(Placement placement) {
        TextDisplay display = find(placement);
        if (display != null) {
            display.remove();
        }
    }

    private @Nullable TextDisplay find(Placement placement) {
        if (placement.entityId() == null) {
            return null;
        }
        Entity entity = Bukkit.getEntity(placement.entityId());
        if (entity instanceof TextDisplay display && display.isValid()) {
            return display;
        }
        return null;
    }

    private boolean entitiesLoaded(World world, Placement placement) {
        int chunkX = Location.locToBlock(placement.x()) >> 4;
        int chunkZ = Location.locToBlock(placement.z()) >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return false;
        }
        return world.getChunkAt(chunkX, chunkZ).isEntitiesLoaded();
    }

    private TextDisplay spawn(LeaderboardDefinition definition, Location location, List<String> lines) {
        return location.getWorld().spawn(location, TextDisplay.class, display -> {
            display.setPersistent(true);
            display.getPersistentDataContainer().set(key, PersistentDataType.STRING, definition.id());
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setLineWidth(400);
            display.setBillboard(billboardOf(definition).toBukkit());
            display.setShadowed(shadowOf(definition));
            display.setBackgroundColor(backgroundColor(backgroundOf(definition)));
            display.text(toComponent(lines));
        });
    }

    private void apply(TextDisplay display, LeaderboardDefinition definition, List<String> lines) {
        Component wanted = toComponent(lines);
        Component current = display.text();
        String currentText = current == null ? "" : SERIALIZER.serialize(current);
        if (!currentText.equals(SERIALIZER.serialize(wanted))) {
            display.text(wanted);
        }

        if (display.getBillboard() != billboardOf(definition).toBukkit()) {
            display.setBillboard(billboardOf(definition).toBukkit());
        }
        if (display.isShadowed() != shadowOf(definition)) {
            display.setShadowed(shadowOf(definition));
        }
        Color wantedBackground = backgroundColor(backgroundOf(definition));
        if (!Objects.equals(display.getBackgroundColor(), wantedBackground)) {
            display.setBackgroundColor(wantedBackground);
        }
    }

    private boolean shadowOf(LeaderboardDefinition definition) {
        return definition.shadow() != null && definition.shadow();
    }

    private BillboardMode billboardOf(LeaderboardDefinition definition) {
        return definition.billboard() == null ? BillboardMode.VERTICAL : definition.billboard();
    }

    private BackgroundSetting backgroundOf(LeaderboardDefinition definition) {
        return definition.background() == null ? BackgroundSetting.NONE : definition.background();
    }

    private @Nullable Color backgroundColor(BackgroundSetting setting) {
        return setting.vanilla() ? null : Color.fromARGB(setting.argb());
    }

    private Component toComponent(List<String> lines) {
        String joined = String.join("\n", lines.stream().map(line -> "&r" + line).toList());
        return SERIALIZER.deserialize(joined);
    }

    private Placement withEntity(Placement placement, UUID entityId) {
        return new Placement(placement.id(), placement.host(), placement.world(),
                placement.x(), placement.y(), placement.z(), entityId);
    }
}