package com.wok.commandersupport;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wok.commandersupport.airstrike.F16PavewayProvider;
import com.wok.commandersupport.artillery.ArtilleryProfile;
import com.wok.commandersupport.drone.ReconDroneProvider;
import com.wok.commandersupport.registry.CommanderSupportEntities;
import com.wok.infantry.support.SupportDefinition;
import com.wok.infantry.support.SupportTargetMode;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * One global value per skill: cooldowns come from the faction table of 《步战模式公示表》, the
 * timing and footprint of the four new skills from their confirmed specification.
 */
class CommanderSupportDefinitionContractTest {
    private static final long TEN_MINUTES_TICKS = 12_000L;
    private static final String ZH_CN = "/assets/wok_commander_support/lang/zh_cn.json";
    private static final String EN_US = "/assets/wok_commander_support/lang/en_us.json";

    /** Everything a skill's definition must declare, one row per skill, in registration order. */
    private record Expected(ResourceLocation id, long cooldownTicks, long inboundTicks,
                            int stepCount, int stepIntervalTicks, double radius,
                            boolean requiresLoadedFootprint) {
    }

    /**
     * Barrage timetable from the specification: the first shell lands 30 ticks after the
     * inbound phase ends (step 3 at 10 ticks per step), rounds of one wave land one step apart,
     * and waves are fired {@code waveIntervalTicks} apart.
     */
    private record Barrage(ResourceLocation id, int preparationTicks, int waves,
                           int roundsPerWave, int waveIntervalTicks, double areaRadius,
                           double scatterMax, double blastRadius) {
        static final int STEP_INTERVAL_TICKS = 10;
        static final int FIRST_IMPACT_STEP = 3;

        long inboundTicks() {
            return preparationTicks - (long) FIRST_IMPACT_STEP * STEP_INTERVAL_TICKS;
        }

        int lastImpactStep() {
            return FIRST_IMPACT_STEP + (waves - 1) * waveIntervalTicks / STEP_INTERVAL_TICKS
                    + roundsPerWave - 1;
        }

        double dangerRadius() {
            return areaRadius + scatterMax + blastRadius;
        }
    }

    private static final List<Expected> EXPECTED = List.of(
            new Expected(WokCommanderSupportMod.RECON_SATELLITE_ID, TEN_MINUTES_TICKS, 0L,
                    6, 100, 150.0D, false),
            new Expected(WokCommanderSupportMod.MILLENNIUM_JDAM_ID, TEN_MINUTES_TICKS, 200L,
                    4, 20, 32.0D, true),
            new Expected(WokCommanderSupportMod.F16C_PAVEWAY_ID, TEN_MINUTES_TICKS, 200L,
                    61, 1, 80.0D, true),
            new Expected(WokCommanderSupportMod.RECON_DRONE_ID, 6_000L, 0L,
                    60, 40, 96.0D, false),
            new Expected(WokCommanderSupportMod.HOWITZER_3ROUND_ID, 3_600L, 170L,
                    46, 10, 58.0D, true),
            new Expected(WokCommanderSupportMod.HOWITZER_105_RAPID_ID, 2_400L, 30L,
                    18, 10, 26.0D, true),
            new Expected(WokCommanderSupportMod.HOWITZER_105_5ROUND_ID, 6_000L, 90L,
                    32, 10, 26.0D, true));

    private static final List<Barrage> BARRAGES = List.of(
            new Barrage(WokCommanderSupportMod.HOWITZER_3ROUND_ID, 200, 5, 3, 100,
                    30.0D, 18.0D, 10.0D),
            new Barrage(WokCommanderSupportMod.HOWITZER_105_RAPID_ID, 60, 3, 3, 60,
                    10.0D, 9.0D, 7.0D),
            new Barrage(WokCommanderSupportMod.HOWITZER_105_5ROUND_ID, 120, 4, 5, 80,
                    10.0D, 9.0D, 7.0D));

    /** Chinese names (the definition fallback) and short names fixed by the specification. */
    private static final Map<ResourceLocation, String> NEW_SHORT_NAMES = Map.of(
            WokCommanderSupportMod.RECON_DRONE_ID, "无人机侦察",
            WokCommanderSupportMod.HOWITZER_3ROUND_ID, "三连发炮击",
            WokCommanderSupportMod.HOWITZER_105_RAPID_ID, "105快速三连发",
            WokCommanderSupportMod.HOWITZER_105_5ROUND_ID, "105五连发");

