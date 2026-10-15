package com.wok.infantry.client.tablet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wok.infantry.client.screen.DeviceArt;
import com.wok.infantry.client.screen.TacticalBoardTheme;
import com.wok.infantry.client.screen.TacticalShellLayout;
import com.wok.infantry.client.tablet.TabletAnimationModel.Key;
import com.wok.infantry.client.tablet.TabletAnimationModel.PhaseSeg;
import com.wok.infantry.client.tablet.TabletAnimationModel.Phases;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.wok.infantry.client.tablet.TabletAnimationModel.*;
import static com.wok.infantry.client.tablet.TabletVectors.near;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link TabletAnimationModel} against the preview's anim-spec ({@code vectors.spec}, which is
 * anim-spec after the Java patches): every constant the Java side uses.
 */
class TabletAnimationModelTest {
    private static JsonElement spec(String dotted) {
        JsonElement at = TabletVectors.section("spec");
        for (String part : dotted.split("\\.")) {
            at = at.getAsJsonObject().get(part);
            if (at == null) {
                throw new AssertionError("spec has no " + dotted);
            }
        }
        return at;
    }

    private static void eq(String dotted, double actual) {
        near(spec(dotted), actual, () -> "spec." + dotted);
    }

    private static void eqPts(String dotted, double[][] actual) {
        double[][] expected = TabletVectors.pairs(spec(dotted));
        assertEquals(expected.length, actual.length, "spec." + dotted + " nodes");
        for (int i = 0; i < expected.length; i++) {
            int index = i;
            near(expected[i][0], actual[i][0], () -> "spec." + dotted + "[" + index + "].ms");
            near(expected[i][1], actual[i][1], () -> "spec." + dotted + "[" + index + "].p");
        }
    }

    private static void eqEase(String dotted, TabletEasing actual) {
        assertEquals(spec(dotted).getAsString(), actual.previewName(), "spec." + dotted);
    }

    private static void eqVec(String dotted, double... actual) {
        double[] expected = TabletVectors.doubles(spec(dotted));
        assertEquals(expected.length, actual.length, "spec." + dotted);
        for (int i = 0; i < expected.length; i++) {
            int index = i;
            near(expected[i], actual[i], () -> "spec." + dotted + "[" + index + "]");
        }
    }

    @Test
    void versionFrameAndTempo() {
        assertEquals(spec("meta.version").getAsString(), VERSION);
        eq("frameMs", FRAME_MS);
        eq("U", U);
        eq("a.tempo", TEMPO);
        assertEquals(0.3D, TEMPO, 0.0D);
    }

    @Test
    void schemeATimeMaps() {
        for (String hand : new String[]{"gun", "empty"}) {
            boolean empty = hand.equals("empty");
            eqPts("a.v1.open." + hand, v1Open(empty));
            eqPts("a.v1.close." + hand, v1Close(empty));
            eqPts("a.v1.closeCapture." + hand, v1CloseCapture(empty));
            eqPts("a.open." + hand, openPts(empty));
            eqPts("a.close." + hand, closePts(empty));
            eqPts("a.closeCapture." + hand, closeCapturePts(empty));
        }
        assertEquals(spec("a.captureDefault").getAsBoolean(), CAPTURE_DEFAULT);
        eq("a.captureSkipWake.from", CAPTURE_SKIP_WAKE_FROM);
        eq("a.captureSkipWake.to", CAPTURE_SKIP_WAKE_TO);
        eqEase("a.curve.closeEase", CLOSE_EASE);
        JsonArray skip = spec("a.curve.skipKeys").getAsJsonArray();
        assertEquals(skip.size(), CLOSE_SKIP_KEYS.size());
        skip.forEach(id -> assertTrue(CLOSE_SKIP_KEYS.contains(id.getAsString())));
    }

