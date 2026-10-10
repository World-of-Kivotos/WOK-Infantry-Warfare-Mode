package com.wok.infantry.client.screen;

import com.google.gson.JsonObject;
import com.wok.infantry.client.screen.TacticalLivery.Livery;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DeviceSkinTest {
    @Test
    void everyLiveryHasItsOwnSkin() {
        assertSame(DeviceSkin.ACADEMY, DeviceSkin.forLivery(Livery.ACADEMY));
        assertSame(DeviceSkin.CAESAR, DeviceSkin.forLivery(Livery.CAESAR));
        assertSame(DeviceSkin.NEUTRAL, DeviceSkin.forLivery(Livery.NEUTRAL));
        for (Livery livery : Livery.values()) {
            assertSame(DeviceSkin.forLivery(livery), livery.skin(), livery.name());
        }
    }

    @Test
    void everySkinValueMatchesThePreviewExport() throws ReflectiveOperationException {
        // ui_palette/p3_palette.json: 17-device.js SKINS under 18-device-livery.js SKINS, with the
        // LED / LED_POWER fallbacks 17-device.js applies while drawing.
        for (Livery livery : Livery.values()) {
            JsonObject skin = PaletteExport.skin(livery);
            Set<String> keys = new LinkedHashSet<>();
            for (RecordComponent component : DeviceSkin.class.getRecordComponents()) {
                String key = previewKey(component.getName());
                keys.add(key);
                int value = (int) component.getAccessor().invoke(livery.skin());
                assertEquals(String.format("0x%08X", PaletteExport.color(skin, key)),
                        String.format("0x%08X", value), livery + " " + key);
            }
            assertEquals(skin.keySet(), keys, livery + ": DeviceSkin and the export list the same keys");
        }
    }

    @Test
    void powerLedIsTheASuccessGreenExceptOnThePaleNeutralCase() {
        // 17-device.js reads K.LED_POWER ?? T.SUCCESS_B; only Neutral sets its own (plan 2.3).
        assertEquals(TacticalPalette.A.get(PaletteToken.SUCCESS_B), DeviceSkin.ACADEMY.powerLed());
        assertEquals(TacticalPalette.A.get(PaletteToken.SUCCESS_B), DeviceSkin.CAESAR.powerLed());
        assertEquals(0xFF2E9E52, DeviceSkin.NEUTRAL.powerLed());
    }

    @Test
    void devicePaintIsOpaque() throws ReflectiveOperationException {
        for (DeviceSkin skin : new DeviceSkin[]{DeviceSkin.ACADEMY, DeviceSkin.CAESAR,
                DeviceSkin.NEUTRAL}) {
            for (RecordComponent component : DeviceSkin.class.getRecordComponents()) {
                int value = (int) component.getAccessor().invoke(skin);
                assertEquals(0xFF, value >>> 24, component.getName());
            }
        }
    }

    @Test
    void derivedKeyColoursAreSolvedPerSkin() {
        // keyHover: brightest twentieth of KEY → KEY_HI keeping KEY_TEXT ≥ 4.5 and KEY_SUB ≥ 3:1.
        assertEquals(0xFF3F6BA3, DeviceSkin.ACADEMY.keyHover(), "Academy stops at 75%");
        assertEquals(DeviceSkin.CAESAR.keyHi(), DeviceSkin.CAESAR.keyHover());
        assertEquals(DeviceSkin.NEUTRAL.keyHi(), DeviceSkin.NEUTRAL.keyHover());
        // keyOff: KEY_SUB mixed toward KEY in twentieths while it still reads at 3:1 on KEY.
        assertEquals(0xFF84A1C5, DeviceSkin.ACADEMY.keyOff(), "30% toward KEY");
        assertEquals(0xFFCD9194, DeviceSkin.CAESAR.keyOff(), "40% toward KEY");
        assertEquals(0xFF7A8280, DeviceSkin.NEUTRAL.keyOff(), "25% toward KEY");
    }

    @Test
    void aSkinOutsideTheThreeLiveriesIsSolvedTheSameWay() {
        DeviceSkin a = DeviceSkin.ACADEMY;
        DeviceSkin copy = new DeviceSkin(a.caseColor(), a.caseHi(), a.caseLo(), a.line(),
                a.recessHi(), a.rubber(), a.rubberHi(), a.rubberLo(), a.rubberEdge(), a.key(),
                a.keyHi(), a.keyLo(), a.keyDown(), a.keyText(), a.keySub(), a.stripe(), a.silk(),
                a.label(), a.status(), a.statusLine(), a.ident(), a.pill(), a.signalOff(), a.led(),
                a.powerLed());
        assertEquals(a.keyHover(), copy.keyHover());
        assertEquals(a.keyOff(), copy.keyOff());
        assertEquals(0xFF, copy.keyOff() >>> 24, "opaque");
        assertEquals(0xFF, copy.keyHover() >>> 24, "opaque");
    }

    /** Record component → preview SKINS key: {@code caseHi} → {@code CASE_HI}, ... */
    private static String previewKey(String component) {
        return switch (component) {
            case "caseColor" -> "CASE";
            case "powerLed" -> "LED_POWER";
            default -> component.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(java.util.Locale.ROOT);
        };
    }
}
