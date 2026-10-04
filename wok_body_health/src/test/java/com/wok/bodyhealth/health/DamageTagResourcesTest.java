package com.wok.bodyhealth.health;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A single required entry that is not registered makes Minecraft drop the whole
 * tag, so every third-party damage type must be an optional entry.
 */
final class DamageTagResourcesTest {
    private static final List<String> ROUTING_TAGS = List.of(
            "explosion_spread", "leg_spread", "systemic_spread", "head_hit");
    private static final List<String> KILL_BYPASS_TAGS = List.of(
            "bypasses_armor", "bypasses_effects", "bypasses_enchantments", "bypasses_shield");

    @Test
    void routingTagsOnlyRequireVanillaEntries() throws IOException {
        for (String tag : ROUTING_TAGS) {
            JsonObject json = read("/data/wok_body_health/tags/damage_type/" + tag + ".json");
            assertFalse(json.get("replace").getAsBoolean(), tag);
            for (JsonElement value : json.getAsJsonArray("values")) {
                if (value.isJsonObject()) {
                    assertFalse(value.getAsJsonObject().get("required").getAsBoolean(),
                            tag + " " + value);
                    continue;
                }
                String id = value.getAsString();
                String namespace = id.replaceFirst("^#", "").split(":", 2)[0];
                assertEquals("minecraft", namespace, tag + " requires " + id);
            }
        }
    }

    @Test
    void routingTagsDoNotOverlap() throws IOException {
        Set<String> seen = new HashSet<>();
        for (String tag : ROUTING_TAGS) {
            for (JsonElement value : read("/data/wok_body_health/tags/damage_type/" + tag + ".json")
                    .getAsJsonArray("values")) {
                String id = value.isJsonObject()
                        ? value.getAsJsonObject().get("id").getAsString() : value.getAsString();
                assertTrue(seen.add(id), id + " appears in more than one routing tag");
            }
        }
    }

    @Test
    void everyFinishingBlowTypeBypassesVanillaReductions() throws IOException {
        for (String tag : KILL_BYPASS_TAGS) {
            Set<String> values = new HashSet<>();
            read("/data/minecraft/tags/damage_type/" + tag + ".json")
                    .getAsJsonArray("values")
                    .forEach(value -> values.add(value.getAsString()));
            for (BodyPart part : BodyPart.values()) {
                assertTrue(values.contains("wok_body_health:pvp_kill_" + part.key()),
                        tag + " misses " + part);
            }
        }
    }

    private static JsonObject read(String path) throws IOException {
        try (InputStream stream = DamageTagResourcesTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }
}
