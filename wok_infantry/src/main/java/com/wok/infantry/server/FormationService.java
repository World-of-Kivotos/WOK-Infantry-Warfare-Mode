package com.wok.infantry.server;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.PlayerRecord;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.deployment.DeploymentService;
import com.wok.infantry.deployment.VehicleDeploymentPoint;
import com.wok.infantry.formation.FactionDefinition;
import com.wok.infantry.formation.FormationClassEditAction;
import com.wok.infantry.formation.FormationClassEditor;
import com.wok.infantry.formation.FormationClassRule;
import com.wok.infantry.formation.FormationCapabilityProfile;
import com.wok.infantry.formation.FormationConfigData;
import com.wok.infantry.formation.FormationDefinition;
import com.wok.infantry.formation.FormationLoadoutEditAction;
import com.wok.infantry.formation.FormationLoadoutRuleEditor;
import com.wok.infantry.formation.FormationSquadDefinition;
import com.wok.infantry.formation.FormationVehicleDefinition;
import com.wok.infantry.formation.vote.FormationVotePhase;
import com.wok.infantry.formation.vote.FormationVoteResult;
import com.wok.infantry.formation.vote.FormationVoteSavedData;
import com.wok.infantry.formation.vote.FormationVoteSnapshot;
import com.wok.infantry.formation.selection.FactionSelectionView;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.selection.FormationSelectionView;
import com.wok.infantry.formation.vehicle.FormationVehicleProvider;
import com.wok.infantry.formation.vehicle.SuperbWarfareVehicleGate;
import com.wok.infantry.formation.vehicle.SuperbWarfareVehicleService;
import com.wok.infantry.formation.vehicle.VehicleAllocationKey;
import com.wok.infantry.formation.vehicle.VehicleDeploymentRequest;
import com.wok.infantry.formation.vehicle.VehicleDeploymentResult;
import com.wok.infantry.formation.vehicle.VehicleOwnership;
import com.wok.infantry.formation.vehicle.VehiclePersistentData;
import com.wok.infantry.formation.vehicle.VehicleRemovalObservation;
import com.wok.infantry.formation.vehicle.VehicleReplenishmentSavedData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

/** Server-authoritative catalog and atomic faction/formation selection coordinator. */
public final class FormationService {
    private static final String VEHICLE_MOD_ID = "superbwarfare";
    private static final Map<MinecraftServer, FormationService> INSTANCES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final MinecraftServer server;
    private final FormationRepository repository = new FormationRepository();
    private final FormationVehicleProvider vehicleProvider = new SuperbWarfareVehicleService();
    private volatile FormationConfigData catalog = new FormationConfigData();
    private volatile long generation;
    private FormationVoteSavedData voteData;
    private VehicleReplenishmentSavedData replenishmentData;
    private boolean initializing;
    private boolean initialized;
    private boolean stopped;

