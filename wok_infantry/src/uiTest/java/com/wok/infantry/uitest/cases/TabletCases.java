package com.wok.infantry.uitest.cases;

import com.mojang.blaze3d.platform.NativeImage;
import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.client.screen.BattleTab;
import com.wok.infantry.client.screen.FormationSelectionScreen;
import com.wok.infantry.client.screen.SquadScreen;
import com.wok.infantry.client.screen.TacticalLivery;
import com.wok.infantry.client.screen.TacticalMapScreen;
import com.wok.infantry.client.tablet.TabletAnimationController;
import com.wok.infantry.client.tablet.TabletAnimationModel;
import com.wok.infantry.client.tablet.TabletCue;
import com.wok.infantry.client.tablet.TabletCues;
import com.wok.infantry.client.tablet.TabletFrame;
import com.wok.infantry.client.tablet.TabletHand;
import com.wok.infantry.client.tablet.TabletMode;
import com.wok.infantry.client.tablet.TabletMotion;
import com.wok.infantry.client.tablet.TabletPath;
import com.wok.infantry.client.tablet.TabletPath2D;
import com.wok.infantry.client.tablet.TabletPose3D;
import com.wok.infantry.client.tablet.TabletSoundBook;
import com.wok.infantry.client.tablet.TabletSoundInstance;
import com.wok.infantry.client.tablet.TabletSounds;
import com.wok.infantry.formation.vote.FormationVotePhase;
import com.wok.infantry.uitest.UiCapture;
import com.wok.infantry.uitest.UiCase;
import com.wok.infantry.uitest.UiCaseContext;
import com.wok.infantry.uitest.UiStep;
import com.wok.infantry.uitest.UiTier;
import com.wok.infantry.uitest.fixtures.FormationFixtures;
import com.wok.infantry.uitest.fixtures.MockData;
import com.wok.infantry.uitest.fixtures.ServerFixtures;
import com.wok.infantry.uitest.fixtures.SquadFixtures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Frozen-frame acceptance of the tablet animation (0.5.0-beta.4 batch B5, group {@code tablet};
 * IMPL_PLAN 4.5). The animation is switched on for these cases only
 * ({@link TabletAnimationController#overrideForAcceptance}), opened from the world and frozen at a
 * key progress of the opening or of the C1 close, so each capture is one frame of the preview's
 * timeline ({@code ui-preview/tablet-anim}, same tier, livery and screen).
 *
 * <p><b>Coverage.</b> Five targets — the squad page in the Academy and Caesar liveries, the
 * formation join page (Neutral), the tactical map in both liveries — at the opening p
 * {@link #OPEN} and the close p {@link #CLOSE} on the preview's two 1920×1080 tiers (480×270 at
 * GUI 4, 640×360 at GUI 3); 320×240, 960×540 and 960×720 are sampled. Each target and tier also
 * has the animation-off reference ({@code off}) and the shown state after a real, unfrozen opening
 * ({@code shown}), which the last case compares with the frozen p = 1 frame (the hand-over).
 * Extra groups: the empty hand and a held item, the scheme B fallback, the quick setting, and the
 * twelve sounds.
 *
 * <p><b>Held item.</b> The development client has no TaCZ runtime, so the gun is played by a
 * vanilla crossbow in the main hand (classified as a held item: the same 1333 ms timeline and the
 * same press-down as a gun, without the gun's holster / raise sounds). The test world holds the
 * player invisible before deployment, which draws no arms: every case makes the client player
 * visible for its captures and restores it afterwards.
 *
 * <p>The cases are report-only for the layout (the page is drawn shrunk) and run no device checks
 * on the animated frames; they fail when the animation is not where it was frozen, when scheme A
 * did not run, on the pixel samples of the glass (black bands closed at READ, lit by the end of
 * the wake, bands on the C1 close, dark below B's scan line), and on the hand-over: the shown
 * state and the frozen p = 1 frame must equal the animation-off picture inside the page (the
 * glass opening S, the whole window for the map). Outside it the live world keeps moving between
 * captures, and the frozen opening frame does not draw the held item the static picture shows.
 */
public final class TabletCases {
    public static final String SURFACE = "tablet";
    /** The preview's two 1920×1080 tiers: every key frame. */
    private static final List<UiTier> MAIN = List.of(UiTier.T480G4, UiTier.T640G3);
    /** Sampled tiers (960×720 window first, then 1920×1080). */
    private static final List<UiTier> SPOT = List.of(UiTier.T320, UiTier.T960, UiTier.T540);
    /** Opening: gun, raise, at the eyes, READ, wake, zoom start, zooming, ready. */
    static final double[] OPEN = {0.17D, 0.37D, 0.58D, 0.61D, 0.66D, 0.74D, 0.87D, 1.00D};
    /** C1 close: shrinking back, sleeping, 3D again, lowering, gun back. */
    static final double[] CLOSE = {0.87D, 0.66D, 0.61D, 0.40D, 0.17D};
    /** Largest channel difference of a pixel that counts as the glass colour. */
    private static final int GLASS_TOLERANCE = 12;

    private static final TacticalLivery.Livery ACADEMY = TacticalLivery.Livery.ACADEMY;
    private static final TacticalLivery.Livery CAESAR = TacticalLivery.Livery.CAESAR;
    private static final TacticalLivery.Livery NEUTRAL = TacticalLivery.Livery.NEUTRAL;

    /** What the tablet opens into. */
    enum Kind {
        SQUAD, JOIN, MAP;

        boolean terminal() {
            return this != MAP;
        }
    }

    /** One screen in one livery. */
    enum Target {
        SQUAD_ACADEMY("a", Kind.SQUAD, MockData.Side.ACADEMY, ACADEMY),
        SQUAD_CAESAR("a", Kind.SQUAD, MockData.Side.CAESAR, CAESAR),
        JOIN_NEUTRAL("join", Kind.JOIN, null, NEUTRAL),
        MAP_ACADEMY("map", Kind.MAP, MockData.Side.ACADEMY, ACADEMY),
        MAP_CAESAR("map", Kind.MAP, MockData.Side.CAESAR, CAESAR);

        final String prefix;
        final Kind kind;
        final MockData.Side side;
        final TacticalLivery.Livery livery;

        Target(String prefix, Kind kind, MockData.Side side, TacticalLivery.Livery livery) {
            this.prefix = prefix;
            this.kind = kind;
            this.side = side;
            this.livery = livery;
        }
    }

    /** What the main hand holds. */
    enum Held {
        /** A crossbow, standing in for the gun (no TaCZ in the development client). */
        GUN_STAND_IN(""),
        EMPTY("-empty"),
        /** A compass. */
        ITEM("-item");

        final String suffix;

        Held(String suffix) {
            this.suffix = suffix;
        }

        ItemStack stack() {
            return switch (this) {
                case GUN_STAND_IN -> new ItemStack(Items.CROSSBOW);
                case EMPTY -> ItemStack.EMPTY;
                case ITEM -> new ItemStack(Items.COMPASS);
            };
        }
    }

    /** How the captured frame is reached. */
    enum Mode {
        /** Opening frozen at p. */
        OPEN,
        /** C1 close frozen at p (after the tablet was shown). */
        CLOSE,
        /** Animation off: the static picture. */
        OFF,
        /** A real (unfrozen) opening that finished: the shown state. */
        SHOWN
    }

    /**
     * One case.
     *
     * @param path      the path the animation must take (A3D, B2D or QUICK)
     * @param setting   the setting it plays with
     */
    record Shot(String stateId, Target target, Mode mode, double p, Held held, TabletPath path,
                TabletMode setting, List<UiTier> tiers) {
        boolean forceB() {
            return path == TabletPath.B2D;
        }

        /** The device checks of beta.3 apply to the static pictures only. */
        boolean deviceChecks() {
            return mode == Mode.OFF || mode == Mode.SHOWN;
        }
    }

    /**
     * A frame the hand-over compares with the animation-off picture of its target and tier.
     *
     * @param rect physical-pixel rectangle {@code x0, y0, x1, y1} that must be identical, or
     *             {@code null} for the whole window
     */
    private record HandOver(String key, String file, Mode mode, int[] rect) {
    }

    /** Animation-off screenshot of each {@code <target>@<tier>}. */
    private static final Map<String, String> REFERENCES = new LinkedHashMap<>();
    private static final List<HandOver> HAND_OVERS = new ArrayList<>();
    /** When the {@code sounds} case opened the tablet (the sound log is read from then on). */
    private static double soundsFrom = Double.NaN;

    private TabletCases() {
    }

    public static List<UiCase> cases() {
        List<UiCase> cases = new ArrayList<>();
        for (Target target : Target.values()) {
            cases.addAll(targetCases(target));
        }
        // The empty hand (vanilla right arm) and a held compass, 480×270 at GUI 4.
        List<UiTier> g4 = List.of(UiTier.T480G4);
        for (double p : new double[]{0.10D, 0.17D, 0.30D, 0.45D}) {
            cases.add(shot(new Shot(stateId("hand", Mode.OPEN, p, Held.EMPTY),
                    Target.SQUAD_ACADEMY, Mode.OPEN, p, Held.EMPTY, TabletPath.A3D,
                    TabletMode.FULL, g4)));
        }
        for (double p : new double[]{0.40D, 0.17D}) {
            cases.add(shot(new Shot(stateId("hand", Mode.CLOSE, p, Held.EMPTY),
                    Target.SQUAD_ACADEMY, Mode.CLOSE, p, Held.EMPTY, TabletPath.A3D,
                    TabletMode.FULL, g4)));
        }
        for (double p : new double[]{0.10D, 0.17D, 0.30D}) {
            cases.add(shot(new Shot(stateId("hand", Mode.OPEN, p, Held.ITEM),
                    Target.SQUAD_ACADEMY, Mode.OPEN, p, Held.ITEM, TabletPath.A3D,
                    TabletMode.FULL, g4)));
        }
        cases.add(shot(new Shot(stateId("hand", Mode.CLOSE, 0.17D, Held.ITEM),
                Target.SQUAD_ACADEMY, Mode.CLOSE, 0.17D, Held.ITEM, TabletPath.A3D,
                TabletMode.FULL, g4)));
        // Scheme B, the automatic fallback (vehicle, drone, spectator, F1, no hand pass).
        for (double p : new double[]{0.30D, 0.70D}) {
            cases.add(shot(new Shot(stateId("b", Mode.OPEN, p, Held.GUN_STAND_IN),
                    Target.SQUAD_ACADEMY, Mode.OPEN, p, Held.GUN_STAND_IN, TabletPath.B2D,
                    TabletMode.FULL, g4)));
        }
        cases.add(shot(new Shot(stateId("b", Mode.CLOSE, 0.30D, Held.GUN_STAND_IN),
                Target.SQUAD_ACADEMY, Mode.CLOSE, 0.30D, Held.GUN_STAND_IN, TabletPath.B2D,
                TabletMode.FULL, g4)));
        for (double p : new double[]{0.40D, 0.80D}) {
            cases.add(shot(new Shot(stateId("bmap", Mode.OPEN, p, Held.GUN_STAND_IN),
                    Target.MAP_ACADEMY, Mode.OPEN, p, Held.GUN_STAND_IN, TabletPath.B2D,
                    TabletMode.FULL, g4)));
        }
        // The quick setting (about 80 ms), 640×360 at GUI 3.
        List<UiTier> g3 = List.of(UiTier.T640G3);
        cases.add(shot(new Shot(stateId("quick", Mode.OPEN, 0.80D, Held.GUN_STAND_IN),
                Target.SQUAD_ACADEMY, Mode.OPEN, 0.80D, Held.GUN_STAND_IN, TabletPath.QUICK,
                TabletMode.QUICK, g3)));
        cases.add(shot(new Shot(stateId("quickmap", Mode.OPEN, 0.80D, Held.GUN_STAND_IN),
                Target.MAP_ACADEMY, Mode.OPEN, 0.80D, Held.GUN_STAND_IN, TabletPath.QUICK,
                TabletMode.QUICK, g3)));
        cases.add(sounds());
        cases.add(handOver());
        return List.copyOf(cases);
    }

    /** Every key frame of {@code target} on the main tiers, a sample of them on the others. */
    private static List<UiCase> targetCases(Target target) {
        List<UiCase> cases = new ArrayList<>();
        for (double p : OPEN) {
            cases.add(shot(new Shot(stateId(target.prefix, Mode.OPEN, p, Held.GUN_STAND_IN),
                    target, Mode.OPEN, p, Held.GUN_STAND_IN, TabletPath.A3D, TabletMode.FULL,
                    tiers(target, Mode.OPEN, p))));
        }
        if (target == Target.SQUAD_ACADEMY) {
            // The smooth downscale (GUI 2 and GUI 1) against whole pixels (GUI 3), mid-zoom.
            cases.add(shot(new Shot(stateId(target.prefix, Mode.OPEN, 0.75D, Held.GUN_STAND_IN),
                    target, Mode.OPEN, 0.75D, Held.GUN_STAND_IN, TabletPath.A3D, TabletMode.FULL,
                    SPOT)));
        }
        for (double p : CLOSE) {
            cases.add(shot(new Shot(stateId(target.prefix, Mode.CLOSE, p, Held.GUN_STAND_IN),
                    target, Mode.CLOSE, p, Held.GUN_STAND_IN, TabletPath.A3D, TabletMode.FULL,
                    tiers(target, Mode.CLOSE, p))));
        }
        for (Mode mode : new Mode[]{Mode.OFF, Mode.SHOWN}) {
            cases.add(shot(new Shot(target.prefix + "-" + mode.name().toLowerCase(Locale.ROOT),
                    target, mode, 1.0D, Held.GUN_STAND_IN,
                    mode == Mode.OFF ? TabletPath.OFF : TabletPath.A3D,
                    mode == Mode.OFF ? TabletMode.OFF : TabletMode.FULL,
                    tiers(target, mode, 1.0D))));
        }
        return cases;
    }

    /** The main tiers, plus the sampled tiers for the frames picked for them. */
    private static List<UiTier> tiers(Target target, Mode mode, double p) {
        boolean sampled = switch (target) {
            case SQUAD_ACADEMY -> mode != Mode.OPEN && mode != Mode.CLOSE
                    || mode == Mode.OPEN && (near(p, 0.37D) || near(p, 0.61D) || near(p, 0.87D)
                    || near(p, 1.0D))
                    || mode == Mode.CLOSE && (near(p, 0.66D) || near(p, 0.40D));
            case MAP_ACADEMY -> mode != Mode.OPEN && mode != Mode.CLOSE
                    || mode == Mode.OPEN && (near(p, 0.61D) || near(p, 0.87D) || near(p, 1.0D))
                    || mode == Mode.CLOSE && near(p, 0.61D);
            case JOIN_NEUTRAL -> mode != Mode.OPEN && mode != Mode.CLOSE
                    || mode == Mode.OPEN && (near(p, 0.61D) || near(p, 1.0D));
            case SQUAD_CAESAR -> mode != Mode.OPEN && mode != Mode.CLOSE
                    || mode == Mode.OPEN && (near(p, 0.87D) || near(p, 1.0D));
            case MAP_CAESAR -> false;
        };
        if (!sampled) {
            return MAIN;
        }
        List<UiTier> tiers = new ArrayList<>();
        tiers.add(UiTier.T320);
        tiers.add(UiTier.T960);
        tiers.addAll(MAIN);
        tiers.add(UiTier.T540);
        return tiers;
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1.0E-9D;
    }

    /** {@code <prefix>-<open|close><p×1000>[-<hand>]}, e.g. {@code a-open0610}. */
    static String stateId(String prefix, Mode mode, double p, Held held) {
        return String.format(Locale.ROOT, "%s-%s%04d%s", prefix,
                mode.name().toLowerCase(Locale.ROOT), Math.round(p * 1000.0D), held.suffix);
    }

    // ---- one frame ------------------------------------------------------------------------------

    private static UiCase shot(Shot shot) {
        Target target = shot.target();
        UiCase.Builder builder = UiCase.builder(SURFACE, shot.stateId())
                .group(SURFACE)
                .tiers(shot.tiers())
                .migrated(false)
                .hudCapture(true)
                .deviceChecks(shot.deviceChecks())
                .budget(500);
        if (target.kind == Kind.JOIN) {
            // The join page a player sees (no operator level), pinned Neutral as FormationCases.
            builder.pinLivery(target.livery).prepare(asPlayer());
        } else {
            builder.livery(target.livery);
        }
        builder.open(context -> {
            // Chat stays on screen while the tablet moves (DESIGN 9.3): no stale lines in frames.
            context.minecraft().gui.getChat().clearMessages(false);
            if (target.side != null) {
                SquadFixtures.start(SquadFixtures.Scenario.ready(SquadFixtures.Role.LEADER)
                        .withSide(target.side));
            }
            hold(context, shot.held());
            return null;
        });
        List<UiStep> steps = new ArrayList<>();
        if (target.side != null) {
            steps.add(UiStep.until("the squad fixture", context -> SquadFixtures.applied()));
        }
        steps.add(UiStep.waitTicks(5));
        steps.add(UiStep.action(context -> {
            context.caseState().remove("terrain");
            // JourneyMap posts its "press [J]" tip again a few ticks after a GUI scale change.
            context.minecraft().gui.getChat().clearMessages(false);
            if (context.minecraft().player != null) {
                context.minecraft().player.setInvisible(false);
            }
            TabletAnimationController.overrideForAcceptance(override(shot));
            context.minecraft().setScreen(screenOf(target));
        }));
        switch (shot.mode()) {
            case OPEN -> steps.add(UiStep.until("the opening frozen", context -> {
                TabletAnimationController.DebugSnapshot snap = TabletAnimationController.debugSnapshot();
                return snap.state() == TabletMotion.State.OPENING && snap.frozen();
            }));
            case CLOSE -> {
                steps.add(UiStep.until("the tablet shown", TabletCases::shown));
                steps.add(UiStep.waitTicks(3));
                steps.add(UiStep.action(context -> context.minecraft().setScreen(null)));
                steps.add(UiStep.until("the close frozen", context -> {
                    TabletAnimationController.DebugSnapshot snap =
                            TabletAnimationController.debugSnapshot();
                    return snap.state() == TabletMotion.State.CLOSING && snap.frozen();
                }));
            }
            case OFF, SHOWN -> steps.add(UiStep.until("the tablet shown", TabletCases::shown));
        }
        if (target.kind == Kind.MAP && (shot.mode() == Mode.OFF || shot.mode() == Mode.SHOWN
                || shot.mode() == Mode.OPEN && shot.p() >= 0.61D)) {
            // The map draws its JourneyMap terrain tiles as they arrive (its footer counts them):
            // the off reference and the shown state must not differ by a tile still loading.
            steps.add(TabletCases::terrainLoaded);
        }
        steps.add(UiStep.waitTicks(3));
        builder.steps(steps.toArray(UiStep[]::new));
        builder.check((context, capture) -> checkShot(context, capture, shot));
        builder.cleanup(context -> {
            TabletAnimationController.clearAcceptanceOverride();
            if (target.side != null) {
                SquadFixtures.stop();
            }
            hold(context, Held.EMPTY);
            if (context.minecraft().player != null) {
                context.minecraft().player.setInvisible(true);
            }
        });
        return builder.build();
    }

    private static boolean shown(UiCaseContext context) {
        return TabletAnimationController.debugSnapshot().state() == TabletMotion.State.SHOWN;
    }

    /**
     * Waits (at most 6 seconds) until the tactical map has every terrain tile it asked for, read
     * the way the live flow reads it ({@code terrainTextures} against
     * {@code desiredTerrainRequests}); the coverage goes into the case state for the check line.
     */
    private static boolean terrainLoaded(UiCaseContext context) {
        if (!(context.screen() instanceof TacticalMapScreen map)) {
            return true;
        }
        int[] coverage = terrainCoverage(map);
        context.caseState().put("terrain", coverage[0] + "/" + coverage[1]);
        return coverage[1] > 0 && coverage[0] >= coverage[1] || context.stepTicks() > 120;
    }

    /** {@code {loaded, desired}} terrain tiles of {@code map} ({@code {-1, -1}} if unreadable). */
    private static int[] terrainCoverage(TacticalMapScreen map) {
        try {
            java.lang.reflect.Field texturesField =
                    TacticalMapScreen.class.getDeclaredField("terrainTextures");
            java.lang.reflect.Field desiredField =
                    TacticalMapScreen.class.getDeclaredField("desiredTerrainRequests");
            texturesField.setAccessible(true);
            desiredField.setAccessible(true);
            if (!(texturesField.get(map) instanceof Map<?, ?> textures)
                    || !(desiredField.get(map) instanceof List<?> desired)) {
                return new int[]{-1, -1};
            }
            int loaded = 0;
            for (Object request : desired) {
                if (textures.containsKey(request)) {
                    loaded++;
                }
            }
            return new int[]{loaded, desired.size()};
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return new int[]{-1, -1};
        }
    }

    private static TabletAnimationController.AcceptanceOverride override(Shot shot) {
        boolean freezes = shot.mode() == Mode.OPEN || shot.mode() == Mode.CLOSE;
        return new TabletAnimationController.AcceptanceOverride(shot.setting(), shot.forceB(), null,
                freezes ? shot.p() : Double.NaN, shot.mode() == Mode.CLOSE, null);
    }

    private static Screen screenOf(Target target) {
        return switch (target.kind) {
            case SQUAD -> SquadScreen.forTab(null, BattleTab.SQUADS);
            case JOIN -> new FormationSelectionScreen(FormationFixtures.Scenario
                    .unjoined(FormationVotePhase.NOT_STARTED).build(), null,
                    FormationSelectionScreen.Entry.TERMINAL);
            case MAP -> new TacticalMapScreen(null);
        };
    }

    /** Puts {@code held} into the selected hotbar slot (client side only). */
    private static void hold(UiCaseContext context, Held held) {
        if (context.minecraft().player == null) {
            return;
        }
        Inventory inventory = context.minecraft().player.getInventory();
        inventory.setItem(inventory.selected, held.stack());
    }

    private static UiStep[] asPlayer() {
        return new UiStep[]{
                UiStep.serverForPlayer("take back the fixture operator level",
                        ServerFixtures::revokeTemporaryOperator),
                UiStep.until("the player view (no operator level)", context ->
                        context.minecraft().player != null
                                && !context.minecraft().player.hasPermissions(
                                BattleRules.ADMIN_PERMISSION_LEVEL))};
    }

    // ---- checks ---------------------------------------------------------------------------------

    private static void checkShot(UiCaseContext context, UiCapture.Result capture, Shot shot)
            throws IOException {
        TabletAnimationController.DebugSnapshot snap = TabletAnimationController.debugSnapshot();
        TabletFrame a = snap.a();
        UiTier tier = context.tier();
        String where = context.uiCase().stateKey() + "@" + tier.id();
        context.observe(String.format(Locale.ROOT,
                "tablet[%s]=state %s path %s p %.4f dir %d hand %s phase %s frozen %s handPass %s"
                        + " capture %s%s%s", where, snap.state(), snap.path(), snap.p(), snap.dir(),
                snap.hand(), snap.phase(), snap.frozen(), snap.handPass(), snap.capture(),
                context.caseState().containsKey("terrain")
                        ? " terrain " + context.caseState().get("terrain") : "",
                a == null ? "" : " a{tablet " + a.tablet().draw() + " hands " + a.hands().draw()
                        + " gun " + a.gun().draw() + " arm " + a.emptyArm().draw()
                        + " ui " + a.ui().draw() + "/" + (a.ui().smooth() ? "smooth" : "pixel")
                        + " cap " + a.capture().draw() + " glass " + rect(a.glassPx()) + "}"));
        Screen expected = capture.screen();
        switch (shot.mode()) {
            case OPEN, CLOSE -> {
                TabletMotion.State state = shot.mode() == Mode.OPEN ? TabletMotion.State.OPENING
                        : TabletMotion.State.CLOSING;
                context.require(snap.state() == state, "state " + snap.state() + " instead of "
                        + state);
                context.require(Math.abs(snap.p() - shot.p()) < 1.0E-6D,
                        "p " + snap.p() + " instead of " + shot.p());
                context.require(snap.path() == shot.path(), "path " + snap.path() + " instead of "
                        + shot.path());
                if (shot.mode() == Mode.OPEN) {
                    context.require(expected != null && kindMatches(shot.target(), expected),
                            "the opening is not on its screen: " + expected);
                }
            }
            case OFF -> context.require(expected != null && kindMatches(shot.target(), expected)
                    && snap.state() == TabletMotion.State.SHOWN, "the static screen is not shown: "
                    + expected + " " + snap.state());
            case SHOWN -> context.require(expected != null && kindMatches(shot.target(), expected),
                    "the shown screen is " + expected);
        }
        if (shot.path() == TabletPath.A3D && shot.mode() != Mode.SHOWN) {
            context.require(snap.handPass(), "the hand pass never reached the animation");
            context.require(a != null, "no scheme-A frame");
            if (shot.mode() == Mode.OPEN && shot.target().kind.terminal() && shot.p() >= 0.61D
                    && shot.p() < 1.0D) {
                boolean smooth = smoothTier(tier);
                context.require(a.ui().smooth() == smooth, "the page is drawn "
                        + (a.ui().smooth() ? "smooth" : "in whole pixels") + " on " + tier.id());
            }
            if (shot.mode() == Mode.CLOSE && near(shot.p(), 0.61D)) {
                context.require(a.tablet().draw() && !a.capture().draw(),
                        "C1 at 0.61 draws the 3D device and the sleep line, not the copy");
            }
            if (shot.mode() == Mode.CLOSE && shot.p() > 0.61D) {
                context.require(a.capture().draw(), "the C1 close does not replay its copy");
            }
        }
        samplePixels(context, shot, snap);
        recordHandOver(context, shot, snap);
    }

    private static boolean kindMatches(Target target, Screen screen) {
        return switch (target.kind) {
            case SQUAD -> screen instanceof SquadScreen;
            case JOIN -> screen instanceof FormationSelectionScreen;
            case MAP -> screen instanceof TacticalMapScreen;
        };
    }

    /** Tiers whose reading position downscales smoothly (GUI 2 and GUI 1). */
    private static boolean smoothTier(UiTier tier) {
        return tier == UiTier.T540 || tier == UiTier.T960 || tier == UiTier.T480
                || tier == UiTier.T640 || tier == UiTier.T427;
    }

    /**
     * Pixel samples of the glass (IMPL_PLAN 4.5 check 3), read from the frame on screen (frozen,
     * so it is the captured one inside the device): at READ the black bands are closed (centre =
     * GLASS), at the end of the wake the page is lit (squad page centre ≠ GLASS), the C1 close at
     * 0.66 has a band at the top and the bottom, and B at 0.70 is dark below its scan line.
     */
    private static void samplePixels(UiCaseContext context, Shot shot,
                                     TabletAnimationController.DebugSnapshot snap) {
        if (!shot.target().kind.terminal()) {
            return;
        }
        List<int[]> wantGlass = new ArrayList<>();
        List<int[]> wantLit = new ArrayList<>();
        TabletFrame a = snap.a();
        if (shot.path() == TabletPath.A3D && a != null && a.glassPx() != null) {
            TabletPose3D.RectPx g = a.glassPx();
            int cx = (int) Math.floor(g.x() + g.w() / 2.0D);
            int cy = (int) Math.floor(g.y() + g.h() / 2.0D);
            if (shot.mode() == Mode.OPEN && near(shot.p(), 0.61D)) {
                // Off the vertical centre: the two wake edges lie merged there (LIGHT α 0.6).
                wantGlass.add(new int[]{cx, (int) Math.floor(g.y() + g.h() * 0.25D)});
                wantGlass.add(new int[]{cx, (int) Math.floor(g.y() + g.h() * 0.75D)});
            } else if (shot.mode() == Mode.OPEN && near(shot.p(), 0.74D)
                    && shot.target().kind == Kind.SQUAD) {
                wantLit.add(new int[]{cx, cy});
            } else if (shot.mode() == Mode.CLOSE && near(shot.p(), 0.66D)) {
                wantGlass.add(new int[]{cx, (int) Math.floor(g.y() + g.h() * 0.06D)});
                wantGlass.add(new int[]{cx, (int) Math.floor(g.y() + g.h() * 0.94D)});
            }
        }
        TabletPath2D.TermFrame term = snap.term();
        if (shot.path() == TabletPath.B2D && shot.mode() == Mode.OPEN && near(shot.p(), 0.70D)
                && term != null && term.scanY() != Integer.MIN_VALUE) {
            TabletPath2D.Box s = term.s();
            double yLp = s.y() + term.dy() + s.h() * 0.9D;
            if (yLp > term.scanY() + 1) {
                wantGlass.add(new int[]{(int) Math.floor((s.x() + s.w() / 2.0D) * term.unit()),
                        (int) Math.floor(yLp * term.unit())});
            }
        }
        if (wantGlass.isEmpty() && wantLit.isEmpty()) {
            return;
        }
        Minecraft minecraft = context.minecraft();
        List<String> seen = new ArrayList<>();
        boolean ok = true;
        try (NativeImage image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
            for (int[] at : wantGlass) {
                int argb = pixel(image, at[0], at[1]);
                seen.add(String.format(Locale.ROOT, "glass(%d,%d)=%06X", at[0], at[1],
                        argb & 0xFFFFFF));
                ok &= nearGlass(argb);
            }
            for (int[] at : wantLit) {
                int argb = pixel(image, at[0], at[1]);
                seen.add(String.format(Locale.ROOT, "lit(%d,%d)=%06X", at[0], at[1],
                        argb & 0xFFFFFF));
                ok &= !nearGlass(argb);
            }
        }
        context.observe("tabletPixels[" + context.uiCase().stateKey() + "@" + context.tier().id()
                + "]=" + seen);
        context.require(ok, "glass pixels not as designed: " + seen);
    }

    /** ARGB of {@code (x, y)} (clamped into the image). */
    private static int pixel(NativeImage image, int x, int y) {
        int cx = Math.max(0, Math.min(image.getWidth() - 1, x));
        int cy = Math.max(0, Math.min(image.getHeight() - 1, y));
        int abgr = image.getPixelRGBA(cx, cy);
        int r = abgr & 0xFF;
        int g = abgr >> 8 & 0xFF;
        int b = abgr >> 16 & 0xFF;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static boolean nearGlass(int argb) {
        int glass = TabletAnimationModel.GLASS;
        for (int shift = 0; shift <= 16; shift += 8) {
            if (Math.abs((argb >> shift & 0xFF) - (glass >> shift & 0xFF)) > GLASS_TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    private static String rect(TabletPose3D.RectPx r) {
        return r == null ? "-" : String.format(Locale.ROOT, "%.1f,%.1f %.1fx%.1f", r.x(), r.y(),
                r.w(), r.h());
    }

    // ---- hand-over ------------------------------------------------------------------------------

    /**
     * Share of the window the map's hand-over may differ in: the tactical map draws the live
     * JourneyMap terrain, which re-renders animals and crops as the world ticks between the two
     * captures (seen only when the live flow ran first). A wrong transform or colour at the end
     * of the animation differs in far more than this.
     */
    private static final double MAP_LIVE_SHARE = 0.0005D;

    /** Remembers the off reference, and the frames the hand-over compares with it. */
    private static void recordHandOver(UiCaseContext context, Shot shot,
                                       TabletAnimationController.DebugSnapshot snap) {
        if (shot.path() == TabletPath.B2D || shot.setting() == TabletMode.QUICK
                || shot.held() != Held.GUN_STAND_IN) {
            return;
        }
        String key = shot.target().name() + "@" + context.tier().id();
        String file = context.uiCase().fileName(context.tier());
        if (shot.mode() == Mode.OFF) {
            REFERENCES.put(key, file);
        } else if (shot.mode() == Mode.SHOWN) {
            // Compared inside the page: around the device the live world shows through the
            // dimmed backdrop and keeps moving between the two captures.
            HAND_OVERS.add(new HandOver(key, file, Mode.SHOWN, GLASS.get(key)));
        } else if (shot.mode() == Mode.OPEN && near(shot.p(), 1.0D)) {
            // The frozen last opening frame: the page (the glass opening, or the whole map) must
            // be the static picture; around it the held item is not drawn while the animation
            // runs (TabletHandPass), but is in the static picture.
            int[] rect = null;
            TabletFrame a = snap.a();
            if (shot.target().kind.terminal() && a != null && a.glassPx() != null) {
                TabletPose3D.RectPx g = a.glassPx();
                rect = new int[]{(int) Math.ceil(g.x()), (int) Math.ceil(g.y()),
                        (int) Math.floor(g.x() + g.w()), (int) Math.floor(g.y() + g.h())};
                GLASS.put(key, rect);
            }
            HAND_OVERS.add(new HandOver(key, file, Mode.OPEN, rect));
        }
    }

    /** The glass opening S of each terminal target and tier (from its frozen p = 1 frame). */
    private static final Map<String, int[]> GLASS = new LinkedHashMap<>();

    /**
     * The hand-over (IMPL_PLAN 4.5 check 2): once every screenshot of the run is written, each
     * frozen p = 1 frame and each shown state is compared with the animation-off picture of the
     * same target and tier. Inside the page — the glass opening S of the terminal, the whole
     * window for the map — they must be identical (the map up to {@link #MAP_LIVE_SHARE}).
     * Differences outside it (the live world around the device, the held item the frozen opening
     * frame does not draw) are reported with their bounding box.
     */
    private static UiCase handOver() {
        return UiCase.builder(SURFACE, "handover")
                .group(SURFACE)
                .tiers(UiTier.T480G4)
                .migrated(false)
                .budget(400)
                .open(context -> null)
                .steps(UiStep.until("every screenshot written", context ->
                        allSaved() || context.stepTicks() > 200))
                .check((context, capture) -> compareHandOvers(context))
                .build();
    }

    private static boolean allSaved() {
        for (HandOver handOver : HAND_OVERS) {
            String reference = REFERENCES.get(handOver.key());
            if (UiCapture.saveMessage(handOver.file()).isEmpty()
                    || reference != null && UiCapture.saveMessage(reference).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static void compareHandOvers(UiCaseContext context) throws IOException {
        Path dir = context.minecraft().gameDirectory.toPath().resolve("screenshots");
        List<String> failures = new ArrayList<>();
        int compared = 0;
        for (HandOver handOver : HAND_OVERS) {
            String reference = REFERENCES.get(handOver.key());
            if (reference == null) {
                context.observe("tabletHandOver[" + handOver.file() + "]=no off reference");
                continue;
            }
            try (NativeImage frame = read(dir.resolve(handOver.file()));
                 NativeImage off = read(dir.resolve(reference))) {
                if (frame.getWidth() != off.getWidth() || frame.getHeight() != off.getHeight()) {
                    failures.add(handOver.file() + " size differs from " + reference);
                    continue;
                }
                int[] rect = handOver.rect() != null ? handOver.rect()
                        : new int[]{0, 0, off.getWidth(), off.getHeight()};
                int total = 0;
                int inside = 0;
                int minX = Integer.MAX_VALUE;
                int minY = Integer.MAX_VALUE;
                int maxX = -1;
                int maxY = -1;
                for (int y = 0; y < off.getHeight(); y++) {
                    for (int x = 0; x < off.getWidth(); x++) {
                        if (frame.getPixelRGBA(x, y) != off.getPixelRGBA(x, y)) {
                            total++;
                            minX = Math.min(minX, x);
                            minY = Math.min(minY, y);
                            maxX = Math.max(maxX, x);
                            maxY = Math.max(maxY, y);
                            if (x >= rect[0] && x < rect[2] && y >= rect[1] && y < rect[3]) {
                                inside++;
                            }
                        }
                    }
                }
                compared++;
                boolean map = handOver.key().startsWith("MAP_");
                int allowed = map ? (int) Math.floor(MAP_LIVE_SHARE * off.getWidth()
                        * off.getHeight()) : 0;
                context.observe(String.format(Locale.ROOT,
                        "tabletHandOver[%s]=%s vs %s: %d px differ, %d inside %d,%d-%d,%d"
                                + " (allowed %d)%s",
                        handOver.file(), handOver.mode().name().toLowerCase(Locale.ROOT),
                        reference, total, inside, rect[0], rect[1], rect[2], rect[3], allowed,
                        total == 0 ? "" : String.format(Locale.ROOT, " bbox %d,%d-%d,%d", minX,
                                minY, maxX + 1, maxY + 1)));
                if (inside > allowed) {
                    failures.add(handOver.file() + ": " + inside + " px");
                }
            }
        }
        context.observe("tabletHandOver=" + compared + " compared, " + failures.size() + " failed");
        context.require(failures.isEmpty(), "hand-over differs from the static picture: " + failures);
    }

    private static NativeImage read(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return NativeImage.read(in);
        }
    }

    // ---- sounds (batch B4) ----------------------------------------------------------------------

    /**
     * Batch B4: the twelve sounds load from sounds.json (event, subtitle, OGG resource), one plays
     * through the client's sound engine, and a full (unfrozen) opening asks for its cues in the
     * preview's order. Acceptance mutes the animation's own sounds, so the order is read from the
     * sound log.
     */
    private static UiCase sounds() {
        SquadFixtures.Scenario scenario = SquadFixtures.Scenario.ready(SquadFixtures.Role.LEADER);
        return UiCase.builder(SURFACE, "sounds")
                .group(SURFACE)
                .tiers(UiTier.T320)
                .migrated(false)
                .hudCapture(true)
                .budget(400)
                .open(context -> {
                    SquadFixtures.start(scenario);
                    return null;
                })
                .steps(UiStep.until("the squad fixture", context -> SquadFixtures.applied()),
                        UiStep.waitTicks(5),
                        UiStep.action(context -> {
                            TabletAnimationController.overrideForAcceptance(
                                    new TabletAnimationController.AcceptanceOverride(TabletMode.FULL,
                                            false, TabletHand.GUN, Double.NaN, false, null));
                            soundsFrom = TabletSounds.clock();
                            context.minecraft().setScreen(SquadScreen.forTab(null, BattleTab.SQUADS));
                        }),
                        UiStep.until("the tablet shown", TabletCases::shown),
                        UiStep.waitTicks(2))
                .check((context, capture) -> checkSounds(context))
                .cleanup(context -> {
                    TabletAnimationController.clearAcceptanceOverride();
                    SquadFixtures.stop();
                })
                .build();
    }

    private static void checkSounds(UiCaseContext context) {
        SoundManager manager = context.minecraft().getSoundManager();
        List<String> missing = new ArrayList<>();
        for (String name : TabletCues.SOUND_NAMES) {
            ResourceLocation id = TabletSounds.location(name);
            WeighedSoundEvents event = manager.getSoundEvent(id);
            if (event == null) {
                missing.add(id + " (no sounds.json entry)");
                continue;
            }
            if (event.getSubtitle() == null) {
                missing.add(id + " (no subtitle)");
            }
            Sound sound = event.getSound(RandomSource.create());
            if (sound == null || sound == SoundManager.EMPTY_SOUND
                    || context.minecraft().getResourceManager().getResource(sound.getPath()).isEmpty()) {
                missing.add(id + " (file " + (sound == null ? "-" : sound.getPath()) + ")");
            }
        }
        context.require(missing.isEmpty(), "tablet sounds not loaded: " + missing);
        TabletSoundInstance probe = new TabletSoundInstance(TabletSounds.event("search"),
                TabletCue.Dir.OPEN, (float) TabletAnimationModel.SFX_VOLUME, TabletSounds::clock);
        manager.play(probe);
        context.observe("tabletSounds[probe]=search active " + manager.isActive(probe));
        List<String> asked = new ArrayList<>();
        for (TabletSoundBook.Entry entry : TabletSounds.recent()) {
            if (entry.atMs() >= soundsFrom && entry.kind() != TabletSoundBook.Entry.Kind.CUT) {
                asked.add(entry.name() + (entry.kind() == TabletSoundBook.Entry.Kind.WAIT ? "?" : ""));
            }
        }
        context.observe("tabletSounds[open]=" + asked);
        context.require(asked.size() >= 7 && asked.subList(0, 5).equals(
                        List.of("holster", "draw", "grip", "power", "boot")) && asked.contains("zoom")
                        && (asked.contains("ready") || asked.contains("ready?")),
                "a full opening asks for holster, draw, grip, power, boot, ready, zoom: " + asked);
    }
}
