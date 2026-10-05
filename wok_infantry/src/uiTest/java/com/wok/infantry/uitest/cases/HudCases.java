package com.wok.infantry.uitest.cases;

import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.MemberState;
import com.wok.infantry.client.ClientBattleState;
import com.wok.infantry.client.ClientBootstrap;
import com.wok.infantry.client.KeyBindingDefaults;
import com.wok.infantry.client.hud.BattleStripModel;
import com.wok.infantry.client.hud.BattleStripOverlay;
import com.wok.infantry.client.hud.CaptureHudBridge;
import com.wok.infantry.client.hud.CaptureObjective;
import com.wok.infantry.client.hud.FormationVoteHudModel;
import com.wok.infantry.client.hud.FormationVoteHudOverlay;
import com.wok.infantry.client.hud.HudFrame;
import com.wok.infantry.client.hud.SquadHudOverlay;
import com.wok.infantry.client.hud.StaminaBarLayout;
import com.wok.infantry.client.hud.StaminaBarModel;
import com.wok.infantry.client.hud.StaminaHudOverlay;
import com.wok.infantry.client.hud.WokHudLayout;
import com.wok.infantry.client.screen.TacticalBoardTheme;
import com.wok.infantry.client.screen.UiRect;
import com.wok.infantry.client.ui.probe.UiLayoutFrame;
import com.wok.infantry.uitest.UiCapture;
import com.wok.infantry.uitest.UiCase;
import com.wok.infantry.uitest.UiCaseContext;
import com.wok.infantry.uitest.UiInputDriver;
import com.wok.infantry.uitest.UiStep;
import com.wok.infantry.uitest.UiTier;
import com.wok.infantry.uitest.fixtures.HudFixtures;
import com.wok.infantry.uitest.fixtures.MockData;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Battle HUD (preview surface {@code 10-hud}), captured with no screen open, or under the vanilla
 * chat for the {@code chat} state. Every state is a migrated surface: the roster, battle strip,
 * notices, ballot plate and stamina bar report their boxes and texts to the layout probe, so any text
 * outside its plate or two overlapping parts fail the run on a required tier.
 *
 * <p>States: {@code battle} (roster, strip, base-supply notice, stamina), {@code chat} (the roster
 * shrinks to its title on tight screens), {@code downed} (the viewer is down; a stand-in add-on
 * panel takes the core's {@code center_low} slot), {@code votewait} / {@code vote} /
 * {@code voted} (the ballot plate in the strip's slot, no roster before the lock),
 * {@code locked} (the 3-second lock notice) and {@code roster8} (eight members, the viewer leads
 * the squad and the faction). Data are the preview's demo data ({@link HudFixtures}), installed
 * on the client caches only.
 *
 * <p>Every state also checks the stamina bar in the vanilla experience row (preview surface
 * {@code stamina-a4}): its grooves where {@link StaminaBarLayout} puts them, nothing on the hotbar,
 * the status rows, the off-hand slot, TaCZ's readout or the chat, and the vanilla experience and
 * jump bars cancelled. The {@code stamina-a4} cases capture the bar's states over the battle HUD
 * (full, sprint, aim, legsout, recover, unlock, vehicle, horse: the preview's states, named like
 * its PNGs so the result page shows them side by side) and check the colours and marks the bar
 * reported to the layout probe.
 */
public final class HudCases {
    /** Roster bottom at 320×240 for eight members (preview {@code 10-hud} notes). */
    private static final int ROSTER_BOTTOM_320 = 95;
    /** Preview surface of the stamina bar states (its PNGs are named after it). */
    public static final String STAMINA_SURFACE = "stamina-a4";
    private static final String STAMINA_BOX_PREFIX = "hud.stamina.";
    private static final String NEUTRAL = hex(TacticalBoardTheme.NEUTRAL_B);
    private static final String ORANGE = hex(TacticalBoardTheme.ACCENT_B);
    private static final String RED = hex(TacticalBoardTheme.DANGER_B);
    private static final String GREEN = hex(TacticalBoardTheme.SUCCESS_B);
    private static final String LIGHT = hex(TacticalBoardTheme.LIGHT);
    private static final String GRAY = hex(TacticalBoardTheme.OFFLINE);

    /** One stamina state of the preview: the pinned values and what the probe note must say. */
    private record StaminaState(String id, StaminaBarModel.State state, List<String> expect) {
    }

    private HudCases() {
    }

    public static List<UiCase> cases() {
        List<UiCase> cases = new ArrayList<>(List.of(
                hud("battle", HudFixtures.Mode.BATTLE),
                hud("chat", HudFixtures.Mode.CHAT),
                hud("downed", HudFixtures.Mode.DOWNED),
                hud("roster8", HudFixtures.Mode.ROSTER8),
                hud("votewait", HudFixtures.Mode.VOTE_WAIT),
                hud("vote", HudFixtures.Mode.VOTE),
                hud("voted", HudFixtures.Mode.VOTED),
                hud("locked", HudFixtures.Mode.LOCKED),
                bossBar()));
        cases.addAll(captureCases());
        cases.addAll(staminaCases());
        return cases;
    }

    // ---- capture objective in the battle strip (0.5.0-beta.2) ---------------------------------

    /**
     * One WOK步战附属-占点 state as the core shows it in the battle strip: the point it pins (built
     * for the viewer's side, so the colours do not depend on the test world's faction) and what
     * the objective tile must then report.
     */
    private enum CaptureState {
        /** Our side takes B at ×2: friendly edge, 62 %, nine seconds left. */
        CAPTURE("capture", CaptureObjective.Look.CAPTURING, true, "62%", false, true),
        /** Both sides in B, progress frozen: orange. */
        CONTESTED("capture-contested", CaptureObjective.Look.CONTESTED, false, "62%", false, true),
        /** Our side holds B: friendly edge, 100 %. */
        SECURED("capture-secured", CaptureObjective.Look.SECURED, true, "100%", false, true),
        /** B switched off: gray, translucent, "停用". */
        DISABLED("capture-disabled", CaptureObjective.Look.DISABLED, false, null, false, false),
        /** The enemy takes B and our side may not take it yet: hostile edge and a lock. */
        LOCKED("capture-locked", CaptureObjective.Look.CAPTURING, false, "35%", true, true);

        private final String id;
        private final CaptureObjective.Look look;
        private final boolean friendlyEdge;
        private final String value;
        private final boolean lock;
        private final boolean solid;

        CaptureState(String id, CaptureObjective.Look look, boolean friendlyEdge, String value,
                     boolean lock, boolean solid) {
            this.id = id;
            this.look = look;
            this.friendlyEdge = friendlyEdge;
            this.value = value;
            this.lock = lock;
            this.solid = solid;
        }

        /** The tile's expected left edge colour. */
        int edge() {
            return switch (this) {
                case CONTESTED -> TacticalBoardTheme.ACCENT_B;
                case DISABLED -> TacticalBoardTheme.OFFLINE;
                default -> friendlyEdge ? TacticalBoardTheme.HUD_FRIENDLY
                        : TacticalBoardTheme.HUD_HOSTILE;
            };
        }

        /** The tile's expected number, or the disabled word. */
        String value() {
            return value != null ? value
                    : Component.translatable(BattleStripModel.DISABLED_KEY).getString();
        }

        /** The point to pin for a viewer of {@code own}. */
        CaptureObjective point(Faction own) {
            Faction enemy = own.opposite();
            double toOwn = own == Faction.BLUE ? 1.0D : -1.0D;
            String name = Component.translatable("uitest.wok_infantry.hud.capture.name")
                    .getString();
            Component ownName = Component.translatable("faction.wok_infantry." + own.id());
            Component enemyName = Component.translatable("faction.wok_infantry." + enemy.id());
            CaptureObjective.Side ownSide = side(own);
            CaptureObjective.Side enemySide = side(enemy);
            CaptureObjective.Side neutral = CaptureObjective.Side.NEUTRAL;
            return switch (this) {
                case CAPTURE -> point(name, 0.62D * toOwn, 62, ownSide, neutral, ownSide, own, 3,
                        1, true, true, true, 2, "capturing", Component.translatable(
                                "uitest.wok_infantry.hud.capture.capturing", ownName), 9);
                case CONTESTED -> point(name, 0.62D * toOwn, 62, ownSide, neutral, neutral, own,
                        2, 2, true, true, true, 0, "contested", Component.translatable(
                                "uitest.wok_infantry.hud.capture.contested"), -1);
                case SECURED -> point(name, toOwn, 100, ownSide, ownSide, ownSide, own, 2, 0,
                        true, true, true, 2, "secured", Component.translatable(
                                "uitest.wok_infantry.hud.capture.secured", ownName), -1);
                case DISABLED -> point(name, 0.3D * toOwn, 30, ownSide, neutral, neutral, own,
                        1, 0, false, true, true, 0, "disabled", Component.translatable(
                                "uitest.wok_infantry.hud.capture.disabled"), -1);
                case LOCKED -> point(name, -0.35D * toOwn, 35, enemySide, neutral, enemySide,
                        own, 1, 2, true, false, true, 2, "capturing", Component.translatable(
                                "uitest.wok_infantry.hud.capture.capturing", enemyName), 15);
            };
        }

        private static CaptureObjective point(String name, double control, int percent,
                                              CaptureObjective.Side leading,
                                              CaptureObjective.Side owner,
                                              CaptureObjective.Side capturing, Faction own,
                                              int ownPlayers, int enemyPlayers, boolean enabled,
                                              boolean ownAllowed, boolean enemyAllowed,
                                              int speed, String state, Component status,
                                              int remaining) {
            boolean blue = own == Faction.BLUE;
            return new CaptureObjective("b", name, "B", control, percent, leading, owner,
                    capturing, blue ? ownPlayers : enemyPlayers, blue ? enemyPlayers : ownPlayers,
                    enabled, blue ? ownAllowed : enemyAllowed, blue ? enemyAllowed : ownAllowed,
                    speed, state, status, remaining);
        }

        private static CaptureObjective.Side side(Faction faction) {
            return faction == Faction.BLUE ? CaptureObjective.Side.BLUE
                    : CaptureObjective.Side.RED;
        }
    }

    /** Standing in a capture point: the battle HUD with the objective tile, five states. */
    public static List<UiCase> captureCases() {
        List<UiCase> cases = new ArrayList<>();
        for (CaptureState state : CaptureState.values()) {
            cases.add(capture(state));
        }
        return cases;
    }

    private static UiCase capture(CaptureState state) {
        return UiCase.builder("hud", state.id)
                .tiers(UiTier.ALL)
                .migrated(true)
                .hudCapture(true)
                .open(context -> {
                    HudFixtures.start(HudFixtures.Mode.BATTLE);
                    chatLines(context, HudFixtures.Mode.BATTLE);
                    CaptureHudBridge.pinForAcceptance(state.point(viewer()));
                    return null;
                })
                .steps(UiStep.action(context ->
                                UiInputDriver.releaseToCentre(context.minecraft())),
                        UiStep.until("the HUD fixture", context -> HudFixtures.applied()))
                .check(HudCases::checkCommon)
                .check((context, capture) -> checkState(context, capture,
                        HudFixtures.Mode.BATTLE))
                .check((context, capture) -> checkObjective(context, capture, state))
                .cleanup(context -> {
                    CaptureHudBridge.pinForAcceptance(null);
                    HudFixtures.stop();
                })
                .build();
    }

    /** The viewer's side in the battle snapshot (blue when it has none, as the strip assumes). */
    private static Faction viewer() {
        BattleSnapshot snapshot = ClientBattleState.snapshot();
        return snapshot == null || snapshot.faction() == null ? Faction.BLUE
                : snapshot.faction();
    }

    /**
     * The objective tile inside the strip with this state's look, colour, number and lock; the
     * strip 30 tall with the second row (name · status · time left) on non-tight screens, 17 tall
     * with the tile only on tight ones; the ticket numbers clear of the tile.
     */
    private static void checkObjective(UiCaseContext context, UiCapture.Result capture,
                                       CaptureState state) {
        UiLayoutFrame frame = capture.frame();
        HudFrame hud = HudFrame.current(capture.guiWidth(), capture.guiHeight());
        context.require(hud != null && hud.objective() != null, "no objective in the HUD frame");
        UiLayoutFrame.Box strip = frame.box(BattleStripOverlay.PROBE_BOX);
        UiLayoutFrame.Box tile = frame.box(BattleStripOverlay.OBJECTIVE_PROBE_BOX);
        context.require(strip != null && tile != null, "the objective tile was not drawn");
        context.require(tile.rect().within(strip.rect(), 0.01F),
                "objective tile " + tile.rect() + " leaves the strip " + strip.rect());
        boolean tight = hud.layout().tight();
        int expectedHeight = (tight ? WokHudLayout.STRIP_HEIGHT : WokHudLayout.STRIP_HEIGHT_WIDE)
                * capture.baseScale();
        context.require(Math.abs(strip.rect().height() - expectedHeight) < 0.01F,
                "strip " + strip.rect() + " should be " + expectedHeight + " tall (tight="
                        + tight + ")");
        String note = frame.notes().stream()
                .filter(line -> line.startsWith(BattleStripOverlay.OBJECTIVE_NOTE))
                .findFirst().orElse("");
        context.require(!note.isEmpty(), "the objective tile left no probe note");
        for (String expected : List.of("look=" + state.look, "edge=" + hex(state.edge()),
                "value=" + state.value(), "lock=" + state.lock, "solid=" + state.solid,
                "line=" + !tight)) {
            context.require(note.contains(expected), "objective " + state.id + " should show '"
                    + expected + "': " + note);
        }
        String line = hud.objective().line().getString();
        boolean lineDrawn = frame.texts().stream().anyMatch(text ->
                line.equals(text.fullText()) || line.equals(text.text()));
        context.require(lineDrawn != tight, "second row '" + line + "' "
                + (tight ? "drawn on a tight screen" : "missing on a wide screen"));
        for (UiLayoutFrame.Text text : frame.texts()) {
            if ((text.text().equals(String.valueOf(MockData.TICKETS_BLUE))
                    || text.text().equals(String.valueOf(MockData.TICKETS_RED)))
                    && text.rect().overlaps(tile.rect())) {
                context.fail("ticket number " + text.text() + " " + text.rect()
                        + " under the objective tile " + tile.rect());
            }
        }
        context.observe("hudObjective[" + state.id + "@" + context.tier().id() + "]="
                + note.substring(BattleStripOverlay.OBJECTIVE_NOTE.length()));
    }

    /** The stamina bar's states (preview {@code surfaces/16-stamina.js} DEMO). */
    public static List<UiCase> staminaCases() {
        List<UiCase> cases = new ArrayList<>();
        for (StaminaState state : staminaStates()) {
            cases.add(stamina(state));
        }
        return cases;
    }

    private static List<StaminaState> staminaStates() {
        StaminaBarModel.Mount none = StaminaBarModel.Mount.NONE;
        return List.of(
                new StaminaState("full", StaminaBarModel.State.of(100.0F, 100.0F),
                        List.of("arms=100%,hand,accent=" + NEUTRAL,
                                "legs=100%,boot,accent=" + NEUTRAL)),
                new StaminaState("sprint", new StaminaBarModel.State(pool(100.0F),
                        new StaminaBarModel.Pool(64.0F, 69.0F, false), false, false, none, 0.0F),
                        List.of("legs=64%,boot,accent=" + NEUTRAL, "ghost=69%")),
                new StaminaState("aim", new StaminaBarModel.State(
                        new StaminaBarModel.Pool(42.0F, 44.0F, false), pool(86.0F), false, false,
                        none, 0.0F),
                        List.of("arms=42%,hand,accent=" + ORANGE + ",fill=" + ORANGE
                                + ",ghost=44%")),
                new StaminaState("legsout", new StaminaBarModel.State(pool(70.0F),
                        new StaminaBarModel.Pool(0.0F, 5.0F, false), true, false, none, 0.0F),
                        List.of("legs=0%,lock,accent=" + RED, "track="
                                + hex(StaminaBarModel.LOCK_TRACK))),
                new StaminaState("recover", new StaminaBarModel.State(
                        new StaminaBarModel.Pool(46.0F, 46.0F, true),
                        new StaminaBarModel.Pool(31.0F, 31.0F, true), false, false, none, 0.0F),
                        List.of("arms=46%,hand,accent=" + ORANGE, "legs=31%,boot,accent="
                                + ORANGE, "rising=true")),
                new StaminaState("unlock", new StaminaBarModel.State(
                        new StaminaBarModel.Pool(88.0F, 88.0F, true),
                        new StaminaBarModel.Pool(16.0F, 16.0F, true), false, true, none, 0.0F),
                        List.of("legs=16%,check,accent=" + GREEN)),
                new StaminaState("vehicle", new StaminaBarModel.State(pool(64.0F),
                        new StaminaBarModel.Pool(78.0F, 78.0F, true), false, false,
                        StaminaBarModel.Mount.SEATED, 0.0F),
                        List.of("legs=78%,boot,accent=" + GRAY + ",fill=" + GRAY,
                                "mount=SEATED")),
                new StaminaState("horse", new StaminaBarModel.State(
                        new StaminaBarModel.Pool(44.0F, 46.0F, false), pool(100.0F), false, false,
                        StaminaBarModel.Mount.JUMP, 0.55F),
                        List.of("arms=44%,hand,accent=" + ORANGE, "legs=55%,jump,accent=" + LIGHT,
                                "mount=JUMP")));
    }

    private static StaminaBarModel.Pool pool(float value) {
        return StaminaBarModel.Pool.of(value);
    }

    private static UiCase stamina(StaminaState state) {
        return UiCase.builder(STAMINA_SURFACE, state.id())
                .group("hud")
                .tiers(UiTier.ALL)
                .migrated(true)
                .hudCapture(true)
                .open(context -> {
                    HudFixtures.start(HudFixtures.Mode.BATTLE, state.state());
                    chatLines(context, HudFixtures.Mode.BATTLE);
                    return null;
                })
                .steps(UiStep.action(context ->
                                UiInputDriver.releaseToCentre(context.minecraft())),
                        UiStep.until("the HUD fixture", context -> HudFixtures.applied()))
                .check(HudCases::checkCommon)
                .check((context, capture) -> checkState(context, capture,
                        HudFixtures.Mode.BATTLE))
                .check((context, capture) -> checkStaminaState(context, capture, state))
                .cleanup(context -> HudFixtures.stop())
                .build();
    }

    /**
     * B11a: the eight-member roster with a vanilla boss bar. Below about 530 GUI pixels the
     * roster reaches into the bar column, so the bars move down under the strip and right of the
     * roster; the shifted bar region must stay clear of the roster and on screen. The boss bar is
     * put into the client's own boss overlay only (no server boss) and removed afterwards.
     */
    private static UiCase bossBar() {
        return UiCase.builder("hud", "boss")
                .tiers(UiTier.ALL)
                .migrated(true)
                .hudCapture(true)
                .open(context -> {
                    HudFixtures.start(HudFixtures.Mode.ROSTER8);
                    chatLines(context, HudFixtures.Mode.ROSTER8);
                    BossHealthOverlay bosses = context.minecraft().gui.getBossOverlay();
                    bosses.reset();
                    bosses.update(ClientboundBossEventPacket.createAddPacket(new ServerBossEvent(
                            Component.translatable("uitest.wok_infantry.hud.boss"),
                            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS)));
                    return null;
                })
                .steps(UiStep.action(context ->
                                UiInputDriver.releaseToCentre(context.minecraft())),
                        UiStep.until("the HUD fixture", context -> HudFixtures.applied()))
                .check(HudCases::checkCommon)
                .check((context, capture) -> checkState(context, capture,
                        HudFixtures.Mode.ROSTER8))
                .check(HudCases::checkBossBar)
                .cleanup(context -> {
                    context.minecraft().gui.getBossOverlay().reset();
                    HudFixtures.stop();
                })
                .build();
    }

    private static UiCase hud(String state, HudFixtures.Mode mode) {
        return UiCase.builder("hud", state)
                .tiers(UiTier.ALL)
                .migrated(true)
                .hudCapture(true)
                .open(context -> {
                    HudFixtures.start(mode);
                    chatLines(context, mode);
                    return mode == HudFixtures.Mode.CHAT ? new ChatScreen("") : null;
                })
                .steps(UiStep.action(context -> {
                            if (mode != HudFixtures.Mode.CHAT) {
                                // No screen: the cursor is grabbed and must not stay parked.
                                UiInputDriver.releaseToCentre(context.minecraft());
                            }
                        }),
                        UiStep.until("the HUD fixture", context -> HudFixtures.applied()))
                .check(HudCases::checkCommon)
                .check((context, capture) -> checkState(context, capture, mode))
                .cleanup(context -> HudFixtures.stop())
                .build();
    }

    /**
     * The preview's chat lines (uiTest language keys): two in battle, four with the chat open,
     * none during the ballot. The chat is cleared for every state, so lines of an earlier case
     * (still shown for ten seconds) never reach another state's screenshot.
     */
    private static void chatLines(UiCaseContext context, HudFixtures.Mode mode) {
        ChatComponent chat = context.minecraft().gui.getChat();
        chat.clearMessages(false);
        if (mode == HudFixtures.Mode.VOTE_WAIT || mode == HudFixtures.Mode.VOTE
                || mode == HudFixtures.Mode.VOTED || mode == HudFixtures.Mode.LOCKED) {
            return;
        }
        if (mode == HudFixtures.Mode.CHAT) {
            chat.addMessage(Component.translatable("uitest.wok_infantry.hud.chat.squad"));
            chat.addMessage(Component.translatable("uitest.wok_infantry.hud.chat.faction"));
        }
        chat.addMessage(Component.translatable("uitest.wok_infantry.hud.chat.kit"));
        chat.addMessage(Component.translatable("uitest.wok_infantry.hud.chat.supply"));
    }

    // ---- checks ---------------------------------------------------------------------------------

    /**
     * Every state: the core parts stay clear of the vanilla hotbar and status rows, the stamina
     * bar takes exactly the experience row ({@link #checkStaminaBar}), and at GUI 1 the HUD lays
     * out as 480×360.
     */
    private static void checkCommon(UiCaseContext context, UiCapture.Result capture) {
        UiLayoutFrame frame = capture.frame();
        int width = capture.guiWidth();
        int height = capture.guiHeight();
        UiLayoutFrame.Rect hotbar = new UiLayoutFrame.Rect(width / 2.0F - 91.0F, height - 39.0F,
                width / 2.0F + 91.0F, height);
        for (UiLayoutFrame.Box box : frame.boxes()) {
            if (box.id().startsWith("hud.") && !box.id().startsWith(STAMINA_BOX_PREFIX)
                    && box.rect().overlaps(hotbar)) {
                context.fail(box.id() + " " + box.rect() + " covers the hotbar or status rows "
                        + hotbar);
            }
        }
        checkStaminaBar(context, capture);
        if (context.tier() == UiTier.T960) {
            context.require(capture.baseScale() == 2 && capture.layoutWidth() == 480
                            && capture.layoutHeight() == 360,
                    "the HUD at 960x720 GUI 1 must lay out as 480x360 at 2x, got "
                            + capture.layoutWidth() + "x" + capture.layoutHeight() + " x"
                            + capture.baseScale());
        }
        context.observe("hudBoxes[" + context.uiCase().stateId() + "@" + context.tier().id()
                + "]=" + frame.boxes().stream().filter(box -> !"hud.plate".equals(box.id()))
                .map(box -> box.id() + box.rect()).toList());
    }

    /**
     * The stamina bar (GUI pixels): its pieces are where {@link StaminaBarLayout} puts them for
     * this screen, the grooves fill the experience row [h − 30, h − 23) of the hotbar column, the
     * ears sit outside the column (on narrow screens the boot's tab in the centre gap between the
     * status rows), nothing reaches the hotbar, its selection frame, the off-hand slot, the status
     * rows, TaCZ's readout keep-out or, above its last line, the chat; the percentages are drawn
     * from 640 layout pixels on; the vanilla experience and jump bars did not run.
     */
    private static void checkStaminaBar(UiCaseContext context, UiCapture.Result capture) {
        UiLayoutFrame frame = capture.frame();
        int w = capture.guiWidth();
        int h = capture.guiHeight();
        float cx = w / 2;
        HudFrame hud = HudFrame.current(w, h);
        context.require(hud != null && hud.staminaShown(), "the stamina bar is not shown");
        StaminaBarLayout.Layout bar = hud.layout().staminaBar();
        UiLayoutFrame.Box band = frame.box(StaminaHudOverlay.BAND_BOX);
        UiLayoutFrame.Box earLeft = frame.box(StaminaHudOverlay.EAR_LEFT_BOX);
        UiLayoutFrame.Box earRight = frame.box(StaminaHudOverlay.EAR_RIGHT_BOX);
        UiLayoutFrame.Box tab = frame.box(StaminaHudOverlay.TAB_BOX);
        context.require(band != null && earLeft != null, "the stamina bar was not drawn");
        context.require(same(band.rect(), bar.band()) && same(earLeft.rect(), bar.earLeft()),
                "stamina bar " + band.rect() + " / " + earLeft.rect() + " is not at its layout "
                        + bar.band() + " / " + bar.earLeft());
        context.require(band.rect().top() == h - 30 && band.rect().bottom() == h - 23,
                "the grooves leave the experience row: " + band.rect());
        context.require(band.rect().left() >= cx - 91 && band.rect().right() <= cx + 91,
                "the grooves leave the hotbar column: " + band.rect());
        context.require(bar.narrow() ? tab != null && earRight == null
                        : tab == null && earRight != null && same(earRight.rect(), bar.earRight()),
                "narrow=" + bar.narrow() + " but tab=" + tab + " right ear=" + earRight);
        List<UiLayoutFrame.Rect> obstacles = List.of(
                // hotbar, its selection frame, off-hand slots and attack indicators
                new UiLayoutFrame.Rect(cx - 120, h - 23, cx + 120, h),
                // status rows above the experience row, TaCZ's ammo readout keep-out
                new UiLayoutFrame.Rect(cx - 91, h - 39, cx - 10, h - 30),
                new UiLayoutFrame.Rect(cx + 10, h - 39, cx + 91, h - 30),
                new UiLayoutFrame.Rect(w - 117, h - 48, w - 5, h - 22));
        int chatRight = StaminaBarLayout.chatRight(context.minecraft().gui.getChat().getWidth(),
                context.minecraft().gui.getChat().getScale());
        for (UiLayoutFrame.Box box : frame.boxes()) {
            if (!box.id().startsWith(STAMINA_BOX_PREFIX)) {
                continue;
            }
            for (UiLayoutFrame.Rect obstacle : obstacles) {
                context.require(!box.rect().overlaps(obstacle), box.id() + " " + box.rect()
                        + " covers a vanilla or TaCZ part " + obstacle);
            }
            context.require(box.rect().top() >= h - 40 || box.rect().left() >= chatRight,
                    box.id() + " " + box.rect() + " reaches into the chat [0, " + chatRight
                            + ") above its last line");
        }
        List<String> percents = new ArrayList<>();
        for (UiLayoutFrame.Text text : frame.texts()) {
            if (text.text().endsWith("%") && text.rect().top() >= h - 40) {
                percents.add(text.text());
            }
        }
        boolean numbers = capture.layoutWidth() >= StaminaBarLayout.NUMBERS_MIN_WIDTH;
        context.require(bar.numbers() == numbers && percents.size() == (numbers ? 2 : 0),
                "percentages " + percents + " (expected " + (numbers ? 2 : 0) + ")");
        context.require(!HudFixtures.vanillaRowDrawn(),
                "the vanilla experience or jump bar still ran under the stamina bar");
        context.observe("hudStamina[" + context.uiCase().id() + "@" + context.tier().id()
                + "]=band" + bar.band() + " earLeft" + bar.earLeft() + " earScale="
                + bar.earScale() + " narrow=" + bar.narrow() + " numbers=" + percents);
    }

    /** The colours and marks the stamina bar reported for this state (probe note). */
    private static void checkStaminaState(UiCaseContext context, UiCapture.Result capture,
                                          StaminaState state) {
        String note = capture.frame().notes().stream()
                .filter(line -> line.startsWith(StaminaHudOverlay.PROBE_NOTE))
                .findFirst().orElse("");
        context.require(!note.isEmpty(), "the stamina bar left no probe note");
        for (String expected : state.expect()) {
            context.require(note.contains(expected), "stamina state " + state.id()
                    + " should show '" + expected + "': " + note);
        }
        context.observe("hudStaminaState[" + state.id() + "@" + context.tier().id() + "]="
                + note.substring(StaminaHudOverlay.PROBE_NOTE.length()));
    }

    private static boolean same(UiLayoutFrame.Rect rect, UiRect expected) {
        return expected != null && Math.abs(rect.left() - expected.left()) < 0.01F
                && Math.abs(rect.top() - expected.top()) < 0.01F
                && Math.abs(rect.right() - expected.right()) < 0.01F
                && Math.abs(rect.bottom() - expected.bottom()) < 0.01F;
    }

    private static String hex(int argb) {
        return String.format(java.util.Locale.ROOT, "%08X", argb);
    }

    /**
     * The vanilla boss bars, moved by {@code bossShift} / {@code bossShiftX} (GUI pixels), never
     * meet the squad roster and stay on screen.
     */
    private static void checkBossBar(UiCaseContext context, UiCapture.Result capture) {
        HudFrame hud = HudFrame.current(capture.guiWidth(), capture.guiHeight());
        context.require(hud != null, "no HUD frame");
        UiLayoutFrame.Box roster = capture.frame().box(SquadHudOverlay.PROBE_BOX);
        context.require(roster != null, "the squad roster was not drawn");
        UiRect bars = WokHudLayout.bossBarRegion(capture.guiWidth(), capture.guiHeight(),
                hud.layout().bossShift());
        int shiftX = hud.layout().bossShiftX();
        UiLayoutFrame.Rect shifted = new UiLayoutFrame.Rect(bars.left() + shiftX, bars.top(),
                bars.right() + shiftX, bars.bottom());
        context.require(!roster.rect().overlaps(shifted), "the squad roster " + roster.rect()
                + " covers the boss bars " + shifted + " (shift " + shiftX + ")");
        context.require(shifted.right() <= capture.guiWidth(),
                "the boss bars were pushed off screen: " + shifted);
        context.observe("hudBossShift[" + context.tier().id() + "]=x" + shiftX + " y"
                + hud.layout().bossShift());
    }

    private static void checkState(UiCaseContext context, UiCapture.Result capture,
                                   HudFixtures.Mode mode) {
        UiLayoutFrame frame = capture.frame();
        HudFrame hud = HudFrame.current(capture.guiWidth(), capture.guiHeight());
        context.require(hud != null, "no HUD frame");
        UiLayoutFrame.Box roster = frame.box(SquadHudOverlay.PROBE_BOX);
        UiLayoutFrame.Box strip = frame.box(BattleStripOverlay.PROBE_BOX);
        UiLayoutFrame.Box vote = frame.box(FormationVoteHudOverlay.PROBE_BOX);
        if (mode == HudFixtures.Mode.VOTE_WAIT || mode == HudFixtures.Mode.VOTE
                || mode == HudFixtures.Mode.VOTED || mode == HudFixtures.Mode.LOCKED) {
            context.require(vote != null, "the ballot plate was not drawn");
            context.require(strip == null, "the battle strip must give its slot to the ballot");
            context.require(mode == HudFixtures.Mode.LOCKED || roster == null,
                    "no roster before the lock (nobody has a squad yet)");
            FormationVoteHudModel.State expected = switch (mode) {
                case VOTE_WAIT -> FormationVoteHudModel.State.WAITING;
                case VOTE -> FormationVoteHudModel.State.OPEN;
                case VOTED -> FormationVoteHudModel.State.VOTED;
                default -> FormationVoteHudModel.State.LOCKED;
            };
            context.require(hud.vote() != null && hud.vote().state() == expected,
                    "ballot plate shows " + (hud.vote() == null ? "nothing" : hud.vote().state())
                            + " instead of " + expected);
            Component key = ClientBootstrap.keyLabel(KeyBindingDefaults.Binding.TERMINAL);
            if (key != null && !key.getString().isEmpty()) {
                context.require(frame.texts().stream().anyMatch(text ->
                                text.text().equals(key.getString())),
                        "the ballot plate does not name the terminal key " + key.getString());
            }
            context.observe("hudBallot[" + context.tier().id() + "]=" + expected);
            return;
        }
        context.require(roster != null, "the squad roster was not drawn");
        context.require(strip != null, "the battle strip was not drawn");
        context.require(vote == null, "no ballot plate after the lock");
        float layoutBottom = roster.rect().bottom() / capture.baseScale();
        boolean collapsed = hud.rosterPresence() == WokHudLayout.RosterPresence.COLLAPSED;
        if (mode == HudFixtures.Mode.CHAT && context.tight()) {
            context.require(collapsed, "with the chat open on a tight screen the roster must"
                    + " shrink to its title row");
        }
        if (context.tier() == UiTier.T320 && !collapsed) {
            context.require(layoutBottom <= ROSTER_BOTTOM_320 + 0.01F,
                    "roster bottom " + layoutBottom + " is below y" + ROSTER_BOTTOM_320
                            + " at 320x240");
        }
        if (mode == HudFixtures.Mode.ROSTER8) {
            context.require(hud.roster() != null && hud.roster().rows().size() == 8,
                    "the roster does not show eight members");
        }
        if (mode == HudFixtures.Mode.DOWNED) {
            context.require(hud.roster() != null && !hud.roster().rows().isEmpty()
                            && hud.roster().rows().get(0).state() == MemberState.DOWNED,
                    "the viewer's roster row does not show the downed state");
            UiLayoutFrame.Box downed = frame.box(HudFixtures.DOWNED_BOX);
            context.require(downed != null, "the add-on panel did not get the center_low slot");
        } else {
            context.require(frame.box(BattleStripOverlay.TOAST_PROBE_BOX) != null,
                    "the base-supply notice was not drawn under the strip");
        }
        context.observe("hudRosterBottom[" + context.uiCase().stateId() + "@"
                + context.tier().id() + "]=" + layoutBottom + (collapsed ? " collapsed" : ""));
    }
}
