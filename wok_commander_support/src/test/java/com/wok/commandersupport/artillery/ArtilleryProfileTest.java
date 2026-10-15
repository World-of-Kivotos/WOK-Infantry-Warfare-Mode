package com.wok.commandersupport.artillery;

import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.infantry.support.SupportDefinition;
import com.wok.infantry.support.SupportTargetMode;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Parameter table of the three howitzer barrages, as specified for 0.1.0-beta.3. */
class ArtilleryProfileTest {
    private static final List<ArtilleryProfile> ALL = List.of(ArtilleryProfile.HOWITZER_3ROUND,
            ArtilleryProfile.RAPID_105, ArtilleryProfile.FIVE_ROUND_105);

    @Test
    void threeRoundBarrageDefinitionMatchesTheTable() {
        assertDefinition(ArtilleryProfile.HOWITZER_3ROUND,
                WokCommanderSupportMod.HOWITZER_3ROUND_ID,
                "三连发榴弹炮击", "三连发炮击", 3_600L, 170L, 46, 58.0D);
    }

    @Test
    void rapid105DefinitionMatchesTheTable() {
        assertDefinition(ArtilleryProfile.RAPID_105,
                WokCommanderSupportMod.HOWITZER_105_RAPID_ID,
                "快速三连发105毫米榴弹炮打击", "105快速三连发", 2_400L, 30L, 18, 26.0D);
    }

    @Test
    void fiveRound105DefinitionMatchesTheTable() {
        assertDefinition(ArtilleryProfile.FIVE_ROUND_105,
                WokCommanderSupportMod.HOWITZER_105_5ROUND_ID,
                "五连发105毫米榴弹炮打击", "105五连发", 6_000L, 90L, 32, 26.0D);
    }

    @Test
    void firePatternsMatchTheTable() {
        assertPattern(ArtilleryProfile.HOWITZER_3ROUND, 200, 5, 3, 100,
                30.0D, 7.0D, 18.0D, 250.0F, 10.0F, "LARGE",
                ArtilleryProfile.Caliber.HOWITZER_155);
        assertPattern(ArtilleryProfile.RAPID_105, 60, 3, 3, 60,
                10.0D, 3.5D, 9.0D, 150.0F, 7.0F, "MEDIUM",
                ArtilleryProfile.Caliber.HOWITZER_105);
        assertPattern(ArtilleryProfile.FIVE_ROUND_105, 120, 4, 5, 80,
                10.0D, 3.5D, 9.0D, 150.0F, 7.0F, "MEDIUM",
                ArtilleryProfile.Caliber.HOWITZER_105);
        assertEquals(15, ArtilleryProfile.HOWITZER_3ROUND.totalRounds());
        assertEquals(9, ArtilleryProfile.RAPID_105.totalRounds());
        assertEquals(20, ArtilleryProfile.FIVE_ROUND_105.totalRounds());
    }

    @Test
    void caliberSetsReportSpacingBatteryOffsetAndWhistlePitch() {
        assertEquals(6, ArtilleryProfile.Caliber.HOWITZER_155.reportSpacingTicks());
        assertEquals(180.0D, ArtilleryProfile.Caliber.HOWITZER_155.batteryOffset());
        assertEquals(0.95F, ArtilleryProfile.Caliber.HOWITZER_155.incomingPitch());
        assertEquals(3, ArtilleryProfile.Caliber.HOWITZER_105.reportSpacingTicks());
        assertEquals(120.0D, ArtilleryProfile.Caliber.HOWITZER_105.batteryOffset());
        assertEquals(1.15F, ArtilleryProfile.Caliber.HOWITZER_105.incomingPitch());
        assertEquals(0.7F, ArtilleryProfile.INCOMING_VOLUME);
        assertEquals(20.0D, ArtilleryProfile.WHISTLE_HEIGHT);
        assertEquals(40.0D, ArtilleryProfile.BATTERY_HEIGHT);
    }

    @Test
    void stepFormulaFollowsTheSpecification() {
        for (ArtilleryProfile profile : ALL) {
            assertEquals(profile.preparationTicks() - 30L, profile.inboundTicks(),
                    profile.id().toString());
            for (int wave = 0; wave < profile.waves(); wave++) {
                for (int round = 0; round < profile.roundsPerWave(); round++) {
                    int impact = 3 + wave * profile.waveIntervalTicks() / 10 + round;
                    assertEquals(impact, profile.impactStep(wave, round));
                    assertEquals(impact - 3, profile.whistleStep(wave, round));
                }
            }
            assertEquals(profile.impactStep(profile.waves() - 1,
                    profile.roundsPerWave() - 1) + 1, profile.stepCount());
        }
    }

    @Test
    void everyWaveLandsExactlyOnePreparationAfterItsFirstReport() {
        for (ArtilleryProfile profile : ALL) {
            for (int wave = 0; wave < profile.waves(); wave++) {
                long firstReport = profile.reportDelayTicks(wave, 0);
                long firstImpact = profile.inboundTicks() + (long) profile.impactStep(wave, 0)
                        * ArtilleryProfile.STEP_INTERVAL_TICKS;
                assertEquals(profile.preparationTicks(), firstImpact - firstReport,
                        profile.id() + " wave " + wave);
                // Rounds of one wave land half a second (one step) apart.
                for (int round = 1; round < profile.roundsPerWave(); round++) {
                    assertEquals(1, profile.impactStep(wave, round)
                            - profile.impactStep(wave, round - 1));
                }
            }
        }
    }