    @Test
    void everySkillUsesItsTableCooldown() {
        assertEquals(TEN_MINUTES_TICKS, WokCommanderSupportMod.RECON_COOLDOWN_TICKS);
        assertEquals(TEN_MINUTES_TICKS, WokCommanderSupportMod.MILLENNIUM_JDAM_COOLDOWN_TICKS);
        assertEquals(TEN_MINUTES_TICKS, WokCommanderSupportMod.F16C_PAVEWAY_COOLDOWN_TICKS);
        for (Expected expected : EXPECTED) {
            assertEquals(expected.cooldownTicks(), definition(expected.id()).cooldownTicks(),
                    expected.id().toString());
        }
        // 无人机 5 分钟、三连发 3 分钟、105 快速三连发 2 分钟、105 五连发 5 分钟。
        assertEquals(5L * 60L * 20L, definition(WokCommanderSupportMod.RECON_DRONE_ID)
                .cooldownTicks());
        assertEquals(3L * 60L * 20L, definition(WokCommanderSupportMod.HOWITZER_3ROUND_ID)
                .cooldownTicks());
        assertEquals(2L * 60L * 20L, definition(WokCommanderSupportMod.HOWITZER_105_RAPID_ID)
                .cooldownTicks());
        assertEquals(5L * 60L * 20L, definition(WokCommanderSupportMod.HOWITZER_105_5ROUND_ID)
                .cooldownTicks());
    }

    @Test
    void timingAndFootprintMatchTheSpecification() {
        for (Expected expected : EXPECTED) {
            SupportDefinition definition = definition(expected.id());
            String id = expected.id().toString();

            assertEquals(SupportTargetMode.POINT, definition.targetMode(), id);
            assertEquals(expected.inboundTicks(), definition.inboundTicks(), id);
            assertEquals(expected.stepCount(), definition.stepCount(), id);
            assertEquals(expected.stepIntervalTicks(), definition.stepIntervalTicks(), id);
            assertEquals(expected.radius(), definition.radius(), 1.0E-9D, id);
            assertEquals(expected.requiresLoadedFootprint(),
                    definition.requiresLoadedFootprint(), id);
        }
    }

    @Test
    void registersSevenDistinctSkillsUnderTheModNamespace() {
        List<SupportDefinition> definitions = WokCommanderSupportMod.definitions();
        assertEquals(EXPECTED.size(), definitions.size());

        Set<ResourceLocation> ids = new HashSet<>();
        for (int index = 0; index < definitions.size(); index++) {
            SupportDefinition definition = definitions.get(index);
            assertEquals(EXPECTED.get(index).id(), definition.id(), "registration order");
            assertTrue(ids.add(definition.id()), "duplicate " + definition.id());
            assertEquals(WokCommanderSupportMod.MOD_ID, definition.id().getNamespace());
            assertEquals("support." + WokCommanderSupportMod.MOD_ID + "."
                    + definition.id().getPath(), definition.translationKey());
        }
        assertEquals(WokCommanderSupportMod.reconDroneDefinition(),
                ReconDroneProvider.definition());
        assertEquals(List.of(ArtilleryProfile.HOWITZER_3ROUND.definition(),
                        ArtilleryProfile.RAPID_105.definition(),
                        ArtilleryProfile.FIVE_ROUND_105.definition()),
                WokCommanderSupportMod.artilleryProfiles().stream()
                        .map(ArtilleryProfile::definition).toList());
    }

    @Test
    void barrageStepsCoverEveryImpact() {
        for (Barrage barrage : BARRAGES) {
            SupportDefinition definition = definition(barrage.id());
            String id = barrage.id().toString();

            assertEquals(Barrage.STEP_INTERVAL_TICKS, definition.stepIntervalTicks(), id);
            assertEquals(barrage.inboundTicks(), definition.inboundTicks(), id);
            // The mission ends on the step after the last impact.
            assertEquals(barrage.lastImpactStep() + 1, definition.stepCount(), id);
            assertTrue(definition.stepCount() <= SupportDefinition.MAX_STEPS, id);
            // Every impact stays inside area + scatter, and its blast inside the danger area.
            assertEquals(barrage.dangerRadius(), definition.radius(), 1.0E-9D, id);
            // The first wave lands exactly when the preparation time is over.
            assertEquals(barrage.preparationTicks(), definition.inboundTicks()
                    + (long) Barrage.FIRST_IMPACT_STEP * definition.stepIntervalTicks(), id);
        }
    }

    @Test
    void reconDroneScansEveryTenSecondsForAboutTwoMinutes() {
        SupportDefinition drone = definition(WokCommanderSupportMod.RECON_DRONE_ID);

        assertEquals(2400L, (long) drone.stepCount() * drone.stepIntervalTicks());
        int scans = 0;
        for (int step = 0; step < drone.stepCount(); step++) {
            if (step % 5 == 0) {
                scans++;
            }
        }
        assertEquals(12, scans);
        assertEquals(200, 5 * drone.stepIntervalTicks(), "one scan every 10 seconds");
    }

    @Test
    void onlyTheReconSkillsMayBeCalledOverUnloadedChunks() {
        for (Expected expected : EXPECTED) {
            boolean recon = expected.id().equals(WokCommanderSupportMod.RECON_SATELLITE_ID)
                    || expected.id().equals(WokCommanderSupportMod.RECON_DRONE_ID);
            assertEquals(!recon, definition(expected.id()).requiresLoadedFootprint(),
                    expected.id().toString());
        }
    }

