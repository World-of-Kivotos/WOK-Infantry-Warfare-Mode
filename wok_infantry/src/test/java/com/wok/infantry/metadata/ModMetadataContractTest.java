package com.wok.infantry.metadata;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModMetadataContractTest {
    @Test
    void processedMetadataPreservesTheWokInfantryProductIdentity() throws IOException {
        String metadata = readMetadata();

        assertTrue(metadata.contains("modId=\"wok_infantry\""));
        assertTrue(metadata.contains("displayName=\"WOK步战核心\""),
                "processResources must preserve the UTF-8 WOK步战 product name");
    }

    @Test
    void mapTerrainEnginesAreOptionalClientIntegrations() throws IOException {
        String metadata = readMetadata();
        int journeyMapBlockStart = metadata.indexOf("modId=\"journeymap\"");
        assertTrue(journeyMapBlockStart >= 0,
                "JourneyMap must be declared as the client terrain dependency");
        int next = metadata.indexOf("[[dependencies.wok_infantry]]",
                journeyMapBlockStart + 1);
        String journeyMapBlock = metadata.substring(journeyMapBlockStart, next);
        assertTrue(journeyMapBlock.contains("mandatory=false"));
        assertTrue(journeyMapBlock.contains(
                        "versionRange=\"[1.20.1-6.0.2,1.20.1-7.0.0)\""),
                "the client must stay on the API-v2-compatible JourneyMap 6 line");
        assertTrue(journeyMapBlock.contains("ordering=\"AFTER\""));
        assertTrue(journeyMapBlock.contains("side=\"CLIENT\""));

        for (String xaeroId : new String[]{"xaerominimapfair", "xaerominimap"}) {
            int start = metadata.indexOf("modId=\"" + xaeroId + "\"");
            assertTrue(start >= 0, xaeroId + " must be declared as an optional client backend");
            int blockEnd = metadata.indexOf("[[dependencies.wok_infantry]]", start + 1);
            String block = metadata.substring(start,
                    blockEnd < 0 ? metadata.length() : blockEnd);
            assertTrue(block.contains("mandatory=false"));
            assertTrue(block.contains("versionRange=\"[24.2.0,25.0.0)\""));
            assertTrue(block.contains("side=\"CLIENT\""));
        }
    }

    @Test
    void nonSupportIntegrationsRemainOptionalAndCbcIsNotDeclared() throws IOException {
        String metadata = readMetadata();
        for (String modId : new String[]{"superbwarfare", "tacz"}) {
            int start = metadata.indexOf("modId=\"" + modId + "\"");
            assertTrue(start >= 0,
                    modId + " must be declared for its existing loadout/vehicle integration");
            int next = metadata.indexOf("[[dependencies.wok_infantry]]", start + 1);
            String block = metadata.substring(start, next < 0 ? metadata.length() : next);
            assertTrue(block.contains("mandatory=false"),
                    modId + " must not become a hard dependency of WOK步战核心");
            assertTrue(block.contains("ordering=\"AFTER\""));
            assertTrue(block.contains("side=\"BOTH\""));
            if (modId.equals("superbwarfare")) {
                assertTrue(block.contains("versionRange=\"[0.8.9,)\""),
                        "the registry-only core integration must accept the 0.8.9 vehicle-rich line");
            }
        }
        assertFalse(metadata.contains("modId=\"createbigcannons\""),
                "the framework-only support catalog must not declare a CBC dependency");
    }

    private String readMetadata() throws IOException {
        try (InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("META-INF/mods.toml")) {
            assertNotNull(stream, "processed mods.toml must be present on the test classpath");
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
