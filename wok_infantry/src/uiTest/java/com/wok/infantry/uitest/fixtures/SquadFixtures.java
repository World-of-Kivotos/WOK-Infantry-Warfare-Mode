package com.wok.infantry.uitest.fixtures;

import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.ClassLimitView;
import com.wok.infantry.battle.ClassQuotaView;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.FormationContextView;
import com.wok.infantry.battle.MemberState;
import com.wok.infantry.battle.MemberView;
import com.wok.infantry.battle.PermissionView;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.battle.SquadView;
import com.wok.infantry.client.ClientBattleState;
import com.wok.infantry.client.ClientFormationState;
import com.wok.infantry.deployment.DeploymentPhase;
import com.wok.infantry.deployment.DeploymentPoint;
import com.wok.infantry.deployment.DeploymentPointKind;
import com.wok.infantry.deployment.DeploymentView;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.vote.FormationVotePhase;
import com.wok.infantry.support.SupportView;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client-side battle states of the preview's squad terminal ({@code 20-squad.js}) for the UI
 * acceptance: the demo squads of {@link MockData} (Alpha led by the viewer, Bravo with the
 * commander, Charlie full, Delta empty, Echo not in the formation), the class quotas, the
 * deployment points (2 to 16) and the deployment phases, plus the formation ballot before the
 * lock. Only the client caches are replaced — never the server — and they are re-installed at the
 * start and end of every client tick and before every GUI frame, so a heartbeat snapshot of the
 * server never reaches a capture. {@link #stop()} puts the server's values back.
 *
 * <p>The fixture viewer has its own id, so a real deployment of the acceptance player never
 * looks like "the viewer just deployed" (which closes the terminal).
 *
 * <p>Every state can be seen from either side ({@link Scenario#withSide}): the Academy (blue,
 * the preview's viewer) or Caesar (red side, own and enemy faction swapped, the locked formation
 * {@code caesar_234_mechanized}, red deployment points). The terminal then resolves its livery
 * from the fixture itself — the battle side, or before the first battle snapshot the joined
 * catalog faction — so these cases also exercise the live livery rule (no pin).
 */
public final class SquadFixtures {
    /** Id of the fixture viewer (never the real player's). */
    public static final UUID VIEWER = new UUID(0x5155AD00L, 1L);
    private static final ResourceLocation OVERWORLD = new ResourceLocation("minecraft", "overworld");
    private static final long SERVER_TICK = 100_000L;

    /** What the battle cache holds. */
    public enum Stage {
        /** Formation locked: squads, classes and deployment. */
        READY,
        /** Joined, vote not opened yet. */
        VOTE_WAIT,
        /** Joined, vote open. */
        VOTE_OPEN,
        /** No battle snapshot at all (the sync placeholder). */
        LOADING
    }

    /** The viewer's place in the faction. */
    public enum Role {
        /** Leads Alpha. */
        LEADER,
        /** Not in a squad. */
        NONE
    }

    /**
     * One state.
     *
     * @param points          deployment points (main base, beacons, a rally point)
     * @param respawnSeconds  countdown while waiting (0 = ready)
     * @param resupplySeconds resupply cooldown while in combat
     * @param side            the viewer's side: the Academy (blue, the preview's viewer) or
     *                        Caesar (red: own and enemy faction swapped, the formation
     *                        {@code caesar_234_mechanized}, red deployment points), which picks
     *                        the tablet livery
     */
    public record Scenario(Stage stage, Role role, DeploymentPhase phase, int points,
                           int respawnSeconds, int resupplySeconds, MockData.Side side) {
        public Scenario {
            side = side == null ? MockData.Side.ACADEMY : side;
        }

        public static Scenario ready(Role role) {
            return new Scenario(Stage.READY, role, DeploymentPhase.WAITING, 3, 12, 0,
                    MockData.Side.ACADEMY);
        }

        public static Scenario of(Stage stage) {
            return new Scenario(stage, Role.NONE, DeploymentPhase.WAITING, 0, 0, 0,
                    MockData.Side.ACADEMY);
        }

        public Scenario withPhase(DeploymentPhase value, int resupply) {
            return new Scenario(stage, role, value, points, value == DeploymentPhase.ACTIVE ? 0
                    : respawnSeconds, resupply, side);
        }

        public Scenario withPoints(int value) {
            return new Scenario(stage, role, phase, value, respawnSeconds, resupplySeconds, side);
        }

        /** The same state seen from {@code value}'s side. */
        public Scenario withSide(MockData.Side value) {
            return new Scenario(stage, role, phase, points, respawnSeconds, resupplySeconds, value);
        }
    }

    private static Scenario scenario;
    private static boolean saved;
    private static BattleSnapshot lastReal;
    private static FormationSelectionSnapshot savedFormation;
    private static BattleSnapshot fixture;
    private static FormationSelectionSnapshot formationFixture;
    private static boolean listening;

    private SquadFixtures() {
    }

    /** Shows {@code next} from now on; the first call remembers the server's values. */
    public static void start(Scenario next) {
        if (!saved) {
            saved = true;
            lastReal = ClientBattleState.snapshot();
            savedFormation = ClientFormationState.snapshot();
        }
        listen();
        scenario = next;
        fixture = next.stage() == Stage.LOADING ? null : battle(next);
        MockData.Side side = next.side();
        formationFixture = switch (next.stage()) {
            case VOTE_WAIT -> FormationFixtures.Scenario.joined(side,
                    FormationVotePhase.NOT_STARTED).build();
            case VOTE_OPEN -> FormationFixtures.Scenario.joined(side, FormationVotePhase.OPEN)
                    .tally(withoutOwnVote(side)).build();
            default -> FormationFixtures.Scenario.joined(side, FormationVotePhase.LOCKED)
                    .own(side.ownVote()).locked(side.lockedFormation()).build();
        };
        apply();
    }

    /** Puts the server's caches back and stops re-installing the fixture. */
    public static void stop() {
        if (!saved) {
            return;
        }
        scenario = null;
        saved = false;
        ClientFormationState.clear();
        ClientFormationState.update(savedFormation);
        if (lastReal != null) {
            ClientBattleState.update(lastReal);
        }
        lastReal = null;
        fixture = null;
        formationFixture = null;
    }

    /** Whether the caches hold the current fixture. */
    public static boolean applied() {
        if (scenario == null) {
            return false;
        }
        return ClientBattleState.snapshot() == fixture
                && ClientFormationState.snapshot() == formationFixture;
    }

    /**
     * Re-installs the fixture at both ends of every client tick, before every HUD frame and before
     * every screen frame (ahead of {@code UiCapture}), so a server heartbeat applied by the packet
     * queue between two ticks never reaches a capture or a check. The listeners are added on the
     * first {@link #start} — an annotated {@code @Mod.EventBusSubscriber} nested class was
     * registered by the scanner of the dev run but never received these events (2026-10-05 run).
     */
    private static void listen() {
        if (listening) {
            return;
        }
        listening = true;
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false,
                TickEvent.ClientTickEvent.class, event -> apply());
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false,
                RenderGuiEvent.Pre.class, event -> apply());
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false,
                ScreenEvent.Render.Pre.class, event -> apply());
    }

    private static void apply() {
        Scenario current = scenario;
        if (current == null || Minecraft.getInstance().player == null) {
            return;
        }
        BattleSnapshot snapshot = ClientBattleState.snapshot();
        if (snapshot != fixture) {
            if (snapshot != null) {
                lastReal = snapshot;
            }
            if (fixture == null) {
                ClientBattleState.clear();
            } else {
                ClientBattleState.update(fixture);
            }
        }
        if (ClientFormationState.snapshot() != formationFixture) {
            ClientFormationState.update(formationFixture);
        }
    }

    /**
     * The open ballot before the viewer voted: {@link MockData#VOTES} minus the viewer's vote
     * for {@code side}'s formation.
     */
    private static Map<String, Integer> withoutOwnVote(MockData.Side side) {
        Map<String, Integer> votes = new LinkedHashMap<>(MockData.VOTES);
        votes.computeIfPresent(side.ownVote(), (id, count) -> Math.max(0, count - 1));
        return votes;
    }

    // ---- battle snapshot ------------------------------------------------------------------------

    private static BattleSnapshot battle(Scenario state) {
        MockData.Side side = state.side();
        MockData.FactionData own = side.own();
        MockData.FactionData other = side.enemy();
        // Own and enemy faction as the viewer's side sees them (Caesar: swapped).
        FormationContextView context = new FormationContextView(
                state.stage() == Stage.READY ? side.lockedFormation() : "",
                state.stage() == Stage.READY ? side.formationName() : "",
                state.stage() == Stage.READY ? MockData.VIEWER_CLASS : "",
                own.id(), own.name(), own.capacity(), other.id(), other.name(),
                other.capacity());
        int population = own.population();
        int enemy = other.population();
        Faction faction = side.faction();
        if (state.stage() != Stage.READY) {
            DeploymentView waiting = new DeploymentView(DeploymentPhase.WAITING, 1L, SERVER_TICK,
                    SERVER_TICK, SERVER_TICK, null, false, false, false, false, List.of());
            return new BattleSnapshot(VIEWER, faction, null, false, false, population,
                    enemy, 40, 1, List.of(), List.of(), List.of(),
                    new PermissionView(false, false, false, false, false, false, false),
                    List.of(), SupportView.unavailable(), waiting, System.currentTimeMillis(),
                    0x5100L + side.ordinal() * 256L + state.stage().ordinal(), context, "",
                    List.of());
        }
        boolean leader = state.role() == Role.LEADER;
        boolean active = state.phase() == DeploymentPhase.ACTIVE;
        List<SquadView> squads = new ArrayList<>();
        for (MockData.SquadData data : MockData.SQUADS) {
            SquadCallsign callsign = SquadCallsign.byId(data.id()).orElseThrow();
            if (callsign == SquadCallsign.ECHO) {
                // Not in the locked formation: the server sends no such call sign.
                continue;
            }
            List<MemberView> members = new ArrayList<>();
            for (int index = 0; index < data.members().size(); index++) {
                MockData.MemberData member = data.members().get(index);
                boolean viewer = MockData.VIEWER_NAME.equals(member.name());
                if (viewer && !leader) {
                    continue;
                }
                UUID id = viewer ? VIEWER : new UUID(0x5155AD01L + callsign.ordinal(), index);
                MemberState memberState = viewer ? active ? MemberState.DEPLOYED
                        : MemberState.WAITING : stateOf(member);
                members.add(new MemberView(id, member.name(), memberState != MemberState.OFFLINE,
                        memberState.hasVitals(), memberState.hasVitals() ? member.health() : 0,
                        member.maxHealth(), "leader".equals(member.role())
                        || "commander".equals(member.role()), "commander".equals(member.role()),
                        callsign, member.classId(), memberState,
                        memberState.hasVitals() ? member.healthRatio()
                                : MemberView.UNKNOWN_HEALTH_RATIO));
            }
            if (!members.isEmpty() && members.stream().noneMatch(MemberView::leader)) {
                MemberView first = members.get(0);
                members.set(0, new MemberView(first.playerId(), first.name(), first.online(),
                        first.alive(), first.health(), first.maxHealth(), true, first.commander(),
                        first.squad(), first.classId(), first.state(), first.healthRatio()));
            }
            UUID leaderId = members.stream().filter(MemberView::leader).map(MemberView::playerId)
                    .findFirst().orElse(null);
            squads.add(new SquadView(callsign, leaderId, members, data.capacity(),
                    limits(members)));
        }
        SquadCallsign ownSquadCallsign = leader ? SquadCallsign.ALPHA : null;
        List<ClassQuotaView> quotas = new ArrayList<>();
        SquadView ownSquad = ownSquadCallsign == null ? null : squads.get(0);
        for (MockData.ClassData data : MockData.CLASSES) {
            int used = ownSquad == null ? 0 : (int) ownSquad.members().stream()
                    .filter(member -> member.classId().equals(data.id())).count();
            quotas.add(new ClassQuotaView(data.id(), "", data.quota(), used));
        }
        List<DeploymentPoint> points = points(state.points(), faction);
        UUID selected = points.isEmpty() ? null
                : (active && points.size() > 1 ? points.get(1) : points.get(0)).id();
        DeploymentPhase phase = state.phase();
        boolean waiting = phase == DeploymentPhase.WAITING && state.respawnSeconds() > 0;
        DeploymentView deployment = new DeploymentView(waiting ? DeploymentPhase.WAITING
                : phase, 7L, SERVER_TICK, SERVER_TICK + state.respawnSeconds() * 20L,
                SERVER_TICK + state.resupplySeconds() * 20L, selected, !active, !active,
                !active && !waiting && leader, false, points);
        PermissionView permissions = leader
                ? new PermissionView(false, false, true, true, true, false, false)
                : new PermissionView(true, true, false, false, false, false, false);
        int members = squads.stream().mapToInt(squad -> squad.members().size()).sum();
        return new BattleSnapshot(VIEWER, faction, ownSquadCallsign, leader, false,
                members + (leader ? 0 : 1), enemy, 40, 8, squads, List.of(), List.of(),
                permissions, quotas, SupportView.unavailable(), deployment,
                System.currentTimeMillis(), 0x5200L + side.ordinal() * 256L
                + state.role().ordinal() * 16L + phase.ordinal(), context, MockData.VIEWER_CLASS,
                List.of());
    }

    private static MemberState stateOf(MockData.MemberData member) {
        if (!member.online()) {
            return MemberState.OFFLINE;
        }
        if (!member.alive()) {
            return MemberState.DEAD;
        }
        return "Ayane_Okusora".equals(member.name()) ? MemberState.WAITING : MemberState.DEPLOYED;
    }

    private static List<ClassLimitView> limits(List<MemberView> members) {
        List<ClassLimitView> limits = new ArrayList<>();
        for (MockData.ClassData data : MockData.CLASSES) {
            int used = (int) members.stream().filter(member -> member.classId().equals(data.id()))
                    .count();
            limits.add(new ClassLimitView(data.id(), data.quota(), Math.min(used, data.quota())));
        }
        return limits;
    }

    /**
     * Main base, {@code count − 2} beacons and a rally point (two points: base and a beacon), all
     * of {@code faction} (the viewer's side: the server only sends the own side's points).
     */
    static List<DeploymentPoint> points(int count, Faction faction) {
        List<DeploymentPoint> points = new ArrayList<>();
        if (count <= 0) {
            return points;
        }
        points.add(point(0, DeploymentPointKind.MAIN_BASE, -160, 64, 110, faction));
        int beacons = count == 2 ? 1 : Math.max(0, count - 2);
        for (int index = 0; index < beacons; index++) {
            points.add(point(1 + index, DeploymentPointKind.FIELD_BEACON,
                    -40 + (index % 4) * 38, 71, 20 - (index / 4) * 42, faction));
        }
        if (count >= 3) {
            points.add(point(99, DeploymentPointKind.RALLY, -30, 66, 60, faction));
        }
        return points;
    }

    private static DeploymentPoint point(int index, DeploymentPointKind kind, int x, int y,
                                         int z, Faction faction) {
        return new DeploymentPoint(new UUID(0x5155AD99L, index), faction, OVERWORLD,
                new BlockPos(x, y, z), 0.0F, DeploymentPoint.DEFAULT_SUPPLY_RADIUS, kind);
    }
}