    @Test
    void schemeAGeometry() {
        eqVec("a.seg.gun", SEG_GUN_FROM, SEG_GUN_TO);
        eqVec("a.seg.raise", SEG_RAISE_FROM, SEG_RAISE_TO);
        eqVec("a.seg.wake", SEG_WAKE_FROM, SEG_WAKE_TO);
        eqVec("a.seg.zoom", SEG_ZOOM_FROM, SEG_ZOOM_TO);
        eq("a.face.width", FACE_WIDTH);
        eq("a.face.maxHeight", FACE_MAX_HEIGHT);
        eq("a.handFovDeg", HAND_FOV_DEG);
        eq("a.read.bottom", READ_BOTTOM);
        eq("a.fill.s", FILL_S);
        eq("a.fill.cy", FILL_CY);
        eq("a.kRead.min", K_READ_MIN);
        eq("a.kRead.max", K_READ_MAX);
        eq("a.kRead.target", K_READ_TARGET);
        eq("a.kRead.fallback", K_READ_FALLBACK);
        eq("a.kRead.minLpPx", K_READ_MIN_LP_PX);
        eq("a.smoothDownscale.halveBelow", SMOOTH_HALVE_BELOW);
        JsonArray keys = spec("a.keys").getAsJsonArray();
        assertEquals(keys.size(), KEYS.size());
        for (int i = 0; i < keys.size(); i++) {
            JsonObject e = keys.get(i).getAsJsonObject();
            Key k = KEYS.get(i);
            assertEquals(e.get("id").getAsString(), k.id());
            near(e.get("p"), k.p(), () -> k.id() + ".p");
            double[] off = TabletVectors.doubles(e.get("off"));
            double[] rot = TabletVectors.doubles(e.get("rot"));
            assertEquals(off[0], k.offX(), 1e-12);
            assertEquals(off[1], k.offY(), 1e-12);
            assertEquals(off[2], k.offZ(), 1e-12);
            assertEquals(rot[0], k.yaw(), 1e-12);
            assertEquals(rot[1], k.pitch(), 1e-12);
            assertEquals(rot[2], k.roll(), 1e-12);
        }
        assertEquals(spec("a.closeNoOvershoot").getAsBoolean(), CLOSE_NO_OVERSHOOT);
        assertEquals("pchip", spec("a.raiseInterp").getAsString(), "Java ports the pchip raise only");
        eq("a.raiseStartSlope", RAISE_START_SLOPE);
        eqEase("a.ease.gun", EASE_GUN);
        eqEase("a.ease.emptyArm", EASE_EMPTY_ARM);
        eqEase("a.ease.wakeSpread", EASE_WAKE_SPREAD);
        eqEase("a.ease.mask", EASE_MASK);
        eqEase("a.ease.zoom", EASE_ZOOM);
    }

