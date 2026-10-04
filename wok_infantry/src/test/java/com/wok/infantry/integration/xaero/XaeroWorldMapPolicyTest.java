package com.wok.infantry.integration.xaero;

import com.wok.infantry.config.InfantryClientConfig;
import com.wok.infantry.integration.xaero.XaeroWorldMapPolicy.Decision;
import net.minecraft.client.KeyMapping;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XaeroWorldMapPolicyTest {
    @Test
    void onlyXaerosWorldMapScreenIsRecognisedByName() {
        // Class and key names checked with javap against XaerosWorldMap 1.39.9 (test modpack).
        assertEquals("xaeroworldmap", XaeroWorldMapPolicy.MOD_ID);
        assertEquals("gui.xaero_open_map", XaeroWorldMapPolicy.OPEN_MAP_KEY_NAME);
        assertTrue(XaeroWorldMapPolicy.isWorldMapScreenName("xaero.map.gui.GuiMap"));
        assertFalse(XaeroWorldMapPolicy.isWorldMapScreenName(
                "xaero.map.gui.GuiWorldMapSettings"));
        assertFalse(XaeroWorldMapPolicy.isWorldMapScreenName(
                "xaero.common.gui.GuiMinimapSettings"), "Xaero's minimap screens stay as they are");
        assertFalse(XaeroWorldMapPolicy.isWorldMapScreenName(
                "journeymap.client.ui.fullscreen.Fullscreen"));
        assertFalse(XaeroWorldMapPolicy.isWorldMapScreenName(
                "com.wok.infantry.client.screen.TacticalMapScreen"));
        assertFalse(XaeroWorldMapPolicy.isWorldMapScreenName(null));
        assertFalse(XaeroWorldMapPolicy.isWorldMapScreen(String.class));
        assertFalse(XaeroWorldMapPolicy.isWorldMapScreen(null));
    }

    @Test
    void redirectsOnlyInABattleAndWhileEnabled() {
        assertEquals(Decision.REDIRECT, XaeroWorldMapPolicy.decide(true, true, true, false));
        assertEquals(Decision.KEEP, XaeroWorldMapPolicy.decide(true, true, false, false),
                "no faction: Xaero's own map opens");
        assertEquals(Decision.KEEP, XaeroWorldMapPolicy.decide(true, false, true, false),
                "config off: Xaero's own map opens");
        assertEquals(Decision.KEEP, XaeroWorldMapPolicy.decide(false, true, true, false),
                "other screens are never touched");
        assertEquals(Decision.KEEP, XaeroWorldMapPolicy.decide(false, true, true, true));
        assertEquals(Decision.CANCEL, XaeroWorldMapPolicy.decide(true, true, true, true),
                "the tactical map is already open: keep it");
        assertEquals(Decision.KEEP, XaeroWorldMapPolicy.decide(true, true, false, true));
    }

    @Test
    void xaeroWorldMapScreensAreNeverTheTacticalMapsReturnScreen() {
        assertTrue(XaeroWorldMapPolicy.keepAsReturnScreen(null), "opened from the game");
        assertTrue(XaeroWorldMapPolicy.keepAsReturnScreen(
                "com.wok.infantry.client.screen.SquadScreen"));
        assertTrue(XaeroWorldMapPolicy.keepAsReturnScreen(
                "net.minecraft.client.gui.screens.inventory.InventoryScreen"));
        assertFalse(XaeroWorldMapPolicy.keepAsReturnScreen("xaero.map.gui.GuiMap"));
        assertFalse(XaeroWorldMapPolicy.keepAsReturnScreen("xaero.map.gui.GuiWorldMapSettings"));
    }

    @Test
    void redirectIsOnByDefaultAndOffWithoutXaero() {
        assertTrue(InfantryClientConfig.DEFAULT_REDIRECT_XAERO_WORLD_MAP);
        assertTrue(InfantryClientConfig.redirectXaeroWorldMap(),
                "an unloaded client config reads the default");
        assertFalse(XaeroWorldMapPolicy.isModLoaded(), "no Xaero in unit tests");
        XaeroWorldMapPolicy.install();
        assertFalse(XaeroWorldMapPolicy.redirectActive(), "install() is a no-op without Xaero");
        assertNull(XaeroWorldMapPolicy.openMapKey(), "no Xaero key to show or to defer to");
        assertFalse(XaeroWorldMapPolicy.handlesSamePress(null));
    }

    @Test
    void xaerosOpenMapKeyIsFoundByItsMappingName() {
        KeyMapping other = new KeyMapping("key.wok_infantry.test_other", GLFW.GLFW_KEY_M,
                "key.categories.wok_infantry");
        KeyMapping xaero = new KeyMapping(XaeroWorldMapPolicy.OPEN_MAP_KEY_NAME,
                GLFW.GLFW_KEY_M, "Xaero's World Map");
        assertSame(xaero, XaeroWorldMapPolicy.findByName(new KeyMapping[]{other, null, xaero},
                XaeroWorldMapPolicy.OPEN_MAP_KEY_NAME));
        assertNull(XaeroWorldMapPolicy.findByName(new KeyMapping[]{other},
                XaeroWorldMapPolicy.OPEN_MAP_KEY_NAME), "renamed in another Xaero build");
        assertNull(XaeroWorldMapPolicy.findByName(null, XaeroWorldMapPolicy.OPEN_MAP_KEY_NAME));
    }
}
