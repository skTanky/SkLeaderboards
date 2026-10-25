package com.tank.skleaderboards.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Timespan;
import com.tank.skleaderboards.BackgroundSetting;
import com.tank.skleaderboards.BillboardMode;
import com.tank.skleaderboards.HostType;
import com.tank.skleaderboards.LeaderboardDefinition;
import com.tank.skleaderboards.SkLeaderboards;
import com.tank.skleaderboards.SortOrder;
import com.tank.skleaderboards.ValueSource;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.entry.KeyValueEntryData;
import org.skriptlang.skript.lang.entry.util.LiteralEntryData;
import org.skriptlang.skript.lang.structure.Structure;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StructLeaderboard extends Structure {

    private static final String[] TITLE = {"title", "header"};
    private static final String[] VALUE = {"value", "variable"};
    private static final String[] HOST = {"host", "display"};
    private static final String[] SIZE = {"size", "limit", "entries"};
    private static final String[] ORDER = {"order", "sort", "sort order"};
    private static final String[] REFRESH = {"refresh", "refresh interval", "update interval", "interval"};
    private static final String[] FORMAT = {"format", "line format"};
    private static final String[] FOOTER = {"footer", "bottom"};
    private static final String[] SHADOW = {"shadow", "text shadow"};
    private static final String[] BILLBOARD = {"billboard", "rotation", "facing"};
    private static final String[] BACKGROUND = {"background", "background colour", "background color"};

    private static final String[] RANK_WORDS = {"first", "second", "third", "fourth", "fifth",
            "sixth", "seventh", "eighth", "ninth", "tenth"};
    private static final String[] RANK_NUMBERS = {"1st", "2nd", "3rd", "4th", "5th",
            "6th", "7th", "8th", "9th", "10th"};

    private String id;
    private LeaderboardDefinition definition;

    public static void register(SkriptAddon addon) {
        EntryValidator.EntryValidatorBuilder builder = EntryValidator.builder();

        for (String key : TITLE) builder.addEntryData(quotedEntry(key));
        for (String key : VALUE) builder.addEntryData(valueEntry(key));
        for (String key : SIZE) builder.addEntryData(new LiteralEntryData<>(key, null, true, Number.class));
        for (String key : ORDER) builder.addEntryData(orderEntry(key));
        for (String key : REFRESH) builder.addEntryData(new LiteralEntryData<>(key, null, true, Timespan.class));
        for (String key : HOST) builder.addEntryData(hostEntry(key));
        for (String key : FORMAT) builder.addEntryData(quotedEntry(key));
        for (int rank = 1; rank <= RANK_WORDS.length; rank++) {
            for (String key : rankKeys(rank)) builder.addEntryData(quotedEntry(key));
        }
        for (String key : FOOTER) builder.addEntryData(quotedEntry(key));
        for (String key : SHADOW) builder.addEntryData(booleanEntry(key));
        for (String key : BILLBOARD) builder.addEntryData(billboardEntry(key));
        for (String key : BACKGROUND) builder.addEntryData(backgroundEntry(key));

        addon.syntaxRegistry().register(SyntaxRegistry.STRUCTURE,
                DefaultSyntaxInfos.Structure.builder(StructLeaderboard.class)
                        .supplier(StructLeaderboard::new)
                        .addPattern("register [a] [new] leaderboard with id %*string%")
                        .entryValidator(builder.build())
                        .build());
    }

    private static String[] rankKeys(int rank) {
        return new String[]{RANK_WORDS[rank - 1] + " format", RANK_NUMBERS[rank - 1] + " format"};
    }

    private static KeyValueEntryData<ValueSource> valueEntry(String key) {
        return new KeyValueEntryData<ValueSource>(key, null, true) {
            @Override
            protected @Nullable ValueSource getValue(String value) {
                return ValueSourceParser.parse(value);
            }
        };
    }

    private static KeyValueEntryData<SortOrder> orderEntry(String key) {
        return new KeyValueEntryData<SortOrder>(key, null, true) {
            @Override
            protected @Nullable SortOrder getValue(String value) {
                SortOrder order = SortOrder.parse(value);
                if (order == null) {
                    Skript.error("'" + key + "' must be descending (highest first) or ascending (lowest first).");
                }
                return order;
            }
        };
    }

    private static KeyValueEntryData<HostType> hostEntry(String key) {
        return new KeyValueEntryData<HostType>(key, null, true) {
            @Override
            protected @Nullable HostType getValue(String value) {
                HostType type = HostType.parse(value);
                if (type == null) {
                    Skript.error("Unknown host '" + value
                            + "'. Use: text display, fancyholograms or decentholograms.");
                }
                return type;
            }
        };
    }

    private static KeyValueEntryData<String> quotedEntry(String key) {
        return new KeyValueEntryData<String>(key, null, true) {
            @Override
            protected @Nullable String getValue(String value) {
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    return value.substring(1, value.length() - 1);
                }
                Skript.error("'" + key + "' must be written in quotes.");
                return null;
            }
        };
    }

    private static KeyValueEntryData<Boolean> booleanEntry(String key) {
        return new KeyValueEntryData<Boolean>(key, null, true) {
            @Override
            protected @Nullable Boolean getValue(String value) {
                return switch (value.trim().toLowerCase(Locale.ROOT)) {
                    case "true", "yes", "on" -> Boolean.TRUE;
                    case "false", "no", "off" -> Boolean.FALSE;
                    default -> {
                        Skript.error("'" + key + "' must be true or false.");
                        yield null;
                    }
                };
            }
        };
    }

    private static KeyValueEntryData<BillboardMode> billboardEntry(String key) {
        return new KeyValueEntryData<BillboardMode>(key, null, true) {
            @Override
            protected @Nullable BillboardMode getValue(String value) {
                BillboardMode mode = BillboardMode.parse(value);
                if (mode == null) {
                    Skript.error("'" + key + "' must be one of: fixed, vertical, horizontal, center.");
                }
                return mode;
            }
        };
    }

    private static KeyValueEntryData<BackgroundSetting> backgroundEntry(String key) {
        return new KeyValueEntryData<BackgroundSetting>(key, null, true) {
            @Override
            protected @Nullable BackgroundSetting getValue(String value) {
                String text = value.trim();
                if (text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
                    text = text.substring(1, text.length() - 1);
                }
                BackgroundSetting setting = BackgroundSetting.parse(text);
                if (setting == null) {
                    Skript.error("'" + key + "' must be none, default, \"#RRGGBB\" or \"#RRGGBBAA\" (hex colours need quotes).");
                }
                return setting;
            }
        };
    }

    private static <T> @Nullable T readAliased(EntryContainer container, Class<T> type, String[] keys) {
        for (String key : keys) {
            T found = container.getOptional(key, type, false);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private boolean usesOneNamePerSetting(EntryContainer container, String[]... groups) {
        for (String[] group : groups) {
            int used = 0;
            for (String key : group) {
                if (container.getOptional(key, Object.class, false) != null) {
                    used++;
                }
            }
            if (used > 1) {
                Skript.error("Leaderboard '" + id + "': use only one of these: " + String.join(", ", group) + ".");
                return false;
            }
        }
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult,
                        EntryContainer entryContainer) {
        id = ((Literal<String>) args[0]).getSingle();

        if (!usesOneNamePerSetting(entryContainer, TITLE, VALUE, HOST, SIZE, ORDER, REFRESH, FORMAT, FOOTER,
                SHADOW, BILLBOARD, BACKGROUND)) {
            return false;
        }
        for (int rank = 1; rank <= RANK_WORDS.length; rank++) {
            if (!usesOneNamePerSetting(entryContainer, rankKeys(rank))) {
                return false;
            }
        }

        String title = readAliased(entryContainer, String.class, TITLE);
        if (title == null) {
            Skript.error("Leaderboard '" + id + "': missing a title (use one of: " + String.join(", ", TITLE) + ").");
            return false;
        }
        ValueSource source = readAliased(entryContainer, ValueSource.class, VALUE);
        if (source == null) {
            Skript.error("Leaderboard '" + id + "': missing a value (use one of: " + String.join(", ", VALUE) + ").");
            return false;
        }

        HostType host = readAliased(entryContainer, HostType.class, HOST);
        String format = readAliased(entryContainer, String.class, FORMAT);
        String footer = readAliased(entryContainer, String.class, FOOTER);
        Number sizeValue = readAliased(entryContainer, Number.class, SIZE);
        SortOrder orderValue = readAliased(entryContainer, SortOrder.class, ORDER);
        Timespan refresh = readAliased(entryContainer, Timespan.class, REFRESH);
        Boolean shadow = readAliased(entryContainer, Boolean.class, SHADOW);
        BillboardMode billboard = readAliased(entryContainer, BillboardMode.class, BILLBOARD);
        BackgroundSetting background = readAliased(entryContainer, BackgroundSetting.class, BACKGROUND);

        Map<Integer, String> rankFormats = new HashMap<>();
        for (int rank = 1; rank <= RANK_WORDS.length; rank++) {
            String rankFormat = readAliased(entryContainer, String.class, rankKeys(rank));
            if (rankFormat != null) {
                rankFormats.put(rank, rankFormat);
            }
        }

        if (host == null) host = HostType.TEXT_DISPLAY;
        if (format == null) format = "#%position% %name% - %value%";
        SortOrder order = orderValue == null ? SortOrder.DESCENDING : orderValue;
        int size = sizeValue == null ? 10 : sizeValue.intValue();
        long refreshTicks = refresh == null ? 600 : refresh.getAs(Timespan.TimePeriod.TICK);

        if (size < 1 || size > 100) {
            Skript.error("Leaderboard '" + id + "': size must be between 1 and 100.");
            return false;
        }
        if (refreshTicks < 20) {
            Skript.error("Leaderboard '" + id + "': refresh can't be shorter than 1 second.");
            return false;
        }

        definition = new LeaderboardDefinition(id, title, source, host, size, order, refreshTicks,
                format, Map.copyOf(rankFormats), footer, shadow, billboard, background);
        return true;
    }

    @Override
    public boolean load() {
        if (!id.matches("[A-Za-z0-9_-]+")) {
            Skript.error("Leaderboard id '" + id + "' can only contain letters, numbers, underscores and dashes.");
            return false;
        }

        SkLeaderboards plugin = SkLeaderboards.instance();
        if (!plugin.registry().register(definition)) {
            Skript.error("A leaderboard with the id '" + id + "' already exists.");
            return false;
        }

        if (definition.host() == HostType.DECENT_HOLOGRAMS
                && (definition.shadow() != null || definition.billboard() != null || definition.background() != null)) {
            Skript.warning("Leaderboard '" + id + "': shadow, billboard and background aren't supported by "
                    + "DecentHolograms and are ignored.");
        }

        plugin.scheduler().start(definition);
        plugin.getLogger().info("Registered leaderboard '" + id + "' -> " + definition.source().describe());
        return true;
    }

    @Override
    public void unload() {
        SkLeaderboards plugin = SkLeaderboards.instance();
        if (plugin.registry().get(id) == definition) {
            plugin.scheduler().stop(id);
            plugin.registry().unregister(id);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "register leaderboard with id \"" + id + "\"";
    }
}