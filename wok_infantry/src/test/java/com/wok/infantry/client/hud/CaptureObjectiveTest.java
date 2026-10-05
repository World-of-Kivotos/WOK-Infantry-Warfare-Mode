package com.wok.infantry.client.hud;

import com.wok.infantry.battle.Faction;
import com.wok.infantry.client.hud.CaptureObjective.Look;
import com.wok.infantry.client.hud.CaptureObjective.Side;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The map WOK步战附属-占点 0.1.0-alpha.4 returns from {@code CaptureHudApi.currentPoint()} (its
 * {@code CaptureHudApiTest} pins the same keys and types from the other side).
 */
class CaptureObjectiveTest {
    /** The add-on's map for B, blue taking it at ×2 with red locked out. */
    static Map<String, Object> pointB() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("version", 1);
        map.put("id", "b");
        map.put("name", "B点 · 指挥所");
        map.put("shortName", "B");
        map.put("control", 0.62D);
        map.put("percent", 62);
        map.put("leading", "blue");
        map.put("owner", "neutral");
        map.put("capturing", "blue");
        map.put("bluePlayers", 3);
        map.put("redPlayers", 1);
        map.put("enabled", true);
        map.put("blueAllowed", true);
        map.put("redAllowed", false);
        map.put("speed", 2);
        map.put("captureSeconds", 90);
        map.put("remainingSeconds", 9);
        map.put("state", "capturing");
        map.put("status", Component.literal("蓝方正在占领 · 速度 ×2"));
        return map;
    }

    @Test
    void readsTheAddOnsMap() {
        CaptureObjective point = CaptureObjective.fromMap(pointB());
        assertEquals("b", point.id());
        assertEquals("B点 · 指挥所", point.name());
        assertEquals("B", point.shortName());
        assertEquals(0.62D, point.control());
        assertEquals(62, point.percent());
        assertEquals(Side.BLUE, point.leading());
        assertEquals(Side.NEUTRAL, point.owner());
        assertEquals(Side.BLUE, point.capturing());
        assertEquals(3, point.bluePlayers());
        assertEquals(1, point.redPlayers());
        assertTrue(point.enabled());
        assertTrue(point.blueAllowed());
        assertFalse(point.redAllowed());
        assertEquals(2, point.speed());
        assertEquals("capturing", point.state());
        assertEquals("蓝方正在占领 · 速度 ×2", point.status().getString());
        assertEquals(9, point.remainingSeconds());
        assertEquals(Look.CAPTURING, point.look());
    }

    @Test
    void optionalValuesFallBackAndNamesFallBackToTheId() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "hq");
        map.put("control", -0.995D);
        map.put("owner", "neutral");
        map.put("capturing", "red");
        map.put("enabled", true);
        CaptureObjective point = CaptureObjective.fromMap(map);
        assertEquals("hq", point.name());
        assertEquals("hq", point.shortName());
        assertEquals(99, point.percent(), "never 100 before it is owned");
        assertEquals(Side.RED, point.leading(), "derived from the control");
        assertEquals(-1, point.remainingSeconds());
        assertEquals("", point.status().getString());
        assertTrue(point.blueAllowed() && point.redAllowed());
    }

    @Test
    void rejectsAMapThatBreaksTheContract() {
        for (String key : new String[]{"id", "control", "owner", "capturing", "enabled"}) {
            Map<String, Object> map = pointB();
            map.remove(key);
            assertThrows(IllegalArgumentException.class, () -> CaptureObjective.fromMap(map),
                    "missing " + key);
        }
        Map<String, Object> wrongType = pointB();
        wrongType.put("control", "0.62");
        assertThrows(IllegalArgumentException.class, () -> CaptureObjective.fromMap(wrongType));
        Map<String, Object> wrongSide = pointB();
        wrongSide.put("owner", "green");
        assertThrows(IllegalArgumentException.class, () -> CaptureObjective.fromMap(wrongSide));
        Map<String, Object> future = pointB();
        future.put("version", 2);
        assertThrows(IllegalArgumentException.class, () -> CaptureObjective.fromMap(future),
                "a later contract version means a key changed meaning");
        Map<String, Object> status = pointB();
        status.put("status", "text");
        assertThrows(IllegalArgumentException.class, () -> CaptureObjective.fromMap(status));
        assertThrows(IllegalArgumentException.class, () -> CaptureObjective.fromMap(null));
    }

    @Test
    void valuesAreClampedAndNamesCapped() {
        Map<String, Object> map = pointB();
        map.put("control", Double.NaN);
        map.put("percent", 140);
        map.put("bluePlayers", -3);
        map.put("name", "名".repeat(200));
        CaptureObjective point = CaptureObjective.fromMap(map);
        assertEquals(0.0D, point.control());
        assertEquals(100, point.percent());
        assertEquals(0, point.bluePlayers());
        assertEquals(CaptureObjective.MAX_NAME_LENGTH, point.name().length());
    }

    @Test
    void lookFollowsTheStateOfThePoint() {
        assertEquals(Look.DISABLED, with("enabled", false).look());
        Map<String, Object> contested = pointB();
        contested.put("capturing", "neutral");
        assertEquals(Look.CONTESTED, CaptureObjective.fromMap(contested).look());
        Map<String, Object> secured = pointB();
        secured.put("capturing", "neutral");
        secured.put("redPlayers", 0);
        secured.put("owner", "red");
        assertEquals(Look.SECURED, CaptureObjective.fromMap(secured).look());
        Map<String, Object> neutral = pointB();
        neutral.put("capturing", "neutral");
        neutral.put("bluePlayers", 0);
        neutral.put("redPlayers", 0);
        assertEquals(Look.NEUTRAL, CaptureObjective.fromMap(neutral).look());
        Map<String, Object> held = pointB();
        held.put("control", 1.0D);
        held.put("owner", "blue");
        held.put("redPlayers", 0);
        assertEquals(Look.SECURED, CaptureObjective.fromMap(held).look(),
                "blue standing in its own point holds it");
        Map<String, Object> retaking = pointB();
        retaking.put("control", 1.0D);
        retaking.put("owner", "blue");
        retaking.put("capturing", "red");
        assertEquals(Look.CAPTURING, CaptureObjective.fromMap(retaking).look());
    }

    @Test
    void lockFollowsTheViewersSide() {
        CaptureObjective point = CaptureObjective.fromMap(pointB());
        assertTrue(point.lockedFor(Faction.RED), "red may not take B yet");
        assertFalse(point.lockedFor(Faction.BLUE));
        assertFalse(point.lockedFor(null), "unknown side: the add-on's own state (capturing)");
        assertTrue(with("state", "locked").lockedFor(null));
        assertFalse(with("enabled", false).lockedFor(Faction.RED), "never on a disabled point");
    }

    @Test
    void percentAndTimeFormatting() {
        assertEquals(62, CaptureObjective.percentOf(0.62));
        assertEquals(100, CaptureObjective.percentOf(-1.0));
        assertEquals(0, CaptureObjective.percentOf(Double.POSITIVE_INFINITY));
        assertEquals("0:09", CaptureObjective.time(9));
        assertEquals("21:24", CaptureObjective.time(1284));
        assertEquals("1:00:05", CaptureObjective.time(3605));
        assertSame(Side.BLUE, Side.byId(" Blue "));
        assertEquals(Faction.RED, Side.RED.faction());
        assertEquals(null, Side.NEUTRAL.faction());
    }

    private static CaptureObjective with(String key, Object value) {
        Map<String, Object> map = pointB();
        map.put(key, value);
        return CaptureObjective.fromMap(map);
    }
}
