package com.wok.infantry.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the authorized DragonRise station conversion and its Forge OBJ UV contract. */
class LargeSupplyStationModelContractTest {
    private static final String ROOT = "assets/wok_infantry/";

    @Test
    void objUsesTheEmbeddedOriginalTextureWithBedrockUvOrientation() throws IOException {
        JsonObject model = JsonParser.parseReader(reader(
                ROOT + "models/block/large_ammo_supply_station.json")).getAsJsonObject();

        assertEquals("forge:obj", model.get("loader").getAsString());
        assertTrue(model.get("flip_v").getAsBoolean(),
                "Bedrock top-origin UVs converted to OBJ must be flipped during Forge baking");
        assertEquals("wok_infantry:block/large_ammo_supply_station",
                model.getAsJsonObject("textures").get("texture0").getAsString());
        assertEquals("#texture0",
                model.getAsJsonObject("textures").get("particle").getAsString());

        String material = readText(ROOT
                + "models/block/large_ammo_supply_station.mtl");
        assertTrue(material.contains("map_Kd #texture0"));
    }

    @Test
    void conversionContainsEveryFaceAndAuthorizedOriginalTextures() throws Exception {
        List<String> obj = readLines(ROOT
                + "models/block/large_ammo_supply_station.obj");
        assertEquals(1_412L, obj.stream().filter(line -> line.startsWith("v ")).count());
        assertEquals(1_412L, obj.stream().filter(line -> line.startsWith("vt ")).count());
        assertEquals(353L, obj.stream().filter(line -> line.startsWith("f ")).count());

        assertEquals("E555283FCB3B883E89BE0527A7F9900E7559E9C090FF26DE464F9433008E88D0",
                sha256(ROOT + "geo/large_ammo_supply_station.geo.json"));
        assertEquals("4AD38DA3E6EA0880338E9381AFC0CDB340B451E95885128F59C6961E2B8CE47E",
                sha256(ROOT + "textures/block/large_ammo_supply_station.png"));
        assertEquals("1DC89E0FAC373E4FB191DFEC106BCA62F3E2CAA93D3BE518FD4111294FB83A7F",
                sha256(ROOT + "textures/item/large_ammo_supply_station.png"));
    }

    private static BufferedReader reader(String path) {
        return new BufferedReader(new InputStreamReader(resource(path), StandardCharsets.UTF_8));
    }

    private static String readText(String path) throws IOException {
        try (InputStream stream = resource(path)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static List<String> readLines(String path) throws IOException {
        try (BufferedReader reader = reader(path)) {
            return reader.lines().toList();
        }
    }

    private static String sha256(String path) throws IOException, NoSuchAlgorithmException {
        try (InputStream stream = resource(path)) {
            return HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(stream.readAllBytes()));
        }
    }

    private static InputStream resource(String path) {
        InputStream stream = LargeSupplyStationModelContractTest.class
                .getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, path + " must be packaged");
        return stream;
    }
}
