package com.wok.commandersupport.artillery;

import com.wok.infantry.support.adapter.SupportSpawnException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtilleryFailuresTest {
    @Test
    void targetOutsideTheBorderBeforeFiringReturnsTheCooldown() {
        SupportSpawnException failure = ArtilleryFailures.targetOutsideBorder();

        assertFalse(failure.providerBroken());
        assertTrue(failure.refundCooldown());
        assertNull(failure.getCause());
        assertEquals("目标点已超出世界边界", failure.getMessage());
    }

    @Test
    void precheckFaultsBreakTheProviderButReturnTheCooldown() {
        ClassNotFoundException cause = new ClassNotFoundException("CustomExplosion$Builder");
        SupportSpawnException api = ArtilleryFailures.explosionUnavailable(cause);
        SupportSpawnException marker = ArtilleryFailures.markerUnavailable();

        assertTrue(api.providerBroken());
        assertTrue(api.refundCooldown());
        assertSame(cause, api.getCause());
        assertEquals(ArtilleryFailures.EXPLOSION_BROKEN_MESSAGE, api.getMessage());
        assertTrue(marker.providerBroken());
        assertTrue(marker.refundCooldown());
        assertChinese(api);
        assertChinese(marker);
    }

    @Test
    void anExplosionFaultMidMissionBreaksTheProviderWithoutRefund() {
        InvocationTargetException cause = new InvocationTargetException(
                new IllegalStateException("explode"));
        SupportSpawnException failure = ArtilleryFailures.explosionFailed(cause);

        assertTrue(failure.providerBroken());
        assertFalse(failure.refundCooldown());
        assertSame(cause, failure.getCause());
        assertChinese(failure);
    }

    @Test
    void everyFailureOnStepZeroReturnsTheCooldown() {
        SupportSpawnException ended = SupportSpawnException.endMission("测试结束");
        SupportSpawnException broken = SupportSpawnException.providerBroken("测试熔断",
                null, false);
        SupportSpawnException refunded = ArtilleryFailures.targetOutsideBorder();
        IllegalStateException fault = new IllegalStateException("plan");

        SupportSpawnException endedOnZero = ArtilleryFailures.classify(0, ended);
        assertTrue(endedOnZero.refundCooldown());
        assertFalse(endedOnZero.providerBroken());
        assertEquals("测试结束", endedOnZero.getMessage());

        SupportSpawnException brokenOnZero = ArtilleryFailures.classify(0, broken);
        assertTrue(brokenOnZero.refundCooldown());
        assertTrue(brokenOnZero.providerBroken(), "a defective integration still trips");

        assertSame(refunded, ArtilleryFailures.classify(0, refunded));

        SupportSpawnException faultOnZero = ArtilleryFailures.classify(0, fault);
        assertTrue(faultOnZero.refundCooldown());
        assertTrue(faultOnZero.providerBroken());
        assertSame(fault, faultOnZero.getCause());
        assertEquals(ArtilleryFailures.EXECUTION_FAULT_MESSAGE, faultOnZero.getMessage());
    }

    @Test
    void laterStepsKeepTheirOutcomeAndNeverRefundAFault() {
        SupportSpawnException ended = SupportSpawnException.endMission("测试结束");
        SupportSpawnException exploded = ArtilleryFailures.explosionFailed(
                new NoSuchMethodException("explode"));
        LinkageError linkage = new NoClassDefFoundError("ParticleTool");

        assertSame(ended, ArtilleryFailures.classify(3, ended));
        assertSame(exploded, ArtilleryFailures.classify(7, exploded));
        SupportSpawnException fault = ArtilleryFailures.classify(1, linkage);
        assertTrue(fault.providerBroken());
        assertFalse(fault.refundCooldown());
        assertSame(linkage, fault.getCause());
    }

    @Test
    void invalidClassificationInputFailsClosed() {
        assertThrows(NullPointerException.class, () -> ArtilleryFailures.classify(0, null));
        assertThrows(IllegalArgumentException.class, () -> ArtilleryFailures.classify(-1,
                new IllegalStateException()));
    }

    private static void assertChinese(SupportSpawnException failure) {
        String message = failure.getMessage();
        assertTrue(message != null && message.codePoints().anyMatch(codePoint ->
                        Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN),
                "失败提示必须是中文短句: " + message);
    }
}