    @Test
    void settleGunWakeBackdropSleepHud() {
        eq("a.settle.gunLift.upP", GUN_LIFT_UP_P);
        eq("a.settle.gunLift.toP", GUN_LIFT_TO_P);
        eq("a.settle.gunLift.y", GUN_LIFT_Y);
        eq("a.settle.gunLift.pitch", GUN_LIFT_PITCH);
        eq("a.settle.breath.from", BREATH_FROM);
        eq("a.settle.breath.to", BREATH_TO);
        eq("a.settle.breath.amp", BREATH_AMP);
        eq("a.settle.breath.peakAt", BREATH_PEAK_AT);
        eq("a.settle.zoom.overshoot", ZOOM_OVERSHOOT);
        eq("a.settle.zoom.peakAt", ZOOM_PEAK_AT);
        eqEase("a.settle.zoom.ease", ZOOM_EASE);
        eqEase("a.settle.zoom.settleEase", ZOOM_SETTLE_EASE);
        eq("a.settle.zoom.edgeSafety", ZOOM_EDGE_SAFETY);
        eqVec("a.gun.offset", GUN_OFFSET_X, GUN_OFFSET_Y, GUN_OFFSET_Z);
        eq("a.gun.pitch", GUN_PITCH);
        eq("a.gun.roll", GUN_ROLL);
        eq("a.gun.hideAt", GUN_HIDE_AT);
        eq("a.wake.from", WAKE_FROM);
        eq("a.wake.spreadTo", WAKE_SPREAD_TO);
        eq("a.wake.maskTo", WAKE_MASK_TO);
        eq("a.wake.maskAlpha", WAKE_MASK_ALPHA);
        eq("a.wake.edgeAlpha", WAKE_EDGE_ALPHA);
        eq("a.wake.edgePx", WAKE_EDGE_PX);
        assertEquals(spec("a.wake.shadowFade").getAsBoolean(), WAKE_SHADOW_FADE);
        eq("a.backdrop.from", BACKDROP_FROM);
        eq("a.backdrop.to", BACKDROP_TO);
        eqEase("a.backdrop.ease", BACKDROP_EASE);
        assertEquals(spec("a.backdrop.captureClose").getAsBoolean(), BACKDROP_CAPTURE_CLOSE);
        eq("a.sleep.from", SLEEP_FROM);
        eq("a.sleep.to", SLEEP_TO);
        eq("a.sleep.linePx", SLEEP_LINE_PX);
        eq("a.sleep.alpha", SLEEP_ALPHA);
        eq("a.hud.restoreCrosshairAtCloseP", HUD_RESTORE_CROSSHAIR_AT_CLOSE_P);
        eq("a.hud.restoreAllAtCloseP", HUD_RESTORE_ALL_AT_CLOSE_P);
        List<String> keep = new ArrayList<>();
        spec("a.hud.keep").getAsJsonArray().forEach(e -> keep.add(e.getAsString()));
        assertEquals(keep, HUD_KEEP);
        eq("a.body.fullBright", FULL_BRIGHT);
    }

    @Test
    void hands() {
        eq("a.hands.dxWide", HANDS_DX_WIDE);
        eq("a.hands.dxTall", HANDS_DX_TALL);
        eq("a.hands.dyWide", HANDS_DY_WIDE);
        eq("a.hands.dyTall", HANDS_DY_TALL);
        eq("a.hands.dzWide", HANDS_DZ_WIDE);
        eq("a.hands.dzTall", HANDS_DZ_TALL);
        eq("a.hands.exit.from", HANDS_EXIT_FROM);
        eq("a.hands.exit.to", HANDS_EXIT_TO);
        eq("a.hands.exit.x", HANDS_EXIT_X);
        eq("a.hands.exit.y", HANDS_EXIT_Y);
        eq("a.hands.exit.z", HANDS_EXIT_Z);
        eqEase("a.hands.exit.ease", HANDS_EXIT_EASE);
        eq("a.hands.behindPlate", HANDS_BEHIND_PLATE);
        eq("a.hands.outerY", HANDS_OUTER_Y);
        eq("a.hands.chain.rotY", HANDS_CHAIN_ROT_Y);
        eq("a.hands.chain.rotX", HANDS_CHAIN_ROT_X);
        eq("a.hands.chain.rotZ", HANDS_CHAIN_ROT_Z);
        eqVec("a.hands.chain.translate", HANDS_CHAIN_TX, HANDS_CHAIN_TY, HANDS_CHAIN_TZ);
        assertEquals(spec("a.hands.mirrorYaw").getAsBoolean(), HANDS_MIRROR_YAW);
        eqVec("a.hands.pivot.right", HANDS_PIVOT_RIGHT_X, HANDS_PIVOT_Y, HANDS_PIVOT_Z);
        eqVec("a.hands.pivot.left", HANDS_PIVOT_LEFT_X, HANDS_PIVOT_Y, HANDS_PIVOT_Z);
        eq("a.hands.bobZRot", HANDS_BOB_Z_ROT);
        eqVec("a.hands.armBox.right", armBox(true));
        eqVec("a.hands.armBox.left", armBox(false));
        eq("a.hands.sleeve", HANDS_SLEEVE);
        eq("a.hands.fistFromY", HANDS_FIST_FROM_Y);
        eq("a.hands.hold.minOverlapLp", HOLD_MIN_OVERLAP_LP);
        eq("a.hands.hold.minOverlapY", HOLD_MIN_OVERLAP_Y);
        eq("a.hands.hold.maxHidden", HOLD_MAX_HIDDEN);
        eqVec("a.emptyArm.translate", EMPTY_ARM_TX, EMPTY_ARM_TY, EMPTY_ARM_TZ);
        eq("a.emptyArm.rotY", EMPTY_ARM_ROT_Y);
        eqVec("a.emptyArm.translate2", EMPTY_ARM_T2X, EMPTY_ARM_T2Y, EMPTY_ARM_T2Z);
        eq("a.emptyArm.rotZ", EMPTY_ARM_ROT_Z);
        eq("a.emptyArm.rotX", EMPTY_ARM_ROT_X);
        eq("a.emptyArm.rotY2", EMPTY_ARM_ROT_Y2);
        eqVec("a.emptyArm.translate3", EMPTY_ARM_T3X, EMPTY_ARM_T3Y, EMPTY_ARM_T3Z);
    }

