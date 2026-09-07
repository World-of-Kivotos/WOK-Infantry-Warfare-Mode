package com.wok.infantry.deployment;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.ammo.AmmoSupplyService;
import com.wok.infantry.block.DeploymentBeaconBlock;
import com.wok.infantry.block.DeploymentBeaconMode;
import com.wok.infantry.block.VehicleDeploymentBlock;
import com.wok.infantry.block.entity.RallyRadioBlockEntity;
import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.formation.FormationDefinition;
import com.wok.infantry.formation.FormationDeployablePolicy;
import com.wok.infantry.registry.InfantryBlocks;
import com.wok.infantry.server.FormationService;
import com.wok.infantry.server.LoadoutService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server-authoritative deployment, life issuance and base-resupply state machine.
 *
 * <p>Every mutation is expected on the Minecraft server thread. Runtime life state is
 * intentionally discarded on restart, while faction main bases live in SavedData.</p>
 */
public final class DeploymentService {
    /** Dedicated void dimension prevents vanilla entity/chunk tracking from leaking battle data. */
    public static final ResourceKey<Level> HOLDING_LEVEL = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(WokInfantryMod.MOD_ID, "holding"));
    public static final ResourceKey<Level> LOBBY_LEVEL = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(WokInfantryMod.MOD_ID, "lobby"));
    private static final double BLUE_HOLDING_X = -4_096.5D;
    private static final double RED_HOLDING_X = 4_096.5D;
    private static final double HOLDING_Y = 64.0D;
    private static final Vec3 REMOVED_PLAYER_LOBBY = new Vec3(0.5D, 2.0D, 0.5D);
    public static final long RESPAWN_DELAY_TICKS = 15L * 20L;
    public static final long RESUPPLY_COOLDOWN_TICKS = 60L * 20L;
    public static final long RALLY_SQUAD_COOLDOWN_TICKS = 8L * 60L * 20L;
    private static final String ARMOR_ESCROW_TAG = WokInfantryMod.MOD_ID + ":armor_escrow";

    private static final Map<MinecraftServer, DeploymentService> INSTANCES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final MinecraftServer server;
    private final DeploymentSavedData savedData;
    private final Map<UUID, DeploymentRecord> records = new LinkedHashMap<>();
    private final Set<UUID> releaseInProgress = new HashSet<>();
    private final Set<UUID> lobbyPlayers = new HashSet<>();
    /** Session-only administrator escape hatch for testing real Superb Warfare vehicles. */
    private final Set<UUID> vehicleTestPlayers = new HashSet<>();
    /** Session-wide gate; removing/re-admitting a roster record must not mint another free batch. */
    private final Set<UUID> initialReserveRecipients = new HashSet<>();
    private UUID sessionId = UUID.randomUUID();

    private DeploymentService(MinecraftServer server) {
        this(server, DeploymentSavedData.get(server));
    }

    /** Isolated state constructor used by Forge GameTest without touching a saved match. */
    DeploymentService(MinecraftServer server, DeploymentSavedData savedData) {
        this.server = Objects.requireNonNull(server, "server");
        if (server.getLevel(HOLDING_LEVEL) == null || server.getLevel(LOBBY_LEVEL) == null) {
            throw new IllegalStateException(
                    "Missing required WOK Infantry holding or lobby dimension");
        }
        this.savedData = Objects.requireNonNull(savedData, "savedData");
    }

    public static void start(MinecraftServer server) {
        if (server == null) {
            return;
        }
        synchronized (INSTANCES) {
            INSTANCES.computeIfAbsent(server, DeploymentService::new);
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

    public static Optional<DeploymentService> get(MinecraftServer server) {
        if (server == null) {
            return Optional.empty();
        }
        synchronized (INSTANCES) {
            return Optional.of(INSTANCES.computeIfAbsent(server, DeploymentService::new));
        }
    }

    public static Optional<DeploymentService> get(ServerPlayer player) {
        return player == null ? Optional.empty() : get(player.server);
    }

    public MinecraftServer server() {
        return server;
    }

    /** Useful only for server-side provenance validation; never put this value in a client DTO. */
    public UUID sessionId() {
        return sessionId;
    }

    /**
     * Deletes legacy personal-armor escrow data from builds that predated destructive deployment.
     * WOK步战 no longer preserves or restores any personal item.
     */
    private static void discardLegacyPersonalItems(ServerPlayer player) {
        if (player == null) {
            return;
        }
        CompoundTag persisted = persistedData(player, false);
        if (persisted != null && persisted.contains(ARMOR_ESCROW_TAG)) {
            persisted.remove(ARMOR_ESCROW_TAG);
            WokInfantryMod.LOGGER.info(
                    "Discarded obsolete personal-item escrow for player {}", player.getUUID());
        }
    }

    public ActionResult onPlayerConnected(ServerPlayer player) {
        if (isVehicleTestMode(player == null ? null : player.getUUID())) {
            prepareVehicleTestPlayer(player);
            return ActionResult.ok("载具测试模式已恢复");
        }
        ActionResult actorError = requireParticipant(player);
        if (actorError != null) {
            handleNonParticipant(player);
            return actorError;
        }
        lobbyPlayers.remove(player.getUUID());
        discardLegacyPersonalItems(player);
        DeploymentRecord record = records.computeIfAbsent(player.getUUID(), ignored ->
                newWaitingRecord(gameTick(), player.getUUID()));
        updateReady(record, gameTick());
        KitProvenance.purgeInvalid(player, sessionId,
                record.phase == DeploymentPhase.ACTIVE ? record.issueToken : null);
        if (record.phase != DeploymentPhase.ACTIVE) {
            resetLifeBoundary(player);
            holdPlayer(player);
        }
        return ActionResult.ok("部署状态已同步");
    }

    /** Disconnects do not grant another life or another kit. */
    public void onPlayerDisconnected(ServerPlayer player) {
        // Runtime state remains reserved with BattleService's reconnect reservation.
    }

    public void onPlayerDeath(ServerPlayer player) {
        if (isVehicleTestMode(player == null ? null : player.getUUID())) {
            return;
        }
        if (requireParticipant(player) != null) {
            handleNonParticipant(player);
            return;
        }
        lobbyPlayers.remove(player.getUUID());
        long now = gameTick();
        DeploymentRecord record = records.computeIfAbsent(player.getUUID(), ignored ->
                newWaitingRecord(now, player.getUUID()));
        if (record.phase == DeploymentPhase.ACTIVE) {
            beginWaiting(record, now, true, player.getUUID());
        }
    }

    public void onPlayerRespawned(ServerPlayer player, boolean endConquered) {
        if (isVehicleTestMode(player == null ? null : player.getUUID())) {
            prepareVehicleTestPlayer(player);
            return;
        }
        // Old builds persisted personal armor across death. The destructive-inventory policy
        // deliberately deletes that obsolete payload instead of restoring it.
        discardLegacyPersonalItems(player);
        if (requireParticipant(player) != null) {
            handleNonParticipant(player);
            return;
        }
        lobbyPlayers.remove(player.getUUID());
        long now = gameTick();
        DeploymentRecord record = records.computeIfAbsent(player.getUUID(), ignored ->
                newWaitingRecord(now, player.getUUID()));
        // Normal death should already have entered WAITING in LivingDeathEvent. Fail closed if
        // another mod bypassed that event; returning from the End is not a death.
        if (!endConquered && record.phase == DeploymentPhase.ACTIVE) {
            beginWaiting(record, now, true, player.getUUID());
        }
        updateReady(record, now);
        KitProvenance.purgeInvalid(player, sessionId,
                record.phase == DeploymentPhase.ACTIVE ? record.issueToken : null);
        if (record.phase != DeploymentPhase.ACTIVE) {
            resetLifeBoundary(player);
            holdPlayer(player);
        }
    }

    /** Per-player enforcement: waiting containment plus stale/foreign kit removal. */
    public void tickPlayer(ServerPlayer player) {
        if (isVehicleTestMode(player == null ? null : player.getUUID())) {
            return;
        }
        if (requireParticipant(player) != null) {
            handleNonParticipant(player);
            return;
        }
        lobbyPlayers.remove(player.getUUID());
        DeploymentRecord record = records.computeIfAbsent(player.getUUID(), ignored ->
                newWaitingRecord(gameTick(), player.getUUID()));
        updateReady(record, gameTick());
        UUID validToken = record.phase == DeploymentPhase.ACTIVE ? record.issueToken : null;
        KitProvenance.purgeInvalid(player, sessionId, validToken);
        if (record.phase == DeploymentPhase.ACTIVE) {
            LoadoutService loadout = LoadoutService.get(player).orElse(null);
            long now = gameTick();
            boolean deepArmorScan = now >= record.nextDeepInventoryScanTick;
            ActionResult inventory = loadout == null
                    ? ActionResult.failure(ActionResult.Code.LOADOUT_INCOMPLETE,
                    "配装服务尚未启动")
                    : loadout.validateActiveInventory(player, sessionId, record.issueToken,
                    deepArmorScan);
            if (inventory.success() && deepArmorScan) {
                record.nextDeepInventoryScanTick = now + 20L;
            }
            if (!inventory.success()) {
                if (loadout != null) {
                    loadout.clearManagedCombatInventory(player);
                }
                beginWaiting(record, now, true, player.getUUID());
                KitProvenance.purgeAllIssued(player);
                resetLifeBoundary(player);
                holdPlayer(player);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        inventory.message()));
                return;
            }
        }
        if (record.phase != DeploymentPhase.ACTIVE) {
            holdPlayer(player);
        }
    }

    /** Once-per-second cleanup called alongside the battle service heartbeat. */
    public void tick() {
        long now = gameTick();
        records.values().forEach(record -> updateReady(record, now));
        BattleService.get(server).ifPresent(battle -> records.entrySet().removeIf(entry ->
                server.getPlayerList().getPlayer(entry.getKey()) == null
                        && battle.factionOf(entry.getKey()).isEmpty()));
    }

    public DeploymentView viewFor(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        long now = gameTick();
        if (!isParticipant(player.getUUID())) {
            return new DeploymentView(DeploymentPhase.WAITING, 0L, now, now, 0L,
                    null, false, false, false, false, List.of());
        }
        DeploymentRecord record = records.computeIfAbsent(player.getUUID(), ignored ->
                newWaitingRecord(now, player.getUUID()));
        updateReady(record, now);
        List<DeploymentPoint> points = pointsFor(player);
        boolean canDeploy = record.phase == DeploymentPhase.READY
                && record.issuedLifeSerial != record.lifeSerial
                && record.selectedPointId != null
                && points.stream().anyMatch(point -> point.id().equals(record.selectedPointId))
                && BattleService.get(player).flatMap(battle ->
                        battle.squadOf(player.getUUID())).isPresent();
        boolean canResupply = record.phase == DeploymentPhase.ACTIVE
                && now >= record.nextResupplyGameTick && atOwnMainBase(player);
        return new DeploymentView(record.phase, record.revision, now, record.eligibleGameTick,
                record.nextResupplyGameTick, record.selectedPointId,
                record.phase != DeploymentPhase.ACTIVE,
                record.phase != DeploymentPhase.ACTIVE,
                canDeploy, canResupply, points);
    }

    /** Returns the caller's main base, faction beacons and own-squad rally radios. */
    public List<DeploymentPoint> pointsFor(ServerPlayer player) {
        if (player == null || player.server != server) {
            return List.of();
        }
        Faction faction = BattleService.get(player).flatMap(battle ->
                battle.factionOf(player.getUUID())).orElse(null);
        if (faction == null) {
            return List.of();
        }
        List<DeploymentPoint> points = new ArrayList<>(
                DeploymentSavedData.MAX_FIELD_POINTS_PER_FACTION + 1);
        savedData.mainBase(faction).ifPresent(points::add);
        savedData.fieldPoints(faction).forEach(field -> points.add(
                new DeploymentPoint(field.id(), field.faction(), field.dimension(),
                        field.spawnPosition(), field.yaw(),
                        DeploymentPoint.DEFAULT_SUPPLY_RADIUS,
                        DeploymentPointKind.FIELD_BEACON)));
        BattleService battle = BattleService.get(player).orElse(null);
        String formationId = battle == null ? null
                : battle.formationOf(player.getUUID()).orElse(null);
        SquadCallsign squad = battle == null ? null
                : battle.squadOf(player.getUUID()).orElse(null);
        savedData.rallies(faction, formationId, squad).forEach(rally -> points.add(
                new DeploymentPoint(rally.id(), rally.faction(), rally.dimension(),
                        rally.spawnPosition(), rally.yaw(),
                        DeploymentPoint.DEFAULT_SUPPLY_RADIUS,
                        DeploymentPointKind.RALLY)));
        return List.copyOf(points);
    }

    /** Commits a just-placed radio as an own-squad rally after all authority checks. */
    public ActionResult placeRally(ServerPlayer player, ServerLevel level, BlockPos anchor) {
        ActionResult actorError = requireParticipant(player);
        if (actorError != null) return actorError;
        if (!isActive(player.getUUID()) || level == null || anchor == null
                || player.serverLevel() != level
                || !level.getBlockState(anchor).is(InfantryBlocks.RALLY_RADIO.get())) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "只有已部署玩家才能放置真实队包无线电");
        }
        BattleService battle = BattleService.get(player).orElse(null);
        FormationService formations = FormationService.get(player).orElse(null);
        Faction faction = battle == null ? null
                : battle.factionOf(player.getUUID()).orElse(null);
        String formationId = battle == null ? null
                : battle.formationOf(player.getUUID()).orElse(null);
        SquadCallsign squad = battle == null ? null
                : battle.squadOf(player.getUUID()).orElse(null);
        FormationDefinition formation = formations == null ? null
                : formations.selectedFormation(player.getUUID()).orElse(null);
        FormationDeployablePolicy policy = formation == null ? null
                : formation.capabilities().rally();
        if (faction == null || formationId == null || squad == null || policy == null
                || !policy.enabled()) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "当前编制未启用队包");
        }
        boolean leader = battle.isSquadLeader(player.getUUID());
        boolean commander = battle.isCommander(player.getUUID());
        if (!player.hasPermissions(BattleRules.ADMIN_PERMISSION_LEVEL)
                && (!leader || !policy.squadLeaderCanPlace())
                && (!commander || !policy.commanderCanPlace())) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                    "当前编制只允许获授权的小队长或指挥官放置队包");
        }
        long gameTime = server.overworld().getGameTime();
        long cooldownRemaining = savedData.rallyCooldownRemainingTicks(
                faction, formationId, squad, gameTime);
        if (cooldownRemaining > 0L) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "本小队的队包部署冷却中，还需 "
                            + formatCooldown(cooldownRemaining));
        }
        if (savedData.rallies(faction, formationId, squad).size() >= policy.maxActive()) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "本小队的队包数量已达编制上限");
        }
        BlockPos spawn = findSafeFeet(level, anchor.above()).orElse(null);
        if (spawn == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "队包附近没有安全落脚位置");
        }
        UUID pointId = nextDeploymentPointId();
        RallyDeploymentPoint rally = new RallyDeploymentPoint(pointId, faction, formationId,
                squad, level.dimension().location(), anchor, spawn,
                normalizeYaw(player.getYRot()));
        if (!savedData.putRally(rally)) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "队包位置或部署点身份发生冲突");
        }
        if (!(level.getBlockEntity(anchor) instanceof RallyRadioBlockEntity radio)) {
            savedData.removeRally(rally.dimension(), rally.anchorPosition());
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "队包生命组件缺失，放置已回滚");
        }
        radio.initialize(pointId, faction, formationId, squad, policy.maxHealth());
        savedData.startRallyCooldown(faction, formationId, squad,
                gameTime + RALLY_SQUAD_COOLDOWN_TICKS);
        return ActionResult.ok("已放置 " + squad.id() + " 小队队包，生命值 "
                + policy.maxHealth() + "，部署冷却 8 分钟");
    }

    private static String formatCooldown(long ticks) {
        long seconds = ticks / 20L + (ticks % 20L == 0L ? 0L : 1L);
        long minutes = seconds / 60L;
        long remainingSeconds = seconds % 60L;
        if (minutes == 0L) {
            return remainingSeconds + " 秒";
        }
        if (remainingSeconds == 0L) {
            return minutes + " 分钟";
        }
        return minutes + " 分 " + remainingSeconds + " 秒";
    }

    public ActionResult removeRally(ServerPlayer player, ServerLevel level, BlockPos anchor) {
        RallyDeploymentPoint rally = level == null ? null
                : savedData.rallyAt(level.dimension().location(), anchor).orElse(null);
        if (player == null || rally == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET, "该位置没有有效队包");
        }
        BattleService battle = BattleService.get(player).orElse(null);
        boolean ownsSquad = battle != null
                && battle.factionOf(player.getUUID()).filter(rally.faction()::equals).isPresent()
                && battle.formationOf(player.getUUID()).filter(rally.formationId()::equals).isPresent()
                && battle.squadOf(player.getUUID()).filter(rally.squad()::equals).isPresent();
        if (!player.hasPermissions(BattleRules.ADMIN_PERMISSION_LEVEL)
                && !(ownsSquad && (battle.isSquadLeader(player.getUUID())
                || battle.isCommander(player.getUUID())))) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                    "只有所属小队长、所属指挥官或管理员能主动撤收队包");
        }
        level.destroyBlock(anchor, false, player);
        return ActionResult.ok("已撤收队包");
    }

    public void onRallyRemoved(ServerLevel level, BlockPos anchor) {
        if (level == null || anchor == null) return;
        savedData.removeRally(level.dimension().location(), anchor)
                .ifPresent(removed -> clearSelections(removed.id()));
    }

    public Optional<DeploymentPoint> mainBase(Faction faction) {
        return savedData.mainBase(faction);
    }

    /** Returns a vehicle origin only when its durable record and loaded physical block agree. */
    public Optional<VehicleDeploymentPoint> vehicleDeploymentPoint(Faction faction) {
        VehicleDeploymentPoint point = savedData.vehiclePoint(faction).orElse(null);
        if (point == null) {
            return Optional.empty();
        }
        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION,
                point.dimension());
        ServerLevel level = server.getLevel(dimensionKey);
        return level == null ? Optional.empty()
                : validatedVehicleDeploymentPoint(level, point.anchorPosition());
    }

    /** Physical-query entry used by empty-hand interaction and lifecycle checks. */
    public Optional<VehicleDeploymentPoint> vehicleDeploymentPointAt(ServerLevel level,
                                                                      BlockPos anchorPosition) {
        if (level == null || level.getServer() != server || anchorPosition == null) {
            return Optional.empty();
        }
        return validatedVehicleDeploymentPoint(level, anchorPosition);
    }

    public ActionResult bindVehicleDeployment(ServerPlayer administrator, Faction faction,
                                               ServerLevel level, BlockPos anchorPosition) {
        ActionResult actorError = validateActor(administrator);
        if (actorError != null) {
            return actorError;
        }
        if (!administrator.hasPermissions(BattleRules.ADMIN_PERMISSION_LEVEL)) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                    "需要服务端管理员权限");
        }
        return bindVehicleDeploymentAuthorized(faction, level, anchorPosition);
    }

    public ActionResult bindVehicleDeployment(CommandSourceStack administrator, Faction faction,
                                               ServerLevel level, BlockPos anchorPosition) {
        ActionResult sourceError = validateCommandSource(administrator);
        return sourceError == null
                ? bindVehicleDeploymentAuthorized(faction, level, anchorPosition)
                : sourceError;
    }

    public ActionResult rotateVehicleDeployment(ServerPlayer administrator, ServerLevel level,
                                                 BlockPos anchorPosition) {
        ActionResult actorError = validateActor(administrator);
        if (actorError != null) {
            return actorError;
        }
        if (!administrator.hasPermissions(BattleRules.ADMIN_PERMISSION_LEVEL)) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                    "需要服务端管理员权限");
        }
        if (level == null || level.getServer() != server || anchorPosition == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "载具部署方块维度或位置无效");
        }
        BlockState original = level.getBlockState(anchorPosition);
        if (!original.is(InfantryBlocks.VEHICLE_DEPLOYMENT.get())) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "目标位置没有载具部署方块");
        }
        Direction nextFacing = original.getValue(VehicleDeploymentBlock.FACING)
                .getClockWise();
        VehicleDeploymentPoint previous = savedData.vehiclePointAt(
                level.dimension().location(), anchorPosition).orElse(null);
        if (previous != null) {
            if (original.getValue(VehicleDeploymentBlock.MODE).faction()
                    .filter(previous.faction()::equals).isEmpty()) {
                return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                        "载具部署方块阵营状态与持久化记录不一致");
            }
            VehicleDeploymentPoint replacement = new VehicleDeploymentPoint(
                    previous.faction(), previous.dimension(), previous.anchorPosition(),
                    nextFacing);
            if (!savedData.bindVehiclePoint(replacement)) {
                return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                        "载具部署方块朝向未能持久化");
            }
        }
        try {
            if (!level.setBlock(anchorPosition,
                    original.setValue(VehicleDeploymentBlock.FACING, nextFacing),
                    Block.UPDATE_ALL)) {
                rollbackVehiclePoint(previous, level.dimension().location(), anchorPosition);
                return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                        "载具部署方块旋转失败，记录已回滚");
            }
        } catch (RuntimeException exception) {
            rollbackVehiclePoint(previous, level.dimension().location(), anchorPosition);
            WokInfantryMod.LOGGER.error("Rolled back vehicle deployment rotation at {} {}",
                    level.dimension().location(), anchorPosition, exception);
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "载具部署方块旋转异常，记录已回滚");
        }
        return ActionResult.ok("载具部署箭头已转向 " + nextFacing.getName()
                + "（yaw=" + nextFacing.toYRot() + "）");
    }

    /** Idempotent physical-lifecycle callback; chunk unload does not invoke this method. */
    public void onVehicleDeploymentRemoved(ServerLevel level, BlockPos anchorPosition) {
        if (level == null || level.getServer() != server || anchorPosition == null) {
            return;
        }
        savedData.removeVehiclePoint(level.dimension().location(), anchorPosition);
    }

    public ActionResult removeVehicleDeployment(CommandSourceStack administrator,
                                                ServerLevel level, BlockPos anchorPosition) {
        ActionResult sourceError = validateCommandSource(administrator);
        if (sourceError != null) {
            return sourceError;
        }
        if (level == null || level.getServer() != server || anchorPosition == null
                || !level.getBlockState(anchorPosition)
                .is(InfantryBlocks.VEHICLE_DEPLOYMENT.get())) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "目标位置没有载具部署方块");
        }
        if (!level.destroyBlock(anchorPosition, true)) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "载具部署方块未能移除，原绑定未修改");
        }
        onVehicleDeploymentRemoved(level, anchorPosition);
        return ActionResult.ok("已移除载具部署方块及其阵营绑定");
    }

    private ActionResult bindVehicleDeploymentAuthorized(Faction faction, ServerLevel level,
                                                          BlockPos anchorPosition) {
        if (faction == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET, "阵营缺失");
        }
        if (level == null || level.getServer() != server || anchorPosition == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "载具部署方块维度或位置无效");
        }
        if (level.dimension().equals(HOLDING_LEVEL) || level.dimension().equals(LOBBY_LEVEL)) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "内部等待维度不能设置载具部署方块");
        }
        if (!level.getWorldBorder().isWithinBounds(anchorPosition)) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "载具部署方块超出世界边界");
        }
        BlockState original = level.getBlockState(anchorPosition);
        if (!original.is(InfantryBlocks.VEHICLE_DEPLOYMENT.get())) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "目标位置必须先放置真实载具部署方块");
        }
        VehicleDeploymentPoint previous = savedData.vehiclePointAt(
                level.dimension().location(), anchorPosition).orElse(null);
        VehicleDeploymentPoint replacement = new VehicleDeploymentPoint(faction,
                level.dimension().location(), anchorPosition,
                original.getValue(VehicleDeploymentBlock.FACING));
        if (!savedData.bindVehiclePoint(replacement)) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "该阵营已有其他载具部署方块；请先移除旧方块");
        }
        DeploymentBeaconMode requestedMode = DeploymentBeaconMode.fromFaction(faction);
        try {
            if (original.getValue(VehicleDeploymentBlock.MODE) != requestedMode
                    && !level.setBlock(anchorPosition,
                    original.setValue(VehicleDeploymentBlock.MODE, requestedMode),
                    Block.UPDATE_ALL)) {
                rollbackVehiclePoint(previous, replacement.dimension(), anchorPosition);
                return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                        "载具部署方块阵营颜色更新失败，绑定已回滚");
            }
        } catch (RuntimeException exception) {
            rollbackVehiclePoint(previous, replacement.dimension(), anchorPosition);
            WokInfantryMod.LOGGER.error("Rolled back vehicle deployment binding at {} {}",
                    replacement.dimension(), anchorPosition, exception);
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "载具部署方块状态更新异常，绑定已回滚");
        }
        return ActionResult.ok("已将载具部署方块绑定为 " + faction.id()
                + " 方，箭头朝向 " + replacement.facing().getName()
                + "（yaw=" + replacement.yaw() + "）");
    }

    private Optional<VehicleDeploymentPoint> validatedVehicleDeploymentPoint(
            ServerLevel level, BlockPos anchorPosition) {
        if (!level.hasChunkAt(anchorPosition)) {
            return Optional.empty();
        }
        VehicleDeploymentPoint point = savedData.vehiclePointAt(
                level.dimension().location(), anchorPosition).orElse(null);
        if (point == null) {
            return Optional.empty();
        }
        BlockState state = level.getBlockState(anchorPosition);
        if (!state.is(InfantryBlocks.VEHICLE_DEPLOYMENT.get())
                || state.getValue(VehicleDeploymentBlock.FACING) != point.facing()
                || state.getValue(VehicleDeploymentBlock.MODE).faction()
                .filter(point.faction()::equals).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(point);
    }

    private void rollbackVehiclePoint(VehicleDeploymentPoint previous,
                                      ResourceLocation dimension, BlockPos anchorPosition) {
        savedData.removeVehiclePoint(dimension, anchorPosition);
        if (previous != null && !savedData.bindVehiclePoint(previous)) {
            WokInfantryMod.LOGGER.error(
                    "Could not restore vehicle deployment point after block-state failure: {}",
                    previous);
        }
    }

    /** Permission-two player entry used by direct interaction with an existing beacon. */
    public ActionResult bindBeacon(ServerPlayer administrator, Faction faction,
                                   ServerLevel level, BlockPos anchorPosition, float yaw) {
        ActionResult actorError = validateActor(administrator);
        if (actorError != null) {
            return actorError;
        }
        if (!administrator.hasPermissions(BattleRules.ADMIN_PERMISSION_LEVEL)) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                    "需要服务端管理员权限");
        }
        return bindBeaconAuthorized(faction, level, anchorPosition, yaw);
    }

    /** Permission-two command entry for console/RCON workflows after the block is placed. */
    public ActionResult bindBeacon(CommandSourceStack administrator, Faction faction,
                                   ServerLevel level, BlockPos anchorPosition, float yaw) {
        ActionResult sourceError = validateCommandSource(administrator);
        if (sourceError != null) {
            return sourceError;
        }
        return bindBeaconAuthorized(faction, level, anchorPosition, yaw);
    }

    /** Returns a faction only when persisted and physical binding state agree. */
    public Optional<Faction> beaconFactionAt(ServerLevel level, BlockPos anchorPosition) {
        if (level == null || level.getServer() != server || anchorPosition == null) {
            return Optional.empty();
        }
        BlockState state = level.getBlockState(anchorPosition);
        if (!state.is(InfantryBlocks.DEPLOYMENT_BEACON.get())) {
            return Optional.empty();
        }
        FieldDeploymentPoint point = savedData.fieldPointAt(level.dimension().location(),
                anchorPosition).orElse(null);
        if (point == null) {
            return Optional.empty();
        }
        return state.getValue(DeploymentBeaconBlock.MODE).faction()
                .filter(point.faction()::equals);
    }

    /** Idempotent physical-lifecycle callback; chunk unloads must never call this method. */
    public void onBeaconRemoved(ServerLevel level, BlockPos anchorPosition) {
        if (level == null || level.getServer() != server || anchorPosition == null) {
            return;
        }
        FieldDeploymentPoint current = savedData.fieldPointAt(level.dimension().location(),
                anchorPosition).orElse(null);
        if (current == null) {
            return;
        }
        savedData.removeFieldPoint(current.dimension(), current.anchorPosition(), current.id())
                .ifPresent(removed -> clearSelections(removed.id()));
    }

    /** Permission-two command entry that removes the block and its binding exactly once. */
    public ActionResult removeBeacon(CommandSourceStack administrator, ServerLevel level,
                                     BlockPos anchorPosition) {
        ActionResult sourceError = validateCommandSource(administrator);
        if (sourceError != null) {
            return sourceError;
        }
        if (level == null || level.getServer() != server || anchorPosition == null
                || !level.getBlockState(anchorPosition)
                .is(InfantryBlocks.DEPLOYMENT_BEACON.get())) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "目标位置没有部署信标");
        }
        if (!level.destroyBlock(anchorPosition, true)) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "部署信标未能移除，原绑定未修改");
        }
        // DeploymentBeaconBlock.onRemove normally performs this synchronously. Keep the explicit
        // call so command removal is still safe if another mod suppresses the block callback.
        onBeaconRemoved(level, anchorPosition);
        return ActionResult.ok("已移除部署信标及其前线部署点");
    }

    private ActionResult bindBeaconAuthorized(Faction faction, ServerLevel level,
                                              BlockPos anchorPosition, float yaw) {
        if (faction == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET, "阵营缺失");
        }
        if (level == null || level.getServer() != server || anchorPosition == null
                || !Float.isFinite(yaw)) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "信标维度、位置或朝向无效");
        }
        if (level.dimension().equals(HOLDING_LEVEL) || level.dimension().equals(LOBBY_LEVEL)) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "内部等待维度不能设置部署信标");
        }
        if (!level.getWorldBorder().isWithinBounds(anchorPosition)) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "部署信标超出世界边界");
        }
        BlockState originalState = level.getBlockState(anchorPosition);
        if (!originalState.is(InfantryBlocks.DEPLOYMENT_BEACON.get())) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "目标位置必须先放置真实部署信标方块");
        }
        BlockPos safeSpawn = findSafeFeet(level, anchorPosition.above()).orElse(null);
        if (safeSpawn == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "部署信标附近没有安全落脚位置");
        }

        ResourceLocation dimension = level.dimension().location();
        FieldDeploymentPoint current = savedData.fieldPointAt(dimension, anchorPosition)
                .orElse(null);
        UUID pointId = current != null && current.faction() == faction
                ? current.id() : nextDeploymentPointId();
        FieldDeploymentPoint replacement = new FieldDeploymentPoint(pointId, faction, dimension,
                anchorPosition, safeSpawn, normalizeYaw(yaw));
        boolean persisted = current == null
                ? savedData.putFieldPoint(replacement)
                : savedData.replaceFieldPointAtAnchor(replacement, current.id());
        if (!persisted) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "该阵营部署信标已达上限，或部署点身份发生冲突");
        }

        DeploymentBeaconMode requestedMode = DeploymentBeaconMode.fromFaction(faction);
        try {
            if (originalState.getValue(DeploymentBeaconBlock.MODE) != requestedMode
                    && !level.setBlock(anchorPosition,
                    originalState.setValue(DeploymentBeaconBlock.MODE, requestedMode), 3)) {
                rollbackBeaconBinding(current, replacement);
                return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                        "部署信标模式未能更新，绑定已回滚");
            }
        } catch (RuntimeException exception) {
            rollbackBeaconBinding(current, replacement);
            WokInfantryMod.LOGGER.error("Rolled back deployment beacon binding at {} {}",
                    dimension, anchorPosition, exception);
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "部署信标模式更新失败，绑定已回滚");
        }
        if (current != null && !current.id().equals(replacement.id())) {
            clearSelections(current.id());
        }
        return ActionResult.ok("已将部署信标绑定为 " + faction.id()
                + " 方前线部署点，安全落脚点 " + safeSpawn.toShortString());
    }

    private void rollbackBeaconBinding(FieldDeploymentPoint previous,
                                       FieldDeploymentPoint replacement) {
        savedData.removeFieldPoint(replacement.dimension(), replacement.anchorPosition(),
                replacement.id());
        if (previous != null && !savedData.putFieldPoint(previous)) {
            WokInfantryMod.LOGGER.error(
                    "Could not restore deployment beacon binding {} after block-state failure",
                    previous.id());
        }
    }

    private UUID nextDeploymentPointId() {
        UUID candidate;
        do {
            candidate = UUID.randomUUID();
        } while (savedData.containsPointId(candidate));
        return candidate;
    }

    public DeploymentPhase phase(UUID playerId) {
        DeploymentRecord record = playerId == null ? null : records.get(playerId);
        if (record == null) {
            return DeploymentPhase.WAITING;
        }
        updateReady(record, gameTick());
        return record.phase;
    }

    public boolean isActive(UUID playerId) {
        DeploymentRecord record = playerId == null ? null : records.get(playerId);
        return record != null && record.phase == DeploymentPhase.ACTIVE;
    }

    /** Server-only authority used to stamp supplemental items into the current managed life. */
    public Optional<UUID> activeIssueToken(ServerPlayer player) {
        if (player == null || player.server != server) {
            return Optional.empty();
        }
        DeploymentRecord record = records.get(player.getUUID());
        return record != null && record.phase == DeploymentPhase.ACTIVE
                ? Optional.ofNullable(record.issueToken) : Optional.empty();
    }

    public boolean isWaitingParticipant(UUID playerId) {
        DeploymentRecord record = playerId == null ? null : records.get(playerId);
        return playerId != null && (lobbyPlayers.contains(playerId)
                || record != null && record.phase != DeploymentPhase.ACTIVE);
    }

    /** True only for the current server process; this flag is intentionally never persisted. */
    public boolean isVehicleTestMode(UUID playerId) {
        return playerId != null && vehicleTestPlayers.contains(playerId);
    }

    /**
     * Temporarily releases one player from deployment containment so an administrator can place,
     * mount and drive real Superb Warfare vehicles. Turning it off immediately restores the
     * authoritative deployment state; restarting or resetting the battle also clears the flag.
     */
    public ActionResult setVehicleTestMode(CommandSourceStack administrator,
                                           ServerPlayer target, boolean enabled) {
        ActionResult sourceError = validateCommandSource(administrator);
        if (sourceError != null) {
            return sourceError;
        }
        ActionResult targetError = validateActor(target);
        if (targetError != null) {
            return targetError;
        }
        UUID targetId = target.getUUID();
        if (enabled) {
            vehicleTestPlayers.add(targetId);
            prepareVehicleTestPlayer(target);
            return ActionResult.ok("已为 " + target.getGameProfile().getName()
                    + " 开启载具测试模式：已切换创造并暂停部署、配装和乘坐限制");
        }

        vehicleTestPlayers.remove(targetId);
        if (isParticipant(targetId)) {
            if (isActive(targetId)) {
                redeploy(target);
            } else {
                tickPlayer(target);
            }
        } else {
            releaseUnmanagedPlayer(target, true);
        }
        return ActionResult.ok("已为 " + target.getGameProfile().getName()
                + " 关闭载具测试模式并恢复正常战局限制");
    }

    public boolean isInternalTransition(UUID playerId) {
        return playerId != null && releaseInProgress.contains(playerId);
    }

    /** Pickup/use guard for equipment that came from the deployment issuer. */
    public boolean isValidIssuedStack(ServerPlayer player, ItemStack stack) {
        if (player == null || player.server != server || !KitProvenance.isIssued(stack)) {
            return false;
        }
        DeploymentRecord record = records.get(player.getUUID());
        return record != null && record.phase == DeploymentPhase.ACTIVE
                && KitProvenance.isValid(stack, sessionId, player.getUUID(), record.issueToken);
    }

    public boolean canChangeClass(UUID playerId) {
        return !isActive(playerId);
    }

    public boolean canChangeSquad(UUID playerId) {
        return !isActive(playerId);
    }

    public ActionResult selectPoint(ServerPlayer player, UUID pointId) {
        ActionResult actorError = requireParticipant(player);
        if (actorError != null) {
            return actorError;
        }
        DeploymentRecord record = records.computeIfAbsent(player.getUUID(), ignored ->
                newWaitingRecord(gameTick(), player.getUUID()));
        if (record.phase == DeploymentPhase.ACTIVE) {
            return ActionResult.failure(ActionResult.Code.NOT_WAITING_FOR_DEPLOYMENT,
                    "存活期间不能重新选择部署点，请先重新部署");
        }
        DeploymentPoint point = pointId == null ? null : pointsFor(player).stream()
                .filter(candidate -> pointId.equals(candidate.id())).findFirst().orElse(null);
        if (point == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "部署点不存在，或不属于你当前的阵营、编制和小队");
        }
        if (!point.id().equals(record.selectedPointId)) {
            record.selectedPointId = point.id();
            touch(record);
        }
        return ActionResult.ok(switch (point.kind()) {
            case MAIN_BASE -> "已选择己方主基地";
            case FIELD_BEACON -> "已选择己方前线部署信标";
            case RALLY -> "已选择本小队队包";
        });
    }

    public ActionResult deploy(ServerPlayer player) {
        ActionResult actorError = requireParticipant(player);
        if (actorError != null) {
            return actorError;
        }
        if (player.isDeadOrDying() || player.getHealth() <= 0.0F) {
            // A modified client can still send the deploy packet while the vanilla death
            // screen is open. Never install a kit into the obsolete pre-clone entity.
            return ActionResult.failure(ActionResult.Code.NOT_WAITING_FOR_DEPLOYMENT,
                    "请先完成重生，再申请部署");
        }
        long now = gameTick();
        DeploymentRecord record = records.computeIfAbsent(player.getUUID(), ignored ->
                newWaitingRecord(now, player.getUUID()));
        updateReady(record, now);
        if (record.phase == DeploymentPhase.WAITING) {
            return ActionResult.failure(ActionResult.Code.RESPAWN_COOLDOWN,
                    "部署倒计时尚未结束");
        }
        if (record.phase != DeploymentPhase.READY) {
            return ActionResult.failure(ActionResult.Code.NOT_WAITING_FOR_DEPLOYMENT,
                    "你当前不处于可部署状态");
        }
        if (record.issuedLifeSerial == record.lifeSerial) {
            return ActionResult.failure(ActionResult.Code.KIT_ALREADY_ISSUED,
                    "当前生命已经发放过装备");
        }

        BattleService battle = BattleService.get(player).orElse(null);
        if (battle == null || battle.factionOf(player.getUUID()).isEmpty()) {
            return ActionResult.failure(ActionResult.Code.NOT_ASSIGNED, "尚未分配战局阵营");
        }
        if (battle.squadOf(player.getUUID()).isEmpty()) {
            return ActionResult.failure(ActionResult.Code.NOT_IN_SQUAD, "必须先加入小队");
        }
        DeploymentPoint point = selectedPoint(player, record).orElse(null);
        if (point == null) {
            return ActionResult.failure(ActionResult.Code.DEPLOYMENT_POINT_REQUIRED,
                    "请选择有效的己方部署点");
        }
        if (point.kind() == DeploymentPointKind.FIELD_BEACON
                && !validateFieldPointAnchor(point)) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "前线部署信标已被拆除或绑定已改变，请重新选择部署点");
        }
        if (point.kind() == DeploymentPointKind.RALLY && !validateRallyAnchor(point)) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "小队队包已被摧毁、撤收或归属改变，请重新选择部署点");
        }
        Destination destination = findSafeDestination(point).orElse(null);
        if (destination == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "部署点当前没有安全落脚位置");
        }
        LoadoutService loadout = LoadoutService.get(player).orElse(null);
        if (loadout == null) {
            return ActionResult.failure(ActionResult.Code.LOADOUT_INCOMPLETE,
                    "配装服务尚未启动");
        }
        LoadoutService.PreparedLoadout prepared = loadout.prepareDeploymentLoadout(player);
        if (!prepared.result().success()) {
            return prepared.result();
        }
        LoadoutService.InventoryClearPlan clearPlan = loadout.prepareInventoryClear(
                player, prepared);
        if (!clearPlan.result().success()) {
            return clearPlan.result();
        }

        UUID issueToken = UUID.randomUUID();
        boolean grantInitialReserve = !initialReserveRecipients.contains(player.getUUID());
        // Close/reset while the record is still READY. PlayerContainerEvent.Close synchronously
        // re-enters tickPlayer; doing this after kit installation would purge the new token.
        resetLifeBoundary(player);
        ActionResult inventoryCommit = loadout.commitDeploymentInventory(player, clearPlan,
                prepared, sessionId, issueToken);
        if (!inventoryCommit.success()) {
            holdPlayer(player);
            return inventoryCommit;
        }
        if (!preservesAdministratorCreative(player)
                && !player.setGameMode(GameType.SURVIVAL)) {
            KitProvenance.purgeAllIssued(player);
            holdPlayer(player);
            return ActionResult.failure(ActionResult.Code.INVENTORY_BLOCKED,
                    "服务器阻止切换到生存模式，部署已安全取消");
        }

        long previousIssuedLifeSerial = record.issuedLifeSerial;
        record.phase = DeploymentPhase.ACTIVE;
        record.issuedLifeSerial = record.lifeSerial;
        record.issueToken = issueToken;
        record.nextResupplyGameTick = now + RESUPPLY_COOLDOWN_TICKS;
        record.nextDeepInventoryScanTick = now;
        touch(record);
        AmmoSupplyService.InitialReserveGrant initialReserve =
                AmmoSupplyService.InitialReserveGrant.none();
        try {
            if (grantInitialReserve) {
                initialReserve = AmmoSupplyService.grantInitialReserve(
                        player, sessionId, issueToken);
            }
            player.setInvulnerable(false);
            teleportInternally(player, destination, point.yaw());
        } catch (RuntimeException exception) {
            record.phase = DeploymentPhase.READY;
            record.issuedLifeSerial = previousIssuedLifeSerial;
            record.issueToken = null;
            record.nextResupplyGameTick = 0L;
            record.nextDeepInventoryScanTick = 0L;
            touch(record);
            KitProvenance.purgeAllIssued(player);
            resetLifeBoundary(player);
            holdPlayer(player);
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "传送未完成，部署装备已回收，请重试");
        }
        if (grantInitialReserve && initialReserve.ammunitionTypes() > 0) {
            initialReserveRecipients.add(player.getUUID());
            if (!initialReserve.complete()) {
                player.sendSystemMessage(Component.literal("首次部署备用弹药仅装入 "
                        + initialReserve.addedRounds() + "/"
                        + initialReserve.requestedRounds()
                        + " 发：背包空间不足，请调整配装或弹药上限"));
            }
        }
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "message.wok_infantry.applied"));
        return ActionResult.ok(switch (point.kind()) {
            case MAIN_BASE -> "已从己方主基地部署";
            case FIELD_BEACON -> "已从己方前线部署信标部署";
            case RALLY -> "已从本小队队包部署";
        });
    }

    public ActionResult redeploy(ServerPlayer player) {
        ActionResult actorError = requireParticipant(player);
        if (actorError != null) {
            return actorError;
        }
        DeploymentRecord record = records.get(player.getUUID());
        if (record == null || record.phase != DeploymentPhase.ACTIVE) {
            return ActionResult.failure(ActionResult.Code.NOT_WAITING_FOR_DEPLOYMENT,
                    "你当前没有可放弃的存活部署");
        }
        beginWaiting(record, gameTick(), true, player.getUUID());
        KitProvenance.purgeAllIssued(player);
        player.inventoryMenu.broadcastChanges();
        resetLifeBoundary(player);
        holdPlayer(player);
        return ActionResult.ok("已进入重新部署等待");
    }

    public ActionResult resupply(ServerPlayer player) {
        ActionResult actorError = requireParticipant(player);
        if (actorError != null) {
            return actorError;
        }
        long now = gameTick();
        DeploymentRecord record = records.get(player.getUUID());
        if (record == null || record.phase != DeploymentPhase.ACTIVE) {
            return ActionResult.failure(ActionResult.Code.NOT_WAITING_FOR_DEPLOYMENT,
                    "只有已部署玩家可以补给");
        }
        if (now < record.nextResupplyGameTick) {
            return ActionResult.failure(ActionResult.Code.RESUPPLY_COOLDOWN,
                    "补给冷却尚未结束");
        }
        if (!atOwnMainBase(player)) {
            return ActionResult.failure(ActionResult.Code.NOT_AT_SUPPLY,
                    "必须位于己方主基地补给范围内");
        }
        LoadoutService loadout = LoadoutService.get(player).orElse(null);
        if (loadout == null) {
            return ActionResult.failure(ActionResult.Code.LOADOUT_INCOMPLETE,
                    "配装服务尚未启动");
        }
        LoadoutService.PreparedLoadout prepared = loadout.prepareDeploymentLoadout(player);
        if (!prepared.result().success()) {
            return prepared.result();
        }
        ActionResult slotValidation = loadout.validateInstallSlots(player, prepared);
        if (!slotValidation.success()) {
            return slotValidation;
        }
        UUID replacementToken = UUID.randomUUID();
        loadout.installPreparedLoadout(player, prepared, sessionId, replacementToken);
        record.issueToken = replacementToken;
        record.nextResupplyGameTick = now + RESUPPLY_COOLDOWN_TICKS;
        record.nextDeepInventoryScanTick = now;
        touch(record);
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "message.wok_infantry.applied"));
        return ActionResult.ok("己方主基地补给完成");
    }

    public ActionResult setMainBase(ServerPlayer administrator, Faction faction) {
        return setMainBaseAt(administrator, faction, administrator == null ? null
                        : administrator.serverLevel(),
                administrator == null ? null : administrator.blockPosition(),
                administrator == null ? 0.0F : administrator.getYRot());
    }

    /** Command-friendly variant; /execute positioned/in can configure a held administrator. */
    public ActionResult setMainBaseAt(ServerPlayer administrator, Faction faction,
                                      ServerLevel level, BlockPos position, float yaw) {
        ActionResult actorError = validateActor(administrator);
        if (actorError != null) {
            return actorError;
        }
        if (!administrator.hasPermissions(BattleRules.ADMIN_PERMISSION_LEVEL)) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                    "需要服务端管理员权限");
        }
        return setMainBaseAtAuthorized(faction, level, position, yaw);
    }

    /** Permission-checked entry for explicit coordinates from players, console or RCON. */
    public ActionResult setMainBaseAt(CommandSourceStack administrator, Faction faction,
                                      ServerLevel level, BlockPos position, float yaw) {
        if (administrator == null || administrator.getServer() != server) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                    "命令源不属于当前服务器");
        }
        if (!administrator.hasPermission(BattleRules.ADMIN_PERMISSION_LEVEL)) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                    "需要服务端管理员权限");
        }
        return setMainBaseAtAuthorized(faction, level, position, yaw);
    }

    private ActionResult setMainBaseAtAuthorized(Faction faction, ServerLevel level,
                                                  BlockPos position, float yaw) {
        if (faction == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET, "阵营缺失");
        }
        if (level == null || level.getServer() != server || position == null
                || !Float.isFinite(yaw)) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "基地维度、位置或朝向无效");
        }
        if (!level.getWorldBorder().isWithinBounds(position)) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "当前位置超出世界边界");
        }
        if (level.dimension().equals(HOLDING_LEVEL) || level.dimension().equals(LOBBY_LEVEL)) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "内部等待维度不能作为主基地，原基地未修改。请在战场安全位置执行 "
                            + mainBaseSetupHint(faction));
        }
        DeploymentPoint point = new DeploymentPoint(UUID.randomUUID(), faction,
                level.dimension().location(), position,
                normalizeYaw(yaw), DeploymentPoint.DEFAULT_SUPPLY_RADIUS);
        if (findSafeDestination(point).isEmpty()) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "当前位置附近没有安全落脚位置，原基地未修改。请站到实心地面，或执行 "
                            + mainBaseSetupHint(faction));
        }
        DeploymentPoint oldPoint = savedData.mainBase(faction).orElse(null);
        savedData.setMainBase(point);
        if (oldPoint != null) {
            clearSelections(oldPoint.id());
        }
        return ActionResult.ok("已将 " + faction.id() + " 方主基地设为 "
                + level.dimension().location() + " " + position.toShortString()
                + "，朝向 " + point.yaw());
    }

    private String mainBaseSetupHint(Faction faction) {
        BlockPos spawn = server.overworld().getSharedSpawnPos();
        return "/battle deployment setbase " + faction.id() + " "
                + Level.OVERWORLD.location() + " " + spawn.getX() + " "
                + spawn.getY() + " " + spawn.getZ()
                + "（请按实际战场维度和坐标调整）";
    }

    public ActionResult clearMainBase(ServerPlayer administrator, Faction faction) {
        ActionResult actorError = validateActor(administrator);
        if (actorError != null) {
            return actorError;
        }
        if (!administrator.hasPermissions(BattleRules.ADMIN_PERMISSION_LEVEL)) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                    "需要服务端管理员权限");
        }
        return clearMainBaseAuthorized(faction);
    }

    /** Permission-checked console/RCON variant matching explicit setbase and beacon commands. */
    public ActionResult clearMainBase(CommandSourceStack administrator, Faction faction) {
        if (administrator == null || administrator.getServer() != server) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                    "命令源不属于当前服务器");
        }
        if (!administrator.hasPermission(BattleRules.ADMIN_PERMISSION_LEVEL)) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                    "需要服务端管理员权限");
        }
        return clearMainBaseAuthorized(faction);
    }

    private ActionResult clearMainBaseAuthorized(Faction faction) {
        if (faction == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET, "阵营缺失");
        }
        DeploymentPoint oldPoint = savedData.mainBase(faction).orElse(null);
        if (oldPoint == null || !savedData.clearMainBase(faction)) {
            return ActionResult.failure(ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "该阵营尚未设置主基地");
        }
        clearSelections(oldPoint.id());
        return ActionResult.ok("已清除 " + faction.id() + " 方主基地");
    }

    /** Called at authoritative roster mutation sites, not merely from packet handlers. */
    public void onRosterChanged(UUID playerId) {
        if (isVehicleTestMode(playerId)) {
            return;
        }
        DeploymentRecord record = playerId == null ? null : records.get(playerId);
        ServerPlayer online = server.getPlayerList().getPlayer(playerId);
        boolean assigned = BattleService.get(server)
                .flatMap(battle -> battle.factionOf(playerId)).isPresent();
        if (!assigned) {
            if (online != null) {
                releaseUnmanagedPlayer(online);
            } else {
                records.remove(playerId);
            }
            return;
        }
        if (record == null) {
            return;
        }
        if (record.phase == DeploymentPhase.ACTIVE) {
            beginWaiting(record, gameTick(), true, playerId);
            if (online != null) {
                KitProvenance.purgeAllIssued(online);
                online.inventoryMenu.broadcastChanges();
                resetLifeBoundary(online);
                holdPlayer(online);
            }
        } else if (record.selectedPointId != null) {
            record.selectedPointId = null;
            touch(record);
        }
    }

    public void forget(UUID playerId) {
        if (playerId != null) {
            records.remove(playerId);
        }
    }

    /** Consumes an administrator removal on login, including removals performed while offline. */
    public void revokePlayer(ServerPlayer player) {
        releaseUnmanagedPlayer(player, true);
    }

    /**
     * Prepares a match reset by rotating vehicle/provenance authority first. The new session is
     * published only after the vehicle provider accepts it, preventing a split-brain session.
     */
    public ActionResult prepareBattleResetSession() {
        UUID nextSession = UUID.randomUUID();
        FormationService formations = FormationService.get(server).orElse(null);
        if (formations == null) {
            return ActionResult.failure(ActionResult.Code.FORMATION_UNAVAILABLE,
                    "阵营编制服务尚未就绪，战局会话未重置");
        }
        ActionResult activation = formations.activateVehicleSession(nextSession);
        if (!activation.success()) {
            WokInfantryMod.LOGGER.warn(
                    "Formation vehicle session rotation to {} failed closed: {}",
                    nextSession, activation.message());
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具会话轮换失败，战局未重置：" + activation.message());
        }
        sessionId = nextSession;
        return ActionResult.ok("已轮换战局与载具会话");
    }

    /** Applies the in-memory player reset after BattleService has committed its roster reset. */
    public int finishBattleReset() {
        vehicleTestPlayers.clear();
        initialReserveRecipients.clear();
        records.clear();
        long now = gameTick();
        int failures = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            try {
                if (!isParticipant(player.getUUID())) {
                    releaseUnmanagedPlayer(player, true);
                    continue;
                }
                lobbyPlayers.remove(player.getUUID());
                KitProvenance.purgeAllIssued(player);
                DeploymentRecord record = newWaitingRecord(now, player.getUUID());
                records.put(player.getUUID(), record);
                resetLifeBoundary(player);
                holdPlayer(player);
            } catch (RuntimeException | LinkageError exception) {
                failures++;
                records.remove(player.getUUID());
                WokInfantryMod.LOGGER.error(
                        "Could not finish battle-reset deployment cleanup for player {}",
                        player.getUUID(), exception);
            }
        }
        return failures;
    }

    private DeploymentRecord newWaitingRecord(long now, UUID playerId) {
        DeploymentRecord record = new DeploymentRecord();
        record.phase = DeploymentPhase.WAITING;
        record.lifeSerial = 1L;
        record.eligibleGameTick = now + respawnDelayTicks(playerId);
        touch(record);
        return record;
    }

    private void beginWaiting(DeploymentRecord record, long now, boolean newLife,
                              UUID playerId) {
        if (newLife && record.lifeSerial < Long.MAX_VALUE) {
            record.lifeSerial++;
        }
        record.phase = DeploymentPhase.WAITING;
        record.eligibleGameTick = now + respawnDelayTicks(playerId);
        record.selectedPointId = null;
        record.issueToken = null;
        record.nextResupplyGameTick = 0L;
        record.nextDeepInventoryScanTick = 0L;
        touch(record);
    }

    private long respawnDelayTicks(UUID playerId) {
        return FormationService.get(server)
                .map(formations -> formations.respawnDelayTicks(playerId,
                        RESPAWN_DELAY_TICKS))
                .orElse(RESPAWN_DELAY_TICKS);
    }

    private void updateReady(DeploymentRecord record, long now) {
        if (record.phase == DeploymentPhase.WAITING && now >= record.eligibleGameTick) {
            record.phase = DeploymentPhase.READY;
            touch(record);
        }
    }

    private Optional<DeploymentPoint> selectedPoint(ServerPlayer player,
                                                     DeploymentRecord record) {
        if (record.selectedPointId == null) {
            return Optional.empty();
        }
        return pointsFor(player).stream()
                .filter(point -> record.selectedPointId.equals(point.id())).findFirst();
    }

    private boolean atOwnMainBase(ServerPlayer player) {
        Faction faction = BattleService.get(player).flatMap(battle ->
                battle.factionOf(player.getUUID())).orElse(null);
        DeploymentPoint point = savedData.mainBase(faction).orElse(null);
        if (point == null || !player.serverLevel().dimension().location().equals(point.dimension())) {
            return false;
        }
        Vec3 center = Vec3.atBottomCenterOf(point.position());
        double radius = point.supplyRadius();
        return player.position().distanceToSqr(center) <= radius * radius;
    }

    private boolean validateFieldPointAnchor(DeploymentPoint point) {
        FieldDeploymentPoint field = savedData.fieldPoint(point.id()).orElse(null);
        if (field == null || field.faction() != point.faction()
                || !field.dimension().equals(point.dimension())
                || !field.spawnPosition().equals(point.position())
                || Float.compare(field.yaw(), point.yaw()) != 0) {
            return false;
        }
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION,
                field.dimension());
        ServerLevel level = server.getLevel(dimension);
        if (level == null) {
            removeStaleFieldPoint(field);
            return false;
        }
        try {
            level.getChunkAt(field.anchorPosition());
            BlockState state = level.getBlockState(field.anchorPosition());
            boolean matchingBeacon = state.is(InfantryBlocks.DEPLOYMENT_BEACON.get())
                    && state.getValue(DeploymentBeaconBlock.MODE).faction()
                    .filter(field.faction()::equals).isPresent();
            if (!matchingBeacon) {
                removeStaleFieldPoint(field);
                return false;
            }
            // The persisted spawn is only the center of the bounded safety search. A player,
            // vehicle or temporary block may occupy that exact cell while a neighbouring cell is
            // still safe; findSafeDestination performs the authoritative runtime search next.
            return true;
        } catch (RuntimeException exception) {
            WokInfantryMod.LOGGER.error(
                    "Could not validate deployment beacon {} at {} {}",
                    field.id(), field.dimension(), field.anchorPosition(), exception);
            return false;
        }
    }

    private boolean validateRallyAnchor(DeploymentPoint point) {
        RallyDeploymentPoint rally = savedData.rally(point.id()).orElse(null);
        if (rally == null || rally.faction() != point.faction()
                || !rally.dimension().equals(point.dimension())
                || !rally.spawnPosition().equals(point.position())
                || Float.compare(rally.yaw(), point.yaw()) != 0) return false;
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, rally.dimension());
        ServerLevel level = server.getLevel(key);
        if (level == null) {
            onRallyMissing(rally);
            return false;
        }
        level.getChunkAt(rally.anchorPosition());
        boolean valid = level.getBlockState(rally.anchorPosition()).is(
                InfantryBlocks.RALLY_RADIO.get())
                && level.getBlockEntity(rally.anchorPosition())
                instanceof RallyRadioBlockEntity radio
                && radio.initialized() && radio.health() > 0
                && rally.id().equals(radio.deploymentPointId())
                && rally.faction() == radio.faction()
                && rally.formationId().equals(radio.formationId())
                && rally.squad() == radio.squad();
        if (!valid) onRallyMissing(rally);
        return valid;
    }

    private void onRallyMissing(RallyDeploymentPoint rally) {
        savedData.removeRally(rally.dimension(), rally.anchorPosition())
                .ifPresent(removed -> clearSelections(removed.id()));
    }

    private void removeStaleFieldPoint(FieldDeploymentPoint point) {
        savedData.removeFieldPoint(point.dimension(), point.anchorPosition(), point.id())
                .ifPresent(removed -> clearSelections(removed.id()));
    }

    private Optional<Destination> findSafeDestination(DeploymentPoint point) {
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, point.dimension());
        ServerLevel level = server.getLevel(dimension);
        if (level == null) {
            return Optional.empty();
        }
        return findSafeFeet(level, point.position()).map(feet ->
                new Destination(level, Vec3.atBottomCenterOf(feet)));
    }

    private Optional<BlockPos> findSafeFeet(ServerLevel level, BlockPos origin) {
        // Every persisted origin is permission-two-owned, so this one bounded chunk load is trusted.
        level.getChunkAt(origin);
        for (int radius = 0; radius <= 2; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    for (int y = 0; y <= 2; y++) {
                        BlockPos feet = origin.offset(x, y, z);
                        if (safeFeet(level, feet)) {
                            return Optional.of(feet.immutable());
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static boolean safeFeet(ServerLevel level, BlockPos feet) {
        if (!level.getWorldBorder().isWithinBounds(feet)
                || feet.getY() < level.getMinBuildHeight()
                || feet.getY() + 1 >= level.getMaxBuildHeight()) {
            return false;
        }
        BlockPos floor = feet.below();
        BlockState floorState = level.getBlockState(floor);
        BlockState feetState = level.getBlockState(feet);
        BlockState headState = level.getBlockState(feet.above());
        boolean dangerousFloor = floorState.is(Blocks.MAGMA_BLOCK)
                || floorState.is(Blocks.CAMPFIRE) || floorState.is(Blocks.SOUL_CAMPFIRE)
                || floorState.is(BlockTags.FIRE);
        boolean dangerousSpace = feetState.is(BlockTags.FIRE)
                || headState.is(BlockTags.FIRE)
                || feetState.is(Blocks.POWDER_SNOW) || headState.is(Blocks.POWDER_SNOW);
        return !dangerousFloor && !dangerousSpace
                && feetState.getFluidState().isEmpty()
                && headState.getFluidState().isEmpty()
                && floorState.isFaceSturdy(level, floor, Direction.UP)
                && feetState.getCollisionShape(level, feet).isEmpty()
                && headState.getCollisionShape(level, feet.above()).isEmpty();
    }

    private void holdPlayer(ServerPlayer player) {
        if (!preservesAdministratorCreative(player)
                && player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) {
            player.setGameMode(GameType.ADVENTURE);
        }
        player.setInvulnerable(true);
        player.setInvisible(true);
        player.setNoGravity(true);
        player.noPhysics = true;
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        Destination holding = holdingDestination(player);
        if (player.serverLevel() != holding.level
                || player.position().distanceToSqr(holding.position) > 4.0D) {
            teleportInternally(player, holding, 0.0F);
        }
    }

    private void prepareVehicleTestPlayer(ServerPlayer player) {
        if (player == null || player.server != server) {
            return;
        }
        player.setInvulnerable(false);
        player.setInvisible(false);
        player.setNoGravity(false);
        player.noPhysics = false;
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.setGameMode(GameType.CREATIVE);

        ResourceKey<Level> current = player.serverLevel().dimension();
        if (!current.equals(HOLDING_LEVEL) && !current.equals(LOBBY_LEVEL)) {
            return;
        }
        Faction faction = BattleService.get(player)
                .flatMap(battle -> battle.factionOf(player.getUUID())).orElse(null);
        Destination destination = savedData.mainBase(faction)
                .flatMap(this::findSafeDestination).orElseGet(this::overworldTestDestination);
        teleportInternally(player, destination, 0.0F);
    }

    private Destination overworldTestDestination() {
        ServerLevel overworld = server.overworld();
        BlockPos origin = overworld.getSharedSpawnPos();
        Vec3 position = findSafeFeet(overworld, origin)
                .map(Vec3::atBottomCenterOf)
                .orElseGet(() -> Vec3.atBottomCenterOf(origin.above()));
        return new Destination(overworld, position);
    }

    private Destination holdingDestination(ServerPlayer player) {
        Faction faction = BattleService.get(player).flatMap(battle ->
                battle.factionOf(player.getUUID())).orElse(null);
        ServerLevel level = server.getLevel(HOLDING_LEVEL);
        if (level == null) {
            // Constructor/startup validation makes this unreachable. Never fall back into a
            // battle dimension: doing so would expose tracked entities and loaded chunks.
            throw new IllegalStateException("WOK Infantry holding dimension became unavailable");
        }
        double x = faction == Faction.BLUE ? BLUE_HOLDING_X
                : faction == Faction.RED ? RED_HOLDING_X : 0.5D;
        return new Destination(level, new Vec3(x, HOLDING_Y, 0.5D));
    }

    /**
     * Runs every deployment-owned cross-dimension transition through the same guard.
     * Waiting/lobby travel protection must reject player-initiated escapes without
     * cancelling the service's own move into containment.
     */
    private void teleportInternally(ServerPlayer player, Destination destination, float yaw) {
        UUID playerId = player.getUUID();
        boolean ownsTransition = releaseInProgress.add(playerId);
        try {
            performTeleport(player, destination, yaw);
        } finally {
            if (ownsTransition) {
                releaseInProgress.remove(playerId);
            }
        }
    }

    private static void performTeleport(ServerPlayer player, Destination destination, float yaw) {
        player.teleportTo(destination.level, destination.position.x, destination.position.y,
                destination.position.z, yaw, 0.0F);
    }

    /** Clears transient combat state at every controlled life boundary. */
    private static void resetLifeBoundary(ServerPlayer player) {
        boolean deathScreenEntity = player.isDeadOrDying() || player.getHealth() <= 0.0F;
        player.stopUsingItem();
        player.closeContainer();
        player.stopRiding();
        player.removeAllEffects();
        player.clearFire();
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.setAirSupply(player.getMaxAirSupply());
        player.setAbsorptionAmount(0.0F);
        // Healing the obsolete death-screen entity makes vanilla reject the client's respawn
        // command (health > 0) and can strand the player forever. Only the post-clone entity may
        // receive a health reset.
        if (!deathScreenEntity) {
            player.setHealth(player.getMaxHealth());
        }
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0F);
        player.getFoodData().setExhaustion(0.0F);
        player.setInvisible(false);
        player.setNoGravity(false);
        player.noPhysics = false;
    }

    private void clearSelections(UUID removedPointId) {
        for (DeploymentRecord record : records.values()) {
            if (removedPointId.equals(record.selectedPointId)) {
                record.selectedPointId = null;
                touch(record);
            }
        }
    }

    private void touch(DeploymentRecord record) {
        if (record.revision < Long.MAX_VALUE) {
            record.revision++;
        }
    }

    private long gameTick() {
        return Math.max(0L, server.overworld().getGameTime());
    }

    private ActionResult validateActor(ServerPlayer player) {
        if (player == null || player.server != server) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                    "操作者不属于当前服务器");
        }
        return null;
    }

    private ActionResult validateCommandSource(CommandSourceStack source) {
        if (source == null || source.getServer() != server) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                    "命令源不属于当前服务器");
        }
        if (!source.hasPermission(BattleRules.ADMIN_PERMISSION_LEVEL)) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                    "需要服务端管理员权限");
        }
        return null;
    }

    private ActionResult requireParticipant(ServerPlayer player) {
        ActionResult actorError = validateActor(player);
        if (actorError != null) {
            return actorError;
        }
        return isParticipant(player.getUUID()) ? null
                : ActionResult.failure(ActionResult.Code.NOT_ASSIGNED,
                "该玩家未获准加入当前战局");
    }

    private boolean isParticipant(UUID playerId) {
        return playerId != null && BattleService.get(server)
                .map(battle -> battle.factionOf(playerId).isPresent()
                        && battle.formationOf(playerId).isPresent())
                .orElse(false);
    }

    private void releaseUnmanagedPlayer(ServerPlayer player) {
        releaseUnmanagedPlayer(player, false);
    }

    private void handleNonParticipant(ServerPlayer player) {
        if (player != null && lobbyPlayers.contains(player.getUUID())) {
            holdLobbyPlayer(player);
        } else {
            releaseUnmanagedPlayer(player);
        }
    }

    private void releaseUnmanagedPlayer(ServerPlayer player, boolean force) {
        if (player == null || player.server != server) {
            return;
        }
        UUID playerId = player.getUUID();
        boolean wasManaged = records.remove(playerId) != null
                || isInWaitingCell(player) || lobbyPlayers.contains(playerId);
        if ((!force && !wasManaged) || !releaseInProgress.add(playerId)) {
            return;
        }
        lobbyPlayers.add(playerId);
        try {
            discardLegacyPersonalItems(player);
            KitProvenance.purgeAllIssued(player);
            resetLifeBoundary(player);
            player.setGameMode(GameType.ADVENTURE);
            player.setInvulnerable(true);
            // A second void dimension keeps non-participants isolated from both configurable
            // battle maps and the red/blue waiting cells.
            ServerLevel lobby = server.getLevel(LOBBY_LEVEL);
            if (lobby == null) {
                throw new IllegalStateException("WOK Infantry lobby dimension became unavailable");
            }
            teleportInternally(player, new Destination(lobby, REMOVED_PLAYER_LOBBY), 0.0F);
        } finally {
            releaseInProgress.remove(playerId);
        }
    }

    private void holdLobbyPlayer(ServerPlayer player) {
        ServerLevel lobby = server.getLevel(LOBBY_LEVEL);
        if (lobby == null) {
            throw new IllegalStateException("WOK Infantry lobby dimension became unavailable");
        }
        if (!preservesAdministratorCreative(player)
                && player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) {
            player.setGameMode(GameType.ADVENTURE);
        }
        player.setInvulnerable(true);
        player.setInvisible(false);
        player.setNoGravity(false);
        player.noPhysics = false;
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        if (player.serverLevel() != lobby
                || player.position().distanceToSqr(REMOVED_PLAYER_LOBBY) > 256.0D) {
            teleportInternally(player, new Destination(lobby, REMOVED_PLAYER_LOBBY), 0.0F);
        }
    }

    private static boolean isInWaitingCell(ServerPlayer player) {
        if (!player.serverLevel().dimension().equals(HOLDING_LEVEL)) {
            return false;
        }
        return Math.abs(player.getX() - BLUE_HOLDING_X) < 64.0D
                || Math.abs(player.getX() - RED_HOLDING_X) < 64.0D;
    }

    private static boolean preservesAdministratorCreative(ServerPlayer player) {
        return player != null && AdministratorCreativePolicy.preservesCreative(
                player.hasPermissions(BattleRules.ADMIN_PERMISSION_LEVEL),
                player.gameMode.getGameModeForPlayer() == GameType.CREATIVE);
    }

    private static CompoundTag persistedData(ServerPlayer player, boolean create) {
        CompoundTag forgeData = player.getPersistentData();
        if (!forgeData.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            if (!create) {
                return null;
            }
            forgeData.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return forgeData.getCompound(Player.PERSISTED_NBT_TAG);
    }

    private static float normalizeYaw(float yaw) {
        float normalized = yaw % 360.0F;
        return normalized < 0.0F ? normalized + 360.0F : normalized;
    }

    private static final class DeploymentRecord {
        DeploymentPhase phase;
        long lifeSerial;
        long issuedLifeSerial;
        long eligibleGameTick;
        UUID selectedPointId;
        UUID issueToken;
        long nextResupplyGameTick;
        long nextDeepInventoryScanTick;
        long revision;
    }

    private record Destination(ServerLevel level, Vec3 position) {
    }
}
