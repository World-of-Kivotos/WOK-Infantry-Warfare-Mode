package com.wok.infantry.formation.vehicle;

import com.wok.infantry.battle.ActionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Optional 卓越前线 vehicle provider implemented only through Forge/Minecraft APIs.
 *
 * <p>The formation lifecycle owns event wiring; this class performs fail-closed validation,
 * transactional spawning, ownership tagging and durable allocation tracking.</p>
 */
public final class SuperbWarfareVehicleService implements FormationVehicleProvider {
    private static final Pattern OWNERSHIP_ID = Pattern.compile("[a-z0-9_.:/-]+");
    private static final Pattern ALLOCATION_ID = Pattern.compile("[a-z0-9._/-]+");
    private static final int MAX_OWNERSHIP_ID_LENGTH = 256;
    private static final int MAX_ALLOCATION_ID_LENGTH = 96;
    private static final double FLOOR_EPSILON = 1.0E-4D;
    private final BooleanSupplier modLoaded;
    private final Function<ResourceLocation, EntityType<?>> entityTypeResolver;
    private final Map<VehicleAllocationKey, TrackedVehicle> allocations = new LinkedHashMap<>();
    private final Set<VehicleAllocationKey> pendingRetirements = new HashSet<>();
    private final Set<UUID> administrativeRemovals = new HashSet<>();
    private MinecraftServer lifecycleServer;
    private VehicleAllocationSavedData savedData;
    private boolean started;
    private UUID activeSession;

    /** Production constructor. It never resolves or loads a 卓越前线 class. */
    public SuperbWarfareVehicleService() {
        this(() -> ModList.get().isLoaded(SuperbWarfareVehicleGate.MOD_ID),
                SuperbWarfareVehicleService::resolveEntityType);
    }

    /** Injectable boundary used by unit/integration fixtures without loading 卓越前线. */
    SuperbWarfareVehicleService(BooleanSupplier modLoaded,
                                Function<ResourceLocation, EntityType<?>> entityTypeResolver) {
        this.modLoaded = Objects.requireNonNull(modLoaded, "modLoaded");
        this.entityTypeResolver = Objects.requireNonNull(entityTypeResolver,
                "entityTypeResolver");
    }

