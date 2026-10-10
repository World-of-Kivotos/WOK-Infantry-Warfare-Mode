package com.wok.infantry.client.screen;

import com.wok.infantry.client.screen.TacticalLivery.Livery;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;

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
    void skinsFollowTheP3Preview() {
        // ui-preview/surfaces/18-device-livery.js SKINS (CASE, STRIPE, STATUS, IDENT, LED).
        assertEquals(0xFF204A82, DeviceSkin.ACADEMY.caseColor());
        assertEquals(0xFF8CC4F5, DeviceSkin.ACADEMY.stripe());
        assertEquals(0xFF091731, DeviceSkin.ACADEMY.status());
        assertEquals(0xFF9ACBF6, DeviceSkin.ACADEMY.ident());
        assertEquals(0xFF8AC4F5, DeviceSkin.ACADEMY.led());
        assertEquals(0xFF8A1E26, DeviceSkin.CAESAR.caseColor());
        assertEquals(0xFF6C1820, DeviceSkin.CAESAR.keyDown());
        assertEquals(0xFFFFE4E8, DeviceSkin.CAESAR.led());
        assertEquals(0xFFC6CCCA, DeviceSkin.NEUTRAL.caseColor());
        assertEquals(0xFF222A29, DeviceSkin.NEUTRAL.keyText());
        assertEquals(0xFFA9B2B4, DeviceSkin.NEUTRAL.signalOff());
        assertEquals(0xFF12A3B4, DeviceSkin.NEUTRAL.led());
    }

    @Test
    void powerLedIsTheASuccessGreenExceptOnThePaleNeutralCase() {
        // 17-device.js reads K.LED_POWER ?? T.SUCCESS_B; only Neutral sets its own (plan 2.3).
        assertEquals(TacticalBoardTheme.SUCCESS_B, DeviceSkin.ACADEMY.powerLed());
        assertEquals(TacticalBoardTheme.SUCCESS_B, DeviceSkin.CAESAR.powerLed());
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
}
