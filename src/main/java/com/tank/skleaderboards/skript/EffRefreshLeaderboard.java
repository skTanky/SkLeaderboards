package com.tank.skleaderboards.skript;

import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.tank.skleaderboards.SkLeaderboards;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class EffRefreshLeaderboard extends Effect {

    private Expression<String> id;

    public static void register(SkriptAddon addon) {
        addon.syntaxRegistry().register(SyntaxRegistry.EFFECT,
                SyntaxInfo.builder(EffRefreshLeaderboard.class)
                        .supplier(EffRefreshLeaderboard::new)
                        .addPattern("refresh [the] leaderboard %string%")
                        .addPattern("refresh all leaderboards")
                        .build());
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed,
                        SkriptParser.ParseResult parseResult) {
        if (matchedPattern == 0) {
            id = (Expression<String>) expressions[0];
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        SkLeaderboards plugin = SkLeaderboards.instance();
        String target = id == null ? null : id.getSingle(event);
        if (id != null && target == null) {
            return;
        }

        Runnable task = () -> {
            if (target == null) {
                plugin.scheduler().refreshAllNow();
            } else {
                plugin.scheduler().refreshNow(target);
            }
        };

        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return id == null ? "refresh all leaderboards" : "refresh leaderboard " + id.toString(event, debug);
    }
}