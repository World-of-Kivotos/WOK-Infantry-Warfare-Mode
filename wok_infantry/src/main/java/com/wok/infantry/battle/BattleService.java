package com.wok.infantry.battle;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.deployment.DeploymentService;
import com.wok.infantry.server.FormationService;
import com.wok.infantry.support.SupportService;
import com.wok.infantry.support.SupportView;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;

/**
 * Server-authoritative battle facade used by packets, commands and the loadout service.
 * Mutations are synchronized so a class reservation and its quota check are atomic.
 */
public final class BattleService {
    private static final Pattern CLASS_ID_PATTERN = Pattern.compile("[a-z0-9_.-]{1,64}");
    private static final Pattern FORMATION_ID_PATTERN = Pattern.compile("[a-z0-9_.-]{1,64}");
    private static final Map<MinecraftServer, BattleService> INSTANCES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final MinecraftServer server;
    private final BattleSavedData data;
    private final FormationSquadCapacityRules squadCapacityRules;
    private final Map<UUID, Deque<Long>> markerRateWindows = new LinkedHashMap<>();
    private final Map<UUID, Long> observedOfflineSince = new LinkedHashMap<>();
    private final Map<SquadKickKey, Long> squadKickCooldowns = new LinkedHashMap<>();
    private long lastHeartbeatMillis = -1L;
    private long lastHistoryPruneMillis = -1L;

    private BattleService(MinecraftServer server) {
        this(server, BattleSavedData.get(server));
    }

    /** Isolated state constructor used by Forge GameTest without touching a saved match. */
    BattleService(MinecraftServer server, BattleSavedData data) {
        this(server, data, (faction, formationId, callsign) -> FormationService.get(server)
                .flatMap(service -> service.squadRule(faction, formationId, callsign.id()))
                .map(rule -> rule.capacity()).orElse(0));
    }

    /**
     * Isolated-state constructor with an isolated formation catalog boundary. Production callers
     * use the authoritative {@link FormationService}; Forge GameTests can model two formations
     * without mutating the server-global catalog while tests execute concurrently.
     */
    BattleService(MinecraftServer server, BattleSavedData data,
                  FormationSquadCapacityRules squadCapacityRules) {
        this.server = Objects.requireNonNull(server, "server");
        this.data = Objects.requireNonNull(data, "data");
        this.squadCapacityRules = Objects.requireNonNull(squadCapacityRules,
                "squadCapacityRules");
    }

