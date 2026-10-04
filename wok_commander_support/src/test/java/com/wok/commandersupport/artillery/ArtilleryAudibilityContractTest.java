package com.wok.commandersupport.artillery;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wok.commandersupport.registry.CommanderSupportSounds;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gun reports must be heard, by friend and foe, at least {@code batteryOffset + dangerRadius + 64}
 * blocks from the virtual battery.
 *
 * <p>Two limits apply. The server sends the packet only within the event's fixed range
 * ({@link CommanderSupportSounds}). The client then attenuates linearly over
 * {@code max(volume, 1) x attenuation_distance} blocks, where volume is the playback volume times
 * this module's wrapper volume in sounds.json, and the attenuation distance is the wrapped
 * third-party sound's own: a {@code "type": "event"} entry cannot override it.</p>
 */
class ArtilleryAudibilityContractTest {
    private static final String SOUNDS = "/assets/wok_commander_support/sounds.json";
    /**
     * Superb Warfare 0.8.9 declares {@code plz_05_veryfar} and {@code mk_42_veryfar} without an
     * {@code attenuation_distance}, so the vanilla default of 16 blocks applies to both.
     */
    private static final double WRAPPED_REPORT_ATTENUATION = 16.0D;
    /** Superb Warfare 0.8.9 {@code shell_fly}. */
    private static final double SBW_SHELL_FLY_ATTENUATION = 96.0D;
    /** Create Big Cannons 5.11.4 {@code shell_flying}. */
    private static final double CBC_SHELL_FLYING_ATTENUATION = 256.0D;
    private static final List<ArtilleryProfile> ALL = List.of(ArtilleryProfile.HOWITZER_3ROUND,
            ArtilleryProfile.RAPID_105, ArtilleryProfile.FIVE_ROUND_105);

    @Test
    void serverBroadcastReachesPastTheDangerArea() {
        for (ArtilleryProfile profile : ALL) {
            double required = requiredReach(profile);
            assertTrue(CommanderSupportSounds.HOWITZER_REPORT_RANGE >= required,
                    profile.id() + " report broadcast " + CommanderSupportSounds
                            .HOWITZER_REPORT_RANGE + " < " + required);
        }
        assertEquals(302.0D, requiredReach(ArtilleryProfile.HOWITZER_3ROUND));
        assertEquals(210.0D, requiredReach(ArtilleryProfile.RAPID_105));
        assertEquals(210.0D, requiredReach(ArtilleryProfile.FIVE_ROUND_105));
    }

    @Test
    void clientsHearTheReportsPastTheDangerArea() throws IOException {
        JsonObject manifest = manifest();
        for (ArtilleryProfile profile : ALL) {
            JsonObject sound = wrapped(manifest, reportEvent(profile.caliber()),
                    wrappedReport(profile.caliber()));
            double reach = Math.max(1.0D, ArtilleryProfile.REPORT_VOLUME * volume(sound))
                    * WRAPPED_REPORT_ATTENUATION;
            double required = requiredReach(profile);
            assertTrue(reach >= required,
                    profile.id() + " report reach " + reach + " < " + required);
            // The battery stands 40 blocks above the ground: the far edge is a slant away.
            assertTrue(reach >= Math.hypot(profile.batteryOffset() + profile.dangerRadius(),
                    ArtilleryProfile.BATTERY_HEIGHT));
        }
    }

    @Test
    void everyoneTheRoundCanHurtHearsItsWhistle() throws IOException {
        JsonObject manifest = manifest();
        JsonObject sbw = wrapped(manifest, "artillery_incoming_sbw", "superbwarfare:shell_fly");
        JsonObject cbc = wrapped(manifest, "artillery_incoming_cbc",
                "createbigcannons:shell_flying");
        double sbwReach = Math.max(1.0D, ArtilleryProfile.INCOMING_VOLUME * volume(sbw))
                * SBW_SHELL_FLY_ATTENUATION;
        double cbcReach = Math.max(1.0D, ArtilleryProfile.INCOMING_VOLUME * volume(cbc))
                * CBC_SHELL_FLYING_ATTENUATION;
        for (ArtilleryProfile profile : ALL) {
            // Anyone inside the danger area around the impact, 20 blocks below the whistle.
            double farthest = Math.hypot(profile.dangerRadius(), ArtilleryProfile.WHISTLE_HEIGHT);
            assertTrue(sbwReach >= farthest, profile.id() + " SBW whistle " + sbwReach);
            assertTrue(cbcReach >= farthest, profile.id() + " CBC whistle " + cbcReach);
            assertTrue(CommanderSupportSounds.ARTILLERY_INCOMING_RANGE >= farthest,
                    profile.id() + " whistle broadcast "
                            + CommanderSupportSounds.ARTILLERY_INCOMING_RANGE);
        }
    }

    private static double requiredReach(ArtilleryProfile profile) {
        return profile.batteryOffset() + profile.dangerRadius()
                + ArtilleryProfile.REPORT_AUDIBLE_MARGIN;
    }

    /** Same mapping as {@code ArtillerySounds.report}. */
    private static String reportEvent(ArtilleryProfile.Caliber caliber) {
        return switch (caliber) {
            case HOWITZER_155 -> "howitzer_155_report";
            case HOWITZER_105 -> "howitzer_105_report";
        };
    }

    private static String wrappedReport(ArtilleryProfile.Caliber caliber) {
        return switch (caliber) {
            case HOWITZER_155 -> "superbwarfare:plz_05_veryfar";
            case HOWITZER_105 -> "superbwarfare:mk_42_veryfar";
        };
    }

    private static JsonObject wrapped(JsonObject manifest, String event, String target) {
        JsonObject entry = manifest.getAsJsonObject(event);
        assertNotNull(entry, "missing sound event " + event);
        assertEquals(1, entry.getAsJsonArray("sounds").size(), event);
        JsonObject sound = entry.getAsJsonArray("sounds").get(0).getAsJsonObject();
        assertEquals("event", sound.get("type").getAsString(),
                event + " must wrap the third-party event by id, not bundle its audio");
        assertEquals(target, sound.get("name").getAsString());
        return sound;
    }

    private static double volume(JsonObject sound) {
        JsonElement volume = sound.get("volume");
        return volume == null ? 1.0D : volume.getAsDouble();
    }

    private static JsonObject manifest() throws IOException {
        try (InputStream stream = ArtilleryAudibilityContractTest.class
                .getResourceAsStream(SOUNDS)) {
            assertNotNull(stream, "missing resource " + SOUNDS);
            return JsonParser.parseString(new String(stream.readAllBytes(),
                    StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
