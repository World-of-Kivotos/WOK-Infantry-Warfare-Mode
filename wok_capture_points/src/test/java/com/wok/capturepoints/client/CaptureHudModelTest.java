package com.wok.capturepoints.client;

import com.wok.capturepoints.capture.CapturePointView;
import com.wok.capturepoints.capture.CaptureTeam;
import com.wok.capturepoints.client.CaptureHudModel.State;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaptureHudModelTest {
    private static final Map<String, String> ZH = LangSupport.bundle(LangSupport.ZH_CN);
    private static final Map<String, String> EN = LangSupport.bundle(LangSupport.EN_US);

    static CapturePointView point(String id, String name, boolean enabled, double control,
                                  CaptureTeam owner, CaptureTeam active, int blue, int red,
                                  int speed, boolean blueAllowed, boolean redAllowed) {
        return new CapturePointView(id, name,
                ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"),
                new BlockPos(0, 60, 0), new BlockPos(10, 70, 10), 0, 90, enabled, control,
                owner, active, blue, red, speed, blueAllowed, redAllowed);
    }

    @Test
    void stateFollowsTheOldPanelsPrecedence() {
        assertEquals(State.DISABLED, CaptureHudModel.state(point("b", "B", false, 0.5,
                CaptureTeam.NEUTRAL, CaptureTeam.BLUE, 3, 3, 1, true, true)));
        assertEquals(State.CONTESTED, CaptureHudModel.state(point("b", "B", true, 0.5,
                CaptureTeam.NEUTRAL, CaptureTeam.NEUTRAL, 2, 2, 0, true, true)));
        assertEquals(State.LOCKED, CaptureHudModel.state(point("b", "B", true, 0.0,
                CaptureTeam.NEUTRAL, CaptureTeam.NEUTRAL, 2, 0, 0, false, true)),
                "blue stands in a point it may not take yet");
        assertEquals(State.CAPTURING, CaptureHudModel.state(point("b", "B", true, 0.62,
                CaptureTeam.NEUTRAL, CaptureTeam.BLUE, 3, 1, 2, true, true)),
                "advantage mode: both present, blue outnumbers red");
        assertEquals(State.SECURED, CaptureHudModel.state(point("b", "B", true, -1.0,
                CaptureTeam.RED, CaptureTeam.NEUTRAL, 0, 0, 0, true, true)));
        assertEquals(State.SECURED, CaptureHudModel.state(point("b", "B", true, 1.0,
                CaptureTeam.BLUE, CaptureTeam.BLUE, 2, 0, 2, true, true)),
                "blue standing in its own point holds it, it does not take it");
        assertEquals(-1, CaptureHudModel.remainingSeconds(point("b", "B", true, 1.0,
                CaptureTeam.BLUE, CaptureTeam.BLUE, 2, 0, 2, true, true)));
        assertEquals(State.NEUTRAL, CaptureHudModel.state(point("b", "B", true, 0.0,
                CaptureTeam.NEUTRAL, CaptureTeam.NEUTRAL, 0, 0, 0, true, true)));
        assertEquals("capturing", State.CAPTURING.id());
    }

    @Test
    void percentIsTheShareOfTheLeadingSideAndNeverReachesOneHundredEarly() {
        assertEquals(62, CaptureHudModel.percent(0.62));
        assertEquals(62, CaptureHudModel.percent(-0.62));
        assertEquals(99, CaptureHudModel.percent(0.998), "not owned yet");
        assertEquals(100, CaptureHudModel.percent(1.0));
        assertEquals(0, CaptureHudModel.percent(Double.NaN));
        assertEquals(CaptureTeam.BLUE, CaptureHudModel.leading(0.35));
        assertEquals(CaptureTeam.RED, CaptureHudModel.leading(-0.35));
        assertEquals(CaptureTeam.NEUTRAL, CaptureHudModel.leading(0.004), "reads 0%");
    }

    @Test
    void remainingTimeIsTheRestOfTheSwingAtTheCurrentSpeed() {
        // 90 s for a full swing of 2.0: blue from 0.62 to 1.0 at speed 2 = 0.38 × 90 / 4 = 8.55
        assertEquals(9, CaptureHudModel.remainingSeconds(point("b", "B", true, 0.62,
                CaptureTeam.NEUTRAL, CaptureTeam.BLUE, 3, 1, 2, true, true)));
        // red retaking a blue-owned point: 2.0 × 90 / 2 = 90
        assertEquals(90, CaptureHudModel.remainingSeconds(point("b", "B", true, 1.0,
                CaptureTeam.BLUE, CaptureTeam.RED, 0, 1, 1, true, true)));
        assertEquals(-1, CaptureHudModel.remainingSeconds(point("b", "B", true, 0.62,
                CaptureTeam.NEUTRAL, CaptureTeam.NEUTRAL, 2, 2, 0, true, true)), "contested");
        assertEquals(-1, CaptureHudModel.remainingSeconds(point("b", "B", false, 0.62,
                CaptureTeam.NEUTRAL, CaptureTeam.BLUE, 2, 0, 1, true, true)), "disabled");
        assertEquals("0:09", CaptureHudModel.time(9));
        assertEquals("21:24", CaptureHudModel.time(1284));
        assertEquals("1:00:05", CaptureHudModel.time(3605));
        assertEquals("0:00", CaptureHudModel.time(-4));
    }

    @Test
    void shortNameIsTheShortIdOrTheDisplayName() {
        assertEquals("B", CaptureHudModel.shortName(point("b", "B点 · 指挥所", true, 0,
                CaptureTeam.NEUTRAL, CaptureTeam.NEUTRAL, 0, 0, 0, true, true)));
        assertEquals("HQ2", CaptureHudModel.shortName(point("hq2", "指挥部", true, 0,
                CaptureTeam.NEUTRAL, CaptureTeam.NEUTRAL, 0, 0, 0, true, true)));
        assertEquals("火车站", CaptureHudModel.shortName(point("station", "火车站", true, 0,
                CaptureTeam.NEUTRAL, CaptureTeam.NEUTRAL, 0, 0, 0, true, true)));
    }

    @Test
    void statusTextNamesTheStateInBothLanguages() {
        CapturePointView capturing = point("b", "B", true, 0.62, CaptureTeam.NEUTRAL,
                CaptureTeam.BLUE, 3, 1, 2, true, true);
        assertEquals("蓝方正在占领 · 速度 ×2", LangSupport.render(
                CaptureHudModel.status(capturing), ZH));
        assertEquals("蓝方正在占领 · 速度 ×2 · 剩余 0:09", LangSupport.render(
                CaptureHudModel.statusWithTime(capturing), ZH));
        assertEquals("Blue capturing · speed ×2 · 0:09 left", LangSupport.render(
                CaptureHudModel.statusWithTime(capturing), EN));
        CapturePointView plain = point("b", "B", true, -0.2, CaptureTeam.NEUTRAL,
                CaptureTeam.RED, 0, 1, 1, true, true);
        assertEquals("红方正在占领", LangSupport.render(CaptureHudModel.status(plain), ZH),
                "speed 1 is not worth a word");
        assertEquals("争夺中 · 进度冻结", LangSupport.render(CaptureHudModel.statusWithTime(
                point("b", "B", true, 0.5, CaptureTeam.NEUTRAL, CaptureTeam.NEUTRAL, 2, 2, 0,
                        true, true)), ZH), "no time while frozen");
        assertEquals("红方控制", LangSupport.render(CaptureHudModel.status(point("b", "B", true,
                -1.0, CaptureTeam.RED, CaptureTeam.NEUTRAL, 0, 0, 0, true, true)), ZH));
        assertEquals("据点已停用", LangSupport.render(CaptureHudModel.status(point("b", "B",
                false, 0, CaptureTeam.NEUTRAL, CaptureTeam.NEUTRAL, 0, 0, 0, true, true)), ZH));
        assertEquals("顺序未解锁", LangSupport.render(CaptureHudModel.status(point("b", "B", true,
                0, CaptureTeam.NEUTRAL, CaptureTeam.NEUTRAL, 2, 0, 0, false, true)), ZH));
        assertEquals("中立 · 等待占领", LangSupport.render(CaptureHudModel.status(point("b", "B",
                true, 0, CaptureTeam.NEUTRAL, CaptureTeam.NEUTRAL, 0, 0, 0, true, true)), ZH));
    }

    @Test
    void accentAndStatusColoursFollowTheState() {
        assertEquals(CaptureHudModel.BLUE, CaptureHudModel.accent(point("b", "B", true, 0.3,
                CaptureTeam.NEUTRAL, CaptureTeam.BLUE, 1, 0, 1, true, true)));
        assertEquals(CaptureHudModel.ORANGE, CaptureHudModel.accent(point("b", "B", true, 0.3,
                CaptureTeam.NEUTRAL, CaptureTeam.NEUTRAL, 1, 1, 0, true, true)));
        assertEquals(CaptureHudModel.RED, CaptureHudModel.accent(point("b", "B", true, -1.0,
                CaptureTeam.RED, CaptureTeam.NEUTRAL, 0, 0, 0, true, true)));
        assertEquals(CaptureHudModel.OFFLINE, CaptureHudModel.accent(point("b", "B", false, 0,
                CaptureTeam.NEUTRAL, CaptureTeam.NEUTRAL, 0, 0, 0, true, true)));
        assertEquals(CaptureHudModel.MUTED, CaptureHudModel.statusColor(point("b", "B", true, 0,
                CaptureTeam.NEUTRAL, CaptureTeam.NEUTRAL, 2, 0, 0, false, true)));
    }
}