    @Test
    void soundsAndCueTable() {
        eq("a.sfx.volume", SFX_VOLUME);
        eq("a.sfx.noRepeatMs", SFX_NO_REPEAT_MS);
        assertEquals(spec("a.sfx.search.name").getAsString(), SFX_SEARCH);
        eq("a.sfx.search.everyMs", SFX_SEARCH_EVERY_MS);
        eq("a.sfx.search.maxTicks", SFX_SEARCH_MAX_TICKS);
        eq("a.sfx.cut.tauMs", SFX_CUT_TAU_MS);
        eq("a.sfx.cut.stopMs", SFX_CUT_STOP_MS);
        List<String> keep = new ArrayList<>();
        spec("a.sfx.finishKeep").getAsJsonArray().forEach(e -> keep.add(e.getAsString()));
        assertEquals(keep.size(), SFX_FINISH_KEEP.size());
        assertTrue(SFX_FINISH_KEEP.containsAll(keep));
        assertCues(spec("a.sfx.cues").getAsJsonArray(), TabletCues.A3D);
        assertCues(spec("b.sfx").getAsJsonArray(), TabletCues.B2D_TERMINAL);
        assertCues(spec("b.map.sfx").getAsJsonArray(), TabletCues.B2D_MAP);
    }

    @Test
    void patchedCuesDifferFromThePreviewOnlyInTheStowSplit() {
        JsonArray preview = TabletVectors.array("previewCues");
        assertEquals(preview.size() + 1, TabletCues.A3D.size(), "stow became two cues");
        int j = 0;
        for (JsonElement element : preview) {
            JsonObject cue = element.getAsJsonObject();
            if (cue.get("name").getAsString().equals("stow")) {
                near(cue.get("atP"), TabletCues.A3D.get(j).atP(), () -> "gun / item stow keeps 0.52");
                near(0.60D, TabletCues.A3D.get(j + 1).atP(), () -> "empty-hand stow 0.60");
                j += 2;
                continue;
            }
            assertEquals(cue.get("name").getAsString(), TabletCues.A3D.get(j).name());
            j++;
        }
    }

