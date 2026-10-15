package com.wok.infantry.client.screen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wok.infantry.client.screen.TacticalLivery.Livery;
import com.wok.infantry.client.screen.TacticalLivery.Scope;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

/**
 * Reads {@code ui_palette/p3_palette.json}, the palette export of the approved preview
 * ({@code ui-preview/tools/export-palette.mjs --module <wok_infantry>}): {@code a} (the A
 * scheme) and per livery the {@code board} and {@code map} screen palettes and the device
 * {@code skin}. Keys are the Java token names, values {@code "0xAARRGGBB"} strings.
 */
final class PaletteExport {
    static final String RESOURCE = "ui_palette/p3_palette.json";
    private static JsonObject root;

    private PaletteExport() {
    }

    static synchronized JsonObject root() {
        if (root == null) {
            InputStream stream = PaletteExport.class.getClassLoader().getResourceAsStream(RESOURCE);
            Objects.requireNonNull(stream, RESOURCE + " is missing; run export-palette.mjs");
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }
        return root;
    }

    /** The A scheme. */
    static JsonObject a() {
        return root().getAsJsonObject("a");
    }

    /** One livery's screen palette on {@code scope}. */
    static JsonObject table(Livery livery, Scope scope) {
        return livery(livery).getAsJsonObject(scope.name().toLowerCase(Locale.ROOT));
    }

    /** One livery's device paint (preview SKINS keys, {@code CASE} … {@code LED_POWER}). */
    static JsonObject skin(Livery livery) {
        return livery(livery).getAsJsonObject("skin");
    }

    private static JsonObject livery(Livery livery) {
        JsonObject entry = root().getAsJsonObject("liveries")
                .getAsJsonObject(livery.name().toLowerCase(Locale.ROOT));
        return Objects.requireNonNull(entry, "no export for " + livery);
    }

    /** The colour {@code name} of {@code table}. */
    static int color(JsonObject table, String name) {
        String value = table.get(name).getAsString();
        if (!value.startsWith("0x") || value.length() != 10) {
            throw new IllegalArgumentException(name + ": not 0xAARRGGBB: " + value);
        }
        return Integer.parseUnsignedInt(value.substring(2), 16);
    }

    /** The swappable token named {@code name}, or {@code null} for a fixed one. */
    static PaletteToken token(String name) {
        for (PaletteToken token : PaletteToken.values()) {
            if (token.name().equals(name)) {
                return token;
            }
        }
        return null;
    }
}
