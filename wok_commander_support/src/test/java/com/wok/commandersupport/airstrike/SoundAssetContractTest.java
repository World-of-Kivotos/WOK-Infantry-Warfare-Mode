package com.wok.commandersupport.airstrike;

import com.google.gson.JsonArray;
import com.wok.commandersupport.WokCommanderSupportMod;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundAssetContractTest {
    private static final String SOUNDS = "/assets/wok_commander_support/sounds.json";
    private static final String ZH_CN = "/assets/wok_commander_support/lang/zh_cn.json";
    private static final String EN_US = "/assets/wok_commander_support/lang/en_us.json";

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
