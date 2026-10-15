package com.wok.commandersupport.airstrike;

import com.google.gson.JsonArray;
import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.commandersupport.artillery.ArtilleryProfile;
import com.wok.commandersupport.registry.CommanderSupportSounds;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundAssetContractTest {
    private static final String SOUNDS = "/assets/wok_commander_support/sounds.json";
    private static final String ZH_CN = "/assets/wok_commander_support/lang/zh_cn.json";
    private static final String EN_US = "/assets/wok_commander_support/lang/en_us.json";

    /** Our wrapper event and the third-party event it plays, referenced by registry id only. */
    private static final Map<String, String> WRAPPED_EVENTS = Map.of(
            "howitzer_155_report", "superbwarfare:plz_05_veryfar",
            "howitzer_105_report", "superbwarfare:mk_42_veryfar",
            "artillery_incoming_cbc", "createbigcannons:shell_flying",
            "artillery_incoming_sbw", "superbwarfare:shell_fly",
            "recon_drone_engine", "superbwarfare:ju_87_engine",
            "recon_drone_destroyed", "superbwarfare:explosion_air");
    /**
     * Attenuation distances of the wrapped events as shipped in superbwarfare 0.8.9 and
     * createbigcannons 5.11.4 (16 is the vanilla default when sounds.json sets none).
     */
    private static final Map<String, Integer> WRAPPED_ATTENUATION = Map.of(
            "superbwarfare:plz_05_veryfar", 16,
            "superbwarfare:mk_42_veryfar", 16,
            "createbigcannons:shell_flying", 256,
            "superbwarfare:shell_fly", 96,
            "superbwarfare:ju_87_engine", 256,
            "superbwarfare:explosion_air", 16);
    /** Report event of each gun class. */
    private static final Map<ArtilleryProfile.Caliber, String> REPORT_EVENTS = Map.of(
            ArtilleryProfile.Caliber.HOWITZER_155, "howitzer_155_report",
            ArtilleryProfile.Caliber.HOWITZER_105, "howitzer_105_report");
    private static final double DRONE_SCAN_RADIUS = 96.0D;
    private static final double DRONE_ORBIT_RADIUS = 24.0D;
    private static final double DRONE_MIN_HEIGHT_ABOVE_TARGET = 64.0D;

    @Test
    void soundManifestRegistersBothJdamFieldRecordings() throws IOException {
        byte[] manifest = resource(SOUNDS);
        String json = new String(manifest, StandardCharsets.UTF_8);

        assertTrue(json.contains("jdam_f15_approach"));
        assertTrue(json.contains("jdam_bomb_tail"));
    }

    @Test
    void jdamRecordingsArePackagedAsNonEmptyOggVorbisAssets()
            throws IOException {
        assertOgg("/assets/wok_commander_support/sounds/jdam_f15_approach.ogg");
        assertOgg("/assets/wok_commander_support/sounds/jdam_bomb_tail.ogg");
    }

    @Test
    void pavewayEventsReuseTheF15RecordingsAtAHigherPitch() throws IOException {
        JsonObject manifest = json(SOUNDS);

        assertReusedRecording(manifest, "paveway_f16_approach", "jdam_f15_approach");
        assertReusedRecording(manifest, "paveway_bomb_tail", "jdam_bomb_tail");
    }

    @Test
    void everySubtitleIsTranslatedInBothLanguages() throws IOException {
        JsonObject manifest = json(SOUNDS);
        JsonObject zh = json(ZH_CN);
        JsonObject en = json(EN_US);

        assertEquals(zh.keySet(), en.keySet(), "zh_cn 与 en_us 的键必须一一对应");
        for (Map.Entry<String, JsonElement> event : manifest.entrySet()) {
            String subtitle = event.getValue().getAsJsonObject()
                    .get("subtitle").getAsString();
            assertFalse(zh.get(subtitle) == null
                    || zh.get(subtitle).getAsString().isBlank(), subtitle);
            assertFalse(en.get(subtitle) == null
                    || en.get(subtitle).getAsString().isBlank(), subtitle);
        }
    }

    @Test
    void missingDesignationNoticeLeavesTheRefundToTheCoreChatLine() throws IOException {
        // The action bar fires before the core's compare-and-set refund; only the core's chat
        // notice knows whether the cooldown actually came back.
        String key = "message.wok_commander_support.paveway_no_designation";
        String zh = json(ZH_CN).get(key).getAsString();
        String en = json(EN_US).get(key).getAsString();

        assertTrue(zh.contains(F16PavewayProvider.NO_DESIGNATION_REASON), zh);
        assertFalse(zh.contains("冷却") || zh.contains("返还"), zh);
        assertFalse(en.toLowerCase(Locale.ROOT).contains("cooldown"), en);
    }

    @Test
    void approachSoundsReachTheGroundAroundTheTarget() throws IOException {
        JsonObject manifest = json(SOUNDS);
        // Client attenuation is linear over max(volume, 1) x attenuation_distance blocks.
        double pavewayReach = attenuationDistance(manifest, "paveway_f16_approach")
                * Math.max(1.0F, F16PavewayProvider.APPROACH_VOLUME);
        double jdamReach = attenuationDistance(manifest, "jdam_f15_approach")
                * Math.max(1.0F, MillenniumJdamProvider.APPROACH_VOLUME);

        // F-16C: from a diagonal release point to the far edge of the permit on the ground.
        double releaseHorizontal = Math.hypot(PavewayGuidancePlan.SPAWN_OFFSET_X,
                PavewayGuidancePlan.SPAWN_OFFSET_Z);
        double pavewayFarthest = Math.hypot(releaseHorizontal
                        + WokCommanderSupportMod.F16C_PAVEWAY_GUIDANCE_RADIUS,
                PavewayGuidancePlan.SPAWN_OFFSET_Y);
        assertTrue(pavewayReach >= pavewayFarthest,
                "F-16C approach must be audible across the permit: " + pavewayReach
                        + " < " + pavewayFarthest);
        // JDAM: from 200 blocks overhead to the edge of its 32-block area.
        double jdamFarthest = Math.hypot(JdamFlightPlan.VERTICAL_OFFSET,
                WokCommanderSupportMod.MILLENNIUM_JDAM_RADIUS);
        assertTrue(jdamReach >= jdamFarthest,
                "JDAM approach must be audible around the target: " + jdamReach
                        + " < " + jdamFarthest);
        // The server only sends the packet within the fixed 256-block event range.
        assertTrue(pavewayFarthest <= 256.0D && jdamFarthest <= 256.0D);
    }

    @Test
    void artilleryAndDroneEventsWrapThirdPartyEventsByRegistryId() throws IOException {
        JsonObject manifest = json(SOUNDS);
        Set<String> events = new HashSet<>(Set.of("jdam_f15_approach", "jdam_bomb_tail",
                "paveway_f16_approach", "paveway_bomb_tail"));
        events.addAll(WRAPPED_EVENTS.keySet());

        // Same names as the RegistryObjects in CommanderSupportSounds.
        assertEquals(events, manifest.keySet());
        for (Map.Entry<String, String> wrapped : WRAPPED_EVENTS.entrySet()) {
            String event = wrapped.getKey();
            JsonObject entry = manifest.getAsJsonObject(event);
            assertNotNull(entry, "missing sound event " + event);
            assertEquals("subtitles.wok_commander_support." + event,
                    entry.get("subtitle").getAsString());
            JsonArray sounds = entry.getAsJsonArray("sounds");
            assertEquals(1, sounds.size(), event);
            JsonObject sound = sounds.get(0).getAsJsonObject();
            assertEquals("event", sound.get("type").getAsString(), event);
            assertEquals(wrapped.getValue(), sound.get("name").getAsString(), event);
            // A wrapper keeps the wrapped sound's attenuation distance; an override here would
            // be silently ignored by the client, so it must not suggest otherwise.
            assertNull(sound.get("attenuation_distance"), event);
            // Only registry ids are referenced: no third-party audio is packaged.
            assertNull(SoundAssetContractTest.class.getResource(
                    "/assets/wok_commander_support/sounds/" + event + ".ogg"), event);
        }
    }

    @Test
    void howitzerReportsReachPastTheFarEdgeOfTheDangerAreaForEveryone() throws IOException {
        JsonObject manifest = json(SOUNDS);
        // Specification: 155 mm battery 180 blocks behind the target, 105 mm 120 blocks, both
        // 40 blocks above the ground near it; heard 64 blocks past the far edge, at volume 1.
        assertEquals(180.0D, ArtilleryProfile.Caliber.HOWITZER_155.batteryOffset());
        assertEquals(120.0D, ArtilleryProfile.Caliber.HOWITZER_105.batteryOffset());
        assertEquals(40.0D, ArtilleryProfile.BATTERY_HEIGHT);
        assertEquals(64.0D, ArtilleryProfile.REPORT_AUDIBLE_MARGIN);
        assertEquals(1.0F, ArtilleryProfile.REPORT_VOLUME);

        for (ArtilleryProfile profile : WokCommanderSupportMod.artilleryProfiles()) {
            String event = REPORT_EVENTS.get(profile.caliber());
            double dangerRadius = profile.definition().radius();
            // From the virtual battery to a listener on the ground beyond the danger area.
            double required = Math.hypot(profile.batteryOffset() + dangerRadius
                            + ArtilleryProfile.REPORT_AUDIBLE_MARGIN,
                    ArtilleryProfile.BATTERY_HEIGHT);
            double reach = clientReach(manifest, event, ArtilleryProfile.REPORT_VOLUME);

            assertTrue(reach >= required, profile.id() + " " + event + " client reach "
                    + reach + " < " + required);
            assertTrue(CommanderSupportSounds.HOWITZER_REPORT_RANGE >= required,
                    profile.id() + " " + event + " broadcast "
                            + CommanderSupportSounds.HOWITZER_REPORT_RANGE + " < " + required);
        }
        assertEquals("howitzer_155_report",
                REPORT_EVENTS.get(ArtilleryProfile.HOWITZER_3ROUND.caliber()));
        assertEquals("howitzer_105_report",
                REPORT_EVENTS.get(ArtilleryProfile.RAPID_105.caliber()));
        assertEquals("howitzer_105_report",
                REPORT_EVENTS.get(ArtilleryProfile.FIVE_ROUND_105.caliber()));
    }

    @Test
    void incomingWhistlesAreHeardAcrossTheDangerArea() throws IOException {
        JsonObject manifest = json(SOUNDS);
        assertEquals(0.7F, ArtilleryProfile.INCOMING_VOLUME, 1.0E-6F);
        assertEquals(20.0D, ArtilleryProfile.WHISTLE_HEIGHT);

        for (ArtilleryProfile profile : WokCommanderSupportMod.artilleryProfiles()) {
            // Played above an impact; anyone in the danger area must hear it.
            double required = Math.hypot(profile.definition().radius(),
                    ArtilleryProfile.WHISTLE_HEIGHT);
            for (String event : new String[]{"artillery_incoming_cbc",
                    "artillery_incoming_sbw"}) {
                double reach = clientReach(manifest, event, ArtilleryProfile.INCOMING_VOLUME);
                assertTrue(reach >= required, profile.id() + " " + event + ": " + reach
                        + " < " + required);
                assertTrue(CommanderSupportSounds.ARTILLERY_INCOMING_RANGE >= required,
                        profile.id() + " " + event);
            }
        }
    }

    @Test
    void droneSoundsReachTheGroundUnderTheOrbit() throws IOException {
        JsonObject manifest = json(SOUNDS);
        // Shoot-down: from the lowest orbit, heard across the whole scanned circle.
        double scanEdge = Math.hypot(DRONE_SCAN_RADIUS + DRONE_ORBIT_RADIUS,
                DRONE_MIN_HEIGHT_ABOVE_TARGET);
        double destroyedReach = clientReach(manifest, "recon_drone_destroyed", 1.0F);

        assertTrue(destroyedReach >= scanEdge, destroyedReach + " < " + scanEdge);
        assertTrue(CommanderSupportSounds.RECON_DRONE_DESTROYED_RANGE >= scanEdge);
        assertTrue(CommanderSupportSounds.RECON_DRONE_DESTROYED_RANGE <= destroyedReach,
                "the broadcast radius never exceeds what the client can hear");
        // Engine: about "flight height + 64" blocks; the client loop does its own attenuation.
        assertTrue(CommanderSupportSounds.RECON_DRONE_ENGINE_RANGE
                >= DRONE_MIN_HEIGHT_ABOVE_TARGET + 64.0D);
        assertTrue(clientReach(manifest, "recon_drone_engine", 1.0F)
                >= DRONE_MIN_HEIGHT_ABOVE_TARGET + 64.0D);
    }

    /**
     * Client reach of a wrapper event: {@code max(play volume x wrapper volume, 1)} times the
     * wrapped sound's own attenuation distance.
     */
    private static double clientReach(JsonObject manifest, String event, float playVolume) {
        JsonObject sound = manifest.getAsJsonObject(event).getAsJsonArray("sounds")
                .get(0).getAsJsonObject();
        float wrapperVolume = sound.has("volume") ? sound.get("volume").getAsFloat() : 1.0F;
        Integer attenuation = WRAPPED_ATTENUATION.get(sound.get("name").getAsString());
        assertNotNull(attenuation, "unknown wrapped event " + sound.get("name"));
        return Math.max(playVolume * wrapperVolume, 1.0F) * attenuation;
    }

    private static double attenuationDistance(JsonObject manifest, String event) {
        JsonObject sound = manifest.getAsJsonObject(event).getAsJsonArray("sounds")
                .get(0).getAsJsonObject();
        assertNotNull(sound.get("attenuation_distance"),
                event + " must override the default 16-block attenuation");
        return sound.get("attenuation_distance").getAsDouble();
    }

    private static void assertReusedRecording(JsonObject manifest, String event,
                                              String recording) throws IOException {
        JsonObject entry = manifest.getAsJsonObject(event);
        assertNotNull(entry, "missing sound event " + event);
        assertEquals("subtitles.wok_commander_support." + event,
                entry.get("subtitle").getAsString());
        JsonArray sounds = entry.getAsJsonArray("sounds");
        assertEquals(1, sounds.size());
        JsonObject sound = sounds.get(0).getAsJsonObject();
        assertEquals("wok_commander_support:" + recording, sound.get("name").getAsString());
        assertEquals(1.1F, sound.get("pitch").getAsFloat(), 1.0E-6F);
        assertOgg("/assets/wok_commander_support/sounds/" + recording + ".ogg");
    }

    private static void assertOgg(String path) throws IOException {
        byte[] bytes = resource(path);
        assertTrue(bytes.length > 8_000, path + " is unexpectedly small");
        assertArrayEquals(new byte[]{'O', 'g', 'g', 'S'},
                new byte[]{bytes[0], bytes[1], bytes[2], bytes[3]});
    }

    private static JsonObject json(String path) throws IOException {
        return JsonParser.parseString(new String(resource(path), StandardCharsets.UTF_8))
                .getAsJsonObject();
    }

    private static byte[] resource(String path) throws IOException {
        try (InputStream stream = SoundAssetContractTest.class
                .getResourceAsStream(path)) {
            assertNotNull(stream, "missing resource " + path);
            return stream.readAllBytes();
        }
    }
}
