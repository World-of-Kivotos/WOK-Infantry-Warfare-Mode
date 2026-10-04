package com.wok.infantry.testmode;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.PlayerRecord;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.deployment.DeploymentPoint;
import com.wok.infantry.deployment.DeploymentService;
import com.wok.infantry.deployment.MainBaseProvision;
import com.wok.infantry.formation.FactionDefinition;
import com.wok.infantry.formation.FormationConfigData;
import com.wok.infantry.formation.FormationDefinition;
import com.wok.infantry.formation.FormationSquadDefinition;
import com.wok.infantry.formation.vote.FormationVotePhase;
import com.wok.infantry.formation.vote.FormationVoteSnapshot;
import com.wok.infantry.network.battle.BattleNetwork;
import com.wok.infantry.network.battle.BattleNetworkLimits;
import com.wok.infantry.network.battle.BattleOpenTarget;
import com.wok.infantry.network.battle.packet.s2c.BattleActionFeedbackPacket;
import com.wok.infantry.network.formation.FormationNetwork;
import com.wok.infantry.network.formation.FormationSeatState;
import com.wok.infantry.server.FormationService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server-wide test mode of WOK步战核心 (0.4.0-beta.2), for testing a battle alone or with two
 * players on a world without prepared bases.
 *
 * <p>While it is on: a side without a main base gets one near the overworld spawn, the
 * deployment countdown and the respawn wait are zero, and every player sees a boss bar saying
 * so. Manpower, loadouts, stamina, body health and the downed state keep their normal rules and
 * deployment is always survival (unlike the per-player vehicle test mode, which this one leaves
 * alone). The switch is saved with the world ({@link TestModeSavedData}) and survives restarts;
 * the bases it added stay after it is turned off.
 *
 * <p>{@link #testStart} is the one-click start of {@code /battle admin test start}: test mode on,
 * faction, locked formation, squad, main bases and the deployment itself, each step reported.
 *
 * <p>Every method runs on the server thread.
 */
public final class TestModeService {
    static final String BOSS_BAR_KEY = "message.wok_infantry.test_mode.bossbar";
    private static final String ENABLED_KEY = "message.wok_infantry.test_mode.enabled";
    private static final String DISABLED_KEY = "message.wok_infantry.test_mode.disabled";
    private static final String BASE_CREATED_KEY = "message.wok_infantry.test_mode.base_created";
    private static final String BASE_FAILED_KEY = "message.wok_infantry.test_mode.base_failed";
    private static final String REMINDER_KEY = "message.wok_infantry.test_mode.reminder";
    /** Name recorded for a switch made from the server console or a command block. */
    public static final String CONSOLE_NAME = "控制台";

    private static final Map<MinecraftServer, TestModeService> INSTANCES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final MinecraftServer server;
    private final TestModeSavedData data;
    private ServerBossEvent bossBar;

    private TestModeService(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
        this.data = TestModeSavedData.get(server);
    }

    /** Restores a saved test mode after a restart: boss bar for the players, missing bases. */
    public static void start(MinecraftServer server) {
        get(server).ifPresent(service -> {
            if (service.enabled()) {
                WokInfantryMod.LOGGER.warn("WOK Infantry test mode is ON (saved with the world, "
                        + "switched on by {}); turn it off with /battle admin test mode off",
                        service.data.changedBy());
                for (String line : service.ensureMainBases(false)) {
                    WokInfantryMod.LOGGER.info("Test mode: {}", line);
                }
            }
            service.syncBossBar();
        });
    }

    public static void stop(MinecraftServer server) {
        if (server == null) {
            return;
        }
        TestModeService service;
        synchronized (INSTANCES) {
            service = INSTANCES.remove(server);
        }
        if (service != null) {
            service.hideBossBar();
        }
    }

    public static Optional<TestModeService> get(MinecraftServer server) {
        if (server == null || server.overworld() == null) {
            return Optional.empty();
        }
        synchronized (INSTANCES) {
            return Optional.of(INSTANCES.computeIfAbsent(server, TestModeService::new));
        }
    }

    /** Whether the server-wide test mode is on (false while the server is not running). */
    public static boolean isEnabled(MinecraftServer server) {
        return get(server).map(TestModeService::enabled).orElse(false);
    }

    public boolean enabled() {
        return data.enabled();
    }

    /** Name of whoever switched the test mode last. */
    public String changedBy() {
        return data.changedBy();
    }

    // ---- switch -------------------------------------------------------------------------------

    /**
     * Lines of a switch or a test start, plus its outcome.
     *
     * @param failureLine index of the line that reports the failure (shown in red), -1 when
     *                    the action succeeded
     */
    public record Report(ActionResult result, List<String> lines, int failureLine) {
        public Report {
            Objects.requireNonNull(result, "result");
            lines = List.copyOf(lines);
        }

        static Report success(ActionResult result, List<String> lines) {
            return new Report(result, lines, -1);
        }

        public boolean success() {
            return result.success();
        }
    }

    /**
     * {@code /battle admin test mode on|off}. Turning it on is announced to everyone, ends the
     * running countdowns and adds the missing main bases (also when it was already on); turning
     * it off is announced, removes the boss bar and keeps the bases.
     *
     * @param actor name of the administrator ({@link #CONSOLE_NAME} for the console)
     */
    public Report setEnabled(boolean requested, String actor) {
        String by = actor == null || actor.isBlank() ? CONSOLE_NAME : actor;
        TestModeRules.Transition transition = TestModeRules.transition(data.enabled(), requested);
        List<String> lines = new ArrayList<>();
        if (transition.changed()) {
            data.set(requested, System.currentTimeMillis(), by);
        }
        if (transition.enabledAfter()) {
            if (transition.changed()) {
                lines.add("已开启全服测试模式：部署倒计时与重生等待为 0，兵力、配装、体力、部位血量、"
                        + "倒地等其余规则照常，部署一律为生存模式；重启后保留");
                broadcast(Component.translatable(ENABLED_KEY, by));
                int skipped = DeploymentService.get(server)
                        .map(DeploymentService::skipWaitingCountdowns).orElse(0);
                if (skipped > 0) {
                    lines.add("已结束 " + skipped + " 名等待玩家的部署倒计时");
                }
            } else {
                lines.add("全服测试模式本来就是开启的（" + data.changedBy() + " 开启），未改动");
            }
            lines.addAll(ensureMainBases(true));
        } else if (transition.changed()) {
            lines.add("已关闭全服测试模式：部署倒计时与重生等待恢复正常；"
                    + "测试模式补出的主基地保留，需要时用 /battle deployment clearbase <blue|red> 清除");
            broadcast(Component.translatable(DISABLED_KEY, by));
        } else {
            lines.add("全服测试模式本来就是关闭的，未改动");
        }
        syncBossBar();
        WokInfantryMod.LOGGER.info("Test mode {} by {}: {}", transition, by, lines);
        return Report.success(ActionResult.ok(lines.get(0)), lines);
    }

    /** {@code /battle admin test mode status}. */
    public List<String> status() {
        List<String> lines = new ArrayList<>();
        String when = data.changedAtMillis() <= 0L ? ""
                : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(data.changedAtMillis()));
        if (data.enabled()) {
            lines.add("全服测试模式：已开启（" + data.changedBy() + (when.isEmpty() ? "" : " 于 " + when)
                    + " 开启，重启后保留）");
            lines.add("部署倒计时与重生等待：0；部署一律为生存模式；其余规则照常");
        } else if (data.changedBy().isEmpty()) {
            lines.add("全服测试模式：已关闭（从未开启）");
        } else {
            lines.add("全服测试模式：已关闭（上次由 " + data.changedBy()
                    + (when.isEmpty() ? "" : " 于 " + when) + " 关闭）");
        }
        DeploymentService deployment = DeploymentService.get(server).orElse(null);
        for (Faction side : Faction.values()) {
            DeploymentPoint base = deployment == null ? null
                    : deployment.mainBase(side).orElse(null);
            lines.add(sideName(side) + "主基地：" + (base == null ? "未设置"
                    + (data.enabled() ? "" : "（开启测试模式时自动补设）")
                    : base.dimension() + " " + base.position().toShortString()));
        }
        return lines;
    }

    /**
     * Gives every side without a main base one near the overworld spawn (existing bases are
     * never moved). Each created or failed base is announced when {@code announce}.
     *
     * @return one report line per side
     */
    public List<String> ensureMainBases(boolean announce) {
        DeploymentService deployment = DeploymentService.get(server).orElse(null);
        if (deployment == null) {
            return List.of("部署服务尚未就绪，主基地未检查");
        }
        List<String> lines = new ArrayList<>();
        for (Faction side : Faction.values()) {
            MainBaseProvision provision = deployment.provisionMainBase(side);
            String name = sideName(side);
            if (provision.created()) {
                DeploymentPoint point = provision.point();
                lines.add("已为" + name + "补设主基地：" + provision.message());
                if (announce) {
                    broadcast(Component.translatable(BASE_CREATED_KEY, name,
                            point.dimension().toString(), point.position().toShortString()));
                }
            } else if (!provision.present()) {
                lines.add("无法为" + name + "补设主基地：" + provision.message());
                if (announce) {
                    broadcast(Component.translatable(BASE_FAILED_KEY, name, provision.message()));
                }
            } else {
                lines.add(name + "主基地已就绪：" + provision.message().replace("已有主基地 ", ""));
            }
        }
        return lines;
    }

    // ---- boss bar -----------------------------------------------------------------------------

    /** Whether the test-mode boss bar is currently shown. */
    public boolean bossBarShown() {
        return bossBar != null && bossBar.isVisible();
    }

    /** Players the boss bar is shown to. */
    public Set<UUID> bossBarViewers() {
        if (bossBar == null) {
            return Set.of();
        }
        Set<UUID> viewers = new LinkedHashSet<>();
        bossBar.getPlayers().forEach(player -> viewers.add(player.getUUID()));
        return Set.copyOf(viewers);
    }

    /**
     * Makes the boss bar match the switch and the online players: shown to every connected
     * player while the test mode is on (including players who log in later and a respawned
     * player entity), removed once it is off.
     */
    public void syncBossBar() {
        if (!data.enabled()) {
            hideBossBar();
            return;
        }
        if (bossBar == null) {
            bossBar = new ServerBossEvent(Component.translatable(BOSS_BAR_KEY),
                    BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
            bossBar.setProgress(1.0F);
        }
        bossBar.setVisible(true);
        Map<UUID, ServerPlayer> online = new LinkedHashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            online.put(player.getUUID(), player);
        }
        for (ServerPlayer shown : List.copyOf(bossBar.getPlayers())) {
            if (online.get(shown.getUUID()) != shown) {
                // Logged out, or a respawn replaced the player entity.
                bossBar.removePlayer(shown);
            }
        }
        for (ServerPlayer player : online.values()) {
            if (!bossBar.getPlayers().contains(player)) {
                bossBar.addPlayer(player);
            }
        }
    }

    private void hideBossBar() {
        if (bossBar != null) {
            bossBar.removeAllPlayers();
            bossBar.setVisible(false);
            bossBar = null;
        }
    }

    /** A player joined: the boss bar and a reminder while the test mode is on. */
    public void onPlayerLoggedIn(ServerPlayer player) {
        if (player == null || !data.enabled()) {
            return;
        }
        syncBossBar();
        player.sendSystemMessage(Component.translatable(REMINDER_KEY, data.changedBy()));
    }

    public void onPlayerLoggedOut(ServerPlayer player) {
        if (player == null || bossBar == null) {
            return;
        }
        for (ServerPlayer shown : List.copyOf(bossBar.getPlayers())) {
            if (shown.getUUID().equals(player.getUUID())) {
                bossBar.removePlayer(shown);
            }
        }
    }

    // ---- test start ---------------------------------------------------------------------------

    /**
     * {@code /battle admin test start [faction] [formation] [player]}: turns the test mode on and
     * takes {@code target} (the administrator when {@code null}) from the waiting space straight
     * to its side's main base. Every step is reported; a failing step stops there and says why.
     * A refused deployment (an incomplete loadout, for example) leaves the player on the
     * deployment page with the faction, formation and squad already set, which is a normal
     * waiting state, never a half one.
     */
    public Report testStart(ServerPlayer administrator, ServerPlayer target, String factionArg,
                            String formationArg) {
        List<String> lines = new ArrayList<>();
        if (administrator == null || administrator.server != server
                || !administrator.hasPermissions(BattleRules.ADMIN_PERMISSION_LEVEL)) {
            return fail(lines, ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                    "测试开局需要服务端管理员权限"), null, null);
        }
        ServerPlayer player = target == null ? administrator : target;
        if (player.server != server) {
            return fail(lines, ActionResult.failure(ActionResult.Code.TARGET_NOT_FOUND,
                    "目标玩家不在当前服务器"), administrator, null);
        }
        String who = player == administrator ? "你" : player.getGameProfile().getName() + " ";
        FormationService formations = FormationService.get(server).orElse(null);
        BattleService battle = BattleService.get(server).orElse(null);
        DeploymentService deployment = DeploymentService.get(server).orElse(null);
        if (formations == null || battle == null || deployment == null) {
            return fail(lines, ActionResult.failure(ActionResult.Code.FORMATION_UNAVAILABLE,
                    "战局、阵营编制或部署服务尚未就绪"), administrator, null);
        }
        if (deployment.isVehicleTestMode(player.getUUID())) {
            return fail(lines, ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    who + "处于载具测试模式，请先执行 /battle admin test vehicle off"
                            + (player == administrator ? "" : " " + player.getGameProfile()
                            .getName())), administrator, null);
        }
        FormationSeatState before = FormationSeatState.of(player);

        // 1. Test mode on (announces it, ends countdowns, adds missing main bases).
        lines.addAll(setEnabled(true, administrator.getGameProfile().getName()).lines());

        // 2. A battle record for the player (a removed player is re-admitted by the assignment).
        ActionResult ensured = battle.ensurePlayer(player);
        if (!ensured.success() && ensured.code() != ActionResult.Code.NOT_ASSIGNED) {
            return fail(lines, ensured, administrator, null);
        }

        // 3. Faction: argument, else the current one, else the first public faction.
        FormationConfigData catalog = formations.catalog();
        List<String> enabledFactions = catalog.factions().stream()
                .filter(FactionDefinition::enabled).map(FactionDefinition::id).toList();
        PlayerRecord record = battle.playerRecord(player.getUUID()).orElse(null);
        Faction previousSide = record == null ? null : record.faction();
        String current = previousSide == null ? "" : catalog.findFaction(previousSide)
                .filter(FactionDefinition::enabled).map(FactionDefinition::id).orElse("");
        TestModeRules.FactionChoice factionChoice = TestModeRules.chooseFaction(factionArg,
                current, enabledFactions);
        if (!factionChoice.ok()) {
            return fail(lines, ActionResult.failure(ActionResult.Code.FORMATION_NOT_FOUND,
                    factionChoice.error()), administrator, null);
        }
        FactionDefinition faction = catalog.findFaction(factionChoice.factionId()).orElse(null);
        if (faction == null) {
            return fail(lines, ActionResult.failure(ActionResult.Code.FORMATION_NOT_FOUND,
                    "阵营不存在或已停用"), administrator, null);
        }
        Faction side = faction.battleSide();
        lines.add("阵营：" + faction.displayName() + "（" + switch (factionChoice.source()) {
            case ARGUMENT -> "按参数";
            case CURRENT -> "当前所在阵营";
            case FIRST_PUBLIC -> "目录里第一个公开阵营";
        } + "）");

        // 4. Formation: argument, else the lock, else default, else the first candidate; an
        //    unlocked faction is locked to it, a locked one keeps its lock.
        FormationVoteSnapshot vote = formations.voteSnapshot(side, null);
        String lockedId = vote.phase() == FormationVotePhase.LOCKED ? vote.lockedFormationId() : "";
        List<String> usable = faction.formations().stream().map(FormationDefinition::id)
                .filter(id -> formations.validateSelection(faction.id(), id).success()).toList();
        TestModeRules.FormationChoice formationChoice = TestModeRules.chooseFormation(
                formationArg, lockedId, usable);
        if (!formationChoice.ok()) {
            return fail(lines, ActionResult.failure(ActionResult.Code.FORMATION_NOT_FOUND,
                    faction.displayName() + "：" + formationChoice.error()), administrator, null);
        }
        boolean lockedNow = false;
        if (formationChoice.needsLock()) {
            String chosen = formationChoice.chosenId();
            if (vote.phase() != FormationVotePhase.OPEN || !vote.candidates().contains(chosen)) {
                boolean reopen = vote.phase() == FormationVotePhase.OPEN;
                ActionResult opened = formations.openVote(administrator, faction.id(), true);
                if (!opened.success()) {
                    return fail(lines, opened, administrator, null);
                }
                lines.add(reopen ? "已重开" + faction.displayName() + "编制投票以刷新候选"
                        : "已开启" + faction.displayName() + "编制投票");
            }
            ActionResult locked = formations.lockVote(administrator, faction.id(), chosen);
            if (!locked.success()) {
                return fail(lines, ActionResult.failure(locked.code(),
                        "锁定编制失败：" + locked.message()), administrator, null);
            }
            lockedNow = true;
            lines.add("已锁定" + faction.displayName() + "编制：" + formationName(faction, chosen)
                    + "（" + switch (formationChoice.source()) {
                case ARGUMENT -> "按参数";
                case DEFAULT -> "default 编制";
                case FIRST_CANDIDATE -> "第一个候选编制";
                case LOCKED -> "已锁定";
            } + "）");
        } else {
            lines.add(faction.displayName() + "本局已锁定编制“"
                    + formationName(faction, formationChoice.effectiveId()) + "”，未改动锁定结果"
                    + (formationChoice.lockKept() ? "（没有改用“"
                    + formationName(faction, formationChoice.chosenId()) + "”）" : ""));
        }
        String formationId = formationChoice.effectiveId();
        String formationName = formationName(faction, formationId);

        // 5. Seat: the faction and its locked formation (switching faction only here).
        record = battle.playerRecord(player.getUUID()).orElse(null);
        boolean seated = ensured.success() && record != null && record.faction() == side
                && formationId.equals(record.formationId());
        if (seated) {
            lines.add(who + "已在" + faction.displayName() + "·" + formationName);
        } else {
            ActionResult assigned = formations.forceAssign(administrator, player, faction.id(),
                    formationId);
            if (!assigned.success()) {
                return fail(lines, ActionResult.failure(assigned.code(),
                        "加入" + faction.displayName() + "失败：" + assigned.message()),
                        administrator, null);
            }
            String switched = previousSide != null && previousSide != side
                    ? "（已从" + catalog.findFaction(previousSide).map(FactionDefinition::displayName)
                    .orElse(sideName(previousSide)) + "换过来，只有测试开局允许本轮换阵营）" : "";
            lines.add(who + "已加入" + faction.displayName() + "，本局编制：" + formationName
                    + switched);
        }

        // 6. Squad: keep the current one, else lead the first free callsign.
        FormationDefinition formation = catalog.findFormation(side, formationId).orElse(null);
        SquadCallsign ownSquad = battle.squadOf(player.getUUID()).orElse(null);
        if (ownSquad != null) {
            lines.add(who + "已在" + squadName(formation, ownSquad) + "小队");
        } else {
            ActionResult squad = joinOrCreateSquad(battle, player, side, formation, who, lines);
            if (!squad.success()) {
                return fail(lines, squad, administrator, player);
            }
        }

        // 7. Already deployed: nothing more to do.
        if (deployment.isActive(player.getUUID())) {
            lines.add(who + "已处于部署状态，没有重复部署");
            return finish(lines, administrator, player, side, lockedNow, before,
                    ActionResult.ok(who + "已处于部署状态"));
        }

        // 8. The side's main base (added in step 1 when it was missing).
        DeploymentPoint base = deployment.mainBase(side).orElse(null);
        if (base == null) {
            return stopAtDeployment(lines, administrator, player, ActionResult.failure(
                    ActionResult.Code.INVALID_DEPLOYMENT_POINT, faction.displayName()
                            + "没有主基地，无法部署；见上方补设主基地的原因"));
        }

        // 9. Deploy from the main base (survival, real kit).
        ActionResult selected = deployment.selectPoint(player, base.id());
        if (!selected.success()) {
            return stopAtDeployment(lines, administrator, player, selected);
        }
        ActionResult deployed = deployment.deploy(player);
        if (!deployed.success()) {
            return stopAtDeployment(lines, administrator, player, deployed);
        }
        lines.add(who + "已从" + faction.displayName() + "主基地部署（生存模式，已发配装）："
                + base.dimension() + " " + player.blockPosition().toShortString());
        if (player != administrator) {
            player.sendSystemMessage(Component.literal("管理员 "
                    + administrator.getGameProfile().getName() + " 为你执行了测试开局，已部署到"
                    + faction.displayName() + "主基地"));
        }
        return finish(lines, administrator, player, side, lockedNow, before,
                ActionResult.ok("测试开局完成：" + faction.displayName() + "·" + formationName));
    }

    private static ActionResult joinOrCreateSquad(BattleService battle, ServerPlayer player,
                                                  Faction side, FormationDefinition formation,
                                                  String who, List<String> lines) {
        if (formation == null) {
            return ActionResult.failure(ActionResult.Code.FORMATION_NOT_FOUND,
                    "编制定义缺失，无法建立小队");
        }
        List<String> configured = formation.squads().stream()
                .map(FormationSquadDefinition::callsign).toList();
        List<String> existing = new ArrayList<>();
        for (String callsign : configured) {
            SquadCallsign parsed = SquadCallsign.byId(callsign).orElse(null);
            if (parsed != null && battle.squadSize(side, formation.id(), parsed) > 0) {
                existing.add(callsign);
            }
        }
        String free = TestModeRules.firstFreeCallsign(configured, existing);
        SquadCallsign target = SquadCallsign.byId(free).orElse(null);
        ActionResult last = ActionResult.failure(ActionResult.Code.SQUAD_NOT_FOUND,
                "当前编制没有可用的小队呼号");
        if (target != null) {
            ActionResult created = battle.createSquad(player, target);
            if (created.success()) {
                lines.add("已创建" + squadName(formation, target) + "小队，" + who + "是队长");
                return created;
            }
            last = created;
        }
        // Every callsign already has a squad (or creating failed): join the first with room.
        for (String callsign : configured) {
            SquadCallsign parsed = SquadCallsign.byId(callsign).orElse(null);
            if (parsed == null) {
                continue;
            }
            ActionResult joined = battle.joinSquad(player, parsed);
            if (joined.success()) {
                lines.add("所有呼号都已有小队，" + who + "已加入" + squadName(formation, parsed)
                        + "小队");
                return joined;
            }
            last = joined;
        }
        return ActionResult.failure(last.code(), "无法建立或加入小队：" + last.message());
    }

    // ---- outcome and network ----------------------------------------------------------------

    /**
     * A step failed before the deployment page applies: the failure is the last line and the
     * administrator's vote page footer shows it. When the player already has a faction and
     * formation ({@code waitingPlayer}), it is sent to the deployment page as well.
     */
    private Report fail(List<String> lines, ActionResult failure, ServerPlayer administrator,
                        ServerPlayer waitingPlayer) {
        lines.add(failure.message());
        if (administrator != null && online(administrator) && FormationNetwork.isInitialized()) {
            safely(() -> FormationNetwork.sendResult(administrator, failure));
        }
        if (waitingPlayer != null) {
            sendDeploymentPage(waitingPlayer, failure);
        }
        return new Report(failure, lines, lines.size() - 1);
    }

    /**
     * The deployment was refused (or could not be tried): the player keeps its faction,
     * formation and squad and waits on the deployment page, whose footer gives the reason;
     * the report says how to continue.
     */
    private Report stopAtDeployment(List<String> lines, ServerPlayer administrator,
                                    ServerPlayer player, ActionResult refused) {
        String hint = switch (refused.code()) {
            case LOADOUT_INCOMPLETE, INVALID_CLASS_ID -> "已停在部署页：请在部署页点“配装”补齐必需槽位"
                    + "（管理员可用 /loadoutadmin 给该编制的兵种配置装备），再点“部署”或重新执行测试开局";
            case INVENTORY_BLOCKED -> "已停在部署页：请先清空提示的快捷栏位置，再点“部署”";
            case NOT_WAITING_FOR_DEPLOYMENT -> "已停在部署页：本局已结束时请先执行 /battle admin reset";
            default -> "已停在部署页：处理后点“部署”，或重新执行 /battle admin test start";
        };
        lines.add("部署未完成：" + refused.message());
        int failureLine = lines.size() - 1;
        lines.add(hint);
        ActionResult failure = ActionResult.failure(refused.code(),
                "部署未完成：" + refused.message());
        if (online(administrator) && FormationNetwork.isInitialized()) {
            safely(() -> FormationNetwork.sendResult(administrator, failure));
        }
        sendDeploymentPage(player, failure);
        if (player != administrator && online(player)) {
            player.sendSystemMessage(Component.literal("管理员为你执行了测试开局，但" + failure.message()
                    + "；" + hint));
        }
        return new Report(failure, lines, failureLine);
    }

    /** Success: refreshes everyone the start changed and answers the administrator's page. */
    private Report finish(List<String> lines, ServerPlayer administrator, ServerPlayer player,
                          Faction side, boolean lockedNow, FormationSeatState before,
                          ActionResult result) {
        publish(player, side, lockedNow, before, BattleOpenTarget.NONE);
        if (online(administrator) && FormationNetwork.isInitialized()) {
            safely(() -> FormationNetwork.sendResult(administrator, result));
        }
        return Report.success(result, lines);
    }

    private void sendDeploymentPage(ServerPlayer player, ActionResult failure) {
        BattleService battle = BattleService.get(server).orElse(null);
        Faction side = battle == null ? null : battle.factionOf(player.getUUID()).orElse(null);
        publish(player, side, false, FormationSeatState.NONE, BattleOpenTarget.DEPLOYMENT);
        if (!online(player) || !BattleNetwork.isInitialized()) {
            return;
        }
        String message = failure.message();
        if (message.length() > BattleNetworkLimits.MAX_ACTION_MESSAGE_LENGTH) {
            message = message.substring(0, BattleNetworkLimits.MAX_ACTION_MESSAGE_LENGTH);
        }
        String footer = message;
        safely(() -> BattleNetwork.sendToPlayer(player,
                new BattleActionFeedbackPacket(false, footer)));
    }

    /**
     * Network side of a test start, for connected players only (a GameTest player has no
     * connection). A lock is published like the administrator lock command (the faction's
     * members get their formation, everyone else a catalog refresh); otherwise a changed seat
     * refreshes the other players' catalogs. The player itself gets its battle snapshot (opening
     * the deployment page when asked) and a catalog without the lock notice, so the client does
     * not open another page on top of the deployment.
     */
    private void publish(ServerPlayer player, Faction side, boolean lockedNow,
                         FormationSeatState before, BattleOpenTarget openTarget) {
        if (!FormationNetwork.isInitialized() || !BattleNetwork.isInitialized()) {
            return;
        }
        BattleService battle = BattleService.get(server).orElse(null);
        FormationService formations = FormationService.get(server).orElse(null);
        if (battle == null || formations == null) {
            return;
        }
        boolean seatChanged = before.seatChanged(FormationSeatState.of(player));
        if (lockedNow || seatChanged) {
            List<ServerPlayer> others = server.getPlayerList().getPlayers().stream()
                    .filter(other -> !other.getUUID().equals(player.getUUID())).toList();
            FormationNetwork.forEachRecipient(others, other -> {
                boolean sameSide = side != null && battle.factionOf(other.getUUID())
                        .filter(side::equals).isPresent();
                if (lockedNow && sameSide
                        && formations.selectedFormation(other.getUUID()).isPresent()) {
                    FormationNetwork.sendFormationApplied(other);
                } else {
                    FormationNetwork.sendSnapshotToPlayer(other, false);
                }
            });
        }
        if (online(player)) {
            safely(() -> BattleNetwork.sendSnapshotToPlayer(battle, player, openTarget));
            safely(() -> FormationNetwork.sendSnapshotToPlayer(player, false));
        }
    }

    private boolean online(ServerPlayer player) {
        return player != null && server.getPlayerList().getPlayer(player.getUUID()) == player;
    }

    private static void safely(Runnable delivery) {
        try {
            delivery.run();
        } catch (RuntimeException exception) {
            WokInfantryMod.LOGGER.error("Test start could not notify a client", exception);
        }
    }

    private void broadcast(Component message) {
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    /** Public faction name of a battle side, with the side ("学院军（蓝方）"). */
    String sideName(Faction side) {
        String plain = side == Faction.RED ? "红方" : "蓝方";
        return FormationService.get(server)
                .flatMap(service -> service.catalog().findFaction(side))
                .map(faction -> faction.displayName() + "（" + plain + "）")
                .orElse(plain);
    }

    private static String formationName(FactionDefinition faction, String formationId) {
        return faction.findFormation(formationId).map(FormationDefinition::displayName)
                .orElse(formationId);
    }

    private static String squadName(FormationDefinition formation, SquadCallsign callsign) {
        return formation == null ? callsign.id() : formation.findSquad(callsign.id())
                .map(FormationSquadDefinition::displayName).orElse(callsign.id());
    }
}
