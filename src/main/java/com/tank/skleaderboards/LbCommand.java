package com.tank.skleaderboards;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public final class LbCommand implements CommandExecutor, TabCompleter {

    private static final TextColor SUCCESS = TextColor.fromHexString("#4FF522");
    private static final TextColor ERROR = TextColor.fromHexString("#BF3636");
    private static final String USAGE = "Usage: /lb <create|delete|move|refresh|info> <id>, or /lb list";
    private static final List<String> ACTIONS = List.of("create", "delete", "move", "refresh", "info", "list");

    private final SkLeaderboards plugin;

    public LbCommand(SkLeaderboards plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            error(sender, USAGE);
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("list")) {
            list(sender);
            return true;
        }

        if (args.length != 2) {
            error(sender, USAGE);
            return true;
        }

        String id = args[1];
        switch (action) {
            case "create" -> create(sender, id);
            case "delete" -> delete(sender, id);
            case "move" -> move(sender, id);
            case "refresh" -> refresh(sender, id);
            case "info" -> info(sender, id);
            default -> error(sender, "Unknown action. Use create, delete, move, refresh, info or list.");
        }
        return true;
    }

    private void create(CommandSender sender, String id) {
        if (!(sender instanceof Player player)) {
            error(sender, "Only players can use this command.");
            return;
        }
        LeaderboardDefinition definition = plugin.registry().get(id);
        if (definition == null) {
            error(player, "No leaderboard called '" + id + "' is loaded. Define it in a script first.");
            return;
        }
        if (plugin.placements().get(id) != null) {
            error(player, "'" + id + "' already has a display. Use /lb move or /lb delete.");
            return;
        }
        HologramHost host = plugin.host(definition.host());
        if (host == null) {
            error(player, "The " + definition.host() + " host isn't available. Is that plugin installed?");
            return;
        }

        List<String> lines = plugin.scheduler().renderNow(definition);
        Location location = anchorFor(host, player.getLocation(), lines.size());

        Placement placement = host.create(definition, location, lines);
        plugin.placements().put(placement);
        success(player, "Created the display for '" + id + "' at your feet.");
    }

    private void delete(CommandSender sender, String id) {
        Placement placement = plugin.placements().get(id);
        if (placement == null) {
            error(sender, "'" + id + "' doesn't have a display.");
            return;
        }
        HologramHost host = plugin.host(placement.host());
        if (host != null) {
            host.delete(placement);
        }
        plugin.placements().remove(id);
        success(sender, "Deleted the display for '" + id + "'.");
    }

    private void move(CommandSender sender, String id) {
        if (!(sender instanceof Player player)) {
            error(sender, "Only players can use this command.");
            return;
        }
        Placement placement = plugin.placements().get(id);
        if (placement == null) {
            error(player, "'" + id + "' doesn't have a display. Use /lb create first.");
            return;
        }
        LeaderboardDefinition definition = plugin.registry().get(id);
        HologramHost host = plugin.host(placement.host());
        if (definition == null || host == null) {
            error(player, "'" + id + "' isn't loaded right now, so it can't be moved.");
            return;
        }

        List<String> lines = plugin.scheduler().renderNow(definition);
        Location location = anchorFor(host, player.getLocation(), lines.size());

        Placement moved = host.move(placement, definition, location, lines);
        plugin.placements().put(moved);
        success(player, "Moved the display for '" + id + "' to your feet.");
    }

    private void refresh(CommandSender sender, String id) {
        if (id.equalsIgnoreCase("all")) {
            plugin.scheduler().refreshAllNow();
            success(sender, "Refreshed every leaderboard.");
            return;
        }
        if (!plugin.scheduler().refreshNow(id)) {
            error(sender, "No leaderboard called '" + id + "' is loaded.");
            return;
        }
        if (plugin.placements().get(id) == null) {
            success(sender, "Refreshed '" + id + "', but it has no display yet. Use /lb create " + id + ".");
            return;
        }
        success(sender, "Refreshed '" + id + "'.");
    }

    private void list(CommandSender sender) {
        if (plugin.registry().all().isEmpty() && plugin.placements().all().isEmpty()) {
            plain(sender, "No leaderboards are loaded and none have a display.");
            return;
        }

        success(sender, "Leaderboards:");
        for (LeaderboardDefinition definition : plugin.registry().all()) {
            Placement placement = plugin.placements().get(definition.id());
            plain(sender, "- " + definition.id() + ": "
                    + (placement == null ? "not placed (use /lb create " + definition.id() + ")" : describe(placement)));
        }
        for (Placement placement : plugin.placements().all()) {
            if (plugin.registry().get(placement.id()) == null) {
                plain(sender, "- " + placement.id() + ": " + describe(placement) + " (script not loaded)");
            }
        }
    }

    private void info(CommandSender sender, String id) {
        LeaderboardDefinition definition = plugin.registry().get(id);
        Placement placement = plugin.placements().get(id);
        if (definition == null && placement == null) {
            error(sender, "No leaderboard called '" + id + "' is loaded or has a display.");
            return;
        }

        success(sender, "Leaderboard '" + id + "'");
        if (definition != null) {
            plain(sender, "Host: " + definition.host());
            plain(sender, "Value: " + definition.source().describe());
            plain(sender, "Shows the top " + definition.size() + ", refreshes every "
                    + LeaderboardRenderer.formatInterval(definition.refreshTicks()));
            plain(sender, "Entries right now: " + definition.source().snapshot().size());
            for (String detail : definition.source().details()) {
                plain(sender, detail);
            }
        } else {
            plain(sender, "Its script isn't loaded, so the display is paused.");
        }
        plain(sender, "Display: " + (placement == null ? "none (use /lb create " + id + ")" : describe(placement)));
    }

    private Location anchorFor(HologramHost host, Location feet, int lineCount) {
        Location location = feet.clone();
        location.setPitch(0);
        if (host.anchoredAtTop()) {
            location.add(0, lineCount * host.lineHeight(), 0);
        }
        return location;
    }

    private static String describe(Placement placement) {
        return placement.host() + " at " + placement.world() + " "
                + coordinate(placement.x()) + ", " + coordinate(placement.y()) + ", " + coordinate(placement.z());
    }

    private static String coordinate(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(ACTIONS, args[0]);
        }
        if (args.length == 2) {
            List<String> ids = switch (args[0].toLowerCase(Locale.ROOT)) {
                case "create" -> plugin.registry().all().stream()
                        .map(LeaderboardDefinition::id)
                        .filter(id -> plugin.placements().get(id) == null)
                        .toList();
                case "delete", "move" -> plugin.placements().all().stream()
                        .map(Placement::id)
                        .toList();
                case "refresh" -> Stream.concat(
                                Stream.of("all"),
                                plugin.registry().all().stream().map(LeaderboardDefinition::id))
                        .toList();
                case "info" -> Stream.concat(
                                plugin.registry().all().stream().map(LeaderboardDefinition::id),
                                plugin.placements().all().stream().map(Placement::id))
                        .distinct()
                        .toList();
                default -> List.of();
            };
            return filter(ids, args[1]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String typed) {
        String lower = typed.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower))
                .toList();
    }

    private static void success(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, SUCCESS));
    }

    private static void error(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, ERROR));
    }

    private static void plain(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.GRAY));
    }
}