package com.wok.commandersupport;

import com.wok.commandersupport.airstrike.F16PavewayProvider;
import com.wok.infantry.support.SupportDefinition;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** One global value per skill, taken from the faction table of 《步战模式公示表》. */
class CommanderSupportDefinitionContractTest {
    private static final long TEN_MINUTES_TICKS = 12_000L;

    @Test
    void everySkillRechargesInTenMinutes() {
        assertEquals(TEN_MINUTES_TICKS, WokCommanderSupportMod.RECON_COOLDOWN_TICKS);
        assertEquals(TEN_MINUTES_TICKS, WokCommanderSupportMod.MILLENNIUM_JDAM_COOLDOWN_TICKS);
        assertEquals(TEN_MINUTES_TICKS, WokCommanderSupportMod.F16C_PAVEWAY_COOLDOWN_TICKS);
        for (SupportDefinition definition : definitions()) {
            assertEquals(TEN_MINUTES_TICKS, definition.cooldownTicks(),
                    definition.id().toString());
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
    void onlyTheReconScanMayBeCalledOverUnloadedChunks() {
        assertFalse(WokCommanderSupportMod.reconSatelliteDefinition()
                .requiresLoadedFootprint());
        assertTrue(WokCommanderSupportMod.millenniumJdamDefinition()
                .requiresLoadedFootprint());
        assertTrue(WokCommanderSupportMod.f16cPavewayDefinition()
                .requiresLoadedFootprint());
    }

    @Test
    void coreDependencyRequiresTheRefundAndFootprintApi() throws IOException {
        String block = coreDependencyBlock();

        assertTrue(block.contains("mandatory=true"), block);
        assertTrue(block.contains("versionRange=\"[0.3.0-beta.5,)\""), block);
    }

    private static List<SupportDefinition> definitions() {
        return List.of(WokCommanderSupportMod.reconSatelliteDefinition(),
                WokCommanderSupportMod.millenniumJdamDefinition(),
                WokCommanderSupportMod.f16cPavewayDefinition());
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