    /** Eager lifecycle hook; callers may also use {@link #get(MinecraftServer)} lazily. */
    public static void start(MinecraftServer server) {
        if (server == null) {
            return;
        }
        synchronized (INSTANCES) {
            INSTANCES.computeIfAbsent(server, BattleService::new);
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

    /** Lazily creates the world-bound service, allowing network/loadout integration before event wiring. */
    public static Optional<BattleService> get(MinecraftServer server) {
        if (server == null) {
            return Optional.empty();
        }
        synchronized (INSTANCES) {
            return Optional.of(INSTANCES.computeIfAbsent(server, BattleService::new));
        }
    }

    public static Optional<BattleService> get(ServerPlayer player) {
        return player == null ? Optional.empty() : get(player.server);
    }

    public MinecraftServer server() {
        return server;
    }

    /** Ensures the identity record exists; faction/formation selection remains explicit. */
    public synchronized ActionResult ensurePlayer(ServerPlayer player) {
        ActionResult actorCheck = validateActor(player);
        if (actorCheck != null) {
            return actorCheck;
        }
        return ensurePlayerInternal(player, false);
    }

    public synchronized ActionResult onPlayerConnected(ServerPlayer player) {
        ActionResult actorCheck = validateActor(player);
        if (actorCheck != null) {
            return actorCheck;
        }
        observedOfflineSince.remove(player.getUUID());
        return ensurePlayerInternal(player, true);
    }

    /** Disconnects never remove faction, squad, class, leadership or command records. */
    public synchronized ActionResult onPlayerDisconnected(ServerPlayer player) {
        ActionResult actorCheck = validateActor(player);
        if (actorCheck != null) {
            return actorCheck;
        }
        BattleSavedData.StoredPlayer record = data.player(player.getUUID());
        if (record == null) {
            return ActionResult.failure(ActionResult.Code.NOT_ASSIGNED, "该玩家尚未加入战局");
        }
        long now = Math.max(0L, System.currentTimeMillis());
        record.lastKnownName = safeName(player);
        record.lastSeenAtMillis = Math.max(record.lastSeenAtMillis, now);
        observedOfflineSince.put(player.getUUID(), now);
        data.changed();
        return ActionResult.ok("已保留玩家的阵营、小队和兵种信息");
    }

    /** Called by the server tick hook; also useful to deterministic tests. */
    public synchronized int tick() {
        return cleanupExpiredReservations(System.currentTimeMillis());
    }

    /**
     * Releases capacity after the reconnect grace period but keeps identity/timestamp history.
     * Online players are always skipped, regardless of timestamps.
     *
     * @return number of player reservations released
     */
    public synchronized int cleanupExpiredReservations(long nowMillis) {
        long now = Math.max(0L, nowMillis);
        squadKickCooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);
        data.removeExpiredMarkers(now);
        heartbeatOnlinePlayers(now);
        int released = 0;
        for (BattleSavedData.StoredPlayer player : data.players()) {
            if (server.getPlayerList().getPlayer(player.playerId) != null) {
                observedOfflineSince.remove(player.playerId);
                continue;
            }
            if (player.faction == null) {
                continue;
            }
            Long observedStart = observedOfflineSince.get(player.playerId);
            long offlineSince = observedStart == null ? now : observedStart;
            if (observedStart == null) {
                observedOfflineSince.put(player.playerId, offlineSince);
            }
            long offlineFor = elapsedSince(now, offlineSince);
            if (offlineFor < BattleRules.RECONNECT_RESERVATION_MILLIS) {
                continue;
            }
            if (player.squad != null) {
                detachFromSquad(player);
            }
            if (player.faction != null && player.playerId.equals(data.commander(player.faction))) {
                data.setCommander(player.faction, null);
            }
            player.faction = null;
            player.formationId = null;
            player.assignedClassId = BattleRules.DEFAULT_CLASS_ID;
            DeploymentService.get(server).ifPresent(service -> service.forget(player.playerId));
            observedOfflineSince.remove(player.playerId);
            released++;
        }
        int pruned = 0;
        if (lastHistoryPruneMillis < 0L
                || elapsedSince(now, lastHistoryPruneMillis)
                >= BattleRules.HISTORY_PRUNE_INTERVAL_MILLIS) {
            lastHistoryPruneMillis = now;
            pruned = pruneHistoricalPlayers(now, BattleRules.MAX_PLAYER_RECORDS);
        }
        if (released > 0 || pruned > 0) {
            data.changed();
        }
        markerRateWindows.entrySet().removeIf(entry -> {
            Deque<Long> timestamps = entry.getValue();
            while (!timestamps.isEmpty()
                    && timestamps.peekFirst() <= now - BattleRules.MARKER_RATE_WINDOW_MILLIS) {
                timestamps.removeFirst();
            }
            return timestamps.isEmpty();
        });
        return released;
    }

    public synchronized Optional<PlayerRecord> playerRecord(UUID playerId) {
        if (playerId == null) {
            return Optional.empty();
        }
        BattleSavedData.StoredPlayer record = data.player(playerId);
        return record == null ? Optional.empty() : Optional.of(record.view());
    }

    public synchronized Optional<Faction> factionOf(UUID playerId) {
        BattleSavedData.StoredPlayer player = playerId == null ? null : data.player(playerId);
        return player == null ? Optional.empty() : Optional.ofNullable(player.faction);
    }

    public synchronized Optional<String> formationOf(UUID playerId) {
        BattleSavedData.StoredPlayer player = playerId == null ? null : data.player(playerId);
        return player == null || player.formationId == null
                ? Optional.empty() : Optional.of(player.formationId);
    }

    public synchronized int formationSize(Faction faction, String formationId) {
        String normalized = normalizeFormationId(formationId);
        if (faction == null || normalized == null) {
            return 0;
        }
        return (int) data.players().stream()
                .filter(player -> player.faction == faction
                        && normalized.equals(player.formationId)).count();
    }

    /**
     * Clears catalog selections that are no longer authoritative after a configuration reload.
     * Identity/admission history is retained so affected players can immediately select again.
     *
     * @return immutable list of player ids whose roster state was cleared
     */
    public synchronized List<UUID> reconcileFormationSelections(
            BiPredicate<Faction, String> validSelection) {
        Objects.requireNonNull(validSelection, "validSelection");
        List<UUID> cleared = new ArrayList<>();
        for (BattleSavedData.StoredPlayer player : data.players()) {
            if (clearInvalidFormationSelection(player, validSelection)) {
                cleared.add(player.playerId);
            }
        }
        if (!cleared.isEmpty()) {
            data.changed();
        }
        return List.copyOf(cleared);
    }

    /** Player-scoped repair used before presenting or committing the selection UI. */
    public synchronized boolean reconcileFormationSelection(
            UUID playerId, BiPredicate<Faction, String> validSelection) {
        Objects.requireNonNull(validSelection, "validSelection");
        BattleSavedData.StoredPlayer player = playerId == null ? null : data.player(playerId);
        if (player == null || !clearInvalidFormationSelection(player, validSelection)) {
            return false;
        }
        data.changed();
        return true;
    }

    /**
     * Reconciles persisted squad membership and class reservations after formation rules reload.
     * The rule boundary deliberately lives in the battle package so catalog code does not expose
     * its mutable configuration objects here. A missing rule means the squad was removed.
     *
     * <p>Retention is deterministic: the current leader is considered first, followed by join
     * time and UUID. Existing legal class reservations are retained in that order; displaced
     * members take any remaining class slot in configuration order. Members
     * for whom no legal class slot remains are removed from the squad.</p>
     *
     * @return immutable, deterministic list of players whose squad or class state changed
     */
    public synchronized List<UUID> reconcileFormationRosters(
            FormationRosterRules authoritativeRules) {
        Objects.requireNonNull(authoritativeRules, "authoritativeRules");
        List<UUID> reconciliation = reconcileFormationRosterState(data,
                authoritativeRules);
        if (!reconciliation.isEmpty()) {
            data.changed();
        }
        return reconciliation;
    }

    /** Repairs formation members outside a squad to that formation's current default profession. */
    public synchronized List<UUID> reconcileFormationDefaults(
            FormationDefaultClassRules authoritativeRules) {
        Objects.requireNonNull(authoritativeRules, "authoritativeRules");
        List<UUID> affected = new ArrayList<>();
        for (BattleSavedData.StoredPlayer player : data.players()) {
            if (player.faction == null || player.formationId == null || player.squad != null) {
                continue;
            }
            Optional<String> resolved = Objects.requireNonNull(authoritativeRules.defaultClassFor(
                    player.faction, player.formationId), "defaultClassFor returned null");
            String defaultClassId = resolved.map(BattleService::normalizeClassId).orElse(null);
            if (defaultClassId != null && !defaultClassId.equals(player.assignedClassId)) {
                player.assignedClassId = defaultClassId;
                affected.add(player.playerId);
            }
        }
        if (!affected.isEmpty()) {
            data.changed();
        }
        return List.copyOf(affected);
    }

    /** Keeps visible profession ownership stable when its private loadout backing ID is replaced. */
    public synchronized List<UUID> remapFormationClass(Faction faction, String formationId,
                                                       String replacedClassId,
                                                       String copiedClassId) {
        List<UUID> affected = remapFormationClassState(data, faction, formationId,
                replacedClassId, copiedClassId);
        if (!affected.isEmpty()) {
            data.changed();
        }
        return affected;
    }

    static List<UUID> remapFormationClassState(BattleSavedData data, Faction faction,
                                               String formationId, String replacedClassId,
                                               String copiedClassId) {
        Objects.requireNonNull(data, "data");
        String normalizedFormation = normalizeFormationId(formationId);
        String normalizedReplaced = normalizeClassId(replacedClassId);
        String normalizedCopied = normalizeClassId(copiedClassId);
        if (faction == null || normalizedFormation == null || normalizedReplaced == null
                || normalizedCopied == null || normalizedReplaced.equals(normalizedCopied)) {
            return List.of();
        }
        List<UUID> affected = new ArrayList<>();
        for (BattleSavedData.StoredPlayer player : data.players()) {
            if (player.faction == faction
                    && normalizedFormation.equals(player.formationId)
                    && normalizedReplaced.equals(player.assignedClassId)) {
                player.assignedClassId = normalizedCopied;
                affected.add(player.playerId);
            }
        }
        return List.copyOf(affected);
    }

    /** Catalog-independent rule lookup used by {@link #reconcileFormationRosters}. */
    @FunctionalInterface
    public interface FormationRosterRules {
        Optional<FormationRosterRule> ruleFor(Faction faction, String formationId,
                                               SquadCallsign callsign);
    }

    /** Catalog-independent lookup for the first/default profession of a formation. */
    @FunctionalInterface
    public interface FormationDefaultClassRules {
        Optional<String> defaultClassFor(Faction faction, String formationId);
    }

    /** Minimal catalog seam used by squad mutations and isolated Forge GameTests. */
    @FunctionalInterface
    interface FormationSquadCapacityRules {
        int capacityFor(Faction faction, String formationId, SquadCallsign callsign);
    }

    /** Immutable normalized squad capacity and ordered class limits for roster reconciliation. */
    public record FormationRosterRule(int capacity, Map<String, Integer> classLimits) {
        public FormationRosterRule {
            if (capacity < 1 || capacity > BattleRules.SQUAD_CAPACITY) {
                throw new IllegalArgumentException("capacity outside battle bounds: " + capacity);
            }
            LinkedHashMap<String, Integer> normalized = new LinkedHashMap<>();
            Objects.requireNonNull(classLimits, "classLimits").forEach((classId, limit) -> {
                String normalizedClassId = normalizeClassId(classId);
                if (normalizedClassId == null || !normalizedClassId.equals(classId)
                        || limit == null || limit < 0 || limit > capacity) {
                    throw new IllegalArgumentException("invalid class limit: " + classId);
                }
                normalized.put(normalizedClassId, limit);
            });
            classLimits = Collections.unmodifiableMap(normalized);
        }
    }

    /**
     * Atomically commits the two-stage player choice after the catalog service has resolved the
     * public faction ID to an internal battle side and validated the formation definition.
     */
    public synchronized ActionResult selectFormation(ServerPlayer actor, Faction targetFaction,
                                                      String formationId, int factionCapacity,
                                                      int formationCapacity) {
        ActionResult actorCheck = validateActor(actor);
        if (actorCheck != null) {
            return actorCheck;
        }
        ActionResult ensured = ensurePlayerInternal(actor, false);
        if (!ensured.success()) {
            return ensured;
        }
        String normalizedFormation = normalizeFormationId(formationId);
        if (targetFaction == null || normalizedFormation == null) {
            return ActionResult.failure(ActionResult.Code.FORMATION_NOT_FOUND,
                    "阵营或编制 ID 无效");
        }
        if (factionCapacity < 1 || factionCapacity > BattleRules.FACTION_CAPACITY
                || formationCapacity < 1 || formationCapacity > factionCapacity) {
            return ActionResult.failure(ActionResult.Code.FORMATION_UNAVAILABLE,
                    "阵营或编制容量配置无效");
        }
        BattleSavedData.StoredPlayer record = data.player(actor.getUUID());
        if (record == null || !record.admitted) {
            return ActionResult.failure(ActionResult.Code.NOT_ASSIGNED,
                    "你未获准加入当前战局");
        }
        if (record.formationId != null) {
            if (record.faction == targetFaction
                    && normalizedFormation.equals(record.formationId)) {
                return ActionResult.ok("阵营与编制选择已确认");
            }
            return ActionResult.failure(ActionResult.Code.FORMATION_LOCKED,
                    "阵营与编制在本轮确认后不能自行更换");
        }
        if (record.faction != null && record.faction != targetFaction) {
            return ActionResult.failure(ActionResult.Code.FORMATION_LOCKED,
                    "阵营在本轮确认后不能自行更换");
        }
        if (record.faction == null && factionSize(targetFaction) >= factionCapacity) {
            return ActionResult.failure(ActionResult.Code.FACTION_FULL, "所选阵营已满");
        }
        if (formationSize(targetFaction, normalizedFormation) >= formationCapacity) {
            return ActionResult.failure(ActionResult.Code.FORMATION_FULL, "所选编制已满");
        }
        record.faction = targetFaction;
        record.formationId = normalizedFormation;
        record.squad = null;
        record.squadJoinedAtMillis = 0L;
        record.assignedClassId = configuredDefaultClassId(targetFaction, normalizedFormation);
        data.changed();
        return ActionResult.ok("已确认阵营与编制");
    }

    /** First step of the shared workflow: reserve only a faction slot while voting remains open. */
    public synchronized ActionResult selectFaction(ServerPlayer actor, Faction targetFaction,
                                                    int factionCapacity) {
        ActionResult actorCheck = validateActor(actor);
        if (actorCheck != null) {
            return actorCheck;
        }
        ActionResult ensured = ensurePlayerInternal(actor, false);
        if (!ensured.success()) {
            return ensured;
        }
        if (targetFaction == null || factionCapacity < 1
                || factionCapacity > BattleRules.FACTION_CAPACITY) {
            return ActionResult.failure(ActionResult.Code.FORMATION_UNAVAILABLE,
                    "阵营或容量配置无效");
        }
        BattleSavedData.StoredPlayer record = data.player(actor.getUUID());
        if (record == null || !record.admitted) {
            return ActionResult.failure(ActionResult.Code.NOT_ASSIGNED,
                    "你未获准加入当前战局");
        }
        if (record.faction != null) {
            return record.faction == targetFaction
                    ? ActionResult.ok("阵营选择已确认")
                    : ActionResult.failure(ActionResult.Code.FORMATION_LOCKED,
                    "阵营在本轮确认后不能自行更换");
        }
        if (factionSize(targetFaction) >= factionCapacity) {
            return ActionResult.failure(ActionResult.Code.FACTION_FULL, "所选阵营已满");
        }
        record.faction = targetFaction;
        record.formationId = null;
        record.squad = null;
        record.squadJoinedAtMillis = 0L;
        record.assignedClassId = BattleRules.DEFAULT_CLASS_ID;
        data.changed();
        return ActionResult.ok("已加入 " + targetFaction.id() + " 方，等待编制投票结果");
    }

    /** Applies one already validated formation to every member of a faction. */
    public synchronized ActionResult applySharedFormation(Faction faction, String formationId,
                                                          int formationCapacity) {
        String normalizedFormation = normalizeFormationId(formationId);
        if (faction == null || normalizedFormation == null || formationCapacity < 1
                || formationCapacity > BattleRules.FACTION_CAPACITY) {
            return ActionResult.failure(ActionResult.Code.FORMATION_UNAVAILABLE,
                    "阵营共享编制或容量配置无效");
        }
        List<BattleSavedData.StoredPlayer> members = data.players().stream()
                .filter(player -> player.faction == faction).toList();
        if (members.size() > formationCapacity) {
            return ActionResult.failure(ActionResult.Code.FORMATION_FULL,
                    "胜出编制容量不足以容纳当前阵营玩家");
        }
        List<UUID> changedPlayers = new ArrayList<>();
        for (BattleSavedData.StoredPlayer member : members) {
            if (normalizedFormation.equals(member.formationId)) {
                continue;
            }
            if (member.squad != null) {
                detachFromSquad(member);
            }
            member.formationId = normalizedFormation;
            member.squad = null;
            member.squadJoinedAtMillis = 0L;
            member.assignedClassId = configuredDefaultClassId(faction, normalizedFormation);
            changedPlayers.add(member.playerId);
        }
        if (!changedPlayers.isEmpty()) {
            data.changed();
            DeploymentService.get(server).ifPresent(deployment ->
                    changedPlayers.forEach(deployment::onRosterChanged));
        }
        return ActionResult.ok("已向 " + faction.id() + " 方应用共享编制 "
                + normalizedFormation);
    }

    public synchronized Optional<SquadCallsign> squadOf(UUID playerId) {
        BattleSavedData.StoredPlayer player = playerId == null ? null : data.player(playerId);
        return player == null ? Optional.empty() : Optional.ofNullable(player.squad);
    }

    public synchronized Optional<UUID> squadLeader(Faction faction, SquadCallsign squad) {
        if (faction == null || squad == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(data.leader(faction, squad));
    }

    public synchronized Optional<UUID> squadLeader(Faction faction, String formationId,
                                                    SquadCallsign squad) {
        String normalizedFormation = normalizeFormationId(formationId);
        if (faction == null || normalizedFormation == null || squad == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(data.leader(faction, normalizedFormation, squad));
    }

    public synchronized Optional<UUID> commander(Faction faction) {
        return faction == null ? Optional.empty() : Optional.ofNullable(data.commander(faction));
    }

    public synchronized boolean isSquadLeader(UUID playerId) {
        BattleSavedData.StoredPlayer player = playerId == null ? null : data.player(playerId);
        return player != null && player.faction != null && player.formationId != null
                && player.squad != null && playerId.equals(data.leader(player.faction,
                player.formationId, player.squad));
    }

    public synchronized boolean isCommander(UUID playerId) {
        BattleSavedData.StoredPlayer player = playerId == null ? null : data.player(playerId);
        return player != null && player.faction != null
                && playerId.equals(data.commander(player.faction));
    }

    public synchronized int factionSize(Faction faction) {
        return faction == null ? 0 : (int) data.players().stream()
                .filter(player -> player.faction == faction).count();
    }

    public synchronized int squadSize(Faction faction, SquadCallsign squad) {
        if (faction == null || squad == null) {
            return 0;
        }
        String formationId = uniqueFormationForSquad(faction, squad);
        return formationId == null ? 0 : squadSize(faction, formationId, squad);
    }

    public synchronized int squadSize(Faction faction, String formationId,
                                      SquadCallsign squad) {
        String normalizedFormation = normalizeFormationId(formationId);
        if (faction == null || normalizedFormation == null || squad == null) {
            return 0;
        }
        return (int) data.players().stream()
                .filter(player -> player.faction == faction
                        && normalizedFormation.equals(player.formationId)
                        && player.squad == squad).count();
    }

    public synchronized ActionResult createSquad(ServerPlayer actor, SquadCallsign callsign) {
        ActionResult ready = requireAssigned(actor);
        if (ready != null) {
            return ready;
        }
        if (DeploymentService.get(server)
                .map(service -> !service.canChangeSquad(actor.getUUID())).orElse(false)) {
            return ActionResult.failure(ActionResult.Code.SQUAD_CHANGE_REQUIRES_DEPLOYMENT,
                    "存活期间不能创建小队，请先重新部署");
        }
        if (callsign == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET, "无效的小队呼号");
        }
        BattleSavedData.StoredPlayer player = data.player(actor.getUUID());
        if (player.squad != null) {
            return ActionResult.failure(ActionResult.Code.ALREADY_IN_SQUAD, "请先退出当前小队");
        }
        int configuredCapacity = configuredSquadCapacity(player.faction, player.formationId,
                callsign);
        if (configuredCapacity <= 0) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "当前编制没有该呼号小队");
        }
        ActionResult kickCooldown = squadKickCooldown(player, callsign,
                System.currentTimeMillis());
        if (kickCooldown != null) {
            return kickCooldown;
        }
        if (squadSize(player.faction, player.formationId, callsign) > 0) {
            return ActionResult.failure(ActionResult.Code.SQUAD_EXISTS, "该呼号小队已存在");
        }
        long now = System.currentTimeMillis();
        player.squad = callsign;
        player.squadJoinedAtMillis = now;
        player.assignedClassId = configuredDefaultClassId(player.faction, player.formationId);
        data.setLeader(player.faction, player.formationId, callsign, player.playerId);
        data.changed();
        DeploymentService.get(server).ifPresent(service ->
                service.onRosterChanged(player.playerId));
        return ActionResult.ok("已创建 " + callsign.id() + " 小队");
    }

