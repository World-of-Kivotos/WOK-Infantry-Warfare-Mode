package com.wok.bodyhealth.prone;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code /wokbodyhealth prone_hitbox ...} (permission level 2): draws the placed segments with
 * dust particles for the command sender only, toggles debug logging and prints counters. Debug
 * output, not player UI, so feedback is plain literal text.
 */
public final class ProneHitboxDebug {
    private static final int DEFAULT_SECONDS = 10;
    private static final int DRAW_INTERVAL_TICKS = 2;
    private static final int POINTS_PER_EDGE = 4;
    private static final float DUST_SCALE = 0.4F;
    private static final double CHEST_LIMIT = 1.0D / 3.0D;
    private static final Vector3f HEAD = new Vector3f(1.0F, 0.1F, 0.1F);
    private static final Vector3f CHEST = new Vector3f(1.0F, 0.55F, 0.0F);
    private static final Vector3f ABDOMEN = new Vector3f(1.0F, 0.95F, 0.1F);
    private static final Vector3f ARM = new Vector3f(0.2F, 0.45F, 1.0F);
    private static final Vector3f LEG = new Vector3f(0.15F, 0.9F, 0.2F);
    private static final Vector3f CORE = new Vector3f(1.0F, 1.0F, 1.0F);
    /** Box edges as pairs of corner indices; bit 0/1/2 of a corner selects +e1/+e2/+e3. */
    private static final int[][] EDGES = {
            {0, 1}, {2, 3}, {4, 5}, {6, 7},
            {0, 2}, {1, 3}, {4, 6}, {5, 7},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}};

    /** One drawing per viewer; server thread only. */
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private record Session(UUID target, long untilTick, int rewindTicks) {
    }

    static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("wokbodyhealth")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("prone_hitbox")
                        .then(Commands.literal("show")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> show(context, DEFAULT_SECONDS, -1))
                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 120))
                                                .executes(context -> show(context,
                                                        IntegerArgumentType.getInteger(context, "seconds"), -1))
                                                .then(Commands.argument("rewindTicks",
                                                                IntegerArgumentType.integer(0, ProneHistory.CAPACITY - 1))
                                                        .executes(context -> show(context,
                                                                IntegerArgumentType.getInteger(context, "seconds"),
                                                                IntegerArgumentType.getInteger(context, "rewindTicks")))))))
                        .then(Commands.literal("log")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ProneHitboxDebug::log)))
                        .then(Commands.literal("stats")
                                .executes(ProneHitboxDebug::stats))));
    }

    private static int show(CommandContext<CommandSourceStack> context, int seconds, int rewindTicks)
            throws CommandSyntaxException {
        ServerPlayer viewer = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        long now = context.getSource().getServer().getTickCount();
        SESSIONS.put(viewer.getUUID(), new Session(target.getUUID(), now + seconds * 20L, rewindTicks));
        String snapshot = rewindTicks < 0 ? "live snapshot" : "history[" + rewindTicks + "]";
        context.getSource().sendSuccess(() -> Component.literal("Showing prone segments of "
                + target.getGameProfile().getName() + " (" + snapshot + ") for " + seconds + " s."), false);
        return 1;
    }

    private static int log(CommandContext<CommandSourceStack> context) {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        ProneHitService.setDebugOverride(enabled);
        context.getSource().sendSuccess(() -> Component.literal(
                "Prone hit debug logging " + (enabled ? "on" : "off") + " until the server stops."), true);
        return 1;
    }

    private static int stats(CommandContext<CommandSourceStack> context) {
        List<String> lines = new ArrayList<>();
        for (ProneConsumer consumer : ProneConsumer.values()) {
            lines.add(consumer + ": hit=" + ProneHitService.outcomes(consumer, ProneHitService.Outcome.HIT)
                    + " miss=" + ProneHitService.outcomes(consumer, ProneHitService.Outcome.MISS)
                    + " na=" + ProneHitService.outcomes(consumer, ProneHitService.Outcome.NOT_APPLICABLE)
                    + " failures=" + ProneHitService.outcomes(consumer, ProneHitService.Outcome.FAILURE)
                    + (ProneHitService.isDisabled(consumer) ? " (disabled)" : ""));
        }
        lines.add("occluded segments=" + ProneHitService.counter(ProneHitService.Counter.OCCLUDED_SEGMENTS)
                + " already_hit=" + ProneHitService.counter(ProneHitService.Counter.ALREADY_HIT));
        lines.add("forced miss requested=" + ProneHitService.counter(ProneHitService.Counter.FORCED_MISS_REQUESTED)
                + " consumed=" + ProneHitService.counter(ProneHitService.Counter.FORCED_MISS_CONSUMED));
        lines.add("broad phase: scoped queries=" + ProneHitService.counter(ProneHitService.Counter.SCOPED_QUERIES)
                + " prone candidates added=" + ProneHitService.counter(ProneHitService.Counter.CANDIDATES_ADDED)
                + " (never outside a find* scope)");
        lines.add("injections: tacz.head=" + ProneMixinStatus.taczHead()
                + " tacz.wrap=" + ProneMixinStatus.taczWrap()
                + " tacz.scope=" + ProneMixinStatus.taczScope()
                + " sbw.head=" + ProneMixinStatus.sbwHead()
                + " sbw.scope=" + ProneMixinStatus.sbwScope()
                + " level=" + ProneMixinStatus.level());
        lines.add("active prone targets=" + ProneHistoryTracker.hasActiveTargets()
                + " segment table loaded=" + (ProneSegmentTables.shared() != null)
                + " TAA installed=" + TaaProneBridge.installed() + " readable=" + TaaProneBridge.available());
        for (String line : lines) {
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || SESSIONS.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        long now = server.getTickCount();
        if (now % DRAW_INTERVAL_TICKS != 0) {
            return;
        }
        Iterator<Map.Entry<UUID, Session>> iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Session> entry = iterator.next();
            Session session = entry.getValue();
            ServerPlayer viewer = server.getPlayerList().getPlayer(entry.getKey());
            ServerPlayer target = server.getPlayerList().getPlayer(session.target());
            if (viewer == null || target == null || now > session.untilTick()) {
                iterator.remove();
                continue;
            }
            if (viewer.serverLevel() == target.serverLevel()) {
                draw(viewer, target, session.rewindTicks());
            }
        }
    }

    static void reset() {
        SESSIONS.clear();
        ProneHitService.setDebugOverride(null);
    }

    private static void draw(ServerPlayer viewer, ServerPlayer target, int rewindTicks) {
        ProneSample sample;
        if (rewindTicks < 0) {
            sample = ProneHistoryTracker.liveSample(target, target.level().getGameTime());
        } else {
            ProneHistory history = ProneHistoryTracker.history(target);
            if (history == null || rewindTicks >= history.size()) {
                return;
            }
            sample = history.get(rewindTicks);
        }
        ServerLevel level = viewer.serverLevel();
        dot(level, viewer, CORE, ProneGeometry.core(sample.x(), sample.y(), sample.z()));

        ProneHitSettings cfg = ProneHitService.settings();
        ProneLayouts.BodyPose pose = ProneLayouts.resolve(sample, cfg, ProneSegmentTables.shared());
        if (pose == null) {
            return;
        }
        for (WorldObb segment : ProneGeometry.place(pose, sample.x(), sample.y(), sample.z(), cfg)) {
            Vec3[] corners = segment.corners();
            for (int[] edge : EDGES) {
                for (int i = 0; i < POINTS_PER_EDGE; i++) {
                    double t = i / (double) (POINTS_PER_EDGE - 1);
                    Vec3 point = corners[edge[0]].lerp(corners[edge[1]], t);
                    dot(level, viewer, color(segment, point), point);
                }
            }
        }
    }

    /** Torso points above the chest/abdomen split (a third of the way from neck to hips) are chest. */
    private static Vector3f color(WorldObb segment, Vec3 point) {
        return switch (segment.id()) {
            case HEAD -> HEAD;
            case TORSO -> segment.torsoHalfLength() > 0.0D
                    && segment.toLocal(point).y / segment.torsoHalfLength() < CHEST_LIMIT ? CHEST : ABDOMEN;
            case RIGHT_ARM, LEFT_ARM -> ARM;
            case RIGHT_LEG, LEFT_LEG -> LEG;
        };
    }

    private static void dot(ServerLevel level, ServerPlayer viewer, Vector3f color, Vec3 point) {
        level.sendParticles(viewer, new DustParticleOptions(color, DUST_SCALE), true,
                point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private ProneHitboxDebug() {
    }
}