    private static void assertCues(JsonArray expected, List<TabletCue> actual) {
        assertEquals(expected.size(), actual.size(), "cue count");
        for (int i = 0; i < expected.size(); i++) {
            JsonObject e = expected.get(i).getAsJsonObject();
            TabletCue c = actual.get(i);
            String what = "cue " + i + " " + c.name();
            assertEquals(e.get("name").getAsString(), c.name(), what);
            assertEquals(e.get("dir").getAsString(), c.dir().previewName(), what);
            near(TabletVectors.num(e, "atP"), c.atP(), () -> what + " atP");
            if (e.has("startRange")) {
                double[] range = TabletVectors.doubles(e.get("startRange"));
                assertEquals(range[0], c.startRange()[0], 1e-12, what);
                assertEquals(range[1], c.startRange()[1], 1e-12, what);
            } else {
                assertEquals(null, c.startRange(), what);
            }
            assertEquals(e.has("link") && e.get("link").getAsBoolean(), c.link(), what);
            for (TabletHand hand : TabletHand.values()) {
                boolean plays;
                if (!e.has("hand")) {
                    plays = true;
                } else if (e.get("hand").isJsonArray()) {
                    plays = false;
                    for (JsonElement h : e.get("hand").getAsJsonArray()) {
                        plays |= h.getAsString().equals(hand.previewName());
                    }
                } else {
                    plays = e.get("hand").getAsString().equals(hand.previewName());
                }
                assertEquals(plays, c.playsFor(hand), what + " for " + hand);
            }
        }
    }

    @Test
    void schemeBQuickInputAndReopen() {
        eqPts("b.open", bOpenPts());
        eqPts("b.close", bClosePts(true));
        eq("b.liftEnd", B_LIFT_END);
        eqEase("b.ease.lift", B_EASE_LIFT);
        eq("b.scan.from", B_SCAN_FROM);
        eq("b.scan.to", B_SCAN_TO);
        eq("b.scan.alpha", B_SCAN_ALPHA);
        eq("b.backdropTo", B_BACKDROP_TO);
        eq("b.closeGlassAlpha", B_CLOSE_GLASS_ALPHA);
        eq("b.map.K0", BM_K0);
        eq("b.map.startFrac", BM_START_FRAC);
        eq("b.map.liftEnd", BM_LIFT_END);
        eq("b.map.zoomFrom", BM_ZOOM_FROM);
        eqEase("b.map.ease.lift", BM_EASE_LIFT);
        eqEase("b.map.ease.zoom", BM_EASE_ZOOM);
        eq("b.map.sleep.from", BM_SLEEP_FROM);
        eq("b.map.sleep.span", BM_SLEEP_SPAN);
        eq("b.map.scan.from", BM_SCAN_FROM);
        eq("b.map.scan.span", BM_SCAN_SPAN);
        eq("b.map.closeGlassAlpha", BM_CLOSE_GLASS_ALPHA);
        eqPts("b.map.close", bClosePts(false));
        assertEquals("topLeft", spec("b.map.contentAnchor").getAsString());
        eq("quick.from", QUICK_FROM);
        eqPts("quick.open", quickOpenPts());
        eqPts("quick.close", quickClosePts());
        eqEase("quick.ease", QUICK_EASE);
        eq("pStar.A3D", P_STAR_A);
        eq("pStar.B2D", P_STAR_B);
        eq("pStar.QUICK", P_STAR_QUICK);
        eq("reopen.windowMs", REOPEN_WINDOW_MS);
        eq("reopen.scale", REOPEN_SCALE);
        eq("keyFinish.ms", KEY_FINISH_MS);
        eqEase("keyFinish.ease", KEY_FINISH_EASE);
        List<String> whileOpening = new ArrayList<>();
        spec("interruptsWhileOpening").getAsJsonArray().forEach(e -> whileOpening.add(e.getAsString()));
        for (TabletMotion.Interrupt cause : TabletMotion.Interrupt.values()) {
            assertEquals(whileOpening.contains(cause.name().toLowerCase(java.util.Locale.ROOT)),
                    cause.whileOpening(), cause.name());
        }
    }

