package com.wok.commandersupport.recon;

import com.wok.infantry.support.adapter.SupportSpawnException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconFailurePolicyTest {
    @Test
    void ordinaryFirstScanFailureReturnsTheCooldownWithoutTrippingTheCircuit() {
        SupportSpawnException plain = new SupportSpawnException("侦察卫星缺少在线指挥官");
        SupportSpawnException classified = ReconFailurePolicy.classify(0, plain);

        assertTrue(classified.refundCooldown());
        assertFalse(classified.providerBroken());
        assertEquals("侦察卫星缺少在线指挥官", classified.getMessage());
        assertSame(plain, classified.getCause());

        SupportSpawnException ended = SupportSpawnException.endMission("战局服务不可用");
        SupportSpawnException endedClassified = ReconFailurePolicy.classify(0, ended);
        assertTrue(endedClassified.refundCooldown());
        assertFalse(endedClassified.providerBroken());
        assertEquals("战局服务不可用", endedClassified.getMessage());
        assertSame(ended, endedClassified.getCause());
    }

    @Test
    void firstScanFailureThatAlreadyRefundsIsKept() {
        SupportSpawnException notDelivered = SupportSpawnException.notDelivered("未投送");
        assertSame(notDelivered, ReconFailurePolicy.classify(0, notDelivered));

        SupportSpawnException brokenRefund =
                SupportSpawnException.providerBroken("损坏", null, true);
        assertSame(brokenRefund, ReconFailurePolicy.classify(0, brokenRefund));
    }

    @Test
    void brokenFirstScanKeepsTheCircuitButReturnsTheCooldown() {
        SupportSpawnException broken =
                SupportSpawnException.providerBroken("临时情报目标超出支援扫描区域", null, false);
        SupportSpawnException classified = ReconFailurePolicy.classify(0, broken);

        assertTrue(classified.providerBroken());
        assertTrue(classified.refundCooldown());
        assertEquals("临时情报目标超出支援扫描区域", classified.getMessage());
        assertSame(broken, classified.getCause());
    }

    @Test
    void uncheckedFirstScanFaultTripsTheCircuitAndReturnsTheCooldown() {
        IllegalStateException runtime = new IllegalStateException("boom");
        SupportSpawnException fromRuntime = ReconFailurePolicy.classify(0, runtime);
        assertTrue(fromRuntime.providerBroken());
        assertTrue(fromRuntime.refundCooldown());
        assertEquals("侦察卫星执行异常", fromRuntime.getMessage());
        assertSame(runtime, fromRuntime.getCause());

        NoClassDefFoundError linkage = new NoClassDefFoundError("missing");
        SupportSpawnException fromLinkage = ReconFailurePolicy.classify(0, linkage);
        assertTrue(fromLinkage.providerBroken());
        assertTrue(fromLinkage.refundCooldown());
        assertSame(linkage, fromLinkage.getCause());
    }

    @Test
    void laterScanFailuresKeepTheirOutcomeAndNeverRefundFaults() {
        SupportSpawnException plain = new SupportSpawnException("任务结束");
        assertSame(plain, ReconFailurePolicy.classify(1, plain));
        SupportSpawnException notDelivered = SupportSpawnException.notDelivered("未投送");
        assertSame(notDelivered, ReconFailurePolicy.classify(3, notDelivered));
        SupportSpawnException broken = SupportSpawnException.providerBroken("损坏", null, false);
        assertSame(broken, ReconFailurePolicy.classify(5, broken));

        IllegalArgumentException runtime = new IllegalArgumentException("bad");
        SupportSpawnException fromRuntime = ReconFailurePolicy.classify(1, runtime);
        assertTrue(fromRuntime.providerBroken());
        assertFalse(fromRuntime.refundCooldown());
        assertEquals("侦察卫星执行异常", fromRuntime.getMessage());
        assertSame(runtime, fromRuntime.getCause());

        SupportSpawnException fromLinkage =
                ReconFailurePolicy.classify(5, new AbstractMethodError("api"));
        assertTrue(fromLinkage.providerBroken());
        assertFalse(fromLinkage.refundCooldown());
    }

    @Test
    void invalidInputFailsFast() {
        assertThrows(NullPointerException.class, () -> ReconFailurePolicy.classify(0, null));
        assertThrows(IllegalArgumentException.class,
                () -> ReconFailurePolicy.classify(-1, new IllegalStateException()));
    }
}
