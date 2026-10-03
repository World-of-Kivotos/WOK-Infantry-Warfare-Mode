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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
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
 *
 * <p>Every mission leaves the active table through {@code finishMission}. A cooldown consumed
 * at acceptance is returned only while no mission step has completed: either because the
 * provider reported {@link SupportSpawnException#refundCooldown()} or because the core itself
 * cancelled the mission. The refund is a compare-and-set, so an administrator who changed the
 * cooldown in the meantime keeps the last word.</p>
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
    // Keyed by the server-generated call id; client request ids only key receipts.
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
        // Providers may reuse the call id as an entity UUID, so it must never be client-chosen.
        UUID callId = UUID.randomUUID();
        while (activeMissions.containsKey(callId)) {
            callId = UUID.randomUUID();
        }
        ActiveMission mission = new ActiveMission(callId, actor.getUUID(), faction, definition,
                authoritativeTarget, actor, savedData.readyAt(faction, supportId), executeAt,
                new MissionCursor(executeAt, 0, definition.stepCount(),
                        definition.stepIntervalTicks()),
                validation.startSurfaceY(), validation.endSurfaceY());
        activeMissions.put(callId, mission);
        ActionResult accepted = definition.inboundTicks() == 0L
                ? ActionResult.ok("支援呼叫已受理，立即执行")
                : ActionResult.ok("支援呼叫已受理，预计 "
                + Math.max(1L, (definition.inboundTicks() + 19L) / 20L) + " 秒后到达");
        return remember(actor, requestId, supportId, authoritativeTarget, now, accepted);
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
        // Snapshot: a provider callback may re-enter this monitor (for example a battle reset)
        // and mutate the live table while one mission is executing.
        for (ActiveMission mission : List.copyOf(activeMissions.values())) {
            if (executionBudget <= 0) {
                break;
            }
            if (activeMissions.get(mission.callId()) != mission) {
                continue;
            }
            try {
                executionBudget -= tickMission(mission, now, executionBudget);
            } catch (RuntimeException | LinkageError failure) {
                // Never let a scheduler or context fault escape into the server tick.
                LOGGER.error("Support mission {} ({}) crashed outside its provider boundary",
                        mission.callId(), mission.supportId(), failure);
                finishMission(mission, MissionEnd.crashed("支援任务执行异常"));
            }
        }
    }

    public synchronized void resetAll() {
        activeMissions.clear();
        requestReceipts.clear();
        savedData.resetAll();
    }

    /** Administrative API: clears one durable faction cooldown without cancelling a mission. */
    public synchronized boolean clearCooldown(Faction faction, ResourceLocation supportId) {
        Objects.requireNonNull(faction, "faction");
        Objects.requireNonNull(supportId, "supportId");
        return savedData.setReadyAt(faction, supportId, 0L);
    }

    /** Stable IDs exposed only for command completion and administrative diagnostics. */
    public synchronized List<ResourceLocation> registeredSupportIds() {
        return registry.definitions().stream().map(SupportDefinition::id).toList();
    }

    /** Runs one due mission and returns the number of provider callbacks it consumed. */
    private int tickMission(ActiveMission mission, long now, int budget) {
        MissionCursor cursor = mission.cursor();
        if (cursor.remainingSteps() <= 0) {
            finishMission(mission, MissionEnd.COMPLETED);
            return 0;
        }
        if (now < cursor.nextStepAtGameTick()) {
            return 0;
        }
        SupportProvider provider = registry.provider(mission.supportId()).orElse(null);
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION,
                mission.target().dimension()));
        Faction currentFaction = BattleService.get(server)
                .flatMap(battle -> battle.factionOf(mission.requesterId()))
                .orElse(null);
        if (currentFaction != mission.faction()) {
            cancelMission(mission, "呼叫者已不在受理阵营", provider, level);
            return 0;
        }
        if (provider == null) {
            cancelMission(mission, "未实现该支援适配器", null, level);
            return 0;
        }
        if (level == null) {
            cancelMission(mission, "支援目标维度已不可用", provider, null);
            return 0;
        }
        // The request boundary never force-loads chunks. Preserve that guarantee during the
        // inbound delay too: a player moving away must not make provider height lookups
        // synchronously load or generate the remote strike corridor.
        String footprintProblem = executionFootprintProblem(level, mission.definition(),
                mission.target());
        if (footprintProblem != null) {
            cancelMission(mission, footprintProblem, provider, level);
            return 0;
        }
        ProviderAvailability availability = provider.availability();
        if (!availability.available()) {
            cancelMission(mission, availability.reason(), provider, level);
            return 0;
        }

        Entity owner = currentOwner(mission);
        StepRun run = runDueSteps(cursor, now, budget, stepIndex -> {
            if (activeMissions.get(mission.callId()) != mission) {
                // Removed by a re-entrant reset during an earlier step of this same pass.
                throw SupportSpawnException.endMission("支援任务已被撤销");
            }
            provider.executeStep(new SupportSpawnContext(level, owner, mission.callId(),
                    mission.definition(), mission.target(), stepIndex, mission.faction()));
        });
        if (run.end() != null) {
            finishMission(mission, run.end());
        }
        return run.callbacks();
    }

    /**
     * Core-initiated end of a mission. Once a step has run, the provider (when still resolvable,
     * even with a tripped circuit) gets one chance to remove what the mission already placed in
     * the world, so a cancelled strike never leaves an inert or unguided shell behind.
     */
    private void cancelMission(ActiveMission mission, String reason,
                               SupportProvider provider, ServerLevel level) {
        int nextStepIndex = mission.cursor().nextStepIndex();
        if (abandonOnCoreCancel(nextStepIndex) && provider != null && level != null
                && activeMissions.get(mission.callId()) == mission) {
            try {
                provider.abandon(new SupportSpawnContext(level, currentOwner(mission),
                        mission.callId(), mission.definition(), mission.target(),
                        nextStepIndex, mission.faction()));
            } catch (RuntimeException | LinkageError failure) {
                LOGGER.warn("Support mission {} ({}) could not hand its cleanup to the provider",
                        mission.callId(), mission.supportId(), failure);
            }
        }
        finishMission(mission, MissionEnd.coreCancelled(reason, nextStepIndex));
    }

    /**
     * Single exit for every mission: removes it only if it is still this exact entry, applies a
     * compare-and-set refund when allowed and tells the requester why a mission did not finish.
     */
    private void finishMission(ActiveMission mission, MissionEnd end) {
        if (!activeMissions.remove(mission.callId(), mission) || end.completed()) {
            return;
        }
        boolean refunded = end.refundCooldown() && savedData.restoreIfUnchanged(
                mission.faction(), mission.supportId(), mission.acceptedReadyAt(), 0L);
        Throwable cause = end.failure() == null ? null : end.failure().getCause();
        if (end.providerBroken()) {
            // The provider guard already logged the first circuit-breaking stack trace.
            LOGGER.error("Support mission {} ({}) ended by a broken provider: {} (refunded: {})",
                    mission.callId(), mission.supportId(), end.reason(), refunded);
        } else if (cause != null) {
            LOGGER.warn("Support mission {} ({}) ended: {} (refunded: {})",
                    mission.callId(), mission.supportId(), end.reason(), refunded, cause);
        } else {
            LOGGER.warn("Support mission {} ({}) ended: {} (refunded: {})",
                    mission.callId(), mission.supportId(), end.reason(), refunded);
        }
        if (end.refundCooldown() && !refunded) {
            LOGGER.info("Support mission {} kept the current cooldown of {} for {}: "
                            + "it was changed after acceptance",
                    mission.callId(), mission.supportId(), mission.faction().id());
        }
        notifyRequester(mission.requesterId(), failureNotice(
                mission.definition().fallbackName(), end.reason(),
                end.refundCooldown(), refunded));
    }

    private void notifyRequester(UUID requesterId, String message) {
        try {
            // Look the player up again: the accepted ServerPlayer may belong to a dead session.
            ServerPlayer requester = server.getPlayerList().getPlayer(requesterId);
            if (requester != null) {
                requester.sendSystemMessage(Component.literal(message));
            }
        } catch (RuntimeException failure) {
            LOGGER.warn("Could not notify support requester {}", requesterId, failure);
        }
    }

    /**
     * Prefers the requester's live session; falls back to the accepted entity when offline. That
     * entity may be a stale pre-respawn instance, so intel publication additionally requires the
     * owner to be the requester's current session.
     */
    private Entity currentOwner(ActiveMission mission) {
        ServerPlayer online = server.getPlayerList().getPlayer(mission.requesterId());
        return online != null ? online : mission.owner();
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
        Footprint footprint = Footprint.of(definition, target);
        if (!footprint.coordinatesValid()) {
            return TargetValidation.failure("支援覆盖范围超出可用世界坐标");
        }
        if (!level.getWorldBorder().isWithinBounds(footprint.bounds(level))) {
            return TargetValidation.failure("支援覆盖范围超出世界边界");
        }
        // The chunk-count cap bounds the loaded-chunk scan below. Definitions that opted out of
        // a loaded footprint never scan chunks, and their radius is already capped by the
        // definition, so the cap is not applied to them and cannot wrongly reject a large scan.
        if (definition.requiresLoadedFootprint()) {
            long chunkCount = footprint.chunkCount();
            if (chunkCount < 1L || chunkCount > MAX_VALIDATED_CHUNKS) {
                return TargetValidation.failure("支援覆盖范围过大");
            }
            if (!footprint.chunksLoaded(level)) {
                return TargetValidation.failure("支援覆盖范围存在未加载区块");
            }
        }
        int startY = surfaceY(level, target.startX(), target.startZ());
        int endY = surfaceY(level, target.endX(), target.endZ());
        BlockPos start = BlockPos.containing(target.startX(), startY, target.startZ());
        BlockPos end = BlockPos.containing(target.endX(), endY, target.endZ());
        if (!level.getWorldBorder().isWithinBounds(start)
                || !level.getWorldBorder().isWithinBounds(end)) {
            return TargetValidation.failure("支援目标超出世界边界");
        }
        return TargetValidation.success(level, startY, endY);
    }

    /** Returns a player-visible reason when an accepted mission may no longer execute. */
    private static String executionFootprintProblem(ServerLevel level,
                                                    SupportDefinition definition,
                                                    SupportTarget target) {
        Footprint footprint = Footprint.of(definition, target);
        if (!footprint.coordinatesValid()) {
            return "支援覆盖范围超出可用世界坐标";
        }
        if (!level.getWorldBorder().isWithinBounds(footprint.bounds(level))) {
            return "支援覆盖范围超出世界边界";
        }
        if (definition.requiresLoadedFootprint()) {
            long chunkCount = footprint.chunkCount();
            if (chunkCount < 1L || chunkCount > MAX_VALIDATED_CHUNKS
                    || !footprint.chunksLoaded(level)) {
                return "支援覆盖范围存在未加载区块";
            }
        }
        return null;
    }

    private static int surfaceY(ServerLevel level, double x, double z) {
        int blockX = Mth.floor(x);
        int blockZ = Mth.floor(z);
        // Only definitions without a loaded-footprint requirement reach an unloaded column; a
        // heightmap lookup there would make the main thread wait for the chunk to load.
        if (!chunkReady(level, SectionPos.blockToSectionCoord(blockX),
                SectionPos.blockToSectionCoord(blockZ))) {
            return level.getMinBuildHeight();
        }
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ);
    }

    /**
     * True only for a chunk that has finished loading. {@code ServerLevel.hasChunk} merely checks
     * the ticket level, so a block or height lookup after it can still block the main thread
     * until a ticketed chunk finishes reading or generating; {@code getChunkNow} never waits.
     */
    private static boolean chunkReady(ServerLevel level, int chunkX, int chunkZ) {
        return level.getChunkSource().getChunkNow(chunkX, chunkZ) != null;
    }

    /**
     * Executes due steps of one mission within {@code budget} provider callbacks and classifies
     * the first failure. Pure apart from the supplied cursor and executor.
     */
    static StepRun runDueSteps(MissionCursor cursor, long now, int budget,
                               StepExecutor executor) {
        int callbacks = 0;
        while (callbacks < budget && cursor.remainingSteps() > 0
                && now >= cursor.nextStepAtGameTick()) {
            int stepIndex = cursor.nextStepIndex();
            callbacks++;
            try {
                executor.execute(stepIndex);
            } catch (SupportSpawnException failure) {
                return new StepRun(callbacks, MissionEnd.failed(failure, stepIndex));
            }
            cursor.advance();
        }
        return new StepRun(callbacks,
                cursor.remainingSteps() <= 0 ? MissionEnd.COMPLETED : null);
    }

    /** Core-initiated cancellations refund only while no mission step has completed. */
    static boolean refundOnCoreCancel(int nextStepIndex) {
        return nextStepIndex == 0;
    }

    /** Exactly the cancellations that do not refund ask the provider to clean up the world. */
    static boolean abandonOnCoreCancel(int nextStepIndex) {
        return nextStepIndex > 0;
    }

    /** Provider failures refund only on request and only while nothing has been delivered. */
    static boolean refundOnFailure(SupportSpawnException failure, int nextStepIndex) {
        return failure.refundCooldown() && nextStepIndex == 0;
    }

    /** Requester chat line for a mission that ended without completing. */
    static String failureNotice(String supportName, String reason,
                                boolean refundRequested, boolean refunded) {
        String safeReason = SupportOptionView.sanitizeAvailabilityReason(reason).strip();
        if (safeReason.isEmpty()) {
            safeReason = "原因未知";
        }
        String prefix = "[支援] " + supportName;
        if (refunded) {
            return prefix + " 未能投送：" + safeReason + "，冷却已返还";
        }
        if (refundRequested) {
            return prefix + " 未能投送：" + safeReason;
        }
        return prefix + " 任务中止：" + safeReason;
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

    /** One provider callback for the given mission step. */
    @FunctionalInterface
    interface StepExecutor {
        void execute(int stepIndex) throws SupportSpawnException;
    }

    /** Result of one {@link #runDueSteps} pass; {@code end} is null while the mission continues. */
    record StepRun(int callbacks, MissionEnd end) {
    }

    /** Terminal classification of a mission: refund, circuit state and player-visible reason. */
    record MissionEnd(boolean completed, boolean refundCooldown, boolean providerBroken,
                      String reason, SupportSpawnException failure) {
        static final MissionEnd COMPLETED = new MissionEnd(true, false, false, "", null);

        static MissionEnd failed(SupportSpawnException failure, int nextStepIndex) {
            return new MissionEnd(false, refundOnFailure(failure, nextStepIndex),
                    failure.providerBroken(),
                    Objects.requireNonNullElse(failure.getMessage(), ""), failure);
        }

        static MissionEnd coreCancelled(String reason, int nextStepIndex) {
            return new MissionEnd(false, refundOnCoreCancel(nextStepIndex), false,
                    reason, null);
        }

        static MissionEnd crashed(String reason) {
            return new MissionEnd(false, false, true, reason, null);
        }
    }

    /** Mutable step position of one accepted mission. */
    static final class MissionCursor {
        private final int stepIntervalTicks;
        private long nextStepAtGameTick;
        private int nextStepIndex;
        private int remainingSteps;

        MissionCursor(long nextStepAtGameTick, int nextStepIndex, int remainingSteps,
                      int stepIntervalTicks) {
            this.nextStepAtGameTick = nextStepAtGameTick;
            this.nextStepIndex = nextStepIndex;
            this.remainingSteps = remainingSteps;
            this.stepIntervalTicks = stepIntervalTicks;
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
            nextStepAtGameTick = saturatingAdd(nextStepAtGameTick, stepIntervalTicks);
        }
    }

    /** Support footprint rectangle; computing it never touches chunk storage. */
    private record Footprint(double minX, double minZ, double maxX, double maxZ) {
        static Footprint of(SupportDefinition definition, SupportTarget target) {
            return new Footprint(
                    Math.min(target.startX(), target.endX()) - definition.radius(),
                    Math.min(target.startZ(), target.endZ()) - definition.radius(),
                    Math.max(target.startX(), target.endX()) + definition.radius(),
                    Math.max(target.startZ(), target.endZ()) + definition.radius());
        }

        boolean coordinatesValid() {
            return validCoordinate(minX) && validCoordinate(minZ)
                    && validCoordinate(maxX) && validCoordinate(maxZ);
        }

        AABB bounds(ServerLevel level) {
            return new AABB(minX, level.getMinBuildHeight(), minZ,
                    Math.nextUp(maxX), level.getMaxBuildHeight(), Math.nextUp(maxZ));
        }

        long chunkCount() {
            return (long) (maxChunkX() - minChunkX() + 1)
                    * (maxChunkZ() - minChunkZ() + 1L);
        }

        boolean chunksLoaded(ServerLevel level) {
            for (int chunkX = minChunkX(); chunkX <= maxChunkX(); chunkX++) {
                for (int chunkZ = minChunkZ(); chunkZ <= maxChunkZ(); chunkZ++) {
                    if (!chunkReady(level, chunkX, chunkZ)) {
                        return false;
                    }
                }
            }
            return true;
        }

        private int minChunkX() {
            return SectionPos.blockToSectionCoord(Mth.floor(minX));
        }

        private int maxChunkX() {
            return SectionPos.blockToSectionCoord(Mth.floor(maxX));
        }

        private int minChunkZ() {
            return SectionPos.blockToSectionCoord(Mth.floor(minZ));
        }

        private int maxChunkZ() {
            return SectionPos.blockToSectionCoord(Mth.floor(maxZ));
        }
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
        private final long acceptedReadyAt;
        private final long executeAtGameTick;
        private final MissionCursor cursor;
        @SuppressWarnings("unused")
        private final int startSurfaceY;
        @SuppressWarnings("unused")
        private final int endSurfaceY;

        private ActiveMission(UUID callId, UUID requesterId, Faction faction,
                              SupportDefinition definition,
                              SupportTarget target, ServerPlayer owner,
                              long acceptedReadyAt, long executeAtGameTick,
                              MissionCursor cursor,
                              int startSurfaceY, int endSurfaceY) {
            this.callId = callId;
            this.requesterId = requesterId;
            this.faction = faction;
            this.definition = definition;
            this.target = target;
            this.owner = owner;
            this.acceptedReadyAt = acceptedReadyAt;
            this.executeAtGameTick = executeAtGameTick;
            this.cursor = cursor;
            this.startSurfaceY = startSurfaceY;
            this.endSurfaceY = endSurfaceY;
        }

        UUID callId() {
            return callId;
        }

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

        /** Entity captured at acceptance; only a fallback when the requester is offline. */
        ServerPlayer owner() {
            return owner;
        }

        /** Cooldown value written at acceptance; the refund compare-and-set expects it. */
        long acceptedReadyAt() {
            return acceptedReadyAt;
        }

        MissionCursor cursor() {
            return cursor;
        }

        SupportMissionView view() {
            return new SupportMissionView(callId, definition.id(), target.dimension(),
                    target.startX(), target.startZ(), target.endX(), target.endZ(),
                    executeAtGameTick, cursor.remainingSteps());
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
