package com.wok.infantry.client.tablet;

import com.wok.infantry.client.tablet.TabletHudPolicy.Visibility;
import com.wok.infantry.client.tablet.TabletMotion.State;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** HUD rules while the tablet is out (DESIGN 3.5 third round, IMPL_PLAN D14). */
class TabletHudPolicyTest {
    private static final List<TabletPath> PATHS = List.of(TabletPath.A3D, TabletPath.B2D,
            TabletPath.QUICK, TabletPath.OFF);

    @Test
    void anOpenTabletScreenHidesTheHudWhateverTheAnimationSays() {
        for (State state : State.values()) {
            for (TabletPath path : PATHS) {
                assertEquals(Visibility.ALL, TabletHudPolicy.of(state, path, 0.0D, true),
                        state + " " + path);
            }
        }
        assertEquals(Visibility.ALL, TabletHudPolicy.of(null, null, 0.0D, true));
    }

    @Test
    void openingAndShownHideOnEveryPathAndSetting() {
        for (TabletPath path : PATHS) {
            for (double p : new double[]{0.0D, 0.3D, 0.61D, 1.0D}) {
                assertEquals(Visibility.ALL, TabletHudPolicy.of(State.OPENING, path, p, false),
                        "opening " + path + " " + p);
            }
            assertEquals(Visibility.ALL, TabletHudPolicy.of(State.SHOWN, path, 1.0D, false),
                    "shown " + path);
        }
    }

    @Test
    void idleHidesNothing() {
        for (TabletPath path : PATHS) {
            assertEquals(Visibility.NONE, TabletHudPolicy.of(State.IDLE, path, 0.0D, false));
        }
        assertEquals(Visibility.NONE, TabletHudPolicy.of(State.IDLE, null, 0.0D, false),
                "before the first animation");
        assertFalse(Visibility.NONE.any());
        assertTrue(Visibility.ALL.any());
    }

    @Test
    void schemeABringsTheCrosshairBackFirstThenTheRestAtK0() {
        assertEquals(Visibility.ALL, TabletHudPolicy.of(State.CLOSING, TabletPath.A3D, 0.87D, false));
        assertEquals(Visibility.ALL, TabletHudPolicy.of(State.CLOSING, TabletPath.A3D, 0.41D, false));
        assertEquals(new Visibility(true, false),
                TabletHudPolicy.of(State.CLOSING, TabletPath.A3D, 0.40D, false), "crosshair at 0.40");
        assertEquals(new Visibility(true, false),
                TabletHudPolicy.of(State.CLOSING, TabletPath.A3D, 0.18D, false));
        assertEquals(Visibility.NONE, TabletHudPolicy.of(State.CLOSING, TabletPath.A3D, 0.17D, false),
                "everything at K0");
        assertEquals(Visibility.NONE, TabletHudPolicy.of(State.CLOSING, TabletPath.A3D, 0.05D, false));
    }

    @Test
    void schemeBAndTheQuickSettingBringEverythingBackOnTheFirstCloseFrame() {
        for (double p : new double[]{1.0D, 0.99D, 0.55D, 0.3D, 0.0D}) {
            assertEquals(Visibility.NONE, TabletHudPolicy.of(State.CLOSING, TabletPath.B2D, p, false));
            assertEquals(Visibility.NONE, TabletHudPolicy.of(State.CLOSING, TabletPath.QUICK, p, false));
        }
    }

    @Test
    void agreesWithTheFrameHudOfSchemeAAndB() {
        TabletPose3D.Context c = TabletPose3DTest.context("640x360", TabletScreenKind.SQUAD);
        for (int step = 0; step <= 100; step++) {
            double p = step / 100.0D;
            TabletFrame close = TabletPose3D.frame(p, c, new TabletPose3D.FrameQuery(false,
                    TabletMotion.Curve.CLOSE, true, 1.0D, TabletHand.GUN, 0.0D));
            Visibility v = TabletHudPolicy.of(State.CLOSING, TabletPath.A3D, p, false);
            assertEquals(close.hud().hidden(), v.hudHidden(), "A close hud p=" + p);
            assertEquals(close.hud().crosshairHidden(), v.crosshairHidden(), "A close crosshair p=" + p);
            TabletFrame open = TabletPose3D.frame(p, c, TabletPose3D.FrameQuery.opening(TabletHand.GUN));
            assertTrue(open.hud().hidden() && open.hud().crosshairHidden(), "A open p=" + p);

            TabletUnits term = TabletVectors.termUnits("640x360");
            TabletPath2D.TermFrame bOpen = TabletPath2D.termFrame(p, term, true, false);
            TabletPath2D.TermFrame bClose = TabletPath2D.termFrame(p, term, false, false);
            assertTrue(bOpen.hideHud() && bOpen.hideCrosshair(), "B open p=" + p);
            assertEquals(bClose.hideHud(), TabletHudPolicy.of(State.CLOSING, TabletPath.B2D, p, false)
                    .hudHidden(), "B close p=" + p);
        }
    }

    @Test
    void theWhiteListKeepsChatTitlesSubtitlesTheWorldTintsAndTheAnimationLayer() {
        for (String kept : List.of("minecraft:chat_panel", "minecraft:title_text",
                "minecraft:subtitles", "minecraft:vignette", "minecraft:helmet",
                "minecraft:portal", "wok_infantry:tablet_motion")) {
            assertFalse(TabletHudPolicy.cancels(kept, Visibility.ALL), kept);
        }
        for (String hidden : List.of("minecraft:hotbar", "minecraft:player_health",
                "minecraft:experience_bar", "minecraft:debug_text", "minecraft:potion_icons",
                "wok_infantry:squad_roster", "wok_infantry:stamina", "tacz:gun_hud_overlay")) {
            assertTrue(TabletHudPolicy.cancels(hidden, Visibility.ALL), hidden);
            assertFalse(TabletHudPolicy.cancels(hidden, Visibility.NONE), hidden + " idle");
        }
        assertFalse(TabletHudPolicy.cancels(null, Visibility.ALL));
        assertFalse(TabletHudPolicy.cancels("minecraft:hotbar", null));
    }

    @Test
    void theCrosshairFollowsItsOwnFlag() {
        Visibility crosshairBack = new Visibility(true, false);
        assertFalse(TabletHudPolicy.cancels(TabletHudPolicy.CROSSHAIR, crosshairBack));
        assertTrue(TabletHudPolicy.cancels("minecraft:hotbar", crosshairBack));
        assertTrue(TabletHudPolicy.cancels(TabletHudPolicy.CROSSHAIR, Visibility.ALL));
        assertTrue(TabletHudPolicy.cancels(TabletHudPolicy.CROSSHAIR, new Visibility(false, true)));
        assertFalse(TabletHudPolicy.cancels("minecraft:hotbar", new Visibility(false, true)));
    }

    @Test
    void theAnimationSpecWhiteListIsCovered() {
        for (String keep : TabletAnimationModel.HUD_KEEP) {
            String id = switch (keep) {
                case "chat" -> "minecraft:chat_panel";
                case "title" -> "minecraft:title_text";
                case "subtitles" -> "minecraft:subtitles";
                default -> throw new AssertionError("unknown keep " + keep);
            };
            assertTrue(TabletHudPolicy.KEEP.contains(id), keep);
        }
    }
}
