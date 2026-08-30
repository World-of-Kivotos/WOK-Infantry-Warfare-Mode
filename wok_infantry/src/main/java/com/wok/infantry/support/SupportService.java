package com.wok.infantry.support;

import com.mojang.logging.LogUtils;
import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.deployment.DeploymentPhase;
import com.wok.infantry.deployment.DeploymentService;
import com.wok.infantry.server.FormationService;
import com.wok.infantry.support.adapter.ProviderAvailability;
import com.wok.infantry.support.adapter.SupportProvider;
import com.wok.infantry.support.adapter.SupportProviders;
import com.wok.infantry.support.adapter.SupportSpawnContext;
import com.wok.infantry.support.adapter.SupportSpawnException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server-authoritative commander support scheduler. Optional-mod failures are isolated behind
 * providers; no request can force chunks, choose Y, bypass deployment state, or spawn fallback
 * vanilla ordnance.
 */
public final class SupportService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_ACTIVE_MISSIONS = 8;
    private static final int MAX_EXECUTION_STEPS_PER_TICK = 4;
    // Independent of catalog size: one request may never validate an unbounded chunk rectangle.
    private static final int MAX_VALIDATED_CHUNKS = 1_024;
    private static final int MAX_REQUEST_RECEIPTS = 512;
    private static final long REQUEST_RECEIPT_TTL_TICKS = 10L * 60L * 20L;
    private static final double MAX_ABSOLUTE_COORDINATE = 29_999_000.0D;

    private static final Map<MinecraftServer, SupportService> INSTANCES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final MinecraftServer server;
    private final SupportSavedData savedData;
    private final SupportRegistry registry;
    private final LinkedHashMap<UUID, ActiveMission> activeMissions = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, RequestReceipt> requestReceipts = new LinkedHashMap<>();

    private SupportService(MinecraftServer server) {
        this(server, SupportSavedData.get(server), SupportProviders.createDefault());
    }

    SupportService(MinecraftServer server, SupportSavedData savedData,
                   SupportRegistry registry) {
        this.server = Objects.requireNonNull(server, "server");
        this.savedData = Objects.requireNonNull(savedData, "savedData");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public static void start(MinecraftServer server) {
        if (server == null) {
            return;
        }
        synchronized (INSTANCES) {
            INSTANCES.computeIfAbsent(server, SupportService::new);
        }
    }

    public static void stop(MinecraftServer server) {
        if (server == null) {
            return;
        }
        synchronized (INSTANCES) {
            INSTANCES.remove(server);
        }
    }

    public static Optional<SupportService> get(MinecraftServer server) {
        if (server == null) {
            return Optional.empty();
        }
        synchronized (INSTANCES) {
            return Optional.of(INSTANCES.computeIfAbsent(server, SupportService::new));
        }
    }

    public static Optional<SupportService> get(ServerPlayer player) {
        return player == null ? Optional.empty() : get(player.server);
    }

    public MinecraftServer server() {
        return server;
    }

    /** Atomically validates, consumes cooldown, and enqueues one idempotent support request. */
    public synchronized ActionResult requestSupport(ServerPlayer actor, UUID requestId,
                                                    ResourceLocation supportId,
                                                    SupportTarget target) {
        long now = gameTick();
        pruneReceipts(now);
        if (actor == null || actor.server != server) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                    "支援呼叫不属于当前服务器");
        }
        if (requestId == null || supportId == null || target == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "支援类型、目标或请求标识无效");
        }

        SupportDefinition definition = registry.definition(supportId).orElse(null);
        // The client submits geometry intent only. Resolve point-vs-directional semantics from
        // the server-owned definition so a point provider can never leak a forged end point into
        // an accepted mission or a snapshot.
        SupportTarget authoritativeTarget = definition != null && !definition.directional()
                ? SupportTarget.point(target.dimension(), target.startX(), target.startZ())
                : target;

        RequestReceipt prior = requestReceipts.get(requestId);
        if (prior != null) {
            if (!prior.actorId().equals(actor.getUUID())
                    || !prior.supportId().equals(supportId)
                    || !prior.target().equals(authoritativeTarget)) {
                return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                        "支援请求标识已被其他请求使用");
            }
            return prior.result();
        }

        BattleService battle = BattleService.get(server).orElse(null);
        Faction faction = battle == null ? null
                : battle.factionOf(actor.getUUID()).orElse(null);
        if (faction == null || battle.formationOf(actor.getUUID()).isEmpty()) {
            return remember(actor, requestId, supportId, authoritativeTarget, now,
                    ActionResult.failure(ActionResult.Code.NOT_ASSIGNED,
                            "只有已分配阵营与编制的玩家可以呼叫支援"));
        }
        if (!battle.isCommander(actor.getUUID())) {
            return remember(actor, requestId, supportId, authoritativeTarget, now,
                    ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                            "只有当前阵营指挥官可以呼叫支援"));
        }
        DeploymentPhase phase = DeploymentService.get(server)
                .map(service -> service.phase(actor.getUUID()))
                .orElse(DeploymentPhase.WAITING);
        if (phase != DeploymentPhase.ACTIVE
                || actor.serverLevel().dimension().equals(DeploymentService.HOLDING_LEVEL)
                || actor.serverLevel().dimension().equals(DeploymentService.LOBBY_LEVEL)) {
            return remember(actor, requestId, supportId, authoritativeTarget, now,
                    ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                            "指挥官必须已部署到战场，不能在等待区呼叫支援"));
        }

        if (definition == null) {
            return remember(actor, requestId, supportId, authoritativeTarget, now,
                    ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                            "未注册该支援能力"));
        }
        if (!FormationService.get(server)
                .map(formations -> formations.allowsSupport(actor.getUUID(), supportId))
                .orElse(false)) {
            return remember(actor, requestId, supportId, authoritativeTarget, now,
                    ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                            "当前阵营编制未开放该支援能力"));
        }
        SupportProvider provider = registry.provider(supportId).orElse(null);
        ProviderAvailability availability = provider == null
                ? ProviderAvailability.unavailable("未实现该支援适配器")
                : provider.availability();
        if (!availability.available()) {
            return remember(actor, requestId, supportId, authoritativeTarget, now,
                    ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                            availability.reason()));
        }
        // Expired unknown ids remain compatible until their cooldown elapses, then become safe
        // capacity-reclamation candidates before a newly accepted call needs a durable slot.
        savedData.pruneExpired(now);
        long readyAt = savedData.readyAt(faction, supportId);
        if (now < readyAt) {
            long seconds = Math.max(1L, (readyAt - now + 19L) / 20L);
            return remember(actor, requestId, supportId, authoritativeTarget, now,
                    ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                            "该支援尚需冷却 " + seconds + " 秒"));
        }
        if (hasActive(faction, supportId)) {
            return remember(actor, requestId, supportId, authoritativeTarget, now,
                    ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                            "该阵营已有同类支援任务正在执行"));
        }
        if (activeMissions.size() >= MAX_ACTIVE_MISSIONS) {
            return remember(actor, requestId, supportId, authoritativeTarget, now,
                    ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                            "支援调度队列已满"));
        }

        TargetValidation validation = validateTarget(actor, definition, authoritativeTarget);
        if (!validation.valid()) {
            return remember(actor, requestId, supportId, authoritativeTarget, now,
                    validation.error());
        }

        long executeAt = saturatingAdd(now, definition.inboundTicks());
        long nextReadyAt = saturatingAdd(now, definition.cooldownTicks());
        // This is the only mutation boundary: persisted cooldown and mission insertion happen
        // while holding the same service monitor, after every fallible validation completed.
        try {
            savedData.setReadyAt(faction, supportId, nextReadyAt);
        } catch (IllegalStateException capacityFailure) {
            LOGGER.warn("Rejected support request {} because cooldown storage is full",
                    requestId);
            return remember(actor, requestId, supportId, authoritativeTarget, now,
                    ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                            "支援冷却存储已满，请联系服务器管理员"));
        }
        ActiveMission mission = new ActiveMission(requestId, actor.getUUID(), faction, definition,
                authoritativeTarget, actor, executeAt, executeAt, 0, definition.stepCount(),
                validation.startSurfaceY(), validation.endSurfaceY());
        activeMissions.put(requestId, mission);
        long inboundSeconds = Math.max(1L, (definition.inboundTicks() + 19L) / 20L);
        return remember(actor, requestId, supportId, authoritativeTarget, now,
                ActionResult.ok("支援呼叫已受理，预计 " + inboundSeconds + " 秒后到达"));
    }

    /** Returns only the viewer's faction cooldowns and missions. */
    public synchronized SupportView viewFor(ServerPlayer viewer) {
        long now = gameTick();
        if (viewer == null || viewer.server != server) {
            String reason = "支援查询不属于当前服务器";
            return SupportView.unavailable(now,
                    visibleRevision(List.of(), List.of(), false, reason), reason);
        }
        Faction faction = BattleService.get(server)
                .flatMap(service -> service.factionOf(viewer.getUUID())).orElse(null);
        if (faction == null) {
            String reason = "尚未分配阵营";
            return SupportView.unavailable(now,
                    visibleRevision(List.of(), List.of(), false, reason), reason);
        }
        List<SupportOptionView> options = new ArrayList<>(registry.definitions().size());
        FormationService formations = FormationService.get(server).orElse(null);
        for (SupportDefinition definition : registry.definitions()) {
            ResourceLocation supportId = definition.id();
            if (formations == null
                    || !formations.allowsSupport(viewer.getUUID(), supportId)) {
                continue;
            }
            SupportProvider provider = registry.provider(supportId).orElse(null);
            ProviderAvailability availability = provider == null
                    ? ProviderAvailability.unavailable("未实现该支援适配器")
                    : provider.availability();
            long storedReadyAt = savedData.readyAt(faction, supportId);
            long visibleReadyAt = storedReadyAt > now ? storedReadyAt : 0L;
            options.add(new SupportOptionView(definition, availability.available(),
                    availability.reason(), visibleReadyAt,
                    hasActive(faction, supportId)));
        }
        List<SupportMissionView> missions = activeMissions.values().stream()
                .filter(mission -> mission.faction() == faction)
                .map(ActiveMission::view)
                .toList();
        if (options.isEmpty()) {
            String message = "尚未注册支援能力";
            return SupportView.empty(now,
                    visibleRevision(options, missions, true, message));
        }
        return new SupportView(options, missions, now,
                visibleRevision(options, missions, true, ""), true, "");
    }

    /** Executes due mission steps without exceeding four provider callbacks in one server tick. */
    public synchronized void tick() {
        long now = gameTick();
        pruneReceipts(now);
        int executionBudget = MAX_EXECUTION_STEPS_PER_TICK;
        Iterator<Map.Entry<UUID, ActiveMission>> iterator = activeMissions.entrySet().iterator();
        while (iterator.hasNext() && executionBudget > 0) {
            ActiveMission mission = iterator.next().getValue();
            if (mission.remainingSteps() <= 0) {
                iterator.remove();
                continue;
            }
            if (now < mission.nextStepAtGameTick()) {
                continue;
            }
            SupportProvider provider = registry.provider(mission.supportId()).orElse(null);
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION,
                    mission.target().dimension()));
            if (provider == null || level == null) {
                LOGGER.error(
                        "Support mission {} failed without cooldown refund: provider or dimension vanished",
                        mission.callId());
                iterator.remove();
                continue;
            }
            if (!footprintChunksLoaded(level, mission.definition(), mission.target())) {
                // The request boundary never force-loads chunks. Preserve that guarantee during
                // the inbound delay too: a player moving away must not make provider height
                // lookups synchronously load or generate the remote strike corridor.
                LOGGER.warn(
                        "Support mission {} cancelled without cooldown refund because its loaded footprint vanished",
                        mission.callId());
                iterator.remove();
                continue;
            }

            boolean failed = false;
            while (executionBudget > 0 && mission.remainingSteps() > 0
                    && now >= mission.nextStepAtGameTick()) {
                try {
                    provider.executeStep(new SupportSpawnContext(level,
                            mission.owner(), mission.callId(), mission.definition(),
                            mission.target(), mission.nextStepIndex()));
                    executionBudget--;
                    mission.advance();
                } catch (SupportSpawnException failure) {
                    // The provider itself logs the first circuit-breaking cause. This mission is
                    // consumed and the accepted cooldown intentionally remains in force.
                    LOGGER.error(
                            "Support mission {} failed without cooldown refund: {}",
                            mission.callId(), failure.getMessage());
                    failed = true;
                    break;
                }
            }
            if (failed || mission.remainingSteps() <= 0) {
                iterator.remove();
            }
        }
    }

    public synchronized void resetAll() {
        activeMissions.clear();
        requestReceipts.clear();
        savedData.resetAll();
    }

    private TargetValidation validateTarget(ServerPlayer actor, SupportDefinition definition,
                                            SupportTarget target) {
        if (!actor.serverLevel().dimension().location().equals(target.dimension())) {
            return TargetValidation.failure("支援目标必须位于指挥官当前维度");
        }
        if (target.dimension().equals(DeploymentService.HOLDING_LEVEL.location())
                || target.dimension().equals(DeploymentService.LOBBY_LEVEL.location())) {
            return TargetValidation.failure("内部等待维度不允许支援");
        }
        if (!validCoordinate(target.startX()) || !validCoordinate(target.startZ())
                || !validCoordinate(target.endX()) || !validCoordinate(target.endZ())) {
            return TargetValidation.failure("支援目标坐标超出可用世界范围");
        }
        if (definition.directional()) {
            if (!SupportTarget.isValidDirection(target.startX(), target.startZ(),
                    target.endX(), target.endZ())) {
                return TargetValidation.failure("方向支援长度必须介于 "
                        + (int) SupportTarget.MIN_DIRECTION_LENGTH + " 与 "
                        + (int) SupportTarget.MAX_DIRECTION_LENGTH + " 格之间");
            }
        } else if (target.length() > 0.25D) {
            return TargetValidation.failure("点目标支援不接受终点坐标");
        }

        ServerLevel level = actor.serverLevel();
        double minX = Math.min(target.startX(), target.endX()) - definition.radius();
        double minZ = Math.min(target.startZ(), target.endZ()) - definition.radius();
        double maxX = Math.max(target.startX(), target.endX()) + definition.radius();
        double maxZ = Math.max(target.startZ(), target.endZ()) + definition.radius();
        if (!validCoordinate(minX) || !validCoordinate(minZ)
                || !validCoordinate(maxX) || !validCoordinate(maxZ)) {
            return TargetValidation.failure("支援覆盖范围超出可用世界坐标");
        }
        AABB footprint = new AABB(minX, level.getMinBuildHeight(), minZ,
                Math.nextUp(maxX), level.getMaxBuildHeight(), Math.nextUp(maxZ));
        if (!level.getWorldBorder().isWithinBounds(footprint)) {
            return TargetValidation.failure("支援覆盖范围超出世界边界");
        }

        int minChunkX = SectionPos.blockToSectionCoord(Mth.floor(minX));
        int maxChunkX = SectionPos.blockToSectionCoord(Mth.floor(maxX));
        int minChunkZ = SectionPos.blockToSectionCoord(Mth.floor(minZ));
        int maxChunkZ = SectionPos.blockToSectionCoord(Mth.floor(maxZ));
        long chunkCount = (long) (maxChunkX - minChunkX + 1)
                * (maxChunkZ - minChunkZ + 1L);
        if (chunkCount < 1L || chunkCount > MAX_VALIDATED_CHUNKS) {
            return TargetValidation.failure("支援覆盖范围过大");
        }
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    return TargetValidation.failure("支援覆盖范围存在未加载区块");
                }
            }
        }
        int startY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mth.floor(target.startX()), Mth.floor(target.startZ()));
        int endY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mth.floor(target.endX()), Mth.floor(target.endZ()));
        BlockPos start = BlockPos.containing(target.startX(), startY, target.startZ());
        BlockPos end = BlockPos.containing(target.endX(), endY, target.endZ());
        if (!level.getWorldBorder().isWithinBounds(start)
                || !level.getWorldBorder().isWithinBounds(end)) {
            return TargetValidation.failure("支援目标超出世界边界");
        }
        return TargetValidation.success(level, startY, endY);
    }

    private static boolean footprintChunksLoaded(ServerLevel level,
                                                 SupportDefinition definition,
                                                 SupportTarget target) {
        double minX = Math.min(target.startX(), target.endX()) - definition.radius();
        double minZ = Math.min(target.startZ(), target.endZ()) - definition.radius();
        double maxX = Math.max(target.startX(), target.endX()) + definition.radius();
        double maxZ = Math.max(target.startZ(), target.endZ()) + definition.radius();
        int minChunkX = SectionPos.blockToSectionCoord(Mth.floor(minX));
        int maxChunkX = SectionPos.blockToSectionCoord(Mth.floor(maxX));
        int minChunkZ = SectionPos.blockToSectionCoord(Mth.floor(minZ));
        int maxChunkZ = SectionPos.blockToSectionCoord(Mth.floor(maxZ));
        long chunkCount = (long) (maxChunkX - minChunkX + 1)
                * (maxChunkZ - minChunkZ + 1L);
        if (chunkCount < 1L || chunkCount > MAX_VALIDATED_CHUNKS) {
            return false;
        }
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    return false;
                }
            }
        }
        return true;
    }

    private ActionResult remember(ServerPlayer actor, UUID requestId,
                                  ResourceLocation supportId,
                                  SupportTarget target, long now, ActionResult result) {
        requestReceipts.put(requestId, new RequestReceipt(actor.getUUID(), supportId, target,
                result, saturatingAdd(now, REQUEST_RECEIPT_TTL_TICKS)));
        while (requestReceipts.size() > MAX_REQUEST_RECEIPTS) {
            Iterator<UUID> iterator = requestReceipts.keySet().iterator();
            if (!iterator.hasNext()) {
                break;
            }
            iterator.next();
            iterator.remove();
        }
        return result;
    }

    private void pruneReceipts(long now) {
        requestReceipts.entrySet().removeIf(entry -> entry.getValue().expiresAtGameTick() <= now);
    }

    private boolean hasActive(Faction faction, ResourceLocation supportId) {
        return activeMissions.values().stream().anyMatch(mission ->
                mission.faction() == faction && mission.supportId().equals(supportId));
    }

    private long gameTick() {
        return Math.max(0L, server.overworld().getGameTime());
    }

    /** Viewer-visible fingerprint: enemy-only cooldowns and missions never participate. */
    static long visibleRevision(List<SupportOptionView> options,
                                List<SupportMissionView> missions,
                                boolean serviceAvailable, String serviceMessage) {
        long hash = 0x6A09E667F3BCC909L;
        hash = mixRevision(hash, serviceAvailable ? 1 : 0);
        hash = mixRevision(hash, Objects.requireNonNullElse(serviceMessage, "").hashCode());
        for (SupportOptionView option : options) {
            hash = mixRevision(hash, option.hashCode());
        }
        hash = mixRevision(hash, options.size());
        for (SupportMissionView mission : missions) {
            hash = mixRevision(hash, mission.hashCode());
        }
        hash = mixRevision(hash, missions.size());
        return hash & Long.MAX_VALUE;
    }

    private static long mixRevision(long current, int value) {
        long mixed = Integer.toUnsignedLong(value) + 0x9E3779B97F4A7C15L;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        return Long.rotateLeft(current ^ mixed ^ (mixed >>> 31), 17)
                * 0x9E3779B97F4A7C15L;
    }

    private static long saturatingAdd(long left, long right) {
        if (left >= Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private static boolean validCoordinate(double coordinate) {
        return Double.isFinite(coordinate)
                && Math.abs(coordinate) <= MAX_ABSOLUTE_COORDINATE;
    }

    private static ActionResult invalidTarget(String message) {
        return ActionResult.failure(ActionResult.Code.INVALID_TARGET, message);
    }

    private record RequestReceipt(UUID actorId, ResourceLocation supportId,
                                  SupportTarget target,
                                  ActionResult result, long expiresAtGameTick) {
    }

    private static final class ActiveMission {
        private final UUID callId;
        private final UUID requesterId;
        private final Faction faction;
        private final SupportDefinition definition;
        private final SupportTarget target;
        private final ServerPlayer owner;
        private final long executeAtGameTick;
        private long nextStepAtGameTick;
        private int nextStepIndex;
        private int remainingSteps;
        @SuppressWarnings("unused")
        private final int startSurfaceY;
        @SuppressWarnings("unused")
        private final int endSurfaceY;

        private ActiveMission(UUID callId, UUID requesterId, Faction faction,
                              SupportDefinition definition,
                              SupportTarget target, ServerPlayer owner,
                              long executeAtGameTick, long nextStepAtGameTick,
                              int nextStepIndex, int remainingSteps,
                              int startSurfaceY, int endSurfaceY) {
            this.callId = callId;
            this.requesterId = requesterId;
            this.faction = faction;
            this.definition = definition;
            this.target = target;
            this.owner = owner;
            this.executeAtGameTick = executeAtGameTick;
            this.nextStepAtGameTick = nextStepAtGameTick;
            this.nextStepIndex = nextStepIndex;
            this.remainingSteps = remainingSteps;
            this.startSurfaceY = startSurfaceY;
            this.endSurfaceY = endSurfaceY;
        }

        UUID callId() {
            return callId;
        }

        @SuppressWarnings("unused")
        UUID requesterId() {
            return requesterId;
        }

        Faction faction() {
            return faction;
        }

        ResourceLocation supportId() {
            return definition.id();
        }

        SupportDefinition definition() {
            return definition;
        }

        SupportTarget target() {
            return target;
        }

        ServerPlayer owner() {
            return owner;
        }

        long nextStepAtGameTick() {
            return nextStepAtGameTick;
        }

        int nextStepIndex() {
            return nextStepIndex;
        }

        int remainingSteps() {
            return remainingSteps;
        }

        void advance() {
            nextStepIndex++;
            remainingSteps--;
            nextStepAtGameTick = saturatingAdd(nextStepAtGameTick,
                    definition.stepIntervalTicks());
        }

        SupportMissionView view() {
            return new SupportMissionView(callId, definition.id(), target.dimension(),
                    target.startX(), target.startZ(), target.endX(), target.endZ(),
                    executeAtGameTick, remainingSteps);
        }
    }

    private record TargetValidation(boolean valid, ServerLevel level,
                                    int startSurfaceY, int endSurfaceY,
                                    ActionResult error) {
        static TargetValidation success(ServerLevel level, int startSurfaceY, int endSurfaceY) {
            return new TargetValidation(true, level, startSurfaceY, endSurfaceY, null);
        }

        static TargetValidation failure(String message) {
            return new TargetValidation(false, null, 0, 0, invalidTarget(message));
        }
    }
}