    @Test
    void colorsAndD2ConstantsMatchTheShell() {
        eq("theme.LIGHT", LIGHT & 0xFFFFFFFFL);
        eq("theme.BACKLIGHT", BACKLIGHT & 0xFFFFFFFFL);
        assertEquals(TacticalBoardTheme.LIGHT, LIGHT, "LIGHT is the A palette's light ink");
        eq("d2.GLASS", DeviceArt.GLASS & 0xFFFFFFFFL);
        eq("d2.WORLD_DIM", DeviceArt.WORLD_DIM & 0xFFFFFFFFL);
        eq("d2.LED_OFF", DeviceArt.LED_OFF & 0xFFFFFFFFL);
        eq("d2.SHADOW", DeviceArt.SHADOW & 0xFFFFFFFFL);
        assertEquals(DeviceArt.GLASS, GLASS);
        assertEquals(DeviceArt.WORLD_DIM, WORLD_DIM);
        assertEquals(DeviceArt.LED_OFF, LED_OFF);
        assertEquals(DeviceArt.SHADOW, SHADOW);
        eq("d2.thickU", D2_THICK_U);
        eq("d2.density.tightW", 400);
        assertEquals(TacticalShellLayout.Density.COMPACT, TacticalShellLayout.density(399, 300));
        assertEquals(TacticalShellLayout.Density.COMPACT, TacticalShellLayout.density(500, 279));
        assertEquals(TacticalShellLayout.Density.ROOMY, TacticalShellLayout.density(800, 500));
        assertEquals(TacticalShellLayout.Density.STANDARD, TacticalShellLayout.density(799, 500));
        Map<String, TacticalShellLayout.Density> classes = Map.of("COMPACT", TacticalShellLayout.Density.COMPACT,
                "STANDARD", TacticalShellLayout.Density.STANDARD, "ROOMY", TacticalShellLayout.Density.ROOMY);
        classes.forEach((name, density) -> {
            TacticalShellLayout.DeviceMetrics k = TacticalShellLayout.DeviceMetrics.of(density);
            String base = "d2.consts." + name + ".";
            eq(base + "mx", k.marginX());
            eq(base + "mt", k.marginTop());
            eq(base + "mb", k.marginBottom());
            eq(base + "side", k.side());
            eq(base + "top", k.top());
            eq(base + "bot", k.bottom());
            eq(base + "glass", k.glassInset());
            eq(base + "status", k.statusHeight());
            eq(base + "bump", k.bump());
            eq(base + "corner", k.corner());
            eq(base + "rad", k.radius());
        });
    }

    @Test
    void phaseTablesMatchThePreview() {
        for (String key : PHASES.keySet()) {
            JsonObject expected = spec("phases." + key).getAsJsonObject();
            Phases actual = PHASES.get(key);
            assertSegs(expected.get("open"), actual.open(), key + ".open");
            assertSegs(expected.get("close"), actual.close(), key + ".close");
            assertEquals(expected.get("openEnd").getAsString(), actual.openEnd(), key);
            assertEquals(expected.get("closeEnd").getAsString(), actual.closeEnd(), key);
            if (expected.has("openEmpty")) {
                assertSegs(expected.get("openEmpty"), actual.openEmpty(), key + ".openEmpty");
                assertSegs(expected.get("closeEmpty"), actual.closeEmpty(), key + ".closeEmpty");
            } else {
                assertEquals(null, actual.openEmpty(), key);
            }
        }
    }

    private static void assertSegs(JsonElement expected, List<PhaseSeg> actual, String what) {
        JsonArray array = expected.getAsJsonArray();
        assertEquals(array.size(), actual.size(), what);
        for (int i = 0; i < array.size(); i++) {
            JsonArray e = array.get(i).getAsJsonArray();
            assertEquals(e.get(0).getAsString(), actual.get(i).name(), what);
            assertEquals(e.get(1).getAsDouble(), actual.get(i).from(), 1e-12, what);
            assertEquals(e.get(2).getAsDouble(), actual.get(i).to(), 1e-12, what);
        }
    }
}