    @Test
    void idsAreTheRegisteredNamespacedSkills() {
        Set<ResourceLocation> ids = new HashSet<>();
        for (ArtilleryProfile profile : ALL) {
            assertEquals(WokCommanderSupportMod.MOD_ID, profile.id().getNamespace());
            assertTrue(ids.add(profile.id()), "duplicate id " + profile.id());
        }
        assertEquals("howitzer_3round_barrage",
                ArtilleryProfile.HOWITZER_3ROUND.id().getPath());
        assertEquals("howitzer_105mm_rapid_3round_barrage",
                ArtilleryProfile.RAPID_105.id().getPath());
        assertEquals("howitzer_105mm_5round_barrage",
                ArtilleryProfile.FIVE_ROUND_105.id().getPath());
    }

    @Test
    void invalidProfilesFailClosed() {
        ResourceLocation id = ArtilleryProfile.RAPID_105.id();
        // Wave interval must be a whole number of half-second steps.
        assertThrows(IllegalArgumentException.class, () -> profile(id, 60, 3, 3, 65));
        // Preparation must cover the 1.5-second whistle lead.
        assertThrows(IllegalArgumentException.class, () -> profile(id, 20, 3, 3, 60));
        assertThrows(IllegalArgumentException.class, () -> profile(id, 60, 0, 3, 60));
        assertThrows(IllegalArgumentException.class, () -> profile(id, 60, 3, 0, 60));
        // 3 + 7 * 10 + 1 steps would exceed the core's 64-step limit.
        assertThrows(IllegalArgumentException.class, () -> profile(id, 60, 8, 1, 100));
        assertThrows(IllegalArgumentException.class, () -> new ArtilleryProfile(id,
                "名称", "短名", 100L, 60, 1, 1, 10, 10.0D, Double.NaN, 9.0D,
                150.0F, 7.0F, "MEDIUM", ArtilleryProfile.Caliber.HOWITZER_105));
        assertThrows(IllegalArgumentException.class, () -> new ArtilleryProfile(id,
                "名称", "短名", 100L, 60, 1, 1, 10, 10.0D, 3.5D, 9.0D,
                0.0F, 7.0F, "MEDIUM", ArtilleryProfile.Caliber.HOWITZER_105));
        assertThrows(IllegalArgumentException.class, () -> new ArtilleryProfile(id,
                "名称", "短名", 100L, 60, 1, 1, 10, 10.0D, 3.5D, 9.0D,
                150.0F, 7.0F, " ", ArtilleryProfile.Caliber.HOWITZER_105));
        assertThrows(IllegalArgumentException.class,
                () -> ArtilleryProfile.RAPID_105.impactStep(3, 0));
        assertThrows(IllegalArgumentException.class,
                () -> ArtilleryProfile.RAPID_105.reportDelayTicks(0, 3));
    }

    private static ArtilleryProfile profile(ResourceLocation id, int preparation, int waves,
                                            int rounds, int waveInterval) {
        return new ArtilleryProfile(id, "名称", "短名", 100L, preparation, waves, rounds,
                waveInterval, 10.0D, 3.5D, 9.0D, 150.0F, 7.0F, "MEDIUM",
                ArtilleryProfile.Caliber.HOWITZER_105);
    }

    private static void assertDefinition(ArtilleryProfile profile, ResourceLocation id,
                                         String name, String shortName, long cooldown,
                                         long inbound, int steps, double radius) {
        SupportDefinition definition = profile.definition();
        assertEquals(id, definition.id());
        assertEquals("support.wok_commander_support." + id.getPath(),
                definition.translationKey());
        assertEquals(name, definition.fallbackName());
        assertEquals(shortName, definition.shortName());
        assertEquals(SupportTargetMode.POINT, definition.targetMode());
        assertEquals(cooldown, definition.cooldownTicks());
        assertEquals(inbound, definition.inboundTicks());
        assertEquals(steps, definition.stepCount());
        assertEquals(10, definition.stepIntervalTicks());
        assertEquals(radius, definition.radius());
        assertTrue(definition.requiresLoadedFootprint(),
                "explosions read terrain, so the danger area must stay loaded");
        assertEquals(profile.areaRadius() + profile.scatterMax() + profile.explosionRadius(),
                definition.radius());
    }

    private static void assertPattern(ArtilleryProfile profile, int preparation, int waves,
                                      int rounds, int waveInterval, double area,
                                      double sigma, double max, float damage,
                                      float blastRadius, String particle,
                                      ArtilleryProfile.Caliber caliber) {
        assertEquals(preparation, profile.preparationTicks());
        assertEquals(waves, profile.waves());
        assertEquals(rounds, profile.roundsPerWave());
        assertEquals(waveInterval, profile.waveIntervalTicks());
        assertEquals(area, profile.areaRadius());
        assertEquals(sigma, profile.scatterSigma());
        assertEquals(max, profile.scatterMax());
        assertEquals(damage, profile.damage());
        assertEquals(blastRadius, profile.explosionRadius());
        assertEquals(particle, profile.explosionParticle());
        assertEquals(caliber, profile.caliber());
    }
}