    /**
     * Loads the durable ledger before deployment or entity lifecycle observation begins.
     * This does not require 卓越前线 itself to be loaded.
     */
    @Override
    public synchronized ActionResult start(MinecraftServer server) {
        if (server == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具服务启动缺少 MinecraftServer");
        }
        if (started) {
            return lifecycleServer == server
                    ? ActionResult.ok("载具 allocation 持久化服务已启动")
                    : ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具服务已绑定其他 MinecraftServer");
        }
        if (lifecycleServer != null && lifecycleServer != server) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具服务仍绑定上一个 MinecraftServer");
        }
        if (!allocations.isEmpty() || !pendingRetirements.isEmpty()) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具服务启动前存在未绑定的运行时台账，拒绝覆盖");
        }

        VehicleAllocationSavedData loaded;
        try {
            loaded = VehicleAllocationSavedData.get(server);
        } catch (RuntimeException | LinkageError exception) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "无法加载载具 allocation 持久化台账: "
                            + exception.getClass().getSimpleName());
        }
        lifecycleServer = server;
        savedData = loaded;
        ActionResult status = loaded.status();
        if (!status.success()) {
            return status;
        }

        for (VehicleLedgerEntry entry : loaded.entries()) {
            VehicleOwnership ownership = new VehicleOwnership(entry.allocation().sessionId(),
                    entry.allocation().factionId(), entry.allocation().formationId(),
                    entry.allocation().allocationId(), entry.entityTypeId(), entry.dimension());
            ActionResult restored = restoreTrackedAllocation(ownership, entry.entityId(),
                    entry.pendingRetirement());
            if (!restored.success()) {
                allocations.clear();
                pendingRetirements.clear();
                loaded.markQuarantined("持久化载具 allocation 恢复失败: "
                        + restored.message());
                return loaded.status();
            }
        }
        started = true;
        return ActionResult.ok("已恢复 " + allocations.size()
                + " 条载具 allocation 持久化记录；"
                + "必须设定 activeSession 后才允许部署");
    }

    /** Recommended one-step startup: restore, retire stale sessions, then enable deployment. */
    @Override
    public synchronized ActionResult start(MinecraftServer server, UUID activeSession) {
        if (activeSession == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具服务启动缺少 activeSession");
        }
        ActionResult loaded = start(server);
        if (!loaded.success()) {
            return loaded;
        }
        return retireSessionsExcept(server, activeSession);
    }

    /**
     * Retires every allocation from older server sessions. Loaded entities are removed now;
     * unloaded entities keep a durable pending-retirement tombstone and are discarded on load.
     */
    @Override
    public synchronized ActionResult retireSessionsExcept(MinecraftServer server,
                                                          UUID activeSession) {
        if (activeSession == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "过期载具 session 清理缺少 activeSession");
        }
        ActionResult lifecycle = requireStarted(server);
        if (!lifecycle.success()) {
            return lifecycle;
        }

        List<VehicleAllocationKey> stale = retirementCandidates(activeSession);
        RetirementPlan planned = preflightRetirements(server, stale);
        if (!planned.result().success()) {
            return planned.result();
        }
        ActionResult committed = commitRetirements(planned, "载具 session 过期清理");
        if (!committed.success()) {
            return committed;
        }
        // Publish the new authority only after every retirement/tombstone is durable. On any
        // failure the caller keeps the previous deployment session and this provider must do so
        // as well, otherwise the two lifecycle owners would split-brain.
        this.activeSession = activeSession;
        return ActionResult.ok("载具 session 过期清理完成: 已清理 "
                + planned.loaded().size() + " 辆，未加载 tombstone "
                + planned.unloaded().size() + " 条；已启用 activeSession "
                + activeSession);
    }

    /**
     * Retires active-session allocations removed or type-changed by a formation hot reload.
     * Loaded entities are fully verified before any entity is discarded. Unloaded entities keep
     * their allocation as a durable tombstone and are treated as a successful delayed cleanup.
     */
    @Override
    public synchronized ActionResult reconcileActiveSession(
            MinecraftServer server, UUID activeSession,
            Map<VehicleAllocationKey, ResourceLocation> expectedAllocations) {
        ActionResult lifecycle = requireStarted(server);
        if (!lifecycle.success()) {
            return lifecycle;
        }
        ActionResult expected = validateExpectedAllocations(activeSession,
                expectedAllocations);
        if (!expected.success()) {
            return expected;
        }
        if (this.activeSession == null || !this.activeSession.equals(activeSession)) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具热重载调和 session 不是当前 activeSession");
        }

        List<VehicleAllocationKey> stale = reconciliationCandidates(activeSession,
                expectedAllocations);
        if (stale.isEmpty()) {
            return ActionResult.ok("当前战局载具 allocation 与编制配置一致");
        }

        RetirementPlan planned = preflightRetirements(server, stale);
        if (!planned.result().success()) {
            return planned.result();
        }
        ActionResult committed = commitRetirements(planned, "载具编制热重载调和");
        if (!committed.success()) {
            return committed;
        }
        return ActionResult.ok("载具编制热重载调和完成: 已清理 "
                + planned.loaded().size() + " 辆，已登记延迟清理 "
                + planned.unloaded().size() + " 辆");
    }

    @Override
    public synchronized VehicleDeploymentResult deployBatch(MinecraftServer server,
                                                             VehicleDeploymentRequest request) {
        ActionResult identity = validateRequestIdentity(server, request);
        if (!identity.success()) {
            return VehicleDeploymentResult.failure(identity);
        }
        ActionResult lifecycle = requireStarted(server);
        if (!lifecycle.success()) {
            return VehicleDeploymentResult.failure(lifecycle);
        }
        if (activeSession == null) {
            return VehicleDeploymentResult.failure(ActionResult.failure(
                    ActionResult.Code.INVALID_TARGET,
                    "载具服务尚未执行 retireSessionsExcept，拒绝部署"));
        }
        if (!activeSession.equals(request.sessionId())) {
            return VehicleDeploymentResult.failure(ActionResult.failure(
                    ActionResult.Code.INVALID_TARGET,
                    "载具部署 session 不是当前 activeSession"));
        }

        VehicleBatchPlanner.PlanResult planned = VehicleBatchPlanner.plan(request.dimension(),
                request.basePosition(), request.baseYaw(), request.vehicles());
        if (!planned.result().success()) {
            return VehicleDeploymentResult.failure(planned.result());
        }
        if (planned.plans().isEmpty()) {
            return VehicleDeploymentResult.success("当前编制没有需要部署的载具", Map.of());
        }

        boolean available;
        try {
            available = modLoaded.getAsBoolean();
        } catch (RuntimeException exception) {
            return VehicleDeploymentResult.failure(ActionResult.failure(
                    ActionResult.Code.INVALID_TARGET,
                    "无法确认卓越前线 MOD 加载状态: "
                            + exception.getClass().getSimpleName()));
        }
        SuperbWarfareVehicleGate.Resolution<EntityType<?>> resolution =
                SuperbWarfareVehicleGate.resolve(available,
                        planned.plans().stream().map(VehicleSpawnPlan::entityId).toList(),
                        entityTypeResolver);
        if (!resolution.result().success()) {
            return VehicleDeploymentResult.failure(resolution.result());
        }

        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION,
                request.dimension());
        ServerLevel level = server.getLevel(dimensionKey);
        if (level == null || !request.dimension().equals(level.dimension().location())) {
            return VehicleDeploymentResult.failure(deploymentFailure(
                    "载具目标维度未加载: " + request.dimension()));
        }

        ExistingResolution existing = resolveExisting(server, level, request, planned.plans());
        if (!existing.result().success()) {
            return VehicleDeploymentResult.failure(existing.result());
        }

        PreflightResult preflight = preflight(level, existing.toSpawn(), resolution.values());
        if (!preflight.result().success()) {
            return VehicleDeploymentResult.failure(preflight.result());
        }

        Set<VehicleAllocationKey> projectedKeys = new HashSet<>(allocations.keySet());
        for (VehicleSpawnPlan plan : planned.plans()) {
            projectedKeys.add(allocationKey(request, plan));
        }
        if (projectedKeys.size() > VehicleAllocationSavedData.MAX_ENTRIES) {
            return VehicleDeploymentResult.failure(ActionResult.failure(
                    ActionResult.Code.INVALID_TARGET,
                    "载具 allocation 台账已达安全容量上限，拒绝生成"));
        }

        List<SpawnedVehicle> spawned = new ArrayList<>();
        Map<VehicleAllocationKey, TrackedVehicle> previousAllocations =
                new LinkedHashMap<>(allocations);
        Set<VehicleAllocationKey> previousPending = new HashSet<>(pendingRetirements);
        Map<String, UUID> resultEntities = new LinkedHashMap<>(existing.entityIds());
        Map<VehicleAllocationKey, TrackedVehicle> committed =
                new LinkedHashMap<>(existing.reconciled());
        for (PreparedSpawn prepared : preflight.spawns()) {
            Entity entity = null;
            try {
                entity = prepared.entityType().create(level);
                if (entity == null) {
                    return rollbackFailure(spawned, entity,
                            "卓越前线载具工厂返回空实体: " + prepared.plan().entityId());
                }
                entity.moveTo(prepared.plan().position().x, prepared.plan().position().y,
                        prepared.plan().position().z, prepared.plan().yaw(), 0.0F);
                if (!level.getWorldBorder().isWithinBounds(entity.getBoundingBox())
                        || !level.noCollision(entity)
                        || level.containsAnyLiquid(entity.getBoundingBox())) {
                    return rollbackFailure(spawned, entity,
                            "载具生成前空间状态已变化: " + prepared.plan().allocationId());
                }

                VehicleOwnership ownership = ownership(request, prepared.plan());
                VehiclePersistentData.stamp(entity, ownership);
                if (!level.addFreshEntity(entity)) {
                    return rollbackFailure(spawned, entity,
                            "服务端拒绝加入载具实体: " + prepared.plan().entityId());
                }
                SpawnedVehicle added = new SpawnedVehicle(entity, ownership);
                spawned.add(added);
                resultEntities.put(prepared.plan().allocationId(), entity.getUUID());
                committed.put(ownership.allocationKey(), new TrackedVehicle(entity.getUUID(),
                        ownership.entityTypeId(), ownership.dimension()));
            } catch (RuntimeException | LinkageError exception) {
                return rollbackFailure(spawned, entity,
                        "生成卓越前线载具失败: " + prepared.plan().entityId()
                                + " (" + exception.getClass().getSimpleName() + ")");
            }
        }

        allocations.putAll(committed);
        ActionResult persisted = persistLedger();
        if (!persisted.success()) {
            allocations.clear();
            allocations.putAll(previousAllocations);
            pendingRetirements.clear();
            pendingRetirements.addAll(previousPending);
            return rollbackFailure(spawned, null,
                    "载具已生成但 allocation 台账持久化失败: "
                            + persisted.message());
        }
        if (spawned.isEmpty()) {
            return VehicleDeploymentResult.success("载具批次已部署，幂等返回现有实体",
                    resultEntities);
        }
        return VehicleDeploymentResult.success("已部署 " + spawned.size()
                + " 辆卓越前线编制载具", resultEntities);
    }

    /** Idempotent cleanup of one allocation; unloaded entities are retained as pending. */
    @Override
    public synchronized ActionResult resetAllocation(MinecraftServer server,
                                                     VehicleAllocationKey key) {
        if (server == null || key == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具 reset 缺少服务端或 allocation");
        }
        ActionResult lifecycle = requireStarted(server);
        if (!lifecycle.success()) {
            return lifecycle;
        }
        TrackedVehicle tracked = allocations.get(key);
        if (tracked == null) {
            if (pendingRetirements.remove(key)) {
                ActionResult persisted = persistLedger();
                if (!persisted.success()) {
                    return persisted;
                }
            }
            return ActionResult.ok("该载具 allocation 已处于清理状态");
        }
        Entity entity = findLoadedEntity(server, tracked);
        if (entity == null) {
            pendingRetirements.add(key);
            ActionResult persisted = persistLedger();
            if (!persisted.success()) {
                return persisted;
            }
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具实体当前未加载，已登记延迟清理: " + key.allocationId());
        }
        Optional<VehicleOwnership> ownership = VehiclePersistentData.read(entity);
        ResourceLocation actualType = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (ownership.isEmpty() || !ownership.get().allocationKey().equals(key)
                || !tracked.entityId().equals(entity.getUUID())
                || !tracked.entityTypeId().equals(ownership.get().entityTypeId())
                || !tracked.entityTypeId().equals(actualType)
                || !tracked.dimension().equals(ownership.get().dimension())
                || !tracked.dimension().equals(entity.level().dimension().location())) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具 ownership 与 allocation 台账不一致，拒绝删除未知实体");
        }
        administrativeRemovals.add(entity.getUUID());
        try {
            entity.discard();
        } catch (RuntimeException exception) {
            administrativeRemovals.remove(entity.getUUID());
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具清理失败: " + exception.getClass().getSimpleName());
        }
        allocations.remove(key);
        pendingRetirements.remove(key);
        ActionResult persisted = persistLedger();
        if (!persisted.success()) {
            return persisted;
        }
        return ActionResult.ok("已清理载具 allocation: " + key.allocationId());
    }

    /** Cleans every tracked allocation from one session and reports any unloaded pending entity. */
    @Override
    public synchronized ActionResult resetSession(MinecraftServer server, UUID sessionId) {
        if (server == null || sessionId == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具 session reset 参数缺失");
        }
        ActionResult lifecycle = requireStarted(server);
        if (!lifecycle.success()) {
            return lifecycle;
        }
        List<VehicleAllocationKey> targets = allocations.keySet().stream()
                .filter(key -> sessionId.equals(key.sessionId())).toList();
        int cleaned = 0;
        int pending = 0;
        for (VehicleAllocationKey key : targets) {
            ActionResult result = resetAllocation(server, key);
            if (result.success()) {
                cleaned++;
            } else if (pendingRetirements.contains(key) && savedData.status().success()) {
                pending++;
            } else {
                return result;
            }
        }
        if (pending > 0) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "已清理 " + cleaned + " 辆载具，另有 " + pending
                            + " 辆未加载实体等待生命周期接线清理");
        }
        return ActionResult.ok("已清理战局会话的 " + cleaned + " 辆编制载具");
    }

    /**
     * EntityJoinLevelEvent bridge. It rebuilds the in-memory index
     * from persistent entity tags and completes a pending reset before the entity can be used.
     */
    @Override
    public synchronized ActionResult observeLoadedVehicle(Entity entity) {
        if (entity == null || entity.level().isClientSide()) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "只能在服务端恢复编制载具台账");
        }
        ResourceLocation actualType = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (!SuperbWarfareVehicleGate.supportsEntity(actualType)) {
            return ActionResult.ok("实体不是卓越前线载具，不应用编制载具台账");
        }
        Optional<VehicleOwnership> parsed = VehiclePersistentData.read(entity);
        if (parsed.isEmpty()) {
            if (VehiclePersistentData.hasVehicleMarker(entity)) {
                return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                        "载具包含损坏的 WOK步战核心 ownership 标记");
            }
            return ActionResult.ok("实体不是 WOK步战核心管理的编制载具");
        }
        VehicleOwnership ownership = parsed.get();
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "编制载具不在 ServerLevel 中");
        }
        ActionResult lifecycle = requireStarted(serverLevel.getServer());
        if (!lifecycle.success()) {
            return lifecycle;
        }
        if (!ownership.entityTypeId().equals(actualType)
                || !ownership.dimension().equals(entity.level().dimension().location())) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具持久化 ownership 与实体类型或维度不一致");
        }
        VehicleAllocationKey key = ownership.allocationKey();
        if (pendingRetirements.contains(key)) {
            administrativeRemovals.add(entity.getUUID());
            try {
                entity.discard();
            } catch (RuntimeException exception) {
                administrativeRemovals.remove(entity.getUUID());
                return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                        "延迟清理载具失败: " + exception.getClass().getSimpleName());
            }
            allocations.remove(key);
            pendingRetirements.remove(key);
            ActionResult persisted = persistLedger();
            if (!persisted.success()) {
                return persisted;
            }
            return ActionResult.ok("已完成未加载载具的延迟清理");
        }
        TrackedVehicle current = allocations.get(key);
        if (current != null && !current.entityId().equals(entity.getUUID())) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "检测到同一 allocation 的重复载具，拒绝覆盖台账");
        }
        VehicleAllocationKey entityOwner = allocationForEntity(entity.getUUID());
        if (entityOwner != null && !entityOwner.equals(key)) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "检测到同一载具 UUID 被多个 allocation 占用");
        }
        if (current == null && allocations.size() >= VehicleAllocationSavedData.MAX_ENTRIES) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具 allocation 台账已达安全容量上限");
        }
        allocations.put(key, new TrackedVehicle(entity.getUUID(), ownership.entityTypeId(),
                ownership.dimension()));
        ActionResult persisted = persistLedger();
        if (!persisted.success()) {
            return persisted;
        }
        return ActionResult.ok("已恢复编制载具 allocation 台账");
    }

    /** EntityLeaveLevelEvent bridge; chunk unload does not release quota. */
    @Override
    public synchronized VehicleRemovalObservation observeRemovedVehicle(Entity entity) {
        if (!isSuperbWarfareEntity(entity)) {
            return VehicleRemovalObservation.success(
                    "实体不是已支持的卓越前线生态载具，不应用编制载具台账");
        }
        Optional<VehicleOwnership> parsed = VehiclePersistentData.read(entity);
        if (parsed.isEmpty()) {
            if (VehiclePersistentData.hasVehicleMarker(entity)) {
                return VehicleRemovalObservation.failure(ActionResult.failure(
                        ActionResult.Code.INVALID_TARGET,
                        "移除的载具包含损坏的 WOK步战核心 ownership 标记"));
            }
            return VehicleRemovalObservation.success(
                    "移除的载具不受 WOK步战核心管理");
        }
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return VehicleRemovalObservation.failure(ActionResult.failure(
                    ActionResult.Code.INVALID_TARGET,
                    "移除的编制载具不在 ServerLevel 中"));
        }
        ActionResult lifecycle = requireStarted(serverLevel.getServer());
        if (!lifecycle.success()) {
            return VehicleRemovalObservation.failure(lifecycle);
        }
        VehicleOwnership ownership = parsed.get();
        VehicleAllocationKey key = ownership.allocationKey();
        boolean administrative = administrativeRemovals.remove(entity.getUUID());
        TrackedVehicle current = allocations.get(key);
        if (current == null) {
            return VehicleRemovalObservation.success(
                    "移除的载具未占用当前 allocation 台账");
        }
        if (!current.entityId().equals(entity.getUUID())) {
            return VehicleRemovalObservation.failure(ActionResult.failure(
                    ActionResult.Code.INVALID_TARGET,
                    "移除的载具 UUID 与 allocation 台账不一致"));
        }
        Entity.RemovalReason reason = entity.getRemovalReason();
        if (reason != null && reason.shouldDestroy()) {
            allocations.remove(key);
            pendingRetirements.remove(key);
            ActionResult persisted = persistLedger();
            if (!persisted.success()) {
                return VehicleRemovalObservation.failure(persisted);
            }
            return administrative
                    ? VehicleRemovalObservation.success("已完成编制载具管理清理")
                    : VehicleRemovalObservation.destroyed(ownership);
        }
        return VehicleRemovalObservation.success(
                "载具仅卸载区块，保留 allocation 台账");
    }

    /** Restores a SavedData allocation before its entity chunk has loaded. */
    public synchronized ActionResult restoreTrackedAllocation(VehicleOwnership ownership,
                                                              UUID entityId) {
        return restoreTrackedAllocation(ownership, entityId, false);
    }

    /** Restores a persisted ledger entry, including an unfinished reset tombstone. */
    public synchronized ActionResult restoreTrackedAllocation(VehicleOwnership ownership,
                                                              UUID entityId,
                                                              boolean pendingRetirement) {
        if (ownership == null || entityId == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "恢复载具 allocation 台账的参数缺失");
        }
        if (!SuperbWarfareVehicleGate.supportsEntity(ownership.entityTypeId())) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "只能恢复已支持的卓越前线生态载具 allocation");
        }
        if (!validOwnershipId(ownership.factionId())
                || !validOwnershipId(ownership.formationId())
                || !validAllocationId(ownership.allocationId())) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "恢复的载具 ownership 标识不合法");
        }
        VehicleAllocationKey key = ownership.allocationKey();
        TrackedVehicle candidate = new TrackedVehicle(entityId, ownership.entityTypeId(),
                ownership.dimension());
        TrackedVehicle current = allocations.get(key);
        if (current != null && !current.equals(candidate)) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "同一 allocation 已绑定其他载具实体");
        }
        VehicleAllocationKey entityOwner = allocationForEntity(entityId);
        if (entityOwner != null && !entityOwner.equals(key)) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "同一载具 UUID 已绑定其他 allocation");
        }
        if (current == null && allocations.size() >= VehicleAllocationSavedData.MAX_ENTRIES) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具 allocation 台账已达安全容量上限");
        }
        allocations.put(key, candidate);
        if (pendingRetirement) {
            pendingRetirements.add(key);
        } else {
            pendingRetirements.remove(key);
        }
        if (started) {
            ActionResult persisted = persistLedger();
            if (!persisted.success()) {
                return persisted;
            }
        }
        return ActionResult.ok("已恢复载具 allocation 台账");
    }

    public synchronized Map<VehicleAllocationKey, UUID> trackedAllocations() {
        Map<VehicleAllocationKey, UUID> snapshot = new LinkedHashMap<>();
        allocations.forEach((key, value) -> snapshot.put(key, value.entityId()));
        return Collections.unmodifiableMap(snapshot);
    }

    /** Complete serialization boundary for vehicle-allocation SavedData. */
    public synchronized List<VehicleLedgerEntry> ledgerSnapshot() {
        List<VehicleLedgerEntry> snapshot = new ArrayList<>(allocations.size());
        allocations.forEach((key, value) -> snapshot.add(new VehicleLedgerEntry(key,
                value.entityId(), value.entityTypeId(), value.dimension(),
                pendingRetirements.contains(key))));
        return List.copyOf(snapshot);
    }

    @Override
    public ActionResult authorizeMount(Entity vehicle, UUID activeSession,
                                       String actorFactionId, String actorFormationId) {
        if (!isSuperbWarfareEntity(vehicle)) {
            return ManagedVehicleAccessPolicy.authorize(null, activeSession, actorFactionId,
                    actorFormationId);
        }
        Optional<VehicleOwnership> ownership = VehiclePersistentData.read(vehicle);
        if (ownership.isEmpty() && VehiclePersistentData.hasVehicleMarker(vehicle)) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具 ownership 标记损坏，拒绝乘坐");
        }
        return ManagedVehicleAccessPolicy.authorize(ownership.orElse(null), activeSession,
                actorFactionId, actorFormationId);
    }

    /**
     * Synchronizes the final runtime ledger into SavedData, then releases this server binding.
     * Call this before the server's normal world-data save completes.
     */
    @Override
    public synchronized ActionResult stop(MinecraftServer server) {
        if (server == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具服务停止缺少 MinecraftServer");
        }
        if (lifecycleServer == null || lifecycleServer != server) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具服务未绑定该 MinecraftServer");
        }

        ActionResult result;
        if (started) {
            result = persistLedger();
        } else if (savedData != null) {
            result = savedData.status();
            if (result.success()) {
                result = ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                        "载具 allocation 持久化服务尚未成功启动");
            }
        } else {
            result = ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具 allocation 持久化台账不可用");
        }

        allocations.clear();
        pendingRetirements.clear();
        administrativeRemovals.clear();
        started = false;
        activeSession = null;
        savedData = null;
        lifecycleServer = null;
        return result.success()
                ? ActionResult.ok("已同步并停止载具 allocation 持久化服务")
                : result;
    }

    private ActionResult requireStarted(MinecraftServer server) {
        if (!started || lifecycleServer == null || savedData == null) {
            if (savedData != null) {
                ActionResult status = savedData.status();
                if (!status.success()) {
                    return status;
                }
            }
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具 allocation 持久化服务未启动");
        }
        if (server == null || lifecycleServer != server) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具 allocation 请求来自未绑定的 MinecraftServer");
        }
        return savedData.status();
    }

    private ActionResult persistLedger() {
        if (!started || savedData == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具 allocation 持久化服务未启动");
        }
        ActionResult synchronizedResult = savedData.synchronize(ledgerSnapshot());
        if (synchronizedResult.success() || savedData.quarantined()) {
            return synchronizedResult;
        }
        savedData.markQuarantined("运行时载具 allocation 台账无法同步: "
                + synchronizedResult.message());
        return savedData.status();
    }

    private VehicleAllocationKey allocationForEntity(UUID entityId) {
        if (entityId == null) {
            return null;
        }
        for (Map.Entry<VehicleAllocationKey, TrackedVehicle> entry : allocations.entrySet()) {
            if (entityId.equals(entry.getValue().entityId())) {
                return entry.getKey();
            }
        }
        return null;
    }

    synchronized List<VehicleAllocationKey> retirementCandidates(UUID activeSession) {
        if (activeSession == null) {
            return List.of();
        }
        return allocations.keySet().stream()
                .filter(key -> !activeSession.equals(key.sessionId()))
                .toList();
    }

    synchronized List<VehicleAllocationKey> reconciliationCandidates(
            UUID activeSession,
            Map<VehicleAllocationKey, ResourceLocation> expectedAllocations) {
        if (activeSession == null || expectedAllocations == null) {
            return List.of();
        }
        return allocations.entrySet().stream()
                .filter(entry -> activeSession.equals(entry.getKey().sessionId()))
                .filter(entry -> pendingRetirements.contains(entry.getKey())
                        || !entry.getValue().entityTypeId().equals(
                        expectedAllocations.get(entry.getKey())))
                .map(Map.Entry::getKey)
                .toList();
    }

    static ActionResult validateExpectedAllocations(
            UUID activeSession,
            Map<VehicleAllocationKey, ResourceLocation> expectedAllocations) {
        if (activeSession == null || expectedAllocations == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具热重载调和参数缺失");
        }
        if (expectedAllocations.size() > VehicleAllocationSavedData.MAX_ENTRIES) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具热重载调和 allocation 超过安全上限");
        }
        for (Map.Entry<VehicleAllocationKey, ResourceLocation> entry
                : expectedAllocations.entrySet()) {
            VehicleAllocationKey key = entry.getKey();
            ResourceLocation entityTypeId = entry.getValue();
            if (key == null || !activeSession.equals(key.sessionId())
                    || !validOwnershipId(key.factionId())
                    || !validOwnershipId(key.formationId())
                    || !validAllocationId(key.allocationId())) {
                return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                        "载具热重载调和包含非当前 session 或非法 allocation");
            }
            if (!SuperbWarfareVehicleGate.supportsEntity(entityTypeId)) {
                return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                        "载具热重载调和包含不受支持的实体类型");
            }
        }
        return ActionResult.ok("载具热重载调和期望台账有效");
    }

    private static ActionResult validateTrackedEntityForRetirement(
            VehicleAllocationKey key, TrackedVehicle tracked, Entity entity) {
        Optional<VehicleOwnership> ownership = VehiclePersistentData.read(entity);
        ResourceLocation actualType = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entity.isRemoved() || ownership.isEmpty()
                || !ownership.get().allocationKey().equals(key)
                || !tracked.entityId().equals(entity.getUUID())
                || !tracked.entityTypeId().equals(ownership.get().entityTypeId())
                || !tracked.entityTypeId().equals(actualType)
                || !tracked.dimension().equals(ownership.get().dimension())
                || !tracked.dimension().equals(entity.level().dimension().location())) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具退役预检检测到 ownership 与 allocation 台账冲突，"
                            + "拒绝删除未知实体");
        }
        return ActionResult.ok("载具退役预检通过");
    }

    /**
     * Phase one of retirement: resolve and validate every target without mutating either the
     * world or the ledger. A conflict in any later target therefore cannot partially delete an
     * earlier target.
     */
    private RetirementPlan preflightRetirements(MinecraftServer server,
                                                List<VehicleAllocationKey> targets) {
        RetirementBatchPlanner.Plan<VehicleAllocationKey, Entity> batch =
                RetirementBatchPlanner.preflight(targets, key -> {
                    TrackedVehicle tracked = allocations.get(key);
                    if (tracked == null) {
                        return RetirementBatchPlanner.Inspection.failure(ActionResult.failure(
                                ActionResult.Code.INVALID_TARGET,
                                "载具退役预检期间 allocation 台账已变更"));
                    }
                    Entity entity = findLoadedEntity(server, tracked);
                    if (entity == null) {
                        return RetirementBatchPlanner.Inspection.unloaded();
                    }
                    ActionResult safe = validateTrackedEntityForRetirement(key, tracked,
                            entity);
                    if (!safe.success()) {
                        return RetirementBatchPlanner.Inspection.failure(safe);
                    }
                    return RetirementBatchPlanner.Inspection.loaded(entity);
                });
        return new RetirementPlan(batch.result(), batch.loaded(), batch.unloaded());
    }

    /**
     * Phase two is the explicit irreversible boundary: Minecraft entity discard cannot be rolled
     * back. Predictable ownership conflicts have already been rejected by phase one. If a runtime
     * discard exception occurs, successfully discarded prefixes are synchronized immediately when
     * possible and the operation fails without publishing a new session authority. A persistence
     * failure after discard quarantines the durable ledger through {@link #persistLedger()}.
     */
    private ActionResult commitRetirements(RetirementPlan planned, String operation) {
        int discarded = 0;
        for (Map.Entry<VehicleAllocationKey, Entity> retirement
                : planned.loaded().entrySet()) {
            administrativeRemovals.add(retirement.getValue().getUUID());
            try {
                retirement.getValue().discard();
            } catch (RuntimeException exception) {
                administrativeRemovals.remove(retirement.getValue().getUUID());
                ActionResult partialPersist = persistLedger();
                String persistence = partialPersist.success() ? "已持久化已删除前缀"
                        : "已删除前缀无法持久化: " + partialPersist.message();
                return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                        operation + "已进入不可回滚的世界 discard 提交阶段；已删除 "
                                + discarded + " 辆后失败 ("
                                + exception.getClass().getSimpleName() + ")，" + persistence);
            }
            allocations.remove(retirement.getKey());
            pendingRetirements.remove(retirement.getKey());
            discarded++;
        }
        pendingRetirements.addAll(planned.unloaded());

        ActionResult persisted = persistLedger();
        if (!persisted.success()) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    operation + "已进入不可回滚提交阶段；世界实体/延迟 tombstone "
                            + "已变更，但台账持久化失败: " + persisted.message());
        }
        return ActionResult.ok(operation + "不可回滚提交已持久化");
    }

    private ExistingResolution resolveExisting(MinecraftServer server, ServerLevel level,
                                               VehicleDeploymentRequest request,
                                               List<VehicleSpawnPlan> plans) {
        Map<VehicleAllocationKey, VehicleSpawnPlan> wanted = new LinkedHashMap<>();
        Set<ResourceLocation> wantedEntityTypes = new HashSet<>();
        for (VehicleSpawnPlan plan : plans) {
            wanted.put(allocationKey(request, plan), plan);
            wantedEntityTypes.add(plan.entityId());
        }

        Map<VehicleAllocationKey, Entity> loadedCandidates = new HashMap<>();
        for (Entity entity : level.getAllEntities()) {
            ResourceLocation actualType = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (!wantedEntityTypes.contains(actualType)) {
                continue;
            }
            Optional<VehicleOwnership> parsed = VehiclePersistentData.read(entity);
            if (parsed.isEmpty() || !wanted.containsKey(parsed.get().allocationKey())
                    || entity.isRemoved()) {
                continue;
            }
            Entity duplicate = loadedCandidates.putIfAbsent(parsed.get().allocationKey(), entity);
            if (duplicate != null && !duplicate.getUUID().equals(entity.getUUID())) {
                return ExistingResolution.failure("世界中存在同一 allocation 的重复载具: "
                        + parsed.get().allocationId());
            }
        }

        Map<String, UUID> existingIds = new LinkedHashMap<>();
        Map<VehicleAllocationKey, TrackedVehicle> reconciled = new LinkedHashMap<>();
        List<VehicleSpawnPlan> toSpawn = new ArrayList<>();
        for (VehicleSpawnPlan plan : plans) {
            VehicleAllocationKey key = allocationKey(request, plan);
            if (pendingRetirements.contains(key)) {
                return ExistingResolution.failure("载具 allocation 正在等待 reset 清理，拒绝重复生成: "
                        + plan.allocationId());
            }
            TrackedVehicle tracked = allocations.get(key);
            Entity loaded = loadedCandidates.get(key);
            if (tracked != null) {
                Entity trackedEntity = findLoadedEntity(server, tracked);
                if (trackedEntity == null) {
                    return ExistingResolution.failure("载具 allocation 已占用但实体当前未加载，"
                            + "为防止复制拒绝补发: " + plan.allocationId());
                }
                if (loaded != null && !loaded.getUUID().equals(trackedEntity.getUUID())) {
                    return ExistingResolution.failure("载具 allocation 台账与世界实体冲突: "
                            + plan.allocationId());
                }
                loaded = trackedEntity;
            }
            if (loaded == null) {
                toSpawn.add(plan);
                continue;
            }
            ActionResult valid = validateExisting(level, key, plan, loaded);
            if (!valid.success()) {
                return new ExistingResolution(valid, Map.of(), Map.of(), List.of());
            }
            TrackedVehicle recovered = new TrackedVehicle(loaded.getUUID(), plan.entityId(),
                    plan.dimension());
            existingIds.put(plan.allocationId(), loaded.getUUID());
            reconciled.put(key, recovered);
        }
        return new ExistingResolution(ActionResult.ok("载具幂等台账校验通过"), existingIds,
                reconciled, toSpawn);
    }

    private static ActionResult validateExisting(ServerLevel targetLevel,
                                                 VehicleAllocationKey key,
                                                 VehicleSpawnPlan plan, Entity entity) {
        if (entity.isRemoved() || !entity.isAlive()) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具 allocation 指向已移除实体: " + plan.allocationId());
        }
        Optional<VehicleOwnership> parsed = VehiclePersistentData.read(entity);
        ResourceLocation actualType = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (parsed.isEmpty() || !key.equals(parsed.get().allocationKey())
                || !plan.entityId().equals(parsed.get().entityTypeId())
                || !plan.entityId().equals(actualType)
                || !plan.dimension().equals(parsed.get().dimension())
                || entity.level() != targetLevel) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "现有载具与 allocation 配置不一致: " + plan.allocationId());
        }
        return ActionResult.ok("现有载具 allocation 有效");
    }

    private static PreflightResult preflight(ServerLevel level,
                                             List<VehicleSpawnPlan> plans,
                                             Map<ResourceLocation, EntityType<?>> types) {
        List<PreparedSpawn> prepared = new ArrayList<>(plans.size());
        for (VehicleSpawnPlan plan : plans) {
            EntityType<?> entityType = types.get(plan.entityId());
            if (entityType == null) {
                return PreflightResult.failure("预检期间载具实体类型缺失: "
                        + plan.entityId());
            }
            ResourceLocation registeredId = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
            if (!plan.entityId().equals(registeredId)) {
                return PreflightResult.failure("载具实体解析结果与配置 ID 不一致，拒绝回退: "
                        + plan.entityId());
            }
            if (!entityType.canSerialize()) {
                return PreflightResult.failure("载具实体类型不可持久化: " + plan.entityId());
            }
            AABB bounds = entityType.getAABB(plan.position().x, plan.position().y,
                    plan.position().z);
            if (!finite(bounds) || bounds.minY < level.getMinBuildHeight()
                    || bounds.maxY > level.getMaxBuildHeight()) {
                return PreflightResult.failure("载具目标超出维度有效高度: "
                        + plan.allocationId());
            }
            if (!level.getWorldBorder().isWithinBounds(bounds)) {
                return PreflightResult.failure("载具目标超出世界边界: "
                        + plan.allocationId());
            }
            if (!allCornerChunksLoaded(level, bounds)) {
                return PreflightResult.failure("载具安全空间所在区块未加载: "
                        + plan.allocationId());
            }
            if (!level.noCollision(bounds)) {
                return PreflightResult.failure("载具目标空间被方块或实体占用: "
                        + plan.allocationId());
            }
            if (level.containsAnyLiquid(bounds)) {
                return PreflightResult.failure("载具目标空间包含液体: "
                        + plan.allocationId());
            }
            BlockPos floor = BlockPos.containing(plan.position().x,
                    plan.position().y - FLOOR_EPSILON, plan.position().z);
            BlockState floorState = level.getBlockState(floor);
            if (!floorState.isFaceSturdy(level, floor, Direction.UP)) {
                return PreflightResult.failure("载具目标中心下方没有稳固地面: "
                        + plan.allocationId());
            }
            for (PreparedSpawn other : prepared) {
                if (bounds.intersects(other.bounds())) {
                    return PreflightResult.failure("同批载具安全空间重叠: "
                            + other.plan().allocationId() + " / " + plan.allocationId());
                }
            }
            prepared.add(new PreparedSpawn(plan, entityType, bounds));
        }
        return new PreflightResult(ActionResult.ok("整批载具世界安全预检通过"), prepared);
    }

    private VehicleDeploymentResult rollbackFailure(List<SpawnedVehicle> spawned,
                                                     Entity uncommitted,
                                                     String message) {
        boolean rollbackComplete = true;
        if (uncommitted != null && !uncommitted.isRemoved()) {
            try {
                uncommitted.discard();
            } catch (RuntimeException ignored) {
                rollbackComplete = false;
            }
        }
        for (int index = spawned.size() - 1; index >= 0; index--) {
            Entity entity = spawned.get(index).entity();
            if (!entity.isRemoved()) {
                try {
                    entity.discard();
                } catch (RuntimeException ignored) {
                    rollbackComplete = false;
                }
            }
        }
        return VehicleDeploymentResult.failure(ActionResult.failure(
                ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                message + (rollbackComplete ? "；已回滚本批新生成载具"
                        : "；本批回滚未完全成功，需要管理员执行载具台账核对")));
    }

    private static ActionResult validateRequestIdentity(MinecraftServer server,
                                                        VehicleDeploymentRequest request) {
        if (server == null || request == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具部署请求或服务端缺失");
        }
        if (request.sessionId() == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具部署战局 session 缺失");
        }
        if (!validOwnershipId(request.factionId())) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具部署 faction 标识不合法");
        }
        if (!validOwnershipId(request.formationId())) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具部署 formation 标识不合法");
        }
        return ActionResult.ok("载具部署 ownership 参数有效");
    }

    private static boolean validOwnershipId(String value) {
        return value != null && !value.isBlank() && value.equals(value.trim())
                && value.length() <= MAX_OWNERSHIP_ID_LENGTH
                && OWNERSHIP_ID.matcher(value).matches();
    }

    private static boolean validAllocationId(String value) {
        return value != null && !value.isBlank() && value.equals(value.trim())
                && value.length() <= MAX_ALLOCATION_ID_LENGTH
                && ALLOCATION_ID.matcher(value).matches();
    }

    private static boolean finite(AABB bounds) {
        return Double.isFinite(bounds.minX) && Double.isFinite(bounds.minY)
                && Double.isFinite(bounds.minZ) && Double.isFinite(bounds.maxX)
                && Double.isFinite(bounds.maxY) && Double.isFinite(bounds.maxZ);
    }

    private static boolean allCornerChunksLoaded(ServerLevel level, AABB bounds) {
        double maxX = Math.nextDown(bounds.maxX);
        double maxZ = Math.nextDown(bounds.maxZ);
        BlockPos northWest = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ);
        BlockPos northEast = BlockPos.containing(maxX, bounds.minY, bounds.minZ);
        BlockPos southWest = BlockPos.containing(bounds.minX, bounds.minY, maxZ);
        BlockPos southEast = BlockPos.containing(maxX, bounds.minY, maxZ);
        return chunkLoaded(level, northWest) && chunkLoaded(level, northEast)
                && chunkLoaded(level, southWest) && chunkLoaded(level, southEast);
    }

    private static boolean chunkLoaded(ServerLevel level, BlockPos position) {
        return level.hasChunk(SectionPos.blockToSectionCoord(position.getX()),
                SectionPos.blockToSectionCoord(position.getZ()));
    }

    private static VehicleAllocationKey allocationKey(VehicleDeploymentRequest request,
                                                      VehicleSpawnPlan plan) {
        return new VehicleAllocationKey(request.sessionId(), request.factionId(),
                request.formationId(), plan.allocationId());
    }

    private static VehicleOwnership ownership(VehicleDeploymentRequest request,
                                              VehicleSpawnPlan plan) {
        return new VehicleOwnership(request.sessionId(), request.factionId(),
                request.formationId(), plan.allocationId(), plan.entityId(), plan.dimension());
    }

    private static Entity findLoadedEntity(MinecraftServer server, TrackedVehicle tracked) {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, tracked.dimension());
        ServerLevel level = server.getLevel(key);
        return level == null ? null : level.getEntity(tracked.entityId());
    }

    private static EntityType<?> resolveEntityType(ResourceLocation entityId) {
        if (entityId == null || !ForgeRegistries.ENTITY_TYPES.containsKey(entityId)) {
            return null;
        }
        EntityType<?> value = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
        return value != null && entityId.equals(ForgeRegistries.ENTITY_TYPES.getKey(value))
                ? value : null;
    }

    private static boolean isSuperbWarfareEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return SuperbWarfareVehicleGate.supportsEntity(entityId);
    }

    private static ActionResult deploymentFailure(String message) {
        return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT, message);
    }

    private record PreparedSpawn(VehicleSpawnPlan plan, EntityType<?> entityType, AABB bounds) {
    }

    private record SpawnedVehicle(Entity entity, VehicleOwnership ownership) {
    }

    private record RetirementPlan(ActionResult result,
                                  Map<VehicleAllocationKey, Entity> loaded,
                                  List<VehicleAllocationKey> unloaded) {
        private RetirementPlan {
            Objects.requireNonNull(result, "result");
            loaded = Collections.unmodifiableMap(new LinkedHashMap<>(loaded));
            unloaded = List.copyOf(unloaded);
        }

    }

    private record TrackedVehicle(UUID entityId, ResourceLocation entityTypeId,
                                  ResourceLocation dimension) {
    }

    public record VehicleLedgerEntry(VehicleAllocationKey allocation, UUID entityId,
                                     ResourceLocation entityTypeId, ResourceLocation dimension,
                                     boolean pendingRetirement) {
        public VehicleLedgerEntry {
            Objects.requireNonNull(allocation, "allocation");
            Objects.requireNonNull(entityId, "entityId");
            Objects.requireNonNull(entityTypeId, "entityTypeId");
            Objects.requireNonNull(dimension, "dimension");
        }
    }

    private record ExistingResolution(ActionResult result, Map<String, UUID> entityIds,
                                      Map<VehicleAllocationKey, TrackedVehicle> reconciled,
                                      List<VehicleSpawnPlan> toSpawn) {
        private ExistingResolution {
            entityIds = Map.copyOf(entityIds);
            reconciled = Map.copyOf(reconciled);
            toSpawn = List.copyOf(toSpawn);
        }

        static ExistingResolution failure(String message) {
            return new ExistingResolution(ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    message), Map.of(), Map.of(), List.of());
        }
    }

    private record PreflightResult(ActionResult result, List<PreparedSpawn> spawns) {
        private PreflightResult {
            spawns = List.copyOf(spawns);
        }

        static PreflightResult failure(String message) {
            return new PreflightResult(deploymentFailure(message), List.of());
        }
    }
}