    public synchronized ActionResult joinSquad(ServerPlayer actor, SquadCallsign callsign) {
        ActionResult ready = requireAssigned(actor);
        if (ready != null) {
            return ready;
        }
        if (DeploymentService.get(server)
                .map(service -> !service.canChangeSquad(actor.getUUID())).orElse(false)) {
            return ActionResult.failure(ActionResult.Code.SQUAD_CHANGE_REQUIRES_DEPLOYMENT,
                    "存活期间不能加入小队，请先重新部署");
        }
        if (callsign == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET, "无效的小队呼号");
        }
        BattleSavedData.StoredPlayer player = data.player(actor.getUUID());
        if (player.squad != null) {
            return ActionResult.failure(ActionResult.Code.ALREADY_IN_SQUAD, "请先退出当前小队");
        }
        int configuredCapacity = configuredSquadCapacity(player.faction, player.formationId,
                callsign);
        if (configuredCapacity <= 0) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "当前编制没有该呼号小队");
        }
        ActionResult kickCooldown = squadKickCooldown(player, callsign,
                System.currentTimeMillis());
        if (kickCooldown != null) {
            return kickCooldown;
        }
        int memberCount = squadSize(player.faction, player.formationId, callsign);
        if (memberCount == 0) {
            return ActionResult.failure(ActionResult.Code.SQUAD_NOT_FOUND, "该呼号小队尚未创建");
        }
        if (memberCount >= configuredCapacity) {
            return ActionResult.failure(ActionResult.Code.SQUAD_FULL,
                    "该小队已满 " + configuredCapacity + " 人");
        }
        player.squad = callsign;
        player.squadJoinedAtMillis = System.currentTimeMillis();
        player.assignedClassId = configuredDefaultClassId(player.faction, player.formationId);
        data.changed();
        DeploymentService.get(server).ifPresent(service ->
                service.onRosterChanged(player.playerId));
        return ActionResult.ok("已加入 " + callsign.id() + " 小队");
    }

    public synchronized ActionResult leaveSquad(ServerPlayer actor) {
        ActionResult ready = requireAssigned(actor);
        if (ready != null) {
            return ready;
        }
        if (DeploymentService.get(server)
                .map(service -> !service.canChangeSquad(actor.getUUID())).orElse(false)) {
            return ActionResult.failure(ActionResult.Code.SQUAD_CHANGE_REQUIRES_DEPLOYMENT,
                    "存活期间不能退出小队，请先重新部署");
        }
        BattleSavedData.StoredPlayer player = data.player(actor.getUUID());
        if (player.squad == null) {
            return ActionResult.failure(ActionResult.Code.NOT_IN_SQUAD, "你尚未加入小队");
        }
        SquadCallsign oldSquad = player.squad;
        detachFromSquad(player);
        data.changed();
        DeploymentService.get(server).ifPresent(service ->
                service.onRosterChanged(player.playerId));
        return ActionResult.ok("已退出 " + oldSquad.id() + " 小队");
    }

    public synchronized ActionResult transferLeadership(ServerPlayer actor, UUID newLeaderId) {
        ActionResult ready = requireAssigned(actor);
        if (ready != null) {
            return ready;
        }
        if (newLeaderId == null || newLeaderId.equals(actor.getUUID())) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET, "请选择小队内的其他成员");
        }
        BattleSavedData.StoredPlayer actorRecord = data.player(actor.getUUID());
        boolean administrator = isAdministrator(actor);
        if (!administrator && !isSquadLeader(actor.getUUID())) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED, "只有小队长可移交队长");
        }
        BattleSavedData.StoredPlayer target = data.player(newLeaderId);
        if (target == null) {
            return ActionResult.failure(administrator
                            ? ActionResult.Code.TARGET_NOT_FOUND
                            : ActionResult.Code.INVALID_TARGET,
                    administrator ? "目标玩家不在战局记录中" : "目标必须是同小队成员");
        }
        Faction faction = administrator ? target.faction : actorRecord.faction;
        String formationId = administrator ? target.formationId : actorRecord.formationId;
        SquadCallsign squad = administrator ? target.squad : actorRecord.squad;
        if (faction == null || formationId == null || squad == null
                || target.faction != faction || !formationId.equals(target.formationId)
                || target.squad != squad) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET, "目标必须是同小队成员");
        }
        if (server.getPlayerList().getPlayer(target.playerId) == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET, "只能将队长移交给在线成员");
        }
        data.setLeader(faction, formationId, squad, target.playerId);
        data.changed();
        return ActionResult.ok("已将小队长移交给 " + target.lastKnownName);
    }

    public synchronized ActionResult kickMember(ServerPlayer actor, UUID targetId) {
        ActionResult ready = requireAssigned(actor);
        if (ready != null) {
            return ready;
        }
        if (targetId == null || targetId.equals(actor.getUUID())) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET, "不能用踢出操作移除自己");
        }
        BattleSavedData.StoredPlayer actorRecord = data.player(actor.getUUID());
        boolean administrator = isAdministrator(actor);
        if (!administrator && !isSquadLeader(actor.getUUID())) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED, "只有小队长可踢出成员");
        }
        BattleSavedData.StoredPlayer target = data.player(targetId);
        if (target == null) {
            return ActionResult.failure(administrator
                            ? ActionResult.Code.TARGET_NOT_FOUND
                            : ActionResult.Code.INVALID_TARGET,
                    administrator ? "目标玩家不在战局记录中" : "目标必须是同小队成员");
        }
        if (!administrator && (target.faction != actorRecord.faction
                || !Objects.equals(target.formationId, actorRecord.formationId)
                || target.squad != actorRecord.squad)) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET, "目标必须是同小队成员");
        }
        if (target.squad == null) {
            return ActionResult.failure(ActionResult.Code.NOT_IN_SQUAD, "目标尚未加入小队");
        }
        if (target.faction == null || target.formationId == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "目标的小队编制记录无效");
        }
        Faction kickedFaction = target.faction;
        String kickedFormation = target.formationId;
        SquadCallsign kickedSquad = target.squad;
        detachFromSquad(target);
        squadKickCooldowns.put(new SquadKickKey(target.playerId, kickedFaction,
                        kickedFormation, kickedSquad),
                System.currentTimeMillis() + BattleRules.SQUAD_KICK_REJOIN_COOLDOWN_MILLIS);
        data.changed();
        DeploymentService.get(server).ifPresent(service ->
                service.onRosterChanged(target.playerId));
        return ActionResult.ok("已将 " + target.lastKnownName + " 踢出小队");
    }

    public synchronized ActionResult disbandSquad(ServerPlayer actor) {
        ActionResult ready = requireAssigned(actor);
        if (ready != null) {
            return ready;
        }
        if (DeploymentService.get(server)
                .map(service -> !service.canChangeSquad(actor.getUUID())).orElse(false)) {
            return ActionResult.failure(ActionResult.Code.SQUAD_CHANGE_REQUIRES_DEPLOYMENT,
                    "存活期间不能解散小队，请先重新部署");
        }
        BattleSavedData.StoredPlayer actorRecord = data.player(actor.getUUID());
        if (actorRecord.squad == null) {
            return ActionResult.failure(ActionResult.Code.NOT_IN_SQUAD, "你尚未加入小队");
        }
        if (!isSquadLeader(actor.getUUID()) && !isAdministrator(actor)) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED, "只有小队长可解散小队");
        }
        Faction faction = actorRecord.faction;
        String formationId = actorRecord.formationId;
        SquadCallsign callsign = actorRecord.squad;
        List<BattleSavedData.StoredPlayer> members = squadMembers(faction, formationId, callsign);
        for (BattleSavedData.StoredPlayer member : members) {
            if (member.playerId.equals(data.commander(faction))) {
                data.setCommander(faction, null);
            }
            member.squad = null;
            member.squadJoinedAtMillis = 0L;
            member.assignedClassId = configuredDefaultClassId(faction, formationId);
        }
        data.setLeader(faction, formationId, callsign, null);
        data.changed();
        DeploymentService.get(server).ifPresent(service -> members.forEach(member ->
                service.onRosterChanged(member.playerId)));
        return ActionResult.ok("已解散 " + callsign.id() + " 小队");
    }

    public synchronized ActionResult claimCommander(ServerPlayer actor) {
        ActionResult ready = requireAssigned(actor);
        if (ready != null) {
            return ready;
        }
        BattleSavedData.StoredPlayer player = data.player(actor.getUUID());
        if (!isSquadLeader(actor.getUUID())) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED, "只有小队长可申请指挥官");
        }
        UUID existing = data.commander(player.faction);
        if (existing != null && !existing.equals(player.playerId)) {
            return ActionResult.failure(ActionResult.Code.COMMANDER_EXISTS, "本阵营已有指挥官");
        }
        if (existing == null) {
            data.setCommander(player.faction, player.playerId);
            data.changed();
        }
        return ActionResult.ok("你已担任本阵营指挥官");
    }

    public synchronized ActionResult resignCommander(ServerPlayer actor) {
        ActionResult ready = requireAssigned(actor);
        if (ready != null) {
            return ready;
        }
        BattleSavedData.StoredPlayer player = data.player(actor.getUUID());
        if (!player.playerId.equals(data.commander(player.faction))) {
            return ActionResult.failure(ActionResult.Code.NOT_COMMANDER, "你不是本阵营指挥官");
        }
        data.setCommander(player.faction, null);
        data.changed();
        return ActionResult.ok("已卸任指挥官");
    }

    public synchronized ActionResult transferCommander(ServerPlayer actor, UUID targetId) {
        ActionResult ready = requireAssigned(actor);
        if (ready != null) {
            return ready;
        }
        if (targetId == null || targetId.equals(actor.getUUID())) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET, "请选择其他小队长");
        }
        BattleSavedData.StoredPlayer actorRecord = data.player(actor.getUUID());
        boolean administrator = isAdministrator(actor);
        if (!administrator && !actorRecord.playerId.equals(data.commander(actorRecord.faction))) {
            return ActionResult.failure(ActionResult.Code.NOT_COMMANDER, "只有现任指挥官可移交指挥权");
        }
        BattleSavedData.StoredPlayer target = data.player(targetId);
        if (target == null) {
            return ActionResult.failure(administrator
                            ? ActionResult.Code.TARGET_NOT_FOUND
                            : ActionResult.Code.INVALID_TARGET,
                    administrator ? "目标玩家不在战局记录中" : "目标必须是同阵营小队长");
        }
        Faction faction = administrator ? target.faction : actorRecord.faction;
        if (faction == null || target.faction != faction) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET, "目标必须是同阵营小队长");
        }
        if (!isSquadLeader(target.playerId)) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET, "目标必须是小队长");
        }
        if (server.getPlayerList().getPlayer(target.playerId) == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET, "只能将指挥权移交给在线小队长");
        }
        data.setCommander(faction, target.playerId);
        data.changed();
        return ActionResult.ok("已将指挥权移交给 " + target.lastKnownName);
    }

    /**
     * Atomically reserves a class slot. The requester's old reservation is excluded from usage.
     * The loadout service must validate that the class definition exists and is enabled first.
     */
    public synchronized ActionResult assignClass(ServerPlayer actor, String classId, int squadLimit) {
        ActionResult ready = requireAssigned(actor);
        if (ready != null) {
            return ready;
        }
        String normalizedClassId = normalizeClassId(classId);
        if (normalizedClassId == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_CLASS_ID, "兵种 ID 无效");
        }
        if (squadLimit < 1 || squadLimit > BattleRules.SQUAD_CAPACITY) {
            return ActionResult.failure(ActionResult.Code.INVALID_CLASS_LIMIT, "小队兵种限额必须为1至8");
        }
        BattleSavedData.StoredPlayer player = data.player(actor.getUUID());
        if (!normalizedClassId.equals(player.assignedClassId)
                && DeploymentService.get(server)
                .map(service -> !service.canChangeClass(actor.getUUID())).orElse(false)) {
            return ActionResult.failure(ActionResult.Code.CLASS_CHANGE_REQUIRES_DEPLOYMENT,
                    "存活期间不能更换兵种，请先重新部署");
        }
        if (player.squad == null) {
            String defaultClassId = configuredDefaultClassId(player.faction, player.formationId);
            if (!defaultClassId.equals(normalizedClassId)) {
                return ActionResult.failure(ActionResult.Code.CLASS_REQUIRES_SQUAD,
                        "未加入小队时只能选择当前编制默认职业 " + defaultClassId);
            }
        } else {
            int usedByOthers = classUsage(player.faction, player.formationId, player.squad,
                    normalizedClassId, player.playerId);
            if (usedByOthers >= squadLimit) {
                return ActionResult.failure(ActionResult.Code.CLASS_LIMIT_REACHED,
                        "该小队的 " + normalizedClassId + " 兵种已达上限 " + squadLimit);
            }
        }
        if (!normalizedClassId.equals(player.assignedClassId)) {
            player.assignedClassId = normalizedClassId;
            data.changed();
        }
        return ActionResult.ok("已预留兵种 " + normalizedClassId);
    }

    public synchronized String assignedClass(UUID playerId) {
        BattleSavedData.StoredPlayer player = playerId == null ? null : data.player(playerId);
        return player == null ? BattleRules.DEFAULT_CLASS_ID : player.assignedClassId;
    }

    public synchronized int classUsage(Faction faction, SquadCallsign squad, String classId) {
        String normalized = normalizeClassId(classId);
        if (faction == null || squad == null || normalized == null) {
            return 0;
        }
        String formationId = uniqueFormationForSquad(faction, squad);
        return formationId == null ? 0
                : classUsage(faction, formationId, squad, normalized, null);
    }

    public synchronized int classUsage(Faction faction, String formationId,
                                       SquadCallsign squad, String classId) {
        String normalizedFormation = normalizeFormationId(formationId);
        String normalizedClass = normalizeClassId(classId);
        if (faction == null || normalizedFormation == null || squad == null
                || normalizedClass == null) {
            return 0;
        }
        return classUsage(faction, normalizedFormation, squad, normalizedClass, null);
    }

    public synchronized Map<String, Integer> classUsageForSquad(Faction faction, SquadCallsign squad) {
        if (faction == null || squad == null) {
            return Map.of();
        }
        String formationId = uniqueFormationForSquad(faction, squad);
        return formationId == null ? Map.of()
                : classUsageForSquad(faction, formationId, squad);
    }

    public synchronized Map<String, Integer> classUsageForSquad(Faction faction,
                                                                 String formationId,
                                                                 SquadCallsign squad) {
        String normalizedFormation = normalizeFormationId(formationId);
        if (faction == null || normalizedFormation == null || squad == null) {
            return Map.of();
        }
        LinkedHashMap<String, Integer> usage = new LinkedHashMap<>();
        for (BattleSavedData.StoredPlayer player : data.players()) {
            if (player.faction == faction && normalizedFormation.equals(player.formationId)
                    && player.squad == squad) {
                usage.merge(player.assignedClassId, 1, Integer::sum);
            }
        }
        return Collections.unmodifiableMap(usage);
    }

    public synchronized ActionResult createMarker(ServerPlayer actor, TacticalMarkerType type,
                                                   Vec3 position, float directionDegrees) {
        if (actor == null) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED, "只有在线玩家可创建标记");
        }
        return createMarker(actor, type, actor.serverLevel().dimension(), position,
                directionDegrees, BattleRules.DEFAULT_MARKER_TTL_MILLIS);
    }

    /** Compatibility entry point for callers that still describe an attack arrow by angle. */
    public synchronized ActionResult createMarker(ServerPlayer actor, TacticalMarkerType type,
                                                   ResourceKey<Level> dimension, Vec3 position,
                                                   float directionDegrees, long requestedTtlMillis) {
        Vec3 endPosition = legacyMarkerEnd(type, position, directionDegrees);
        return createMarker(actor, type, dimension, position, endPosition, requestedTtlMillis);
    }

    public synchronized ActionResult createMarker(ServerPlayer actor, TacticalMarkerType type,
                                                   ResourceKey<Level> dimension,
                                                   Vec3 position, Vec3 endPosition,
                                                   long requestedTtlMillis) {
        ActionResult ready = requireAssigned(actor);
        if (ready != null) {
            return ready;
        }
        BattleSavedData.StoredPlayer player = data.player(actor.getUUID());
        if (!isSquadLeader(player.playerId) && !isCommander(player.playerId)) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                    "只有小队长或指挥官可创建战术标记");
        }
        if (type == null || dimension == null || position == null || endPosition == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_MARKER, "标记类型、维度或坐标缺失");
        }
        if (dimension.equals(DeploymentService.HOLDING_LEVEL)
                || dimension.equals(DeploymentService.LOBBY_LEVEL)) {
            return ActionResult.failure(ActionResult.Code.INVALID_MARKER,
                    "内部等待维度不能创建战术标记");
        }
        boolean currentDimension = actor.serverLevel().dimension().equals(dimension);
        boolean waitingDeploymentDimension = DeploymentService.get(actor)
                .filter(deployment -> deployment.isWaitingParticipant(actor.getUUID()))
                .map(deployment -> deployment.pointsFor(actor).stream()
                        .anyMatch(point -> point.dimension().equals(dimension.location())))
                .orElse(false);
        if (!currentDimension && !waitingDeploymentDimension) {
            return ActionResult.failure(ActionResult.Code.INVALID_MARKER,
                    "只能标记当前维度或己方部署点所在维度");
        }
        if (dimension.location().toString().length() > BattleRules.MAX_DIMENSION_ID_LENGTH) {
            return ActionResult.failure(ActionResult.Code.INVALID_MARKER, "维度标识超出服务端安全长度");
        }
        long ttlMillis = requestedTtlMillis == 0L
                ? BattleRules.DEFAULT_MARKER_TTL_MILLIS : requestedTtlMillis;
        if (ttlMillis < BattleRules.MIN_MARKER_TTL_MILLIS
                || ttlMillis > BattleRules.MAX_MARKER_TTL_MILLIS) {
            return ActionResult.failure(ActionResult.Code.INVALID_MARKER,
                    "标记持续时间必须在5秒至10分钟之间");
        }
        if (!coordinatesFinite(position) || !coordinatesFinite(endPosition)) {
            return ActionResult.failure(ActionResult.Code.INVALID_MARKER, "标记坐标不合法");
        }
        if (type == TacticalMarkerType.ATTACK_DIRECTION
                && !TacticalMarker.isValidAttackGeometry(position.x, position.z,
                endPosition.x, endPosition.z)) {
            return ActionResult.failure(ActionResult.Code.INVALID_MARKER,
                    "进攻方向长度必须在4至4096格之间");
        }
        ServerLevel level = server.getLevel(dimension);
        if (level == null || position.y < level.getMinBuildHeight()
                || position.y >= level.getMaxBuildHeight()
                || endPosition.y < level.getMinBuildHeight()
                || endPosition.y >= level.getMaxBuildHeight()) {
            return ActionResult.failure(ActionResult.Code.INVALID_MARKER, "标记不在已加载维度的有效高度内");
        }
        BlockPos blockPos = BlockPos.containing(position);
        BlockPos endBlockPos = BlockPos.containing(endPosition);
        if (!level.getWorldBorder().isWithinBounds(blockPos)
                || !level.getWorldBorder().isWithinBounds(endBlockPos)) {
            return ActionResult.failure(ActionResult.Code.INVALID_MARKER, "标记超出世界边界");
        }

        long now = System.currentTimeMillis();
        data.removeExpiredMarkers(now);
        int factionMarkers = (int) data.markers().stream()
                .filter(marker -> marker.faction() == player.faction).count();
        int creatorMarkers = (int) data.markers().stream()
                .filter(marker -> marker.creatorId().equals(player.playerId)).count();
        if (factionMarkers >= BattleRules.MAX_MARKERS_PER_FACTION
                || creatorMarkers >= BattleRules.MAX_MARKERS_PER_CREATOR) {
            return ActionResult.failure(ActionResult.Code.MARKER_LIMIT_REACHED,
                    "战术标记数量已达服务端上限");
        }
        Deque<Long> rateWindow = markerRateWindows.computeIfAbsent(player.playerId,
                ignored -> new ArrayDeque<>());
        while (!rateWindow.isEmpty()
                && rateWindow.peekFirst() <= now - BattleRules.MARKER_RATE_WINDOW_MILLIS) {
            rateWindow.removeFirst();
        }
        if (rateWindow.size() >= BattleRules.MAX_MARKERS_PER_RATE_WINDOW) {
            return ActionResult.failure(ActionResult.Code.MARKER_RATE_LIMITED,
                    "标记操作过于频繁，请稍后再试");
        }

        TacticalMarker marker = new TacticalMarker(UUID.randomUUID(), player.faction, type,
                dimension.location(), position.x, position.y, position.z,
                endPosition.x, endPosition.z,
                player.playerId, player.squad, now, now + ttlMillis);
        data.addMarker(marker);
        rateWindow.addLast(now);
        data.changed();
        return ActionResult.ok(BattleFeedbackMessages.markerCreated(type));
    }

    public synchronized ActionResult removeMarker(ServerPlayer actor, UUID markerId) {
        ActionResult ready = requireAssigned(actor);
        if (ready != null) {
            return ready;
        }
        if (markerId == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET, "标记 ID 缺失");
        }
        data.removeExpiredMarkers(System.currentTimeMillis());
        TacticalMarker marker = data.marker(markerId);
        if (marker == null) {
            return ActionResult.failure(ActionResult.Code.MARKER_NOT_FOUND, "标记不存在或已过期");
        }
        BattleSavedData.StoredPlayer player = data.player(actor.getUUID());
        boolean administrator = isAdministrator(actor);
        boolean sameFaction = player.faction == marker.faction();
        // Do not let an unprivileged enemy distinguish a live hostile marker UUID from a
        // missing/expired one. Administrators retain the explicit cross-faction override.
        if (!administrator && !sameFaction) {
            return ActionResult.failure(ActionResult.Code.MARKER_NOT_FOUND,
                    "标记不存在或已过期");
        }
        boolean creator = player.playerId.equals(marker.creatorId());
        boolean commander = sameFaction && isCommander(player.playerId);
        BattleSavedData.StoredPlayer markerCreator = data.player(marker.creatorId());
        boolean leaderOfCreatorSquad = sameFaction && marker.creatorSquad() != null
                && markerCreator != null
                && Objects.equals(player.formationId, markerCreator.formationId)
                && markerCreator.squad == marker.creatorSquad()
                && player.squad == marker.creatorSquad() && isSquadLeader(player.playerId);
        if (!administrator && !creator && !commander && !leaderOfCreatorSquad) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                    "只有创建者、所属小队长或指挥官可删除该标记");
        }
        data.removeMarker(markerId);
        data.changed();
        return ActionResult.ok("已删除战术标记");
    }

    /** Returns a formation-scoped roster/position view plus faction-wide aggregate counts/markers. */
    public synchronized BattleSnapshot snapshotFor(ServerPlayer viewer) {
        return snapshotFor(viewer, BattleRules.DEFAULT_CLASS_LIMITS, Map.of());
    }

    public synchronized BattleSnapshot snapshotFor(ServerPlayer viewer,
                                                    Map<String, Integer> configuredClassLimits) {
        return snapshotFor(viewer, configuredClassLimits, Map.of());
    }

    public synchronized BattleSnapshot snapshotFor(ServerPlayer viewer,
                                                    Map<String, Integer> configuredClassLimits,
                                                    Map<String, String> configuredClassDisplayNames) {
        Objects.requireNonNull(viewer, "viewer");
        if (viewer.server != server) {
            throw new IllegalArgumentException("Player belongs to a different MinecraftServer");
        }
        long now = System.currentTimeMillis();
        data.removeExpiredMarkers(now);
        BattleSavedData.StoredPlayer self = data.player(viewer.getUUID());
        Faction faction = self == null ? null : self.faction;
        String formationId = self == null ? null : self.formationId;
        SquadCallsign ownSquad = self == null ? null : self.squad;
        boolean leader = self != null && isSquadLeader(self.playerId);
        boolean commander = self != null && isCommander(self.playerId);

        List<SquadView> squads = new ArrayList<>();
        List<MemberPosition> positions = new ArrayList<>();
        List<TacticalMarker> markers = new ArrayList<>();
        int factionCount = 0;
        int enemyCount = 0;
        int formationSquadCapacity = 0;
        DeploymentService deployments = DeploymentService.get(server).orElse(null);
        if (faction != null) {
            EnumMap<SquadCallsign, List<BattleSavedData.StoredPlayer>> membersBySquad =
                    new EnumMap<>(SquadCallsign.class);
            for (SquadCallsign callsign : SquadCallsign.values()) {
                membersBySquad.put(callsign, new ArrayList<>());
            }
            List<BattleSavedData.StoredPlayer> factionPlayers = new ArrayList<>();
            for (BattleSavedData.StoredPlayer candidate : data.players()) {
                if (candidate.faction == faction) {
                    factionCount++;
                    if (Objects.equals(candidate.formationId, formationId)) {
                        factionPlayers.add(candidate);
                        if (candidate.squad != null) {
                            membersBySquad.get(candidate.squad).add(candidate);
                        }
                    }
                } else if (candidate.faction == faction.opposite()) {
                    enemyCount++;
                }
            }
            for (SquadCallsign callsign : SquadCallsign.values()) {
                int configuredCapacity = configuredSquadCapacity(faction, formationId, callsign);
                if (configuredCapacity <= 0) {
                    continue;
                }
                formationSquadCapacity = Math.max(formationSquadCapacity, configuredCapacity);
                List<BattleSavedData.StoredPlayer> members = membersBySquad.get(callsign);
                members.sort(memberComparator(faction, formationId, callsign));
                List<MemberView> memberViews = new ArrayList<>(members.size());
                for (BattleSavedData.StoredPlayer member : members) {
                    ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(member.playerId);
                    boolean online = onlinePlayer != null;
                    boolean deployed = deployments == null
                            || deployments.isActive(member.playerId);
                    boolean alive = online && deployed && onlinePlayer.isAlive();
                    float health = alive ? onlinePlayer.getHealth() : 0.0F;
                    float maxHealth = online ? onlinePlayer.getMaxHealth() : 20.0F;
                    memberViews.add(new MemberView(member.playerId, member.lastKnownName,
                            online, alive, health, maxHealth,
                            member.playerId.equals(data.leader(faction, formationId, callsign)),
                            member.playerId.equals(data.commander(faction)), callsign,
                            member.assignedClassId));
                }
                squads.add(new SquadView(callsign, data.leader(faction, formationId, callsign),
                        memberViews, configuredCapacity));
            }

            for (BattleSavedData.StoredPlayer ally : factionPlayers) {
                ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(ally.playerId);
                if (onlinePlayer == null) {
                    continue;
                }
                if (deployments != null && !deployments.isActive(ally.playerId)) {
                    continue;
                }
                Vec3 position = onlinePlayer.position();
                positions.add(new MemberPosition(ally.playerId,
                        onlinePlayer.serverLevel().dimension().location(),
                        position.x, position.y, position.z, onlinePlayer.getYRot()));
            }
            data.markers().stream().filter(marker -> marker.faction() == faction)
                    .sorted(Comparator.comparingLong(TacticalMarker::createdAtMillis))
                    .forEach(markers::add);
        }

        PermissionView permissions = permissionView(viewer, self);
        List<ClassQuotaView> quotas = buildClassQuotaViews(self, configuredClassLimits,
                configuredClassDisplayNames);
        SupportView support = SupportService.get(server)
                .map(service -> service.viewFor(viewer))
                .orElseGet(SupportView::unavailable);
        long visibleRevision = Integer.toUnsignedLong(Objects.hash(faction, formationId, ownSquad, leader,
                commander, factionCount, enemyCount, squads, markers, permissions, quotas,
                support.structuralRevision()));
        return new BattleSnapshot(viewer.getUUID(), faction, ownSquad, leader, commander,
                factionCount, enemyCount, BattleRules.FACTION_CAPACITY,
                Math.max(1, formationSquadCapacity),
                squads, positions, markers, permissions, quotas, now, visibleRevision)
                .withSupport(support);
    }

    /** Administrator-only reassignment, primarily for match setup and recovery. */
    public synchronized ActionResult forceAssignFaction(ServerPlayer administrator, UUID targetId,
                                                        Faction targetFaction) {
        return forceAssignFormation(administrator, targetId, targetFaction, "default",
                BattleRules.FACTION_CAPACITY, BattleRules.FACTION_CAPACITY);
    }

    /** Administrator-only complete assignment; ordinary clients never choose capacities. */
    public synchronized ActionResult forceAssignFormation(ServerPlayer administrator,
                                                          UUID targetId,
                                                          Faction targetFaction,
                                                          String formationId,
                                                          int factionCapacity,
                                                          int formationCapacity) {
        ActionResult actorCheck = validateActor(administrator);
        if (actorCheck != null) {
            return actorCheck;
        }
        if (!isAdministrator(administrator)) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED, "需要服务端管理员权限");
        }
        String normalizedFormation = normalizeFormationId(formationId);
        if (targetId == null || targetFaction == null || normalizedFormation == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "目标玩家、阵营或编制缺失");
        }
        BattleSavedData.StoredPlayer target = data.player(targetId);
        if (target == null) {
            return ActionResult.failure(ActionResult.Code.TARGET_NOT_FOUND, "目标玩家不在战局记录中");
        }
        if (target.admitted && target.faction == targetFaction
                && normalizedFormation.equals(target.formationId)) {
            return ActionResult.ok("目标已在该阵营与编制");
        }
        int targetFactionSize = factionSize(targetFaction)
                - (target.faction == targetFaction ? 1 : 0);
        int targetFormationSize = formationSize(targetFaction, normalizedFormation)
                - (target.faction == targetFaction
                && normalizedFormation.equals(target.formationId) ? 1 : 0);
        if (factionCapacity < 1 || factionCapacity > BattleRules.FACTION_CAPACITY
                || targetFactionSize >= factionCapacity) {
            return ActionResult.failure(ActionResult.Code.FACTION_FULL, "目标阵营已满");
        }
        if (formationCapacity < 1 || formationCapacity > factionCapacity
                || targetFormationSize >= formationCapacity) {
            return ActionResult.failure(ActionResult.Code.FORMATION_FULL, "目标编制已满");
        }
        if (target.squad != null) {
            detachFromSquad(target);
        }
        if (target.faction != null && target.playerId.equals(data.commander(target.faction))) {
            data.setCommander(target.faction, null);
        }
        target.faction = targetFaction;
        target.formationId = normalizedFormation;
        target.admitted = true;
        target.assignedClassId = configuredDefaultClassId(targetFaction, normalizedFormation);
        data.changed();
        DeploymentService.get(server).ifPresent(service ->
                service.onRosterChanged(target.playerId));
        return ActionResult.ok("已将目标分配至 " + targetFaction.id() + "/"
                + normalizedFormation);
    }

    /** Removes a player from team capacity while retaining the reconnectable player record. */
    public synchronized ActionResult removeFromBattle(ServerPlayer administrator, UUID targetId) {
        ActionResult actorCheck = validateActor(administrator);
        if (actorCheck != null) {
            return actorCheck;
        }
        if (!isAdministrator(administrator)) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED, "需要服务端管理员权限");
        }
        BattleSavedData.StoredPlayer target = targetId == null ? null : data.player(targetId);
        if (target == null) {
            return ActionResult.failure(ActionResult.Code.TARGET_NOT_FOUND, "目标玩家不在战局记录中");
        }
        if (target.squad != null) {
            detachFromSquad(target);
        }
        if (target.faction != null && target.playerId.equals(data.commander(target.faction))) {
            data.setCommander(target.faction, null);
        }
        target.faction = null;
        target.formationId = null;
        target.admitted = false;
        target.assignedClassId = BattleRules.DEFAULT_CLASS_ID;
        data.changed();
        DeploymentService.get(server).ifPresent(service ->
                service.onRosterChanged(target.playerId));
        return ActionResult.ok("已将目标移出当前战局");
    }

    public synchronized ActionResult resetBattle(ServerPlayer administrator) {
        ActionResult actorCheck = validateActor(administrator);
        if (actorCheck != null) {
            return actorCheck;
        }
        if (!isAdministrator(administrator)) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED, "需要服务端管理员权限");
        }
        DeploymentService deployment = DeploymentService.get(server).orElse(null);
        if (deployment == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "部署服务尚未就绪，战局未重置");
        }
        ActionResult sessionRotation = deployment.prepareBattleResetSession();
        if (!sessionRotation.success()) {
            return sessionRotation;
        }
        data.clear();
        markerRateWindows.clear();
        observedOfflineSince.clear();
        squadKickCooldowns.clear();
        lastHeartbeatMillis = -1L;
        lastHistoryPruneMillis = -1L;
        int postCommitFailures = 0;
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            try {
                ensurePlayerInternal(online, true);
            } catch (RuntimeException | LinkageError exception) {
                postCommitFailures++;
                WokInfantryMod.LOGGER.error(
                        "Could not rebuild battle identity after reset for player {}",
                        online.getUUID(), exception);
            }
        }
        postCommitFailures += deployment.finishBattleReset();
        try {
            SupportService.get(server).ifPresent(SupportService::resetAll);
        } catch (RuntimeException | LinkageError exception) {
            postCommitFailures++;
            WokInfantryMod.LOGGER.error(
                    "Could not finish support-state cleanup after committed battle reset",
                    exception);
        }
        try {
            FormationService.get(server).ifPresent(FormationService::clearVotesForBattleReset);
        } catch (RuntimeException | LinkageError exception) {
            postCommitFailures++;
            WokInfantryMod.LOGGER.error(
                    "Could not finish formation-vote cleanup after committed battle reset",
                    exception);
        }
        return postCommitFailures == 0
                ? ActionResult.ok("已重置战局、小队、兵种占位和标记")
                : ActionResult.ok("战局重置已提交，但有 " + postCommitFailures
                + " 项玩家/支援清理失败；已记录服务端日志并保持新会话");
    }

    private ActionResult ensurePlayerInternal(ServerPlayer player, boolean updateLastSeen) {
        long now = Math.max(0L, System.currentTimeMillis());
        observedOfflineSince.remove(player.getUUID());
        BattleSavedData.StoredPlayer record = data.player(player.getUUID());
        boolean changed = false;
        if (record == null) {
            int pruned = data.playerCount() >= BattleRules.MAX_PLAYER_RECORDS
                    ? pruneHistoricalPlayers(now, BattleRules.MAX_PLAYER_RECORDS - 1) : 0;
            changed = pruned > 0;
            if (data.playerCount() >= BattleRules.MAX_PLAYER_RECORDS) {
                if (changed) {
                    data.changed();
                }
                return ActionResult.failure(ActionResult.Code.BATTLE_FULL,
                        "战局玩家历史记录已达安全上限，请稍后重试或由管理员重置战局");
            }
            record = data.addPlayer(player.getUUID(), safeName(player), now);
            if (record == null) {
                if (changed) {
                    data.changed();
                }
                return ActionResult.failure(ActionResult.Code.BATTLE_FULL,
                        "战局玩家记录已达服务端安全上限");
            }
            changed = true;
        }
        String currentName = safeName(player);
        if (!record.lastKnownName.equals(currentName)) {
            record.lastKnownName = currentName;
            changed = true;
        }
        if (updateLastSeen) {
            long updatedLastSeen = Math.max(record.lastSeenAtMillis, now);
            if (record.lastSeenAtMillis != updatedLastSeen) {
                record.lastSeenAtMillis = updatedLastSeen;
                changed = true;
            }
        }
        if (!record.admitted) {
            if (changed) {
                data.changed();
            }
            return ActionResult.failure(ActionResult.Code.NOT_ASSIGNED,
                    "你已被管理员移出当前战局");
        }
        if (changed) {
            data.changed();
        }
        return ActionResult.ok("玩家战局记录已就绪");
    }

    private ActionResult requireAssigned(ServerPlayer actor) {
        ActionResult actorCheck = validateActor(actor);
        if (actorCheck != null) {
            return actorCheck;
        }
        ActionResult ensured = ensurePlayerInternal(actor, false);
        if (!ensured.success()) {
            return ensured;
        }
        BattleSavedData.StoredPlayer record = data.player(actor.getUUID());
        if (record == null || record.faction == null || record.formationId == null) {
            return ActionResult.failure(ActionResult.Code.FORMATION_SELECTION_REQUIRED,
                    "请先选择阵营与编制");
        }
        return null;
    }

    private ActionResult validateActor(ServerPlayer actor) {
        if (actor == null || actor.server != server) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                    "操作者不属于当前服务器");
        }
        return null;
    }

    private void heartbeatOnlinePlayers(long nowMillis) {
        if (lastHeartbeatMillis >= 0L
                && elapsedSince(nowMillis, lastHeartbeatMillis)
                < BattleRules.PLAYER_HEARTBEAT_MILLIS) {
            return;
        }
        lastHeartbeatMillis = nowMillis;
        boolean timestampChanged = false;
        boolean visibleChanged = false;
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            UUID playerId = online.getUUID();
            observedOfflineSince.remove(playerId);
            BattleSavedData.StoredPlayer record = data.player(playerId);
            if (record == null) {
                continue;
            }
            String currentName = safeName(online);
            if (!record.lastKnownName.equals(currentName)) {
                record.lastKnownName = currentName;
                visibleChanged = true;
            }
            long heartbeat = Math.max(record.lastSeenAtMillis, nowMillis);
            if (record.lastSeenAtMillis != heartbeat) {
                record.lastSeenAtMillis = heartbeat;
                timestampChanged = true;
            }
        }
        if (visibleChanged) {
            data.changed();
        } else if (timestampChanged) {
            data.touch();
        }
    }

    /**
     * Prunes only offline, unassigned identities whose reconnect window has elapsed. Old history
     * is removed normally; under the hard cap, the oldest otherwise-safe history is evicted first.
     */
    private int pruneHistoricalPlayers(long nowMillis, int maximumAfterPrune) {
        int boundedMaximum = Math.max(0,
                Math.min(BattleRules.MAX_PLAYER_RECORDS, maximumAfterPrune));
        List<BattleSavedData.StoredPlayer> candidates = data.players().stream()
                .filter(player -> player.faction == null && player.squad == null)
                .filter(player -> server.getPlayerList().getPlayer(player.playerId) == null)
                .filter(player -> reconnectWindowElapsed(player, nowMillis))
                .sorted(Comparator.comparingLong(
                                (BattleSavedData.StoredPlayer player) -> player.lastSeenAtMillis)
                        .thenComparing(player -> player.playerId))
                .toList();

        int removed = 0;
        for (BattleSavedData.StoredPlayer candidate : candidates) {
            if (elapsedSince(nowMillis, candidate.lastSeenAtMillis)
                    < BattleRules.UNASSIGNED_HISTORY_TTL_MILLIS) {
                continue;
            }
            if (removeHistoricalPlayer(candidate.playerId)) {
                removed++;
            }
        }
        for (BattleSavedData.StoredPlayer candidate : candidates) {
            if (data.playerCount() <= boundedMaximum) {
                break;
            }
            if (removeHistoricalPlayer(candidate.playerId)) {
                removed++;
            }
        }
        return removed;
    }

    private boolean reconnectWindowElapsed(BattleSavedData.StoredPlayer player, long nowMillis) {
        long reservationStart = player.lastSeenAtMillis;
        Long observedStart = observedOfflineSince.get(player.playerId);
        if (observedStart != null && observedStart > reservationStart) {
            reservationStart = observedStart;
        }
        return elapsedSince(nowMillis, reservationStart)
                >= BattleRules.RECONNECT_RESERVATION_MILLIS;
    }

    private boolean removeHistoricalPlayer(UUID playerId) {
        if (!data.removePlayer(playerId)) {
            return false;
        }
        observedOfflineSince.remove(playerId);
        markerRateWindows.remove(playerId);
        squadKickCooldowns.keySet().removeIf(key -> key.playerId().equals(playerId));
        return true;
    }

    /** Package-visible deterministic core so persistence-focused tests need no live server. */
    static List<UUID> reconcileFormationRosterState(
            BattleSavedData data, FormationRosterRules authoritativeRules) {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(authoritativeRules, "authoritativeRules");

        Map<RosterKey, List<BattleSavedData.StoredPlayer>> grouped = new LinkedHashMap<>();
        for (BattleSavedData.StoredPlayer player : data.players()) {
            if (player.faction == null || player.formationId == null || player.squad == null) {
                continue;
            }
            RosterKey key = new RosterKey(player.faction, player.formationId, player.squad);
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(player);
        }

        List<RosterKey> keys = new ArrayList<>(grouped.keySet());
        keys.sort(Comparator.comparing((RosterKey key) -> key.faction().ordinal())
                .thenComparing(RosterKey::formationId)
                .thenComparing(key -> key.callsign().ordinal()));

        // Resolve the complete rule snapshot before the first mutation. A broken provider therefore
        // cannot leave only the first half of the persisted roster reconciled.
        Map<RosterKey, Optional<FormationRosterRule>> rules = new LinkedHashMap<>();
        for (RosterKey key : keys) {
            Optional<FormationRosterRule> resolved = Objects.requireNonNull(
                    authoritativeRules.ruleFor(key.faction(), key.formationId(), key.callsign()),
                    "ruleFor returned null");
            rules.put(key, resolved);
        }

        Set<UUID> affected = new LinkedHashSet<>();
        for (RosterKey key : keys) {
            List<BattleSavedData.StoredPlayer> members = grouped.get(key);
            UUID previousLeader = data.leader(key.faction(), key.formationId(), key.callsign());
            members.sort(rosterRetentionComparator(previousLeader));
            FormationRosterRule rule = rules.get(key).orElse(null);
            if (rule == null) {
                for (BattleSavedData.StoredPlayer member : members) {
                    clearSquadForReconciliation(data, member, null);
                    affected.add(member.playerId);
                }
                data.setLeader(key.faction(), key.formationId(), key.callsign(), null);
                continue;
            }

            List<BattleSavedData.StoredPlayer> candidates = new ArrayList<>(
                    members.subList(0, Math.min(rule.capacity(), members.size())));
            for (int index = rule.capacity(); index < members.size(); index++) {
                BattleSavedData.StoredPlayer removed = members.get(index);
                clearSquadForReconciliation(data, removed,
                        firstConfiguredClass(rule.classLimits()));
                affected.add(removed.playerId);
            }

            Map<String, Integer> usage = new LinkedHashMap<>();
            List<BattleSavedData.StoredPlayer> displaced = new ArrayList<>();
            for (BattleSavedData.StoredPlayer member : candidates) {
                String currentClass = normalizeClassId(member.assignedClassId);
                Integer limit = currentClass == null ? null : rule.classLimits().get(currentClass);
                int used = currentClass == null ? 0 : usage.getOrDefault(currentClass, 0);
                if (limit != null && used < limit) {
                    usage.put(currentClass, used + 1);
                } else {
                    displaced.add(member);
                }
            }

            for (BattleSavedData.StoredPlayer member : displaced) {
                String replacement = firstAvailableClass(rule.classLimits(), usage);
                if (replacement == null) {
                    clearSquadForReconciliation(data, member,
                            firstConfiguredClass(rule.classLimits()));
                    affected.add(member.playerId);
                    continue;
                }
                usage.merge(replacement, 1, Integer::sum);
                if (!replacement.equals(member.assignedClassId)) {
                    member.assignedClassId = replacement;
                    affected.add(member.playerId);
                }
            }

            List<BattleSavedData.StoredPlayer> retained = candidates.stream()
                    .filter(member -> member.squad == key.callsign())
                    .toList();
            UUID nextLeader = retained.isEmpty() ? null : retained.get(0).playerId;
            if (!Objects.equals(previousLeader, nextLeader)) {
                if (previousLeader != null) {
                    affected.add(previousLeader);
                }
                if (nextLeader != null) {
                    affected.add(nextLeader);
                }
                data.setLeader(key.faction(), key.formationId(), key.callsign(), nextLeader);
            }
        }
        return List.copyOf(affected);
    }

    private static Comparator<BattleSavedData.StoredPlayer> rosterRetentionComparator(
            UUID leaderId) {
        return Comparator.comparing((BattleSavedData.StoredPlayer member) ->
                        !member.playerId.equals(leaderId))
                .thenComparingLong(member -> member.squadJoinedAtMillis)
                .thenComparing(member -> member.playerId);
    }

    private static String firstAvailableClass(Map<String, Integer> limits,
                                              Map<String, Integer> usage) {
        for (Map.Entry<String, Integer> entry : limits.entrySet()) {
            if (usage.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static String firstConfiguredClass(Map<String, Integer> limits) {
        return limits.keySet().stream().findFirst().orElse(BattleRules.DEFAULT_CLASS_ID);
    }

    private static void clearSquadForReconciliation(BattleSavedData data,
                                                     BattleSavedData.StoredPlayer player,
                                                     String fallbackClassId) {
        if (player.faction != null
                && player.playerId.equals(data.commander(player.faction))) {
            data.setCommander(player.faction, null);
        }
        player.squad = null;
        player.squadJoinedAtMillis = 0L;
        player.assignedClassId = Objects.requireNonNullElse(fallbackClassId,
                BattleRules.DEFAULT_CLASS_ID);
    }

    private record RosterKey(Faction faction, String formationId, SquadCallsign callsign) {
    }

    private boolean clearInvalidFormationSelection(
            BattleSavedData.StoredPlayer player,
            BiPredicate<Faction, String> validSelection) {
        boolean hasAnySelection = player.faction != null || player.formationId != null;
        if (!hasAnySelection) {
            return false;
        }
        boolean valid = player.faction != null
                && validSelection.test(player.faction, player.formationId);
        if (valid) {
            return false;
        }
        Faction previousFaction = player.faction;
        if (player.squad != null) {
            detachFromSquad(player);
        }
        if (previousFaction != null
                && player.playerId.equals(data.commander(previousFaction))) {
            data.setCommander(previousFaction, null);
        }
        // Also repair a malformed partial selection for which detachFromSquad could not run.
        player.squad = null;
        player.squadJoinedAtMillis = 0L;
        player.faction = null;
        player.formationId = null;
        player.assignedClassId = BattleRules.DEFAULT_CLASS_ID;
        return true;
    }

    private void detachFromSquad(BattleSavedData.StoredPlayer player) {
        Faction faction = player.faction;
        String formationId = player.formationId;
        SquadCallsign squad = player.squad;
        if (faction == null || formationId == null || squad == null) {
            return;
        }
        boolean wasLeader = player.playerId.equals(data.leader(faction, formationId, squad));
        if (player.playerId.equals(data.commander(faction))) {
            data.setCommander(faction, null);
        }
        player.squad = null;
        player.squadJoinedAtMillis = 0L;
        player.assignedClassId = configuredDefaultClassId(faction, formationId);
        if (wasLeader) {
            List<BattleSavedData.StoredPlayer> remaining = squadMembers(faction, formationId, squad);
            remaining.sort(Comparator
                    .comparing((BattleSavedData.StoredPlayer member) ->
                            server.getPlayerList().getPlayer(member.playerId) == null)
                    .thenComparingLong(
                            (BattleSavedData.StoredPlayer member) -> member.squadJoinedAtMillis)
                    .thenComparing(member -> member.playerId));
            data.setLeader(faction, formationId, squad,
                    remaining.isEmpty() ? null : remaining.get(0).playerId);
        }
    }

    private List<BattleSavedData.StoredPlayer> squadMembers(Faction faction, String formationId,
                                                             SquadCallsign squad) {
        List<BattleSavedData.StoredPlayer> members = new ArrayList<>();
        for (BattleSavedData.StoredPlayer player : data.players()) {
            if (player.faction == faction && Objects.equals(player.formationId, formationId)
                    && player.squad == squad) {
                members.add(player);
            }
        }
        return members;
    }

    private Comparator<BattleSavedData.StoredPlayer> memberComparator(Faction faction,
                                                                      String formationId,
                                                                      SquadCallsign squad) {
        UUID leaderId = data.leader(faction, formationId, squad);
        return Comparator.comparing((BattleSavedData.StoredPlayer member) ->
                        !member.playerId.equals(leaderId))
                .thenComparingLong(member -> member.squadJoinedAtMillis)
                .thenComparing(member -> member.playerId);
    }

    private PermissionView permissionView(ServerPlayer viewer, BattleSavedData.StoredPlayer self) {
        if (self == null || self.faction == null || self.formationId == null) {
            return new PermissionView(false, false, false, false,
                    false, isAdministrator(viewer), false);
        }
        boolean inSquad = self.squad != null;
        boolean leader = isSquadLeader(self.playerId);
        boolean commander = isCommander(self.playerId);
        boolean freeCallsign = false;
        boolean joinableSquad = false;
        for (SquadCallsign callsign : SquadCallsign.values()) {
            int capacity = configuredSquadCapacity(self.faction, self.formationId, callsign);
            if (capacity <= 0) {
                continue;
            }
            int size = squadSize(self.faction, self.formationId, callsign);
            freeCallsign |= size == 0;
            joinableSquad |= size > 0 && size < capacity;
        }
        return new PermissionView(!inSquad && freeCallsign,
                !inSquad && joinableSquad, inSquad,
                leader || isAdministrator(viewer), leader || commander,
                commander || isAdministrator(viewer),
                leader && data.commander(self.faction) == null);
    }

    private List<ClassQuotaView> buildClassQuotaViews(BattleSavedData.StoredPlayer self,
                                                      Map<String, Integer> configuredLimits,
                                                      Map<String, String> configuredDisplayNames) {
        LinkedHashMap<String, Integer> limits = new LinkedHashMap<>();
        Map<String, Integer> source = configuredLimits == null || configuredLimits.isEmpty()
                ? BattleRules.DEFAULT_CLASS_LIMITS : configuredLimits;
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            String classId = normalizeClassId(entry.getKey());
            Integer limit = entry.getValue();
            if (classId != null && limit != null && limit >= 0 && limits.size() < 64) {
                limits.put(classId, Math.min(BattleRules.SQUAD_CAPACITY, limit));
            }
        }
        if (self != null && !limits.containsKey(self.assignedClassId) && limits.size() < 64) {
            limits.put(self.assignedClassId, BattleRules.defaultClassLimit(self.assignedClassId));
        }
        List<ClassQuotaView> result = new ArrayList<>(limits.size());
        Map<String, Integer> usage = new LinkedHashMap<>();
        if (self != null && self.faction != null && self.formationId != null
                && self.squad != null) {
            for (BattleSavedData.StoredPlayer member : data.players()) {
                if (member.faction == self.faction
                        && self.formationId.equals(member.formationId)
                        && member.squad == self.squad) {
                    usage.merge(member.assignedClassId, 1, Integer::sum);
                }
            }
        }
        for (Map.Entry<String, Integer> entry : limits.entrySet()) {
            int used = 0;
            if (self != null && self.faction != null) {
                if (self.squad != null) {
                    used = usage.getOrDefault(entry.getKey(), 0);
                } else if (entry.getKey().equals(self.assignedClassId)) {
                    used = 1;
                }
            }
            String displayName = configuredDisplayNames == null
                    ? "" : configuredDisplayNames.get(entry.getKey());
            result.add(new ClassQuotaView(entry.getKey(), displayName, entry.getValue(), used));
        }
        return result;
    }

    private int classUsage(Faction faction, String formationId, SquadCallsign squad,
                           String classId, UUID excludedId) {
        int count = 0;
        for (BattleSavedData.StoredPlayer player : data.players()) {
            if (player.faction == faction && Objects.equals(player.formationId, formationId)
                    && player.squad == squad
                    && player.assignedClassId.equals(classId)
                    && (excludedId == null || !excludedId.equals(player.playerId))) {
                count++;
            }
        }
        return count;
    }

    private int configuredSquadCapacity(Faction faction, String formationId,
                                        SquadCallsign callsign) {
        if (faction == null || formationId == null || callsign == null) {
            return 0;
        }
        return Math.max(0, Math.min(BattleRules.SQUAD_CAPACITY,
                squadCapacityRules.capacityFor(faction, formationId, callsign)));
    }

    private String configuredDefaultClassId(Faction faction, String formationId) {
        if (faction == null || formationId == null) {
            return BattleRules.DEFAULT_CLASS_ID;
        }
        return FormationService.get(server)
                .flatMap(service -> service.defaultClassId(faction, formationId))
                .map(BattleService::normalizeClassId)
                .orElse(BattleRules.DEFAULT_CLASS_ID);
    }

    private static String normalizeClassId(String classId) {
        if (classId == null) {
            return null;
        }
        String normalized = classId.trim().toLowerCase(java.util.Locale.ROOT);
        return CLASS_ID_PATTERN.matcher(normalized).matches() ? normalized : null;
    }

    private static String normalizeFormationId(String formationId) {
        if (formationId == null) {
            return null;
        }
        String normalized = formationId.trim().toLowerCase(java.util.Locale.ROOT);
        return FORMATION_ID_PATTERN.matcher(normalized).matches() ? normalized : null;
    }

    /** Returns the only formation owning this legacy two-part squad key, or null if absent/ambiguous. */
    private String uniqueFormationForSquad(Faction faction, SquadCallsign squad) {
        String result = null;
        for (BattleSavedData.StoredPlayer player : data.players()) {
            if (player.faction != faction || player.squad != squad || player.formationId == null) {
                continue;
            }
            if (result != null && !result.equals(player.formationId)) {
                return null;
            }
            result = player.formationId;
        }
        return result;
    }

    private static boolean coordinatesFinite(Vec3 position) {
        return Double.isFinite(position.x) && Double.isFinite(position.y)
                && Double.isFinite(position.z)
                && Math.abs(position.x) <= BattleRules.MAX_COORDINATE
                && Math.abs(position.y) <= BattleRules.MAX_COORDINATE
                && Math.abs(position.z) <= BattleRules.MAX_COORDINATE;
    }

    private static Vec3 legacyMarkerEnd(TacticalMarkerType type, Vec3 position,
                                        float directionDegrees) {
        if (position == null || type != TacticalMarkerType.ATTACK_DIRECTION
                || !Float.isFinite(directionDegrees)) {
            return position;
        }
        double radians = Math.toRadians(directionDegrees);
        return position.add(-Math.sin(radians) * BattleRules.LEGACY_ATTACK_DIRECTION_LENGTH_BLOCKS,
                0.0D,
                Math.cos(radians) * BattleRules.LEGACY_ATTACK_DIRECTION_LENGTH_BLOCKS);
    }

    private static long elapsedSince(long nowMillis, long earlierMillis) {
        return nowMillis >= earlierMillis ? nowMillis - earlierMillis : 0L;
    }

    private ActionResult squadKickCooldown(BattleSavedData.StoredPlayer player,
                                           SquadCallsign callsign, long nowMillis) {
        if (player == null || player.faction == null || player.formationId == null
                || callsign == null) {
            return null;
        }
        SquadKickKey key = new SquadKickKey(player.playerId, player.faction,
                player.formationId, callsign);
        Long expiresAt = squadKickCooldowns.get(key);
        if (expiresAt == null) {
            return null;
        }
        if (nowMillis >= expiresAt) {
            squadKickCooldowns.remove(key);
            return null;
        }
        long seconds = Math.max(1L, (expiresAt - nowMillis + 999L) / 1_000L);
        return ActionResult.failure(ActionResult.Code.SQUAD_REJOIN_COOLDOWN,
                "你刚被该小队踢出，请等待 " + seconds + " 秒后再加入");
    }

    private record SquadKickKey(UUID playerId, Faction faction, String formationId,
                                SquadCallsign squad) {
    }

    private static String safeName(ServerPlayer player) {
        String name = player.getGameProfile().getName();
        if (name == null) {
            return "";
        }
        return name.length() <= BattleRules.MAX_PLAYER_NAME_LENGTH
                ? name : name.substring(0, BattleRules.MAX_PLAYER_NAME_LENGTH);
    }

    private static boolean isAdministrator(ServerPlayer player) {
        return player != null && player.hasPermissions(BattleRules.ADMIN_PERMISSION_LEVEL);
    }
}
