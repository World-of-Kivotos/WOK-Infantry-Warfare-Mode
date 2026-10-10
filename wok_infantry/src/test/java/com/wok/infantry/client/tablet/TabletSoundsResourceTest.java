package com.wok.infantry.client.tablet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The twelve sound resources (batch B4): {@code assets/wok_infantry/sounds.json} is the preview's
 * {@code sounds/sounds.json} (as exported into {@code vectors.sounds.json}) plus one subtitle per
 * event, every event has its OGG file (Vorbis, mono, 44.1 kHz) and its subtitle in both
 * languages, and the event locations are {@code wok_infantry:tablet.<name>}.
 */
class TabletSoundsResourceTest {
    private static final String SOUNDS_JSON = "assets/wok_infantry/sounds.json";
    private static final String OGG_DIR = "assets/wok_infantry/sounds/tablet/";
    private static final String[] LANGS = {"assets/wok_infantry/lang/en_us.json",
            "assets/wok_infantry/lang/zh_cn.json"};

    @Test
    void soundsJsonIsThePreviewsWithSubtitles() {
        JsonObject json = read(SOUNDS_JSON);
        JsonObject preview = TabletVectors.object("sounds").getAsJsonObject("json");
        JsonObject volumes = TabletVectors.object("sounds").getAsJsonObject("volumes");
        Set<String> expected = new HashSet<>();
        for (String name : TabletCues.SOUND_NAMES) {
            expected.add("tablet." + name);
        }
        assertEquals(expected, json.keySet(), "the twelve events and nothing else");
        assertEquals(preview.keySet(), json.keySet());
        for (String name : TabletCues.SOUND_NAMES) {
            JsonObject event = json.getAsJsonObject("tablet." + name);
            assertEquals(Set.of("subtitle", "sounds"), event.keySet(), name);
            assertEquals("subtitles.wok_infantry.tablet." + name, event.get("subtitle").getAsString(), name);
            JsonArray sounds = event.getAsJsonArray("sounds");
            assertEquals(preview.getAsJsonObject("tablet." + name).getAsJsonArray("sounds"), sounds,
                    name + ": same files, balanced volumes and preload as the preview");
            assertEquals(1, sounds.size(), name);
            JsonObject sound = sounds.get(0).getAsJsonObject();
            assertEquals(Set.of("name", "volume", "preload"), sound.keySet(), name);
            assertEquals("wok_infantry:tablet/" + name, sound.get("name").getAsString(), name);
            double volume = sound.get("volume").getAsDouble();
            assertTrue(volume > 0.0D && volume <= 1.0D, name + " volume " + volume);
            assertEquals(volumes.get(name).getAsDouble(), volume, 0.0D, name);
            assertTrue(sound.get("preload").getAsBoolean(), name + " is preloaded (first play on time)");
        }
    }

    @Test
    void everyEventHasItsVorbisFile() throws IOException {
        for (String name : TabletCues.SOUND_NAMES) {
            byte[] data = bytes(OGG_DIR + name + ".ogg");
            assertTrue(data.length > 1000, name + ".ogg is too small");
            // First Ogg page: "OggS", header type 2 (beginning of stream), then the Vorbis
            // identification header right after the segment table.
            assertEquals("OggS", new String(data, 0, 4, StandardCharsets.US_ASCII), name);
            assertEquals(2, data[5] & 0xFF, name + " first page starts the stream");
            int segments = data[26] & 0xFF;
            int at = 27 + segments;
            assertEquals(1, data[at], name + " identification packet");
            assertEquals("vorbis", new String(data, at + 1, 6, StandardCharsets.US_ASCII), name);
            assertEquals(0, le32(data, at + 7), name + " vorbis version");
            assertEquals(1, data[at + 11] & 0xFF, name + " is mono");
            assertEquals(44100, le32(data, at + 12), name + " sample rate");
        }
    }

    @Test
    void theTabletFolderHoldsExactlyTheTwelveFiles() throws IOException, URISyntaxException {
        URL url = getClass().getClassLoader().getResource(OGG_DIR);
        assertNotNull(url, OGG_DIR);
        Path dir = Path.of(url.toURI());
        Set<String> oggs = new TreeSet<>();
        try (Stream<Path> files = Files.list(dir)) {
            files.map(path -> path.getFileName().toString()).filter(file -> file.endsWith(".ogg"))
                    .forEach(oggs::add);
        }
        Set<String> expected = new TreeSet<>();
        for (String name : TabletCues.SOUND_NAMES) {
            expected.add(name + ".ogg");
        }
        assertEquals(expected, oggs);
        String readme = new String(bytes(OGG_DIR + "readme.txt"), StandardCharsets.UTF_8);
        assertTrue(readme.contains("make-sounds.mjs") && readme.contains("20261010"),
                "the folder says how the sounds were made");
        // Every file under assets must be a valid resource path (lower case), or the game logs
        // "Invalid path in pack" at ERROR on every resource reload.
        try (Stream<Path> files = Files.list(dir)) {
            files.map(path -> path.getFileName().toString()).forEach(file ->
                    assertTrue(file.matches("[a-z0-9_.-]+"), "invalid resource path " + file));
        }
    }

    @Test
    void subtitlesExistInBothLanguages() {
        for (String lang : LANGS) {
            JsonObject bundle = read(lang);
            for (String name : TabletCues.SOUND_NAMES) {
                String key = "subtitles.wok_infantry.tablet." + name;
                JsonElement value = bundle.get(key);
                assertNotNull(value, lang + " misses " + key);
                assertFalse(value.getAsString().isBlank(), lang + " " + key);
            }
        }
    }

    @Test
    void eventLocationsAreTheSoundsJsonKeys() {
        Set<String> keys = read(SOUNDS_JSON).keySet();
        for (String name : TabletCues.SOUND_NAMES) {
            var location = TabletSounds.location(name);
            assertEquals("wok_infantry", location.getNamespace(), name);
            assertTrue(keys.contains(location.getPath()), name + " → " + location);
        }
        assertTrue(TabletCues.SOUND_NAMES.contains(TabletLinkChime.READY));
    }

    private static JsonObject read(String resource) {
        try (InputStream stream = TabletSoundsResourceTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(stream, resource);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException exception) {
            throw new AssertionError(resource, exception);
        }
    }

    private static byte[] bytes(String resource) throws IOException {
        try (InputStream stream = TabletSoundsResourceTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(stream, resource);
            return stream.readAllBytes();
        }
    }

    private static int le32(byte[] data, int at) {
        return (data[at] & 0xFF) | (data[at + 1] & 0xFF) << 8 | (data[at + 2] & 0xFF) << 16
                | (data[at + 3] & 0xFF) << 24;
    }
}
