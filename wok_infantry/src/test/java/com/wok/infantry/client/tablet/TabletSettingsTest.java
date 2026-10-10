package com.wok.infantry.client.tablet;

import com.wok.infantry.config.InfantryClientConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@code ui.tabletAnimation} / {@code ui.tabletSounds} and the two system properties (D18). */
class TabletSettingsTest {
    @Test
    void defaultsWithoutALoadedConfig() {
        assertEquals(TabletMode.FULL, InfantryClientConfig.DEFAULT_TABLET_ANIMATION);
        assertEquals(TabletMode.FULL, InfantryClientConfig.tabletAnimation());
        assertTrue(InfantryClientConfig.DEFAULT_TABLET_SOUNDS);
        assertTrue(InfantryClientConfig.tabletSounds());
    }

    @Test
    void modePropertyOverridesTheOption() {
        assertEquals(TabletMode.OFF, TabletSettings.effectiveMode("OFF", TabletMode.FULL));
        assertEquals(TabletMode.QUICK, TabletSettings.effectiveMode(" quick ", TabletMode.FULL));
        assertEquals(TabletMode.FULL, TabletSettings.effectiveMode("full", TabletMode.OFF));
        assertEquals(TabletMode.QUICK, TabletSettings.effectiveMode(null, TabletMode.QUICK));
        assertEquals(TabletMode.QUICK, TabletSettings.effectiveMode("sideways", TabletMode.QUICK),
                "an invalid value is ignored");
        assertEquals(TabletMode.FULL, TabletSettings.effectiveMode(null, null));
    }

    @Test
    void freezePropertyParsesOpeningAndClosingProgress() {
        assertEquals(new TabletSettings.Freeze(0.87D, false), TabletSettings.parseFreeze("0.87"));
        assertEquals(new TabletSettings.Freeze(0.66D, true), TabletSettings.parseFreeze("close:0.66"));
        assertEquals(new TabletSettings.Freeze(0.3D, false), TabletSettings.parseFreeze("open: 0.3"));
        assertEquals(new TabletSettings.Freeze(1.0D, false), TabletSettings.parseFreeze("1"));
        assertNull(TabletSettings.parseFreeze(null));
        assertNull(TabletSettings.parseFreeze(""));
        assertNull(TabletSettings.parseFreeze("close:"));
        assertNull(TabletSettings.parseFreeze("abc"));
        assertNull(TabletSettings.parseFreeze("1.5"));
        assertNull(TabletSettings.parseFreeze("-0.1"));
        assertNull(TabletSettings.parseFreeze("NaN"));
    }

    @Test
    void surfaceKindOfNonTabletScreensIsNull() {
        assertNull(TabletSurface.kindOf(null));
        assertNull(TabletSurface.kindOf(new Object()));
        assertEquals(TabletScreenKind.FULLSCREEN, TabletSurface.kindOf(new TabletSurface() {
        }));
        assertEquals(TabletScreenKind.SQUAD, TabletSurface.kindOf(new TabletSurface() {
            @Override
            public TabletScreenKind tabletKind() {
                return TabletScreenKind.SQUAD;
            }
        }));
    }

    @Test
    void handClassification() {
        assertEquals(TabletHand.EMPTY, TabletHand.classify(true, false, false));
        assertEquals(TabletHand.GUN, TabletHand.classify(false, true, false));
        assertEquals(TabletHand.GUN, TabletHand.classify(false, false, true));
        assertEquals(TabletHand.ITEM, TabletHand.classify(false, false, false));
        assertEquals(TabletHand.EMPTY, TabletHand.classify(true, true, true), "an empty hand holds no gun");
    }
}
