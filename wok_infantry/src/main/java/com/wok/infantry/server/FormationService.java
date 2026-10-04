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
import com.wok.infantry.formation.FormationSupportPolicy;
import com.wok.infantry.formation.FormationVehicleDefinition;
import com.wok.infantry.formation.vote.FormationVotePhase;
import com.wok.infantry.formation.vote.FormationVotePolicy;
import com.wok.infantry.formation.vote.FormationVoteResult;
import com.wok.infantry.formation.vote.FormationVoteSavedData;
import com.wok.infantry.formation.vote.FormationVoteSnapshot;
import com.wok.infantry.formation.selection.FactionSelectionView;
import com.wok.infantry.formation.selection.FormationDetailView;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.selection.FormationSelectionView;
import com.wok.infantry.formation.selection.FormationSupportLabel;
import com.wok.infantry.network.formation.FormationSelectionCodec;
import com.wok.infantry.support.SupportDefinition;
import com.wok.infantry.support.SupportService;
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
import java.util.Set;
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
        ActionResult opened = voteResult(voteData.open(faction.battleSide(), candidates,
                allowVoteChange));
        return opened.success()
                ? ActionResult.ok(faction.displayName() + "编制投票已开启") : opened;
    }

    /**
     * Player's faction choice. While the faction still votes only the faction slot is reserved;
     * once its shared formation is locked the player joins straight into the locked formation
     * with its default class (vote-01), bounded by the locked formation's capacity.
     */
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
        Faction side = faction.battleSide();
        FormationVoteSnapshot vote = voteData == null ? null : voteData.snapshot(side, null);
        ActionResult joined;
        String successMessage;
        if (vote != null && vote.phase() == FormationVotePhase.LOCKED) {
            FormationDefinition locked = faction.findFormation(vote.lockedFormationId())
                    .orElse(null);
            if (locked == null || !availability(faction, locked).available()) {
                return ActionResult.failure(ActionResult.Code.FORMATION_UNAVAILABLE,
                        faction.displayName() + "本局锁定的编制当前不可用，请联系管理员");
            }
            joined = battle.joinFactionWithSharedFormation(player, side, faction.maxPlayers(),
                    locked.id(), locked.capacity());
            if (!joined.success() && joined.code() == ActionResult.Code.FACTION_FULL) {
                int capacity = FormationVotePolicy.joinCapacity(faction.maxPlayers(),
                        FormationVotePhase.LOCKED, locked.capacity());
                return ActionResult.failure(ActionResult.Code.FACTION_FULL,
                        faction.displayName() + "本局已锁定编制“" + locked.displayName()
                                + "”，最多 " + capacity + " 人");
            }
            successMessage = "已加入" + faction.displayName() + "，本局编制："
                    + locked.displayName();
        } else {
            joined = battle.selectFaction(player, side, faction.maxPlayers());
            successMessage = "已加入" + faction.displayName() + "，等待编制投票";
        }
        if (!joined.success()) {
            return joined;
        }
        DeploymentService.get(server).ifPresent(deployment ->
                deployment.onPlayerConnected(player));
        return ActionResult.ok(successMessage);
    }

    /**
     * Gives a faction member without a formation the faction's locked formation (vote-01): a
     * player who joined, was released or was reconciled before the lock and comes back after it
     * goes straight to deployment. Called on login before participation is decided.
     *
     * @return whether the locked formation was applied
     */
    public boolean inheritLockedFormation(ServerPlayer player) {
        if (player == null || player.server != server || voteData == null) {
            return false;
        }
        BattleService battle = BattleService.get(server).orElse(null);
        PlayerRecord record = battle == null ? null
                : battle.playerRecord(player.getUUID()).orElse(null);
        if (record == null || record.faction() == null) {
            return false;
        }
        FactionDefinition faction = catalog.findFaction(record.faction()).orElse(null);
        if (faction == null || !faction.enabled()) {
            return false;
        }
        FormationVoteSnapshot vote = voteData.snapshot(record.faction(), null);
        FormationDefinition locked = vote.phase() == FormationVotePhase.LOCKED
                ? faction.findFormation(vote.lockedFormationId()).orElse(null) : null;
        boolean available = locked != null && availability(faction, locked).available();
        if (!FormationVotePolicy.inheritsLockedFormation(true,
                !record.formationId().isBlank(), vote.phase(), available)) {
            return false;
        }
        ActionResult result = battle.joinFactionWithSharedFormation(player, record.faction(),
                faction.maxPlayers(), locked.id(), locked.capacity());
        if (!result.success()) {
            WokInfantryMod.LOGGER.warn("Could not give {} the locked formation {}/{}: {}",
                    player.getGameProfile().getName(), faction.id(), locked.id(),
                    result.message());
            return false;
        }
        return true;
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
        // A full faction does not stop its own members from voting (vote-08); a formation that
        // cannot hold the whole faction is refused before it could win (vote-11).
        int members = battle.factionSize(record.faction());
        if (FormationVotePolicy.capacityShortfall(formation.capacity(), members)) {
            return ActionResult.failure(ActionResult.Code.FORMATION_FULL,
                    "“" + formation.displayName() + "”最多容纳 " + formation.capacity()
                            + " 人，" + faction.displayName() + "已有 " + members
                            + " 人，不能作为共享编制");
        }
        if (voteData == null) {
            return ActionResult.failure(ActionResult.Code.FORMATION_UNAVAILABLE,
                    "编制投票存档尚未就绪");
        }
        ActionResult cast = voteResult(voteData.cast(record.faction(), player.getUUID(),
                formation.id()));
        return cast.success()
                ? ActionResult.ok(cast.message() + "：" + formation.displayName()) : cast;
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
        FactionDefinition target = catalog.findFaction(publicFactionId).orElse(null);
        FormationVoteSnapshot targetVote = target == null || voteData == null ? null
                : voteData.snapshot(target.battleSide(), null);
        if (targetVote != null && targetVote.phase() == FormationVotePhase.LOCKED
                && !targetVote.lockedFormationId().equals(formationId)) {
            // Checked before joining, so a refused request never changes the faction.
            return ActionResult.failure(ActionResult.Code.FORMATION_LOCKED,
                    "阵营共享编制已经锁定，不能提交其他编制");
        }
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
        FormationVoteSnapshot vote = voteData == null ? null
                : voteData.snapshot(resolved.faction().battleSide(), null);
        if (vote != null && vote.phase() == FormationVotePhase.LOCKED
                && !vote.lockedFormationId().equals(resolved.formation().id())) {
            return ActionResult.failure(ActionResult.Code.FORMATION_LOCKED,
                    lockedOnlyMessage(resolved.faction(), vote.lockedFormationId()));
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

    /**
     * Administrator assignment of {@code /battle admin formation assign} (vote-02): after the
     * faction's lock only the locked formation can be assigned; before it only the faction is
     * assigned and the formation follows the lock, so nobody is placed into a formation the
     * faction did not vote for. {@code formationId} may be blank ("whatever is locked").
     */
    public ActionResult adminAssign(ServerPlayer administrator, ServerPlayer target,
                                    String publicFactionId, String formationId) {
        if (administrator == null || target == null || administrator.server != server
                || target.server != server) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                    "操作者或目标玩家不属于当前服务器");
        }
        FactionDefinition faction = catalog.findFaction(publicFactionId).orElse(null);
        if (faction == null || !faction.enabled()) {
            return ActionResult.failure(ActionResult.Code.FORMATION_NOT_FOUND,
                    "阵营不存在或已停用");
        }
        boolean formationGiven = formationId != null && !formationId.isBlank();
        if (formationGiven && faction.findFormation(formationId).isEmpty()) {
            return ActionResult.failure(ActionResult.Code.FORMATION_NOT_FOUND,
                    "阵营或编制不存在");
        }
        FormationVoteSnapshot vote = voteData == null ? null
                : voteData.snapshot(faction.battleSide(), null);
        FormationVotePhase phase = vote == null ? FormationVotePhase.NOT_STARTED : vote.phase();
        String lockedId = vote == null ? "" : vote.lockedFormationId();
        return switch (FormationVotePolicy.adminAssign(phase, lockedId,
                formationGiven ? faction.findFormation(formationId).orElseThrow().id() : null)) {
            case REJECT_NOT_LOCKED_FORMATION -> ActionResult.failure(
                    ActionResult.Code.FORMATION_LOCKED, lockedOnlyMessage(faction, lockedId));
            case ASSIGN_LOCKED -> forceAssign(administrator, target, faction.id(), lockedId);
            case FACTION_ONLY -> {
                BattleService battle = BattleService.get(server).orElse(null);
                if (battle == null) {
                    yield ActionResult.failure(ActionResult.Code.FORMATION_UNAVAILABLE,
                            "战局服务尚未就绪");
                }
                ActionResult result = battle.forceAssignFactionPending(administrator,
                        target.getUUID(), faction.battleSide(), faction.maxPlayers());
                yield result.success()
                        ? ActionResult.ok("已将 " + target.getGameProfile().getName() + " 分配到"
                        + faction.displayName() + "；本阵营编制尚未锁定，锁定后统一下发")
                        : result;
            }
        };
    }

    /**
     * {@code /battle admin assign <player> <blue|red>} (vote-03): the side's public faction under
     * the same rule as {@link #adminAssign}, instead of a hard-coded {@code default} formation.
     */
    public ActionResult adminAssignBattleSide(ServerPlayer administrator, ServerPlayer target,
                                              Faction side) {
        FactionDefinition faction = side == null ? null : catalog.findFaction(side).orElse(null);
        if (faction == null || !faction.enabled()) {
            return ActionResult.failure(ActionResult.Code.FORMATION_NOT_FOUND,
                    "该战斗方没有启用的公开阵营");
        }
        return adminAssign(administrator, target, faction.id(), null);
    }

    private static String lockedOnlyMessage(FactionDefinition faction, String lockedId) {
        String lockedName = faction.findFormation(lockedId)
                .map(FormationDefinition::displayName).orElse(lockedId);
        return faction.displayName() + "本局已锁定编制“" + lockedName + "”，只能分配到该编制";
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
        if (com.wok.infantry.battle.tickets.TicketService.finished(server)) {
            return VehicleDeploymentResult.failure(ActionResult.failure(
                    ActionResult.Code.INVALID_TARGET, "本局已结束，开始新局后才能部署编制载具"));
        }
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
        if (com.wok.infantry.battle.tickets.TicketService.finished(server)) return;
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
        List<String> registeredSupports = registeredSupportIds();
        List<FactionSelectionView> factions = active.factions().stream()
                .map(faction -> factionView(battle, faction, registeredSupports)).toList();
        boolean required = selectedSide == null || selectedFormation.isBlank()
                || selectedPublicFaction.isBlank();
        // Only current members count, so released or reassigned voters drop out (vote-10).
        Set<UUID> members = selectedSide == null ? Set.of()
                : battle.factionMemberIds(selectedSide);
        FormationVoteSnapshot vote = selectedSide == null || voteData == null ? null
                : voteData.snapshot(selectedSide, player.getUUID(), members::contains);
        return new FormationSelectionSnapshot(activeGeneration, required, selectedPublicFaction,
                selectedFormation,
                vote == null ? FormationVotePhase.NOT_STARTED : vote.phase(),
                vote != null && vote.voteChangeAllowed(),
                vote == null ? "" : vote.ownVote(),
                vote == null ? "" : vote.lockedFormationId(),
                vote == null ? Map.of() : vote.tally(), factions,
                supportLabels(active, registeredSupports));
    }

    /**
     * Public faction view (formation protocol 5): the ballot phase and locked formation go to
     * every viewer, "available" means "a new player may join" (after the lock bounded by the
     * locked formation's capacity), and formation availability is candidate validity only.
     */
    private FactionSelectionView factionView(BattleService battle, FactionDefinition faction,
                                             List<String> registeredSupports) {
        int population = battle.factionSize(faction.battleSide());
        FormationVoteSnapshot vote = voteData == null ? null
                : voteData.snapshot(faction.battleSide(), null);
        FormationVotePhase phase = vote == null ? FormationVotePhase.NOT_STARTED : vote.phase();
        String lockedId = phase == FormationVotePhase.LOCKED ? vote.lockedFormationId() : "";
        FormationDefinition locked = lockedId.isEmpty() ? null
                : faction.findFormation(lockedId).orElse(null);
        if (phase == FormationVotePhase.LOCKED && locked == null) {
            // A lock whose formation left the catalog is cleared on reload; show it as not started.
            phase = FormationVotePhase.NOT_STARTED;
            lockedId = "";
        }
        List<FormationSelectionView> formations = faction.formations().stream()
                .map(formation -> formationView(battle, faction, formation, registeredSupports))
                .toList();
        int joinCapacity = FormationVotePolicy.joinCapacity(faction.maxPlayers(), phase,
                locked == null ? 0 : locked.capacity());
        boolean lockedUsable = locked == null || availability(faction, locked).available();
        boolean available = faction.enabled() && lockedUsable
                && FormationVotePolicy.canJoin(population, joinCapacity)
                && formations.stream().anyMatch(FormationSelectionView::available);
        return new FactionSelectionView(faction.id(), faction.displayName(),
                faction.description(), population, faction.maxPlayers(), available, formations,
                phase, lockedId);
    }

    private FormationSelectionView formationView(BattleService battle, FactionDefinition faction,
                                                  FormationDefinition formation,
                                                  List<String> registeredSupports) {
        // Candidate validity only: a full faction or a formation smaller than the faction is
        // judged by the client against the faction population (vote-08, vote-11).
        Availability availability = availability(faction, formation);
        int population = battle.formationSize(faction.battleSide(), formation.id());
        List<String> classes = formation.classes().stream()
                .map(FormationClassRule::classId).toList();
        List<String> squads = formation.squads().stream()
                .map(squad -> squad.displayName() + " (" + squad.capacity() + ")").toList();
        List<String> vehicles = vehicleSummaries(formation);
        List<String> capabilities = capabilitySummaries(formation);
        return new FormationSelectionView(formation.id(), formation.displayName(),
                formation.description(), formation.icon(), formation.category().id(),
                formation.category().displayName(), population, formation.capacity(),
                availability.available(), availability.reason(), classes, squads, vehicles,
                capabilities, detailView(formation, registeredSupports));
    }

    /**
     * Structured composition with public names (player-09): profession display names instead of
     * class IDs, grouped vehicles with their replenishment, squads, deployables, respawn and
     * support IDs (named by the snapshot's support labels).
     */
    static FormationDetailView detailView(FormationDefinition formation,
                                          List<String> registeredSupports) {
        // Every number is clamped to the codec's wire bounds: an out-of-range value from a hand
        // edited catalog must never make the whole catalog unencodable (as with clipped text).
        List<FormationDetailView.ClassQuota> classes = formation.classes().stream()
                .map(rule -> new FormationDetailView.ClassQuota(
                        rule.displayName() == null || rule.displayName().isBlank()
                                ? rule.classId() : rule.displayName(),
                        Math.min(rule.squadLimit(), FormationSelectionCodec.MAX_DETAIL_COUNT)))
                .limit(FormationSelectionCodec.MAX_DETAIL_ENTRIES)
                .toList();
        LinkedHashMap<VehicleSummaryKey, Integer> counts = new LinkedHashMap<>();
        for (FormationVehicleDefinition vehicle : formation.vehicles()) {
            counts.merge(new VehicleSummaryKey(vehicle.displayName(), vehicle.entityId(),
                    vehicle.replenishmentCooldownSeconds()), 1, Integer::sum);
        }
        List<FormationDetailView.Vehicle> vehicles = new ArrayList<>(counts.size());
        counts.forEach((key, count) -> vehicles.add(new FormationDetailView.Vehicle(
                key.displayName(), Math.min(count, FormationSelectionCodec.MAX_DETAIL_COUNT),
                key.cooldownSeconds() < 0
                ? FormationDetailView.Vehicle.NEVER
                : Math.min(key.cooldownSeconds(), FormationSelectionCodec.MAX_COOLDOWN_SECONDS))));
        List<FormationDetailView.Squad> squads = formation.squads().stream()
                .map(squad -> new FormationDetailView.Squad(squad.displayName(),
                        Math.min(squad.capacity(), FormationSelectionCodec.MAX_DETAIL_COUNT)))
                .limit(FormationSelectionCodec.MAX_DETAIL_ENTRIES)
                .toList();
        FormationCapabilityProfile capabilities = formation.capabilities();
        int outposts = capabilities.outpost().enabled()
                ? Math.min(capabilities.outpost().maxActive(),
                FormationSelectionCodec.MAX_DEPLOYABLES) : 0;
        int rally = capabilities.rally().enabled()
                ? Math.min(capabilities.rally().maxActive(),
                FormationSelectionCodec.MAX_DEPLOYABLES) : 0;
        int respawn = capabilities.respawn().inheritsGlobalDelay()
                ? FormationDetailView.INHERIT_RESPAWN
                : Math.min(capabilities.respawn().delaySeconds(),
                FormationSelectionCodec.MAX_RESPAWN_SECONDS);
        List<String> mobile = capabilities.respawn().mobileSpawnVehicleIds().stream()
                .map(id -> formation.findVehicle(id)
                        .map(FormationVehicleDefinition::displayName).orElse(id))
                .limit(FormationSelectionCodec.MAX_DETAIL_ENTRIES)
                .toList();
        FormationSupportPolicy support = capabilities.support();
        List<String> supportIds = switch (support.mode()) {
            case ALL -> registeredSupports;
            case NONE -> List.of();
            case ALLOW_LIST -> support.allowList();
        };
        return new FormationDetailView(classes, vehicles.stream()
                .limit(FormationSelectionCodec.MAX_DETAIL_ENTRIES).toList(), squads, outposts,
                rally, respawn, mobile, support.mode(), supportIds.stream()
                .limit(FormationSelectionCodec.MAX_DETAIL_ENTRIES).toList());
    }

    private List<String> registeredSupportIds() {
        try {
            return SupportService.get(server).map(service -> service.registeredSupportIds()
                    .stream().map(ResourceLocation::toString).toList()).orElse(List.of());
        } catch (RuntimeException | LinkageError exception) {
            WokInfantryMod.LOGGER.debug("Support registry unavailable for the formation catalog",
                    exception);
            return List.of();
        }
    }

    /**
     * Names of every support the catalog's details mention: registered definitions first, then
     * unknown allow-list IDs (named by their ID), at most the codec's table size.
     */
    private List<FormationSupportLabel> supportLabels(FormationConfigData active,
                                                      List<String> registeredSupports) {
        LinkedHashMap<String, FormationSupportLabel> labels = new LinkedHashMap<>();
        SupportService supports;
        try {
            supports = SupportService.get(server).orElse(null);
        } catch (RuntimeException | LinkageError exception) {
            supports = null;
        }
        for (String id : registeredSupports) {
            if (labels.size() >= FormationSelectionCodec.MAX_SUPPORT_LABELS) {
                break;
            }
            ResourceLocation key = ResourceLocation.tryParse(id);
            SupportDefinition definition = supports == null || key == null ? null
                    : supports.definition(key).orElse(null);
            labels.put(id, definition == null ? new FormationSupportLabel(id, "", id)
                    : new FormationSupportLabel(id, definition.translationKey(),
                    definition.fallbackName()));
        }
        for (FactionDefinition faction : active.factions()) {
            for (FormationDefinition formation : faction.formations()) {
                for (String id : formation.capabilities().support().allowList()) {
                    if (labels.size() >= FormationSelectionCodec.MAX_SUPPORT_LABELS) {
                        return List.copyOf(labels.values());
                    }
                    labels.putIfAbsent(id, new FormationSupportLabel(id, "", id));
                }
            }
        }
        return List.copyOf(labels.values());
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

    static List<String> capabilitySummaries(FormationDefinition formation) {
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
            addIdSummaries(summaries, "移动重生载具 ",
                    capabilities.respawn().mobileSpawnVehicleIds());
        }
        switch (capabilities.support().mode()) {
            case ALL -> summaries.add("支援：全部已注册项目");
            case NONE -> summaries.add("支援：无");
            case ALLOW_LIST -> addIdSummaries(summaries, "支援：白名单 ",
                    capabilities.support().allowList());
        }
        return List.copyOf(summaries);
    }

    /**
     * One summary per id: a joined list can exceed the catalog's per-summary wire limit and
     * fail the login snapshot. The client joins entries with "、", so the text is unchanged.
     */
    private static void addIdSummaries(List<String> summaries, String prefix, List<String> ids) {
        for (int index = 0; index < ids.size(); index++) {
            summaries.add(index == 0 ? prefix + ids.get(index) : ids.get(index));
        }
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
