package com.wok.capturepoints.capture;

import com.wok.capturepoints.config.CaptureConfig;
import com.wok.capturepoints.network.CaptureNetwork;
import com.wok.capturepoints.network.CaptureSnapshotPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public final class CaptureService {
    private static final Map<MinecraftServer, CaptureService> INSTANCES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final MinecraftServer server;
    private final CaptureSavedData data;
    private final Map<String, RuntimeState> runtime = new LinkedHashMap<>();
    private long ticks;

    private CaptureService(MinecraftServer server) {
        this.server = server;
        this.data = CaptureSavedData.get(server);
    }

    public static CaptureService start(MinecraftServer server) {
        synchronized (INSTANCES) {
            return INSTANCES.computeIfAbsent(server, CaptureService::new);
        }
    }

    public static Optional<CaptureService> get(MinecraftServer server) {
        if (server == null) return Optional.empty();
        return Optional.of(start(server));
    }

    public static void stop(MinecraftServer server) {
        synchronized (INSTANCES) {
            INSTANCES.remove(server);
        }
    }

    public CaptureSavedData data() {
        return data;
    }

    public boolean sequential() {
        return data.sequentialOverride() != null
                ? data.sequentialOverride() : CaptureConfig.SEQUENTIAL_CAPTURE.get();
    }

    public int defaultCaptureSeconds() {
        return data.durationOverride() > 0
                ? data.durationOverride() : CaptureConfig.DEFAULT_CAPTURE_SECONDS.get();
    }

    public void tick() {
        ticks++;
        int calculationInterval = CaptureConfig.TICK_INTERVAL.get();
        boolean calculated = ticks % calculationInterval == 0L;
        if (calculated) calculate(calculationInterval);
        if (ticks % CaptureConfig.SYNC_INTERVAL.get() == 0L) {
            syncAll();
        }
    }

    public void sync(ServerPlayer player) {
        CaptureNetwork.send(player, snapshotFor(player));
    }

    public void syncAll() {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) sync(player);
    }

    public List<CapturePointView> views() {
        List<CapturePointView> result = new ArrayList<>();
        for (CapturePoint point : data.points()) {
            RuntimeState state = runtime.getOrDefault(point.id(), RuntimeState.EMPTY);
            int seconds = point.captureSecondsOverride() > 0
                    ? point.captureSecondsOverride() : defaultCaptureSeconds();
            result.add(new CapturePointView(point.id(), point.displayName(), point.dimension(),
                    point.min(), point.max(), point.order(), seconds, point.enabled(),
                    point.control(), point.owner(), state.activeTeam(), state.bluePlayers(),
                    state.redPlayers(), state.speedMultiplier(), state.blueAllowed(),
                    state.redAllowed()));
        }
        return List.copyOf(result);
    }

    private void calculate(int elapsedTicks) {
        List<CapturePoint> points = data.points();
        Map<String, Counts> counts = new LinkedHashMap<>();
        for (CapturePoint point : points) counts.put(point.id(), new Counts());

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator() || (!CaptureConfig.COUNT_CREATIVE.get() && player.isCreative())) {
                continue;
            }
            CaptureTeam team = CaptureTeamResolver.resolve(player);
            if (team == CaptureTeam.NEUTRAL) continue;
            ResourceLocation dimension = player.level().dimension().location();
            for (CapturePoint point : points) {
                if (point.contains(dimension, player.blockPosition())) {
                    Counts pointCounts = counts.get(point.id());
                    if (team == CaptureTeam.BLUE) pointCounts.blue++;
                    if (team == CaptureTeam.RED) pointCounts.red++;
                }
            }
        }

        boolean changed = false;
        boolean sequential = sequential();
        Map<String, CaptureTeam> owners = new LinkedHashMap<>();
        for (CapturePoint point : points) owners.put(point.id(), point.owner());

        for (CapturePoint point : points) {
            Counts pointCounts = counts.get(point.id());
            boolean blueAllowed = point.enabled()
                    && (!sequential || CaptureOrderRules.allowed(
                    points, owners, point, CaptureTeam.BLUE));
            boolean redAllowed = point.enabled()
                    && (!sequential || CaptureOrderRules.allowed(
                    points, owners, point, CaptureTeam.RED));
            CaptureTeam beforeOwner = point.owner();
            double beforeControl = point.control();
            int seconds = point.captureSecondsOverride() > 0
                    ? point.captureSecondsOverride() : defaultCaptureSeconds();
            CaptureMath.Result result = point.enabled()
                    ? CaptureMath.step(beforeControl, pointCounts.blue, pointCounts.red,
                    blueAllowed, redAllowed, elapsedTicks, seconds,
                    CaptureConfig.MAX_SPEED_PLAYERS.get(),
                    CaptureConfig.CONTEST_MODE.get() == CaptureConfig.ContestMode.ADVANTAGE)
                    : new CaptureMath.Result(beforeControl, CaptureTeam.NEUTRAL, 0);
            point.setControl(result.control());
            if (Double.compare(beforeControl, result.control()) != 0) changed = true;
            runtime.put(point.id(), new RuntimeState(pointCounts.blue, pointCounts.red,
                    result.activeTeam(), result.speedMultiplier(), blueAllowed, redAllowed));
            CaptureTeam afterOwner = point.owner();
            if (beforeOwner != afterOwner) announce(point, beforeOwner, afterOwner);
        }
        runtime.keySet().removeIf(id -> data.point(id) == null);
        if (changed) data.changed();
    }

    private void announce(CapturePoint point, CaptureTeam before, CaptureTeam after) {
        if (!CaptureConfig.ANNOUNCE_CHANGES.get()) return;
        Component message = after == CaptureTeam.NEUTRAL
                ? Component.translatable("message.wok_capture_points.neutralized",
                point.displayName(), before.id())
                : Component.translatable("message.wok_capture_points.captured",
                point.displayName(), after.id());
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    private CaptureSnapshotPacket snapshotFor(ServerPlayer player) {
        String inside = data.points().stream()
                .filter(point -> point.contains(player.level().dimension().location(),
                        player.blockPosition()))
                .min(Comparator.comparingLong(CapturePoint::volume)
                        .thenComparingInt(CapturePoint::order))
                .map(CapturePoint::id).orElse(null);
        return new CaptureSnapshotPacket(views(), inside, sequential(), defaultCaptureSeconds());
    }

    private static final class Counts {
        int blue;
        int red;
    }

    private record RuntimeState(int bluePlayers, int redPlayers, CaptureTeam activeTeam,
                                int speedMultiplier, boolean blueAllowed, boolean redAllowed) {
        private static final RuntimeState EMPTY = new RuntimeState(0, 0,
                CaptureTeam.NEUTRAL, 0, true, true);
    }
}
