package com.wok.infantry.uitest.fixtures;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.MemberState;
import com.wok.infantry.battle.MemberView;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.battle.SquadView;
import com.wok.infantry.battle.tickets.TicketNetwork;
import com.wok.infantry.client.ClientBattleState;
import com.wok.infantry.client.ClientFormationState;
import com.wok.infantry.client.ClientStaminaState;
import com.wok.infantry.client.hud.HudPaint;
import com.wok.infantry.client.hud.InfantryHudApi;
import com.wok.infantry.client.hud.TacticalHud;
import com.wok.infantry.client.screen.TacticalBoardTheme;
import com.wok.infantry.client.screen.UiRect;
import com.wok.infantry.client.screen.UiScale;
import com.wok.infantry.client.ui.probe.UiLayoutProbe;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.vote.FormationVotePhase;
import com.wok.infantry.stamina.StaminaSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client-side HUD states of the preview's {@code 10-hud} surface for the UI acceptance: the
 * battle roster (preview demo data), the battle strip and base-supply notice, stamina, the
 * formation ballot phases and the 3-second lock notice. Only client caches are touched — never the
 * server, whose kit policy would end the deployment — and they are re-installed every client tick
 * and before every HUD frame, so a heartbeat snapshot from the server never reaches a capture.
 * {@link #stop()} puts the server's values back.
 *
 * <p>The downed state also draws a stand-in for WOK步战附属-倒地's panel in the core's
 * {@link InfantryHudApi#CENTER_LOW} slot, so the slot API is exercised with the core alone.
 */
public final class HudFixtures {
    /** HUD states of {@code 10-hud.js}, plus the eight-member roster. */
    public enum Mode {
        BATTLE,
        CHAT,
        DOWNED,
        VOTE_WAIT,
        VOTE,
        VOTED,
        LOCKED,
        ROSTER8;

        boolean ballot() {
            return this == VOTE_WAIT || this == VOTE || this == VOTED || this == LOCKED;
        }
    }

    /** Layout-probe box of the stand-in downed panel. */
    public static final String DOWNED_BOX = "hud.downed";
    /** Ticket counts of the preview ({@code MOCK.battle}). */
    private static final TicketNetwork.Snapshot TICKETS = new TicketNetwork.Snapshot(
            MockData.TICKETS_BLUE, MockData.TICKETS_RED, MockData.TICKETS_MAX, true);
    private static final TicketNetwork.Snapshot NO_TICKETS = new TicketNetwork.Snapshot(0, 0, 1,
            false);

    /** Camera pitch of the HUD captures: a little below the horizon. */
    private static final float VIEW_PITCH = 8.0F;

    private static Mode mode;
    private static boolean saved;
    private static float viewYaw;
    private static boolean viewCaptured;
    private static FormationSelectionSnapshot savedFormation;
    private static TicketNetwork.Snapshot savedTickets;
    private static StaminaSnapshot savedStamina;
    private static BattleSnapshot lastReal;
    private static BattleSnapshot lastFixture;
    private static FormationSelectionSnapshot formationFixture;

    private HudFixtures() {
    }

    /** Shows {@code next} from now on; the first call remembers the server's values. */
    public static void start(Mode next) {
        if (!saved) {
            saved = true;
            savedFormation = ClientFormationState.snapshot();
            savedTickets = TicketNetwork.snapshot();
            savedStamina = ClientStaminaState.snapshot();
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!viewCaptured && minecraft.player != null) {
            // The first HUD case fixes the view direction for all of them.
            viewCaptured = true;
            viewYaw = minecraft.player.getYRot();
        }
        mode = next;
        lastFixture = null;
        lastReal = null;
        formationFixture = ballot(next);
        // As the preview: the base-supply notice in battle, none in the downed state or before
        // the lock.
        TicketNetwork.showSupplyHint(next.ballot() || next == Mode.DOWNED ? "" : supplyHint());
        if (next == Mode.LOCKED) {
            ClientFormationState.update(formationFixture);
            ClientFormationState.recordLock(MockData.VIEWER_FACTION, MockData.LOCKED_FORMATION,
                    lockedName());
        }
        apply();
    }

    /** Puts the server's caches back and stops re-installing the fixture. */
    public static void stop() {
        if (!saved) {
            return;
        }
        mode = null;
        saved = false;
        ClientFormationState.clear();
        ClientFormationState.update(savedFormation);
        TicketNetwork.acceptClient(savedTickets == null ? NO_TICKETS : savedTickets);
        TicketNetwork.showSupplyHint("");
        if (savedStamina != null) {
            ClientStaminaState.update(savedStamina);
        }
        if (lastReal != null) {
            ClientBattleState.update(lastReal);
        }
        lastReal = null;
        lastFixture = null;
        formationFixture = null;
    }

    /** The state being shown, or null. */
    public static Mode mode() {
        return mode;
    }

    /** Whether the battle cache currently holds this fixture's snapshot. */
    public static boolean applied() {
        return mode != null && lastFixture != null && ClientBattleState.snapshot() == lastFixture;
    }

    // ---- re-installation ------------------------------------------------------------------------

    @Mod.EventBusSubscriber(modid = WokInfantryMod.MOD_ID, value = Dist.CLIENT,
            bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                apply();
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
            apply();
        }
    }

    @Mod.EventBusSubscriber(modid = WokInfantryMod.MOD_ID, value = Dist.CLIENT,
            bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Overlays {
        private Overlays() {
        }

        @SubscribeEvent
        public static void register(RegisterGuiOverlaysEvent event) {
            event.registerBelow(VanillaGuiOverlay.DEBUG_TEXT.id(), "uitest_downed",
                    (gui, graphics, partialTick, width, height) ->
                            renderDowned(graphics, width, height));
        }
    }

    private static void apply() {
        Mode current = mode;
        Minecraft minecraft = Minecraft.getInstance();
        if (current == null || minecraft.player == null) {
            return;
        }
        BattleSnapshot snapshot = ClientBattleState.snapshot();
        if (snapshot != null && snapshot != lastFixture) {
            lastReal = snapshot;
            lastFixture = battle(snapshot, current, minecraft.player.getUUID(),
                    minecraft.player.getGameProfile().getName());
            ClientBattleState.update(lastFixture);
        }
        if (TicketNetwork.snapshot() != (current.ballot() ? NO_TICKETS : TICKETS)) {
            TicketNetwork.acceptClient(current.ballot() ? NO_TICKETS : TICKETS);
        }
        if (!current.ballot() && current != Mode.DOWNED && TicketNetwork.supplyHint().isEmpty()) {
            // The notice lasts five seconds; keep it up for every tier of the case.
            TicketNetwork.showSupplyHint(supplyHint());
        }
        if (ClientFormationState.snapshot() != formationFixture) {
            ClientFormationState.update(formationFixture);
        }
        StaminaSnapshot stamina = new StaminaSnapshot(MockData.STAMINA_ARMS,
                MockData.STAMINA_LEGS, true);
        if (!stamina.equals(ClientStaminaState.snapshot())) {
            ClientStaminaState.update(stamina);
        }
        // Same view in every capture (towards the horizon), whatever the cursor did in between.
        minecraft.player.setYRot(viewYaw);
        minecraft.player.yRotO = viewYaw;
        minecraft.player.setXRot(VIEW_PITCH);
        minecraft.player.xRotO = VIEW_PITCH;
    }

    // ---- fixtures -------------------------------------------------------------------------------

    /**
     * The formation catalog of a mode: during the ballot the viewer's faction has no formation
     * yet; in battle the academy's formation is locked and held by the viewer.
     */
    private static FormationSelectionSnapshot ballot(Mode mode) {
        return switch (mode) {
            case VOTE_WAIT -> FormationFixtures.Scenario.joined(FormationVotePhase.NOT_STARTED)
                    .build();
            case VOTE -> FormationFixtures.Scenario.joined(FormationVotePhase.OPEN)
                    .tally(withoutOwnVote()).build();
            case VOTED -> FormationFixtures.Scenario.joined(FormationVotePhase.OPEN)
                    .own(MockData.OWN_VOTE).build();
            default -> FormationFixtures.Scenario.joined(FormationVotePhase.LOCKED)
                    .own(MockData.OWN_VOTE).locked(MockData.LOCKED_FORMATION).build();
        };
    }

    /** Base-supply notice of the demo (server text on a real server; a uiTest language key here). */
    private static String supplyHint() {
        return Component.translatable("uitest.wok_infantry.hud.supply_hint").getString();
    }

    /** The open ballot before the viewer voted: {@link MockData#VOTES} minus the viewer's vote. */
    private static Map<String, Integer> withoutOwnVote() {
        Map<String, Integer> votes = new java.util.LinkedHashMap<>(MockData.VOTES);
        votes.computeIfPresent(MockData.OWN_VOTE, (id, count) -> Math.max(0, count - 1));
        return votes;
    }

    private static String lockedName() {
        FormationSelectionSnapshot locked = ballot(Mode.LOCKED);
        var faction = locked.faction(MockData.VIEWER_FACTION);
        var formation = faction == null ? null : faction.formation(MockData.LOCKED_FORMATION);
        return formation == null ? MockData.LOCKED_FORMATION : formation.displayName();
    }

    /** The server's snapshot with the viewer's squad replaced by the preview's demo squad. */
    private static BattleSnapshot battle(BattleSnapshot real, Mode mode, UUID viewer,
                                         String viewerName) {
        if (mode.ballot()) {
            // Before the lock nobody has a formation or a squad: no roster.
            return new BattleSnapshot(viewer, real.faction(), null, false, false,
                    real.factionMemberCount(), real.enemyFactionMemberCount(),
                    real.factionCapacity(), real.squadCapacity(), real.squads(),
                    real.alliedPositions(), real.markers(), real.permissions(),
                    real.classQuotas(), real.support(), real.deployment(),
                    real.serverTimeMillis(), real.revision(), real.formationContext(),
                    real.viewerClassId(), real.kickCooldowns());
        }
        SquadCallsign own = real.ownSquad() != null ? real.ownSquad() : SquadCallsign.ALPHA;
        List<MemberView> members = mode == Mode.ROSTER8 ? rosterOfEight(viewer, viewerName, own)
                : demoSquad(viewer, viewerName, own, mode == Mode.DOWNED);
        List<SquadView> squads = new ArrayList<>();
        boolean replaced = false;
        for (SquadView squad : real.squads()) {
            if (squad.callsign() == own) {
                squads.add(new SquadView(own, viewer, members, 8));
                replaced = true;
            } else {
                squads.add(squad);
            }
        }
        if (!replaced) {
            squads.add(new SquadView(own, viewer, members, 8));
        }
        return new BattleSnapshot(viewer, real.faction(), own, true, mode == Mode.ROSTER8,
                real.factionMemberCount(), real.enemyFactionMemberCount(), real.factionCapacity(),
                real.squadCapacity(), squads, real.alliedPositions(), real.markers(),
                real.permissions(), real.classQuotas(), real.support(), real.deployment(),
                real.serverTimeMillis(), real.revision(), real.formationContext(),
                real.viewerClassId(), real.kickCooldowns());
    }

    /**
     * The preview's Alpha squad: in battle Shiroko is downed and Ayane waits to deploy; in the
     * downed state the viewer is the one down and Shiroko is dead.
     */
    private static List<MemberView> demoSquad(UUID viewer, String viewerName, SquadCallsign own,
                                              boolean viewerDowned) {
        List<MemberView> members = new ArrayList<>();
        List<MockData.MemberData> data = MockData.viewerSquad().members();
        for (int index = 0; index < data.size(); index++) {
            MockData.MemberData member = data.get(index);
            boolean self = index == 0;
            MemberState state = switch (member.name()) {
                case "Shiroko" -> viewerDowned ? MemberState.DEAD : MemberState.DOWNED;
                case "Nonomi" -> MemberState.OFFLINE;
                case "Ayane_Okusora" -> MemberState.WAITING;
                default -> self && viewerDowned ? MemberState.DOWNED : MemberState.DEPLOYED;
            };
            members.add(member(self ? viewer : new UUID(0x5157L, index), self ? viewerName
                    : member.name(), state, member.healthRatio(), "leader".equals(member.role()),
                    false, own, member.classId()));
        }
        return members;
    }

    /** Eight members, the viewer leading the squad and the faction (both role marks). */
    private static List<MemberView> rosterOfEight(UUID viewer, String viewerName,
                                                  SquadCallsign own) {
        List<MemberView> members = new ArrayList<>();
        members.add(member(viewer, viewerName, MemberState.DEPLOYED, 0.85F, true, true, own,
                MockData.VIEWER_CLASS));
        List<MockData.MemberData> charlie = MockData.SQUADS.get(2).members();
        MemberState[] states = {MemberState.DEPLOYED, MemberState.DOWNED, MemberState.DEAD,
                MemberState.WAITING, MemberState.OFFLINE, MemberState.DEPLOYED,
                MemberState.DEPLOYED};
        float[] health = {0.9F, 0.3F, 0.0F, 1.0F, 1.0F, 0.2F, 0.6F};
        for (int index = 1; index < 8; index++) {
            MockData.MemberData member = charlie.get(index);
            members.add(member(new UUID(0x5158L, index), member.name(), states[index - 1],
                    health[index - 1], false, false, own, member.classId()));
        }
        return members;
    }

    private static MemberView member(UUID id, String name, MemberState state, float ratio,
                                     boolean leader, boolean commander, SquadCallsign squad,
                                     String classId) {
        boolean online = state != MemberState.OFFLINE;
        boolean alive = state == MemberState.DEPLOYED || state == MemberState.DOWNED;
        return new MemberView(id, name, online, alive, 20.0F * Math.max(0.0F, ratio), 20.0F,
                leader, commander, squad, classId, state,
                state.hasVitals() ? ratio : MemberView.UNKNOWN_HEALTH_RATIO);
    }

    // ---- stand-in downed panel ------------------------------------------------------------------

    /**
     * WOK步战附属-倒地's panel as an add-on would draw it with the core installed: in the slot the
     * core hands out ({@link InfantryHudApi#slot}), at the core HUD scale, with its own texts.
     */
    private static void renderDowned(GuiGraphics graphics, int width, int height) {
        if (mode != Mode.DOWNED) {
            return;
        }
        int[] slot = InfantryHudApi.slot(InfantryHudApi.CENTER_LOW, width, height);
        if (slot == null) {
            return;
        }
        int factor = Math.max(1, UiScale.hudFactor());
        Font font = Minecraft.getInstance().font;
        int left = slot[0] / factor;
        int top = slot[1] / factor;
        int right = (slot[0] + slot[2]) / factor;
        Component title = Component.translatable("uitest.wok_infantry.hud.downed.title");
        Component hint = Component.translatable("uitest.wok_infantry.hud.downed.hint");
        String countdown = Component.translatable("uitest.wok_infantry.hud.downed.seconds", 38)
                .getString();
        int bottom = top + 34;
        UiRect plate = new UiRect(left, top, right, bottom);
        HudPaint.begin(graphics, factor);
        try {
            HudPaint.probeBox(graphics, DOWNED_BOX, plate);
            TacticalHud.plate(graphics, plate.left(), plate.top(), plate.right(), plate.bottom(),
                    TacticalHud.Edge.TOP, TacticalBoardTheme.DANGER_B, true);
            int countdownX = plate.right() - 5 - font.width(countdown);
            TacticalHud.readout(graphics, font, countdown, countdownX, plate.top() + 5,
                    TacticalBoardTheme.DANGER_B);
            HudPaint.text(graphics, font, title.getString(), plate.left() + 5, plate.top() + 5,
                    countdownX - 4 - plate.left() - 5, TacticalBoardTheme.LIGHT);
            TacticalHud.meter(graphics, plate.left() + 5, plate.top() + 16, plate.right() - 5,
                    plate.top() + 18, 38.0F / 60.0F, TacticalBoardTheme.DANGER_B);
            HudPaint.text(graphics, font, hint.getString(), plate.left() + 5, plate.top() + 22,
                    plate.width() - 10, TacticalBoardTheme.LIGHT_MUTED);
            UiLayoutProbe.end(graphics);
        } finally {
            HudPaint.end(graphics);
        }
    }
}
