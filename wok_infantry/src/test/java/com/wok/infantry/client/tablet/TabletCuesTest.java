package com.wok.infantry.client.tablet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.wok.infantry.client.tablet.TabletVectors.near;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sound cue tables (three hands × C1 / old close) against {@code vectors.cueTables}, and the
 * stow-by-hand patch (IMPL_PLAN D3): the empty hand's main hit now lands within one frame of K0.
 */
class TabletCuesTest {
    @Test
    void cueTimingMatchesThePreviewForEveryHand() {
        for (JsonElement element : TabletVectors.array("cueTables")) {
            JsonObject table = element.getAsJsonObject();
            TabletHand hand = TabletHand.byPreviewName(table.get("hand").getAsString());
            boolean capture = table.get("capture").getAsBoolean();
            TabletTimeMap open = TabletMotion.openMap(TabletPath.A3D, hand, false);
            TabletTimeMap close = TabletMotion.closeMap(TabletPath.A3D, hand, capture, TabletScreenKind.SQUAD);
            near(table.get("openDur"), open.durMs(), () -> hand + " open");
            near(table.get("closeDur"), close.durMs(), () -> hand + " close");
            List<TabletCue> cues = new ArrayList<>();
            for (TabletCue cue : TabletCues.A3D) {
                if (cue.playsFor(hand)) {
                    cues.add(cue);
                }
            }
            assertEquals(table.getAsJsonArray("cues").size(), cues.size(), hand + " " + capture);
            for (int i = 0; i < cues.size(); i++) {
                JsonObject e = table.getAsJsonArray("cues").get(i).getAsJsonObject();
                TabletCue cue = cues.get(i);
                String what = hand + " " + (capture ? "C1" : "old") + " " + cue.name() + " " + cue.dir();
                assertEquals(e.get("name").getAsString(), cue.name(), what);
                assertEquals(e.get("dir").getAsString(), cue.dir().previewName(), what);
                near(TabletVectors.num(e, "atP"), cue.atP(), () -> what + " atP");
                TabletTimeMap map = cue.dir() == TabletCue.Dir.OPEN ? open : close;
                double fire = cue.hasAtP() ? map.invert(cue.atP()) : Double.NaN;
                near(TabletVectors.num(e, "fireMs"), fire, () -> what + " fireMs");
                assertEquals(e.get("link").getAsBoolean(), cue.link(), what);
                if (TabletVectors.isNull(e.get("startRange"))) {
                    assertEquals(null, cue.startRange(), what);
                } else {
                    double[] range = TabletVectors.doubles(e.get("startRange"));
                    assertEquals(range[0], cue.startRange()[0], 1e-12, what);
                    assertEquals(range[1], cue.startRange()[1], 1e-12, what);
                }
            }
        }
    }

    @Test
    void stowHitsLandOnK0ForEveryHand() {
        JsonObject hit = TabletVectors.object("sounds").getAsJsonObject("hitMs");
        double stowHit = hit.get("stow").getAsDouble();
        double k0 = TabletAnimationModel.KEYS.get(0).p();
        for (TabletHand hand : TabletHand.values()) {
            TabletTimeMap close = TabletMotion.closeMap(TabletPath.A3D, hand, true, TabletScreenKind.SQUAD);
            for (TabletCue cue : TabletCues.A3D) {
                if (!cue.name().equals("stow") || !cue.playsFor(hand)) {
                    continue;
                }
                double error = close.invert(cue.atP()) + stowHit - close.invert(k0);
                assertTrue(Math.abs(error) <= TabletAnimationModel.FRAME_MS,
                        hand + " stow hit lands " + error + " ms from K0");
            }
        }
        JsonObject patch = TabletVectors.object("meta").getAsJsonArray("javaPatches").get(0).getAsJsonObject();
        assertEquals("stow-by-hand", patch.get("id").getAsString());
        near(patch.get("emptyAtP"), TabletCues.STOW_EMPTY_AT_P, () -> "empty stow atP");
        for (JsonElement element : patch.getAsJsonArray("alignment")) {
            JsonObject a = element.getAsJsonObject();
            TabletHand hand = TabletHand.byPreviewName(a.get("hand").getAsString());
            TabletTimeMap close = TabletMotion.closeMap(TabletPath.A3D, hand, true, TabletScreenKind.SQUAD);
            double atP = a.get("atP").getAsDouble();
            near(a.get("fireMs"), close.invert(atP), () -> hand + " fire");
            near(a.get("k0Ms"), close.invert(k0), () -> hand + " K0");
        }
    }

    @Test
    void soundNamesMatchSoundsJson() {
        Set<String> keys = new HashSet<>(TabletVectors.object("sounds").getAsJsonObject("json").keySet());
        Set<String> names = new HashSet<>();
        for (String name : TabletCues.SOUND_NAMES) {
            names.add("tablet." + name);
        }
        assertEquals(keys, names, "twelve sounds, named as in sounds.json");
        for (List<TabletCue> table : List.of(TabletCues.A3D, TabletCues.B2D_TERMINAL, TabletCues.B2D_MAP)) {
            for (TabletCue cue : table) {
                assertTrue(TabletCues.SOUND_NAMES.contains(cue.name()), cue.name());
            }
        }
        assertTrue(TabletCues.SOUND_NAMES.contains(TabletAnimationModel.SFX_SEARCH));
    }

    @Test
    void handsPickTheirOwnSounds() {
        // guns sling and shoulder (holster / raise); empty hands and items only rustle
        for (TabletHand hand : TabletHand.values()) {
            Set<String> names = new HashSet<>();
            for (TabletCue cue : TabletCues.A3D) {
                if (cue.playsFor(hand)) {
                    names.add(cue.name());
                }
            }
            assertEquals(hand == TabletHand.GUN, names.contains("holster"), hand.name());
            assertEquals(hand == TabletHand.GUN, names.contains("raise"), hand.name());
            assertEquals(hand != TabletHand.GUN, names.contains("rustle"), hand.name());
        }
        // B plays electronic sounds only
        for (TabletCue cue : TabletCues.B2D_TERMINAL) {
            assertTrue(Set.of("boot", "ready", "sleep").contains(cue.name()), cue.name());
        }
        assertEquals(List.of(), TabletCues.forPath(TabletPath.QUICK, TabletScreenKind.SQUAD));
        assertEquals(List.of(), TabletCues.forPath(TabletPath.OFF, TabletScreenKind.SQUAD));
        assertEquals(TabletCues.B2D_MAP, TabletCues.forPath(TabletPath.B2D, TabletScreenKind.FULLSCREEN));
    }
}