    private FormationService(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    /**
     * Performs side effects only after the lightweight instance has been published in INSTANCES.
     * Vehicle retirement can synchronously emit entity lifecycle events, so constructing inside
     * computeIfAbsent would allow recursive construction of a second provider.
     */
    private void initialize() {
        synchronized (this) {
            if (initialized || initializing || stopped) {
                return;
            }
            initializing = true;
        }
        try {
            voteData = FormationVoteSavedData.get(server);
            replenishmentData = VehicleReplenishmentSavedData.get(server);
            ActionResult replenishmentStatus = replenishmentData.status();
            if (!replenishmentStatus.success()) {
                WokInfantryMod.LOGGER.error("Vehicle replenishment ledger started fail-closed: {}",
                        replenishmentStatus.message());
            }
            UUID activeSession = DeploymentService.get(server)
                    .map(DeploymentService::sessionId).orElse(null);
            ActionResult vehicleStartup = activeSession == null
                    ? vehicleProvider.start(server)
                    : vehicleProvider.start(server, activeSession);
            if (!vehicleStartup.success()) {
                WokInfantryMod.LOGGER.warn("Formation vehicle ledger started fail-closed: {}",
                        vehicleStartup.message());
            }
            ActionResult catalogStartup = reload();
            if (!catalogStartup.success()) {
                WokInfantryMod.LOGGER.error("Formation catalog started fail-closed: {}",
                        catalogStartup.message());
            }
        } catch (RuntimeException | LinkageError exception) {
            WokInfantryMod.LOGGER.error(
                    "Formation service initialization failed closed after publication", exception);
        } finally {
            synchronized (this) {
                initializing = false;
                initialized = true;
            }
        }
    }

    public static void start(MinecraftServer server) {
        if (server == null) {
            return;
        }
        FormationService service;
        synchronized (INSTANCES) {
            service = INSTANCES.get(server);
            if (service == null) {
                service = new FormationService(server);
                INSTANCES.put(server, service);
            }
        }
        service.initialize();
    }

    public static void stop(MinecraftServer server) {
        if (server == null) {
            return;
        }
        FormationService service;
        synchronized (INSTANCES) {
            service = INSTANCES.remove(server);
        }
        if (service != null) {
            synchronized (service) {
                service.stopped = true;
            }
            ActionResult result = service.vehicleProvider.stop(server);
            if (!result.success()) {
                WokInfantryMod.LOGGER.warn("Formation vehicle ledger stopped fail-closed: {}",
                        result.message());
            }
        }
    }

    public static Optional<FormationService> get(MinecraftServer server) {
        if (server == null) {
            return Optional.empty();
        }
        synchronized (INSTANCES) {
            return Optional.ofNullable(INSTANCES.get(server));
        }
    }

    public static Optional<FormationService> get(ServerPlayer player) {
        return player == null ? Optional.empty() : get(player.server);
    }

    public ActionResult reload() {
        FormationRepository.LoadResult load = repository.load();
        if (!load.success()) {
            WokInfantryMod.LOGGER.error("Formation catalog reload rejected: {}", load.message());
            return ActionResult.failure(ActionResult.Code.FORMATION_UNAVAILABLE, load.message());
        }
        return publishCatalog(load.config(), load.message());
    }

    /** The transfer transaction has already written both files before publishing either. */
    ActionResult publishImportedCatalog(FormationConfigData imported) {
        repository.publishImported(imported);
        return publishCatalog(repository.config(), "阵营配装数据包已应用");
    }

    private ActionResult publishCatalog(FormationConfigData loaded, String message) {
        long loadedGeneration;
        synchronized (this) {
            catalog = loaded;
            generation = generation == Long.MAX_VALUE ? 1L : generation + 1L;
            loadedGeneration = generation;
        }
        loaded.diagnostics().forEach(diagnostic -> WokInfantryMod.LOGGER.warn(
                "Formation config {} at {}: {}", diagnostic.kind(), diagnostic.path(),
                diagnostic.message()));
        WokInfantryMod.LOGGER.info("Loaded {} public factions from {} (generation {})",
                loaded.factions().size(), repository.path(), loadedGeneration);
        try {
            reconcileVoteCandidates(loaded);
            int cleared = reconcileSelections();
            if (cleared > 0) {
                WokInfantryMod.LOGGER.warn(
                        "Reconciled {} player roster records after formation catalog reload",
                        cleared);
            }
            ActionResult vehicleReconciliation = reconcileVehicles(loaded);
            if (!vehicleReconciliation.success()) {
                WokInfantryMod.LOGGER.error(
                        "Formation catalog generation {} applied, but vehicle reconciliation failed: {}",
                        loadedGeneration, vehicleReconciliation.message());
                return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                        "编制目录已应用，但载具台账调和失败："
                                + vehicleReconciliation.message());
            }
        } catch (RuntimeException | LinkageError exception) {
            WokInfantryMod.LOGGER.error(
                    "Formation catalog generation {} was applied before reconciliation failed",
                    loadedGeneration, exception);
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "编制目录已应用，但状态调和异常；客户端目录已刷新，请检查服务端日志");
        }
        return ActionResult.ok(message + "；generation=" + loadedGeneration);
    }

    private void reconcileVoteCandidates(FormationConfigData active) {
        if (voteData == null) {
            return;
        }
        for (Faction side : Faction.values()) {
            FactionDefinition faction = active.findFaction(side).orElse(null);
            List<String> candidates = faction == null || !faction.enabled() ? List.of()
                    : faction.formations().stream()
                    .filter(formation -> availability(faction, formation).available())
                    .map(FormationDefinition::id).toList();
            voteData.reconcileCandidates(side, candidates);
        }
    }

    public long generation() {
        return generation;
    }

    public Path configPath() {
        return repository.path();
    }

    public FormationConfigData catalog() {
        return catalog.copy();
    }

    /** Atomically persists one formation-scoped loadout rule without rewriting unrelated data. */
    public ActionResult editLoadoutRule(String publicFactionId, String formationId,
                                        String classId, String slotId, String entryId,
                                        FormationLoadoutEditAction action) {
        FormationConfigData updated;
        try {
            updated = FormationLoadoutRuleEditor.apply(catalog, publicFactionId, formationId,
                    classId, slotId, entryId, action);
        } catch (IllegalArgumentException exception) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    exception.getMessage());
        }
        if (!repository.replace(updated)) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "formations.json 写入失败；原配置继续生效");
        }
        long updatedGeneration;
        synchronized (this) {
            catalog = updated;
            generation = generation == Long.MAX_VALUE ? 1L : generation + 1L;
            updatedGeneration = generation;
        }
        return ActionResult.ok("编制配装白名单已保存；generation=" + updatedGeneration);
    }

    /** Persists one formation-owned profession and reconciles any affected live roster. */
    public ActionResult editClass(String publicFactionId, String formationId,
                                  String classId, String displayName, int squadLimit,
                                  FormationClassEditAction action) {
        FormationConfigData updated;
        try {
            updated = FormationClassEditor.apply(catalog, publicFactionId, formationId,
                    classId, displayName, squadLimit, action);
        } catch (IllegalArgumentException exception) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    exception.getMessage());
        }
        if (!repository.replace(updated)) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "formations.json 写入失败；原配置继续生效");
        }
        long updatedGeneration;
        synchronized (this) {
            catalog = updated;
            generation = generation == Long.MAX_VALUE ? 1L : generation + 1L;
            updatedGeneration = generation;
        }
        int affected = reconcileSelections();
        return ActionResult.ok("编制职业已保存；generation=" + updatedGeneration
                + (affected == 0 ? "" : "；已调和 " + affected + " 名玩家"));
    }

    /** Publishes a planner-validated single-profession copy after loadouts are durable. */
    public ActionResult applyCopiedClassLoadoutCatalog(FormationConfigData updated,
                                                        String targetFactionId,
                                                        String targetFormationId,
                                                        String replacedClassId,
                                                        String copiedClassId) {
        FactionDefinition currentFaction = catalog.findFaction(targetFactionId).orElse(null);
        Faction targetSide = currentFaction == null ? null : currentFaction.battleSide();
        FormationDefinition currentFormation = currentFaction == null ? null
                : currentFaction.findFormation(targetFormationId).orElse(null);
        if (updated == null || targetSide == null || currentFormation == null
                || currentFormation.findClass(replacedClassId).isEmpty()
                || updated.findFormation(targetFactionId, targetFormationId)
                .flatMap(formation -> formation.findClass(copiedClassId)).isEmpty()) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "复制的兵种配装无效");
        }
        if (!repository.replace(updated)) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "formations.json 写入失败；原编制继续生效");
        }
        long updatedGeneration;
        synchronized (this) {
            catalog = repository.config();
            generation = generation == Long.MAX_VALUE ? 1L : generation + 1L;
            updatedGeneration = generation;
        }
        int remapped = BattleService.get(server).map(battle -> battle.remapFormationClass(
                targetSide, targetFormationId, replacedClassId, copiedClassId).size())
                .orElse(0);
        int affected = reconcileSelections();
        return ActionResult.ok("已粘贴兵种配装；generation=" + updatedGeneration
                + (remapped == 0 ? "" : "；保留 " + remapped + " 名玩家的当前职业")
                + (affected == 0 ? "" : "；另调和 " + affected + " 名玩家"));
    }

    public boolean classReferenced(String classId) {
        if (classId == null || classId.isBlank()) {
            return false;
        }
        return catalog.factions().stream().flatMap(faction -> faction.formations().stream())
                .anyMatch(formation -> formation.findClass(classId).isPresent());
    }

    public String classDisplayName(UUID playerId, String classId) {
        return classRule(playerId, classId).map(FormationClassRule::displayName)
                .orElse("");
    }

    /** Opens a policy-neutral ballot containing every currently selectable formation. */
    public ActionResult openVote(ServerPlayer administrator, String publicFactionId,
                                 boolean allowVoteChange) {
        if (administrator == null || administrator.server != server
                || !administrator.hasPermissions(BattleRules.ADMIN_PERMISSION_LEVEL)) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                    "需要服务端管理员权限开启编制投票");
        }
        FactionDefinition faction = catalog.findFaction(publicFactionId).orElse(null);
        if (faction == null || !faction.enabled()) {
            return ActionResult.failure(ActionResult.Code.FORMATION_NOT_FOUND,
                    "阵营不存在或已停用");
        }
        List<String> candidates = faction.formations().stream()
                .filter(formation -> availability(faction, formation).available())
                .map(FormationDefinition::id).toList();
        if (voteData == null) {
            return ActionResult.failure(ActionResult.Code.FORMATION_UNAVAILABLE,
                    "编制投票存档尚未就绪");
        }
        return voteResult(voteData.open(faction.battleSide(), candidates, allowVoteChange));
    }

    /** Player's first-step faction choice while formation assignment waits for the shared result. */
    public ActionResult selectFaction(ServerPlayer player, long clientGeneration,
                                      String publicFactionId) {
        if (player == null || player.server != server) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                    "操作者不属于当前服务器");
        }
        if (clientGeneration != generation) {
            return ActionResult.failure(ActionResult.Code.FORMATION_UNAVAILABLE,
                    "阵营编制目录已更新，请重新选择阵营");
        }
        FactionDefinition faction = catalog.findFaction(publicFactionId).orElse(null);
        if (faction == null || !faction.enabled()) {
            return ActionResult.failure(ActionResult.Code.FORMATION_NOT_FOUND,
                    "阵营不存在或已停用");
        }
        BattleService battle = BattleService.get(server).orElse(null);
        if (battle == null) {
            return ActionResult.failure(ActionResult.Code.FORMATION_UNAVAILABLE,
                    "战局服务尚未就绪");
        }
        ActionResult joined = battle.selectFaction(player, faction.battleSide(),
                faction.maxPlayers());
        if (!joined.success()) {
            return joined;
        }
        DeploymentService.get(server).ifPresent(deployment ->
                deployment.onPlayerConnected(player));
        return joined;
    }

    /** Casts one server-validated vote; the caller explicitly owns the change-vote policy. */
    public ActionResult castVote(ServerPlayer player, long clientGeneration,
                                 String formationId) {
        if (player == null || player.server != server) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                    "投票玩家不属于当前服务器");
        }
        if (clientGeneration != generation) {
            return ActionResult.failure(ActionResult.Code.FORMATION_UNAVAILABLE,
                    "阵营编制目录已更新，请刷新后重新投票");
        }
        BattleService battle = BattleService.get(server).orElse(null);
        PlayerRecord record = battle == null ? null
                : battle.playerRecord(player.getUUID()).orElse(null);
        FactionDefinition faction = record == null || record.faction() == null ? null
                : catalog.findFaction(record.faction()).orElse(null);
        FormationDefinition formation = faction == null ? null
                : faction.findFormation(formationId).orElse(null);
        if (faction == null || formation == null
                || !availability(faction, formation).available()) {
            return ActionResult.failure(ActionResult.Code.FORMATION_NOT_FOUND,
                    "只能为自己当前阵营的有效具体编制投票");
        }
        if (voteData == null) {
            return ActionResult.failure(ActionResult.Code.FORMATION_UNAVAILABLE,
                    "编制投票存档尚未就绪");
        }
        return voteResult(voteData.cast(record.faction(), player.getUUID(), formation.id()));
    }

    /** Locks an explicit result without inventing a winner or tie-break rule. */
    public ActionResult lockVote(ServerPlayer administrator, String publicFactionId,
                                 String formationId) {
        if (administrator == null || administrator.server != server
                || !administrator.hasPermissions(BattleRules.ADMIN_PERMISSION_LEVEL)) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                    "需要服务端管理员权限锁定编制结果");
        }
        ResolvedSelection resolved = resolveSelection(catalog, publicFactionId, formationId);
        if (!resolved.result().success()) {
            return resolved.result();
        }
        if (voteData == null) {
            return ActionResult.failure(ActionResult.Code.FORMATION_UNAVAILABLE,
                    "编制投票存档尚未就绪");
        }
        FormationVoteSnapshot vote = voteData.snapshot(resolved.faction().battleSide(), null);
        if (vote.phase() != FormationVotePhase.OPEN
                || !vote.candidates().contains(resolved.formation().id())) {
            return voteResult(voteData.lock(resolved.faction().battleSide(),
                    resolved.formation().id()));
        }
        BattleService battle = BattleService.get(server).orElse(null);
        if (battle == null) {
            return ActionResult.failure(ActionResult.Code.FORMATION_UNAVAILABLE,
                    "战局服务尚未就绪，投票结果未锁定");
        }
        ActionResult applied = battle.applySharedFormation(resolved.faction().battleSide(),
                resolved.formation().id(), resolved.formation().capacity());
        if (!applied.success()) {
            return applied;
        }
        FormationVoteResult locked = voteData.lock(resolved.faction().battleSide(),
                resolved.formation().id());
        return locked.success()
                ? ActionResult.ok("已锁定阵营共享编制："
                + resolved.formation().displayName())
                : voteResult(locked);
    }

    public FormationVoteSnapshot voteSnapshot(Faction faction, UUID viewerId) {
        if (faction == null || voteData == null) {
            Faction safeFaction = faction == null ? Faction.BLUE : faction;
            return new FormationVoteSnapshot(safeFaction, 0L,
                    FormationVotePhase.NOT_STARTED, false, "", "", List.of(), Map.of());
        }
        return voteData.snapshot(faction, viewerId);
    }

    public void clearVotesForBattleReset() {
        if (voteData != null) {
            voteData.clearAll();
        }
    }

    public Optional<FactionDefinition> selectedFaction(UUID playerId) {
        BattleService battle = BattleService.get(server).orElse(null);
        if (battle == null || playerId == null) {
            return Optional.empty();
        }
        PlayerRecord record = battle.playerRecord(playerId).orElse(null);
        if (record == null || record.faction() == null) {
            return Optional.empty();
        }
        FormationConfigData active = catalog;
        FactionDefinition faction = active.findFaction(record.faction()).orElse(null);
        return faction != null && faction.enabled()
                ? Optional.of(faction) : Optional.empty();
    }

    /** Resolves a persisted public faction id (for example academy) to its blue/red battle side. */
    public Optional<Faction> battleSideForPublicFaction(String publicFactionId) {
        if (publicFactionId == null || publicFactionId.isBlank()) {
            return Optional.empty();
        }
        FactionDefinition faction = catalog.findFaction(publicFactionId).orElse(null);
        return faction == null || !faction.enabled()
                ? Optional.empty() : Optional.ofNullable(faction.battleSide());
    }

    public Optional<FormationDefinition> selectedFormation(UUID playerId) {
        BattleService battle = BattleService.get(server).orElse(null);
        if (battle == null || playerId == null) {
            return Optional.empty();
        }
        PlayerRecord record = battle.playerRecord(playerId).orElse(null);
        FormationConfigData active = catalog;
        if (record == null || record.faction() == null || record.formationId() == null
                || !validSelection(active, record.faction(), record.formationId())) {
            return Optional.empty();
        }
        return active.findFormation(record.faction(), record.formationId());
    }

    public Optional<FormationClassRule> classRule(UUID playerId, String classId) {
        return selectedFormation(playerId).flatMap(formation -> formation.findClass(classId));
    }

    /** Direct lookup used by battle mutations; display order does not redefine the safe fallback. */
    public Optional<String> defaultClassId(Faction battleSide, String formationId) {
        if (battleSide == null || formationId == null) {
            return Optional.empty();
        }
        return catalog.findFormation(battleSide, formationId)
                .flatMap(FormationDefinition::defaultClass)
                .map(FormationClassRule::classId);
    }

    public Optional<FormationSquadDefinition> squadRule(UUID playerId, String callsign) {
        return selectedFormation(playerId).flatMap(formation -> formation.findSquad(callsign));
    }

    /** Direct server rule lookup for battle internals that already own the selected side/id. */
    public Optional<FormationSquadDefinition> squadRule(Faction battleSide,
                                                         String formationId,
                                                         String callsign) {
        FormationConfigData active = catalog;
        return validSelection(active, battleSide, formationId)
                ? active.findFormation(battleSide, formationId)
                .flatMap(formation -> formation.findSquad(callsign))
                : Optional.empty();
    }

    /** Empty restrictions mean all configured entries in that slot are allowed. */
    public boolean allowsLoadoutEntry(UUID playerId, String classId,
                                     String slotId, String entryId) {
        FormationClassRule rule = classRule(playerId, classId).orElse(null);
        return rule != null && rule.allowsEntry(slotId, entryId);
    }

    /** Formation-level support gate; an absent selection or a NONE policy fails closed. */
    public boolean allowsSupport(UUID playerId, ResourceLocation supportId) {
        if (playerId == null || supportId == null) {
            return false;
        }
        FormationDefinition formation = selectedFormation(playerId).orElse(null);
        return formation != null
                && formation.capabilities().support().allows(supportId.toString());
    }

    /** Uses the formation override when configured, otherwise preserves the caller's default. */
    public long respawnDelayTicks(UUID playerId, long fallbackTicks) {
        FormationDefinition formation = selectedFormation(playerId).orElse(null);
        if (formation == null || formation.capabilities().respawn().inheritsGlobalDelay()) {
            return Math.max(0L, fallbackTicks);
        }
        return Math.multiplyExact((long) formation.capabilities().respawn().delaySeconds(), 20L);
    }

    public Map<String, Integer> classLimits(UUID playerId, String callsign) {
        FormationDefinition formation = selectedFormation(playerId).orElse(null);
        if (formation == null) {
            return Map.of();
        }
        FormationSquadDefinition squad = formation.findSquad(callsign).orElse(null);
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        for (FormationClassRule rule : formation.classes()) {
            int limit = squad == null ? rule.squadLimit()
                    : squad.classLimit(rule.classId(), rule.squadLimit());
            result.put(rule.classId(), Math.min(BattleRules.SQUAD_CAPACITY, Math.min(limit,
                    squad == null ? BattleRules.SQUAD_CAPACITY : squad.capacity())));
        }
        return java.util.Collections.unmodifiableMap(result);
    }

    public ActionResult select(ServerPlayer player, long clientGeneration,
                               String publicFactionId, String formationId) {
        ActionResult joined = selectFaction(player, clientGeneration, publicFactionId);
        if (!joined.success()) {
            return joined;
        }
        FactionDefinition faction = selectedFaction(player.getUUID()).orElse(null);
        FormationVoteSnapshot vote = faction == null ? null
                : voteSnapshot(faction.battleSide(), player.getUUID());
        if (vote != null && vote.phase() == FormationVotePhase.LOCKED) {
            return formationId.equals(vote.lockedFormationId())
                    ? joined
                    : ActionResult.failure(ActionResult.Code.FORMATION_LOCKED,
                    "阵营共享编制已经锁定，不能提交其他编制");
        }
        return castVote(player, clientGeneration, formationId);
    }

    /** Shared authority check for commands, UI selection and future match orchestration. */
    public ActionResult validateSelection(String publicFactionId, String formationId) {
        return resolveSelection(catalog, publicFactionId, formationId).result();
    }

    /** Administrator assignment uses the same catalog and optional-MOD gate as normal selection. */
    public ActionResult forceAssign(ServerPlayer administrator, ServerPlayer target,
                                    String publicFactionId, String formationId) {
        if (administrator == null || target == null || administrator.server != server
                || target.server != server) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                    "操作者或目标玩家不属于当前服务器");
        }
        ResolvedSelection resolved = resolveSelection(catalog, publicFactionId, formationId);
        if (!resolved.result().success()) {
            return resolved.result();
        }
        BattleService battle = BattleService.get(server).orElse(null);
        if (battle == null) {
            return ActionResult.failure(ActionResult.Code.FORMATION_UNAVAILABLE,
                    "战局服务尚未就绪");
        }
        ActionResult result = battle.forceAssignFormation(administrator, target.getUUID(),
                resolved.faction().battleSide(), resolved.formation().id(),
                resolved.faction().maxPlayers(), resolved.formation().capacity());
        if (result.success()) {
            DeploymentService.get(server).ifPresent(deployment ->
                    deployment.onPlayerConnected(target));
        }
        return result;
    }

    /** Repairs every persisted roster entry against the current authoritative catalog. */
    public int reconcileSelections() {
        FormationConfigData active = catalog;
        BattleService battle = BattleService.get(server).orElse(null);
        if (battle == null) {
            return 0;
        }
        EnumMap<Faction, Integer> factionCounts = new EnumMap<>(Faction.class);
        Map<String, Integer> formationCounts = new LinkedHashMap<>();
        List<UUID> cleared = battle.reconcileFormationSelections((side, formationId) -> {
            FactionDefinition faction = active.findFaction(side).orElse(null);
            if (faction == null || !faction.enabled()) {
                return false;
            }
            int factionPopulation = factionCounts.getOrDefault(side, 0);
            if (factionPopulation >= faction.maxPlayers()) {
                return false;
            }
            if (formationId == null || formationId.isBlank()) {
                factionCounts.put(side, factionPopulation + 1);
                return true;
            }
            FormationDefinition formation = faction.findFormation(formationId).orElse(null);
            if (formation == null || !availability(faction, formation).available()) {
                return false;
            }
            String formationKey = side.id() + "/" + formation.id();
            int formationPopulation = formationCounts.getOrDefault(formationKey, 0);
            if (formationPopulation >= formation.capacity()) {
                return false;
            }
            factionCounts.put(side, factionPopulation + 1);
            formationCounts.put(formationKey, formationPopulation + 1);
            return true;
        });
        List<UUID> rosterChanged = battle.reconcileFormationRosters(
                (side, formationId, callsign) -> rosterRuleFor(active, side,
                        formationId, callsign));
        List<UUID> defaultsChanged = battle.reconcileFormationDefaults(
                (side, formationId) -> active.findFormation(side, formationId)
                        .flatMap(FormationDefinition::defaultClass)
                        .map(FormationClassRule::classId));
        List<UUID> affected = mergeAffectedPlayers(
                mergeAffectedPlayers(cleared, rosterChanged), defaultsChanged);
        DeploymentService.get(server).ifPresent(deployment ->
                affected.forEach(deployment::onRosterChanged));
        return affected.size();
    }

    /** Catalog adapter kept package-visible for rule-boundary unit tests. */
    static Optional<BattleService.FormationRosterRule> rosterRuleFor(
            FormationConfigData active, Faction side, String formationId,
            SquadCallsign callsign) {
        if (active == null || side == null || callsign == null) {
            return Optional.empty();
        }
        FactionDefinition faction = active.findFaction(side).orElse(null);
        FormationDefinition formation = faction == null ? null
                : faction.findFormation(formationId).orElse(null);
        if (faction == null || formation == null
                || !availability(faction, formation).available()) {
            return Optional.empty();
        }
        FormationSquadDefinition squad = formation.findSquad(callsign.id()).orElse(null);
        return squad == null ? Optional.empty() : Optional.of(
                new BattleService.FormationRosterRule(squad.capacity(),
                        orderedClassLimits(formation, squad)));
    }

    /** Stable unique union used before deployment receives roster-change callbacks. */
    static List<UUID> mergeAffectedPlayers(List<UUID> selectionChanges,
                                           List<UUID> rosterChanges) {
        LinkedHashSet<UUID> affected = new LinkedHashSet<>(
                Objects.requireNonNull(selectionChanges, "selectionChanges"));
        affected.addAll(Objects.requireNonNull(rosterChanges, "rosterChanges"));
        return List.copyOf(affected);
    }

    private boolean reconcileSelection(UUID playerId) {
        FormationConfigData active = catalog;
        BattleService battle = BattleService.get(server).orElse(null);
        if (battle == null || !battle.reconcileFormationSelection(playerId,
                (side, formationId) -> validRosterSelection(active, side, formationId))) {
            return false;
        }
        DeploymentService.get(server).ifPresent(deployment ->
                deployment.onRosterChanged(playerId));
        return true;
    }

    /** Deploys one idempotent vehicle batch at the side's physical arrow block. */
    public VehicleDeploymentResult deployVehicles(String publicFactionId, String formationId) {
        ResolvedSelection resolved = resolveSelection(catalog, publicFactionId, formationId);
        if (!resolved.result().success()) {
            return VehicleDeploymentResult.failure(resolved.result());
        }
        if (resolved.formation().vehicles().isEmpty()) {
            return VehicleDeploymentResult.success("当前编制没有配置载具", Map.of());
        }
        DeploymentService deployment = DeploymentService.get(server).orElse(null);
        if (deployment == null) {
            return VehicleDeploymentResult.failure(ActionResult.failure(
                    ActionResult.Code.INVALID_DEPLOYMENT_POINT, "部署服务尚未就绪"));
        }
        VehicleDeploymentPoint origin = deployment.vehicleDeploymentPoint(
                resolved.faction().battleSide()).orElse(null);
        if (origin == null) {
            return VehicleDeploymentResult.failure(ActionResult.failure(
                    ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                    "该阵营缺少有效载具部署方块，或方块状态与记录不一致"));
        }
        if (replenishmentData == null || !replenishmentData.status().success()) {
            return VehicleDeploymentResult.failure(replenishmentData == null
                    ? ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具补充台账尚未就绪") : replenishmentData.status());
        }
        UUID sessionId = deployment.sessionId();
        List<FormationVehicleDefinition> deployable = resolved.formation().vehicles().stream()
                .filter(vehicle -> !replenishmentData.contains(new VehicleAllocationKey(
                        sessionId, resolved.faction().id(), resolved.formation().id(),
                        vehicle.id())))
                .toList();
        if (deployable.isEmpty()) {
            return VehicleDeploymentResult.success(
                    "当前载具槽位均处于不可再生或补充冷却状态", Map.of());
        }
        VehicleDeploymentRequest request = VehicleDeploymentRequest.fromFormationDefinitions(
                sessionId, resolved.faction().id(), resolved.formation().id(),
                origin.dimension(), Vec3.atBottomCenterOf(origin.anchorPosition()),
                origin.yaw(),
                deployable);
        return vehicleProvider.deployBatch(server, request);
    }

    public ActionResult resetActiveVehicleSession() {
        DeploymentService deployment = DeploymentService.get(server).orElse(null);
        return deployment == null
                ? ActionResult.failure(ActionResult.Code.INVALID_TARGET, "部署服务尚未就绪")
                : resetVehicleSession(deployment.sessionId());
    }

    public ActionResult resetVehicleSession(UUID sessionId) {
        if (sessionId == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET, "战局会话缺失");
        }
        ActionResult reset = vehicleProvider.resetSession(server, sessionId);
        if (reset.success() && replenishmentData != null) {
            replenishmentData.clearSession(sessionId);
        }
        return reset;
    }

    /** Rotates the provider authority and retires every allocation from older sessions. */
    public ActionResult activateVehicleSession(UUID sessionId) {
        if (sessionId == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET, "战局会话缺失");
        }
        ActionResult started = vehicleProvider.start(server);
        if (!started.success()) {
            return started;
        }
        ActionResult retired = vehicleProvider.retireSessionsExcept(server, sessionId);
        if (!retired.success()) {
            return retired;
        }
        return replenishmentData == null
                ? ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                "载具补充台账尚未就绪")
                : replenishmentData.reconcile(sessionId,
                expectedVehicleAllocations(catalog, sessionId));
    }

    public ActionResult observeLoadedVehicle(Entity entity) {
        return vehicleProvider.observeLoadedVehicle(entity);
    }

    public ActionResult observeRemovedVehicle(Entity entity) {
        VehicleRemovalObservation observation = vehicleProvider.observeRemovedVehicle(entity);
        if (!observation.result().success()) {
            return observation.result();
        }
        return observation.destroyed().map(this::recordVehicleLoss)
                .orElse(observation.result());
    }

    /** Replenishes ready combat losses; called once per second by the server lifecycle bridge. */
    public void tickVehicles() {
        if (replenishmentData == null || !replenishmentData.status().success()) {
            return;
        }
        DeploymentService deployment = DeploymentService.get(server).orElse(null);
        if (deployment == null) {
            return;
        }
        long now = server.overworld().getGameTime();
        for (VehicleReplenishmentSavedData.Entry pending
                : replenishmentData.ready(now, 8)) {
            VehicleAllocationKey key = pending.key();
            if (!deployment.sessionId().equals(key.sessionId())) {
                continue;
            }
            FactionDefinition faction = catalog.findFaction(key.factionId()).orElse(null);
            FormationDefinition formation = faction == null ? null
                    : faction.findFormation(key.formationId()).orElse(null);
            FormationVehicleDefinition vehicle = formation == null ? null
                    : formation.findVehicle(key.allocationId()).orElse(null);
            ResourceLocation configuredType = vehicle == null ? null
                    : ResourceLocation.tryParse(vehicle.entityId());
            VehicleDeploymentPoint origin = faction == null ? null
                    : deployment.vehicleDeploymentPoint(faction.battleSide()).orElse(null);
            if (faction == null || formation == null || vehicle == null || origin == null
                    || !pending.entityTypeId().equals(configuredType)) {
                continue;
            }
            VehicleDeploymentRequest request = VehicleDeploymentRequest
                    .fromFormationDefinitions(key.sessionId(), key.factionId(),
                            key.formationId(), origin.dimension(),
                            Vec3.atBottomCenterOf(origin.anchorPosition()), origin.yaw(),
                            List.of(vehicle));
            VehicleDeploymentResult result = vehicleProvider.deployBatch(server, request);
            if (result.result().success()) {
                replenishmentData.remove(key);
                WokInfantryMod.LOGGER.info("Replenished formation vehicle {} for {}/{}",
                        key.allocationId(), key.factionId(), key.formationId());
            }
        }
    }

    private ActionResult recordVehicleLoss(VehicleOwnership ownership) {
        if (ownership == null || replenishmentData == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具损失补充台账尚未就绪");
        }
        DeploymentService deployment = DeploymentService.get(server).orElse(null);
        if (deployment == null || !deployment.sessionId().equals(ownership.sessionId())) {
            return ActionResult.ok("过期战局载具已移除，不安排补充");
        }
        FactionDefinition faction = catalog.findFaction(ownership.factionId()).orElse(null);
        FormationDefinition formation = faction == null ? null
                : faction.findFormation(ownership.formationId()).orElse(null);
        FormationVehicleDefinition vehicle = formation == null ? null
                : formation.findVehicle(ownership.allocationId()).orElse(null);
        ResourceLocation configuredType = vehicle == null ? null
                : ResourceLocation.tryParse(vehicle.entityId());
        if (vehicle == null || !ownership.entityTypeId().equals(configuredType)) {
            return ActionResult.ok("载具槽位已从当前编制移除，不安排补充");
        }
        int cooldownSeconds = vehicle.replenishmentCooldownSeconds();
        long readyAt = cooldownSeconds < 0
                ? VehicleReplenishmentSavedData.NEVER_REPLENISH
                : saturatingAdd(server.overworld().getGameTime(), cooldownSeconds * 20L);
        return replenishmentData.recordLoss(ownership.allocationKey(),
                ownership.entityTypeId(), readyAt);
    }

    private static long saturatingAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    /** Unmanaged vehicles pass through; managed vehicles require current session and faction. */
    public ActionResult authorizeMount(ServerPlayer player, Entity vehicle) {
        if (player == null || player.server != server) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                    "乘员不属于当前服务器");
        }
        reconcileSelection(player.getUUID());
        UUID activeSession = DeploymentService.get(server)
                .map(DeploymentService::sessionId).orElse(null);
        String publicFactionId = selectedFaction(player.getUUID())
                .map(FactionDefinition::id).orElse("");
        String formationId = selectedFormation(player.getUUID())
                .map(FormationDefinition::id).orElse("");
        Optional<VehicleOwnership> ownership = VehiclePersistentData.read(vehicle);
        if (ownership.isPresent()
                && !currentVehicleDefinitionMatches(catalog, ownership.get())) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具不再属于当前生效的编制配置");
        }
        return vehicleProvider.authorizeMount(vehicle, activeSession, publicFactionId,
                formationId);
    }

    public FormationSelectionSnapshot snapshotFor(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (player.server != server) {
            throw new IllegalArgumentException("Player belongs to a different server");
        }
        reconcileSelection(player.getUUID());
        FormationConfigData active;
        long activeGeneration;
        synchronized (this) {
            active = catalog;
            activeGeneration = generation;
        }
        BattleService battle = BattleService.get(server).orElseThrow();
        PlayerRecord selected = battle.playerRecord(player.getUUID()).orElse(null);
        Faction selectedSide = selected == null ? null : selected.faction();
        String selectedFormation = selected == null ? "" : selected.formationId();
        String selectedPublicFaction = selectedSide == null ? ""
                : active.findFaction(selectedSide).map(FactionDefinition::id).orElse("");
        List<FactionSelectionView> factions = active.factions().stream()
                .map(faction -> factionView(battle, faction)).toList();
        boolean required = selectedSide == null || selectedFormation.isBlank()
                || selectedPublicFaction.isBlank();
        FormationVoteSnapshot vote = selectedSide == null || voteData == null ? null
                : voteData.snapshot(selectedSide, player.getUUID());
        return new FormationSelectionSnapshot(activeGeneration, required, selectedPublicFaction,
                selectedFormation,
                vote == null ? FormationVotePhase.NOT_STARTED : vote.phase(),
                vote != null && vote.voteChangeAllowed(),
                vote == null ? "" : vote.ownVote(),
                vote == null ? "" : vote.lockedFormationId(),
                vote == null ? Map.of() : vote.tally(), factions);
    }

    private FactionSelectionView factionView(BattleService battle, FactionDefinition faction) {
        int population = battle.factionSize(faction.battleSide());
        List<FormationSelectionView> formations = faction.formations().stream()
                .map(formation -> formationView(battle, faction, formation)).toList();
        boolean available = faction.enabled() && population < faction.maxPlayers()
                && formations.stream().anyMatch(FormationSelectionView::available);
        return new FactionSelectionView(faction.id(), faction.displayName(),
                faction.description(), population, faction.maxPlayers(), available, formations);
    }

    private FormationSelectionView formationView(BattleService battle, FactionDefinition faction,
                                                  FormationDefinition formation) {
        Availability availability = availability(faction, formation);
        int population = battle.formationSize(faction.battleSide(), formation.id());
        boolean selectable = availability.available() && population < formation.capacity()
                && battle.factionSize(faction.battleSide()) < faction.maxPlayers();
        String reason = availability.reason();
        if (reason.isBlank() && population >= formation.capacity()) {
            reason = "编制人数已满";
        } else if (reason.isBlank()
                && battle.factionSize(faction.battleSide()) >= faction.maxPlayers()) {
            reason = "阵营人数已满";
        }
        List<String> classes = formation.classes().stream()
                .map(FormationClassRule::classId).toList();
        List<String> squads = formation.squads().stream()
                .map(squad -> squad.displayName() + " (" + squad.capacity() + ")").toList();
        List<String> vehicles = vehicleSummaries(formation);
        List<String> capabilities = capabilitySummaries(formation);
        return new FormationSelectionView(formation.id(), formation.displayName(),
                formation.description(), formation.icon(), formation.category().id(),
                formation.category().displayName(), population, formation.capacity(), selectable,
                reason, classes, squads, vehicles, capabilities);
    }

    private static List<String> vehicleSummaries(FormationDefinition formation) {
        LinkedHashMap<VehicleSummaryKey, Integer> counts = new LinkedHashMap<>();
        for (FormationVehicleDefinition vehicle : formation.vehicles()) {
            VehicleSummaryKey key = new VehicleSummaryKey(vehicle.displayName(),
                    vehicle.entityId(), vehicle.replenishmentCooldownSeconds());
            counts.merge(key, 1, Integer::sum);
        }
        List<String> summaries = new ArrayList<>(counts.size());
        counts.forEach((vehicle, count) -> {
            String quantity = count > 1 ? " ×" + count : "";
            String replenishment = vehicle.cooldownSeconds() < 0 ? "不可再生"
                    : vehicle.cooldownSeconds() % 60 == 0
                    ? vehicle.cooldownSeconds() / 60 + "分钟"
                    : vehicle.cooldownSeconds() + "秒";
            summaries.add(vehicle.displayName() + quantity + "（" + replenishment + "）");
        });
        return List.copyOf(summaries);
    }

    private static List<String> capabilitySummaries(FormationDefinition formation) {
        FormationCapabilityProfile capabilities = formation.capabilities();
        List<String> summaries = new ArrayList<>();
        if (capabilities.outpost().enabled()) {
            summaries.add("兵站上限 " + capabilities.outpost().maxActive());
        } else {
            summaries.add("无兵站");
        }
        if (capabilities.rally().enabled()) {
            summaries.add("队包上限/小队 " + capabilities.rally().maxActive());
        }
        if (!capabilities.respawn().inheritsGlobalDelay()) {
            summaries.add("重生等待 " + capabilities.respawn().delaySeconds() + " 秒");
        }
        if (!capabilities.respawn().mobileSpawnVehicleIds().isEmpty()) {
            summaries.add("移动重生载具 "
                    + String.join("、", capabilities.respawn().mobileSpawnVehicleIds()));
        }
        summaries.add(switch (capabilities.support().mode()) {
            case ALL -> "支援：全部已注册项目";
            case NONE -> "支援：无";
            case ALLOW_LIST -> "支援：白名单 "
                    + String.join("、", capabilities.support().allowList());
        });
        return List.copyOf(summaries);
    }

    private static Availability availability(FactionDefinition faction,
                                              FormationDefinition formation) {
        if (!faction.enabled()) {
            return Availability.unavailable("阵营已停用");
        }
        if (!formation.enabled()) {
            return Availability.unavailable("编制已停用或配置无效");
        }
        FormationClassRule defaultClass = formation.defaultClass().orElse(null);
        if (defaultClass == null) {
            return Availability.unavailable("编制必须至少包含一个职业");
        }
        for (FormationSquadDefinition squad : formation.squads()) {
            int defaultLimit = squad.classLimit(defaultClass.classId(),
                    defaultClass.squadLimit());
            if (defaultLimit < squad.capacity()) {
                return Availability.unavailable(
                        "默认职业 " + defaultClass.classId()
                                + " 的限额必须覆盖小队容量：" + squad.callsign());
            }
        }
        for (FormationVehicleDefinition vehicle : formation.vehicles()) {
            ResourceLocation entityId = ResourceLocation.tryParse(vehicle.entityId());
            if (!SuperbWarfareVehicleGate.supportsEntity(entityId)) {
                return Availability.unavailable("载具必须来自已支持的卓越前线生态 MOD");
            }
            if (!ModList.get().isLoaded(VEHICLE_MOD_ID)) {
                return Availability.unavailable("缺少卓越前线 MOD");
            }
            if (!VEHICLE_MOD_ID.equals(entityId.getNamespace())
                    && !ModList.get().isLoaded(entityId.getNamespace())) {
                return Availability.unavailable("缺少载具扩展 MOD："
                        + entityId.getNamespace());
            }
            if (!ForgeRegistries.ENTITY_TYPES.containsKey(entityId)) {
                return Availability.unavailable("卓越前线生态未注册载具：" + entityId);
            }
        }
        return Availability.AVAILABLE;
    }

    private static Map<String, Integer> orderedClassLimits(FormationDefinition formation,
                                                            FormationSquadDefinition squad) {
        LinkedHashMap<String, Integer> limits = new LinkedHashMap<>();
        for (FormationClassRule rule : formation.classes()) {
            limits.put(rule.classId(), Math.min(squad.capacity(),
                    squad.classLimit(rule.classId(), rule.squadLimit())));
        }
        return Collections.unmodifiableMap(limits);
    }

    private ActionResult reconcileVehicles(FormationConfigData active) {
        DeploymentService deployment = DeploymentService.get(server).orElse(null);
        if (deployment == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "部署服务尚未就绪，无法调和载具台账");
        }
        UUID sessionId = deployment.sessionId();
        Map<VehicleAllocationKey, ResourceLocation> expected =
                expectedVehicleAllocations(active, sessionId);
        ActionResult allocationResult = vehicleProvider.reconcileActiveSession(server,
                sessionId, expected);
        if (!allocationResult.success()) {
            return allocationResult;
        }
        return replenishmentData == null
                ? ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                "载具补充台账尚未就绪")
                : replenishmentData.reconcile(sessionId, expected);
    }

    private static Map<VehicleAllocationKey, ResourceLocation> expectedVehicleAllocations(
            FormationConfigData active, UUID sessionId) {
        LinkedHashMap<VehicleAllocationKey, ResourceLocation> expected = new LinkedHashMap<>();
        if (active == null || sessionId == null) {
            return Map.of();
        }
        for (FactionDefinition faction : active.factions()) {
            if (!faction.enabled()) {
                continue;
            }
            for (FormationDefinition formation : faction.formations()) {
                if (!formation.enabled()) {
                    continue;
                }
                for (FormationVehicleDefinition vehicle : formation.vehicles()) {
                    ResourceLocation entityId = ResourceLocation.tryParse(vehicle.entityId());
                    if (!SuperbWarfareVehicleGate.supportsEntity(entityId)) {
                        continue;
                    }
                    expected.put(new VehicleAllocationKey(sessionId, faction.id(),
                            formation.id(), vehicle.id()), entityId);
                }
            }
        }
        return Map.copyOf(expected);
    }

    private static boolean currentVehicleDefinitionMatches(FormationConfigData active,
                                                             VehicleOwnership ownership) {
        FactionDefinition faction = active.findFaction(ownership.factionId()).orElse(null);
        FormationDefinition formation = faction == null ? null
                : faction.findFormation(ownership.formationId()).orElse(null);
        if (faction == null || formation == null || !faction.enabled() || !formation.enabled()) {
            return false;
        }
        FormationVehicleDefinition vehicle = formation.findVehicle(ownership.allocationId())
                .orElse(null);
        ResourceLocation configuredType = vehicle == null ? null
                : ResourceLocation.tryParse(vehicle.entityId());
        return configuredType != null && configuredType.equals(ownership.entityTypeId());
    }

    private static boolean validSelection(FormationConfigData active, Faction battleSide,
                                          String formationId) {
        if (active == null || battleSide == null || formationId == null
                || formationId.isBlank()) {
            return false;
        }
        FactionDefinition faction = active.findFaction(battleSide).orElse(null);
        FormationDefinition formation = faction == null ? null
                : faction.findFormation(formationId).orElse(null);
        return faction != null && formation != null
                && availability(faction, formation).available();
    }

    private static boolean validRosterSelection(FormationConfigData active, Faction battleSide,
                                                String formationId) {
        FactionDefinition faction = active == null || battleSide == null ? null
                : active.findFaction(battleSide).orElse(null);
        return faction != null && faction.enabled()
                && (formationId == null || formationId.isBlank()
                || validSelection(active, battleSide, formationId));
    }

    private static ResolvedSelection resolveSelection(FormationConfigData active,
                                                       String publicFactionId,
                                                       String formationId) {
        FactionDefinition faction = active == null ? null
                : active.findFaction(publicFactionId).orElse(null);
        FormationDefinition formation = faction == null ? null
                : faction.findFormation(formationId).orElse(null);
        if (faction == null || formation == null) {
            return ResolvedSelection.failure(ActionResult.failure(
                    ActionResult.Code.FORMATION_NOT_FOUND, "阵营或编制不存在"));
        }
        Availability availability = availability(faction, formation);
        if (!availability.available()) {
            return ResolvedSelection.failure(ActionResult.failure(
                    ActionResult.Code.FORMATION_UNAVAILABLE, availability.reason()));
        }
        return new ResolvedSelection(faction, formation,
                ActionResult.ok("阵营与编制配置有效"));
    }

    private static ActionResult voteResult(FormationVoteResult result) {
        if (result.success()) {
            return ActionResult.ok(result.message());
        }
        ActionResult.Code code = switch (result.code()) {
            case INVALID_FACTION, INVALID_CANDIDATES, CANDIDATE_NOT_FOUND ->
                    ActionResult.Code.FORMATION_NOT_FOUND;
            case BALLOT_NOT_OPEN, RESULT_ALREADY_LOCKED ->
                    ActionResult.Code.FORMATION_LOCKED;
            case VOTE_ALREADY_CAST -> ActionResult.Code.NOT_AUTHORIZED;
            case OK -> ActionResult.Code.INVALID_TARGET;
        };
        return ActionResult.failure(code, result.message());
    }

    private record Availability(boolean available, String reason) {
        private static final Availability AVAILABLE = new Availability(true, "");

        private Availability {
            reason = Objects.requireNonNullElse(reason, "");
        }

        private static Availability unavailable(String reason) {
            return new Availability(false, reason);
        }
    }

    private record ResolvedSelection(FactionDefinition faction,
                                     FormationDefinition formation,
                                     ActionResult result) {
        private ResolvedSelection {
            Objects.requireNonNull(result, "result");
        }

        private static ResolvedSelection failure(ActionResult result) {
            return new ResolvedSelection(null, null, result);
        }
    }

    private record VehicleSummaryKey(String displayName, String entityId,
                                     int cooldownSeconds) {
    }
}
