package com.wok.capturepoints.api;

import com.wok.capturepoints.capture.CapturePointView;
import com.wok.capturepoints.capture.CaptureTeam;
import com.wok.capturepoints.client.ClientCaptureState;
import com.wok.capturepoints.client.LangSupport;
import com.wok.capturepoints.network.CaptureSnapshotPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The contract WOK步战核心 0.5.0-beta.2 reads by reflection ({@code CaptureHudBridge}): key names,
 * value types and meanings. Changing any of them breaks the core's battle strip tile.
 */
class CaptureHudApiTest {
    private static final CapturePointView B = new CapturePointView("b", "B点 · 指挥所",
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"),
            new BlockPos(0, 60, 0), new BlockPos(10, 70, 10), 1, 90, true, 0.62D,
            CaptureTeam.NEUTRAL, CaptureTeam.BLUE, 3, 1, 2, true, false);
    private static final CapturePointView A = new CapturePointView("a", "A点",
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"),
            new BlockPos(20, 60, 0), new BlockPos(30, 70, 10), 0, 60, true, -1.0D,
            CaptureTeam.RED, CaptureTeam.NEUTRAL, 0, 0, 0, true, true);

    @AfterEach
    void clear() {
        ClientCaptureState.clear();
    }

    @Test
    void currentPointDescribesThePointTheViewerStandsIn() {
        ClientCaptureState.update(new CaptureSnapshotPacket(List.of(A, B), "b", true, 90));
        Map<String, Object> map = CaptureHudApi.currentPoint();
        assertEquals(1, map.get(CaptureHudApi.API_VERSION));
        assertEquals("b", map.get(CaptureHudApi.ID));
        assertEquals("B点 · 指挥所", map.get(CaptureHudApi.NAME));
        assertEquals("B", map.get(CaptureHudApi.SHORT_NAME));
        assertEquals(0.62D, map.get(CaptureHudApi.CONTROL));
        assertEquals(62, map.get(CaptureHudApi.PERCENT));
        assertEquals("blue", map.get(CaptureHudApi.LEADING));
        assertEquals("neutral", map.get(CaptureHudApi.OWNER));
        assertEquals("blue", map.get(CaptureHudApi.CAPTURING));
        assertEquals(3, map.get(CaptureHudApi.BLUE_PLAYERS));
        assertEquals(1, map.get(CaptureHudApi.RED_PLAYERS));
        assertEquals(true, map.get(CaptureHudApi.ENABLED));
        assertEquals(true, map.get(CaptureHudApi.BLUE_ALLOWED));
        assertEquals(false, map.get(CaptureHudApi.RED_ALLOWED));
        assertEquals(2, map.get(CaptureHudApi.SPEED));
        assertEquals(90, map.get(CaptureHudApi.CAPTURE_SECONDS));
        assertEquals(9, map.get(CaptureHudApi.REMAINING_SECONDS));
        assertEquals("capturing", map.get(CaptureHudApi.STATE));
        Component status = assertInstanceOf(Component.class, map.get(CaptureHudApi.STATUS));
        assertEquals("蓝方正在占领 · 速度 ×2",
                LangSupport.render(status, LangSupport.bundle(LangSupport.ZH_CN)));
        assertEquals(Set.of("version", "id", "name", "shortName", "control", "percent",
                "leading", "owner", "capturing", "bluePlayers", "redPlayers", "enabled",
                "blueAllowed", "redAllowed", "speed", "captureSeconds", "remainingSeconds",
                "state", "status"), map.keySet());
        assertThrows(UnsupportedOperationException.class, () -> map.put("id", "x"));
    }

    @Test
    void sameMapUntilTheNextSynchronization() {
        ClientCaptureState.update(new CaptureSnapshotPacket(List.of(A, B), "b", true, 90));
        Map<String, Object> first = CaptureHudApi.currentPoint();
        assertSame(first, CaptureHudApi.currentPoint());
        ClientCaptureState.update(new CaptureSnapshotPacket(List.of(A, B), "a", true, 90));
        Map<String, Object> other = CaptureHudApi.currentPoint();
        assertNotSame(first, other);
        assertEquals("secured", other.get(CaptureHudApi.STATE));
        assertEquals(100, other.get(CaptureHudApi.PERCENT));
        assertEquals("red", other.get(CaptureHudApi.LEADING));
        assertEquals(-1, other.get(CaptureHudApi.REMAINING_SECONDS));
        assertEquals("A", other.get(CaptureHudApi.SHORT_NAME));
    }

    @Test
    void nullOutsideEveryPoint() {
        ClientCaptureState.update(new CaptureSnapshotPacket(List.of(A, B), null, false, 90));
        assertNull(CaptureHudApi.currentPoint());
        ClientCaptureState.update(new CaptureSnapshotPacket(List.of(A), "b", false, 90));
        assertNull(CaptureHudApi.currentPoint(), "a stale id that no longer exists");
        ClientCaptureState.clear();
        assertNull(CaptureHudApi.currentPoint());
    }
}