    @Test
    void pavewayDangerAreaIsThePermitPlusTheBlast() {
        assertEquals(64.0D, WokCommanderSupportMod.F16C_PAVEWAY_GUIDANCE_RADIUS);
        assertEquals(80.0D, WokCommanderSupportMod.F16C_PAVEWAY_DANGER_RADIUS);
        assertEquals(WokCommanderSupportMod.F16C_PAVEWAY_GUIDANCE_RADIUS
                        + F16PavewayProvider.EXPLOSION_RADIUS,
                WokCommanderSupportMod.F16C_PAVEWAY_DANGER_RADIUS);
        assertEquals(80.0D, WokCommanderSupportMod.f16cPavewayDefinition().radius());
    }

    @Test
    void everySkillAndTheDroneAreTranslatedInBothLanguages() throws IOException {
        JsonObject zh = json(ZH_CN);
        JsonObject en = json(EN_US);

        assertEquals(zh.keySet(), en.keySet(), "zh_cn 与 en_us 的键必须一一对应");
        for (Map.Entry<String, JsonElement> entry : zh.entrySet()) {
            assertFalse(entry.getValue().getAsString().isBlank(), entry.getKey());
            assertFalse(en.get(entry.getKey()).getAsString().isBlank(), entry.getKey());
        }
        for (SupportDefinition definition : WokCommanderSupportMod.definitions()) {
            String key = definition.translationKey();
            assertNotNull(zh.get(key), "zh_cn missing " + key);
            assertNotNull(en.get(key), "en_us missing " + key);
            // The fallback name shown without the resource pack is the Chinese table name.
            assertEquals(zh.get(key).getAsString(), definition.fallbackName(), key);
        }
        assertNotNull(zh.get("entity.wok_commander_support.recon_drone"));
        assertNotNull(en.get("entity.wok_commander_support.recon_drone"));
        for (Map.Entry<ResourceLocation, String> shortName : NEW_SHORT_NAMES.entrySet()) {
            assertEquals(shortName.getValue(), definition(shortName.getKey()).shortName(),
                    shortName.getKey().toString());
        }
    }

    @Test
    void reconDroneShipsItsTextureAndIsAPantsirAirTarget() throws IOException {
        String droneId = WokCommanderSupportMod.MOD_ID + ":"
                + CommanderSupportEntities.RECON_DRONE_PATH;
        assertEquals(WokCommanderSupportMod.RECON_DRONE_ID.toString(), droneId);

        // Soft compatibility with vvp: a plain data tag, read only when vvp is installed.
        JsonObject tag = json("/data/vvp/tags/entity_types/pantsir_air_target.json");
        assertFalse(tag.get("replace").getAsBoolean(), "must merge into vvp's own tag");
        Set<String> values = new HashSet<>();
        tag.getAsJsonArray("values").forEach(value -> values.add(value.isJsonObject()
                ? value.getAsJsonObject().get("id").getAsString() : value.getAsString()));
        assertEquals(Set.of(droneId), values);

        try (InputStream texture = CommanderSupportDefinitionContractTest.class
                .getResourceAsStream("/assets/wok_commander_support/textures/entity/"
                        + CommanderSupportEntities.RECON_DRONE_PATH + ".png")) {
            assertNotNull(texture, "missing recon drone texture");
            byte[] header = texture.readNBytes(8);
            assertEquals(8, header.length);
            assertEquals((byte) 0x89, header[0]);
            assertEquals("PNG", new String(header, 1, 3, StandardCharsets.US_ASCII));
        }
    }

    @Test
    void coreDependencyRequiresTheAcceptanceCueApi() throws IOException {
        String block = coreDependencyBlock();

        assertTrue(block.contains("mandatory=true"), block);
        assertTrue(block.contains("versionRange=\"[0.3.0-beta.6,)\""), block);
    }

    private static SupportDefinition definition(ResourceLocation id) {
        for (SupportDefinition definition : WokCommanderSupportMod.definitions()) {
            if (definition.id().equals(id)) {
                return definition;
            }
        }
        return fail("no definition registered for " + id);
    }

    private static JsonObject json(String path) throws IOException {
        try (InputStream stream = CommanderSupportDefinitionContractTest.class
                .getResourceAsStream(path)) {
            assertNotNull(stream, "missing resource " + path);
            return JsonParser.parseString(new String(stream.readAllBytes(),
                    StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /**
     * Other mods on the test classpath (the core itself, Forge) ship their own mods.toml, so
     * only dependency tables declared by this mod's processed manifest are considered.
     */
    private static String coreDependencyBlock() throws IOException {
        ClassLoader loader = CommanderSupportDefinitionContractTest.class.getClassLoader();
        for (URL url : Collections.list(loader.getResources("META-INF/mods.toml"))) {
            String toml;
            try (InputStream stream = url.openStream()) {
                toml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
            String[] tables = toml.split("\\[\\[dependencies\\.");
            // tables[0] is the [[mods]] header, which never declares a dependency.
            for (int index = 1; index < tables.length; index++) {
                if (tables[index].startsWith(WokCommanderSupportMod.MOD_ID + "]]")
                        && tables[index].contains("modId=\"wok_infantry\"")) {
                    return tables[index];
                }
            }
        }
        return fail("wok_commander_support mods.toml has no wok_infantry dependency");
    }
}
