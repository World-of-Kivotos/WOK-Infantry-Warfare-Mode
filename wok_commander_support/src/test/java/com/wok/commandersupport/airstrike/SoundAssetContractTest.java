package com.wok.commandersupport.airstrike;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundAssetContractTest {
    @Test
    void soundManifestRegistersBothJdamFieldRecordings() throws IOException {
        byte[] manifest = resource(
                "/assets/wok_commander_support/sounds.json");
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

    private static void assertOgg(String path) throws IOException {
        byte[] bytes = resource(path);
        assertTrue(bytes.length > 8_000, path + " is unexpectedly small");
        assertArrayEquals(new byte[]{'O', 'g', 'g', 'S'},
                new byte[]{bytes[0], bytes[1], bytes[2], bytes[3]});
    }

    private static byte[] resource(String path) throws IOException {
        try (InputStream stream = SoundAssetContractTest.class
                .getResourceAsStream(path)) {
            assertNotNull(stream, "missing resource " + path);
            return stream.readAllBytes();
        }
    }
}
