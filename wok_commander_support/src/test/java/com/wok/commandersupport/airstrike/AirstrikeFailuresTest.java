package com.wok.commandersupport.airstrike;

import com.wok.infantry.support.adapter.SupportSpawnException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AirstrikeFailuresTest {
    @Test
    void borderMovedBeforeReleaseReturnsTheCooldownWithoutBreakingTheProvider() {
        SupportSpawnException failure = AirstrikeFailures.borderMoved();

        assertNotDeliveredOnly(failure);
        assertEquals("目标点已超出世界边界", failure.getMessage());
    }

    @Test
    void borderMovedAfterReleaseOnlyEndsTheMission() {
        SupportSpawnException failure = AirstrikeFailures.borderMovedAfterRelease();

        assertEndsMissionOnly(failure);
        assertChineseMessage(failure);
    }

    @Test
    void rejectedSpawnReturnsTheCooldownWithoutBreakingTheProvider() {
        SupportSpawnException failure = AirstrikeFailures.spawnRejected();

        assertNotDeliveredOnly(failure);
        assertEquals("弹体未能进入世界", failure.getMessage());
    }

    @Test
    void unloadedReleaseRouteReturnsTheCooldownAndSaysWhatToDo() {
        SupportSpawnException failure = AirstrikeFailures.releaseUnloaded();

        assertNotDeliveredOnly(failure);
        assertEquals("释放航线区块未加载，需有友军靠近目标外围", failure.getMessage());
    }

    @Test
    void lostShellOnlyEndsTheMissionAndNamesTheOrdnance() {
        SupportSpawnException failure =
                AirstrikeFailures.shellLost(MillenniumJdamProvider.SHELL_NAME);

        assertEndsMissionOnly(failure);
        assertEquals("JDAM 炸弹 已在命中前消失", failure.getMessage());
    }

    @Test
    void reflectionFaultBeforeReleaseBreaksTheProviderAndReturnsTheCooldown() {
        NoSuchMethodException cause = new NoSuchMethodException("setOrientation");
        SupportSpawnException failure = AirstrikeFailures.brokenBeforeRelease(cause);

        assertTrue(failure.providerBroken());
        assertTrue(failure.refundCooldown());
        assertSame(cause, failure.getCause());
        assertEquals("CBC 炮弹接口不可用", failure.getMessage());
    }

    @Test
    void reflectionFaultAfterReleaseBreaksTheProviderAndKeepsTheCooldown() {
        NoSuchMethodException cause = new NoSuchMethodException("setExplosionCountdown");
        SupportSpawnException failure = AirstrikeFailures.brokenAfterRelease(cause);

        assertTrue(failure.providerBroken());
        assertFalse(failure.refundCooldown());
        assertSame(cause, failure.getCause());
        assertEquals("CBC 炮弹接口不可用", failure.getMessage());
    }

    @Test
    void everyFactoryReturnsAFreshExceptionSoSuppressedCleanupFaultsDoNotLeak() {
        SupportSpawnException first = AirstrikeFailures.shellLost("JDAM 炸弹");
        first.addSuppressed(new IllegalStateException("discard failed"));

        SupportSpawnException second = AirstrikeFailures.shellLost("JDAM 炸弹");

        assertNotSame(first, second);
        assertEquals(0, second.getSuppressed().length);
    }

    private static void assertNotDeliveredOnly(SupportSpawnException failure) {
        assertFalse(failure.providerBroken());
        assertTrue(failure.refundCooldown());
        assertNull(failure.getCause());
        assertChineseMessage(failure);
    }

    private static void assertEndsMissionOnly(SupportSpawnException failure) {
        assertFalse(failure.providerBroken());
        assertFalse(failure.refundCooldown());
        assertNull(failure.getCause());
        assertChineseMessage(failure);
    }

    private static void assertChineseMessage(SupportSpawnException failure) {
        String message = failure.getMessage();
        assertTrue(message != null && !message.isBlank(), "失败提示必须对玩家可见");
        assertTrue(message.codePoints().anyMatch(codePoint ->
                        Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN),
                "失败提示必须是中文短句: " + message);
    }
}
