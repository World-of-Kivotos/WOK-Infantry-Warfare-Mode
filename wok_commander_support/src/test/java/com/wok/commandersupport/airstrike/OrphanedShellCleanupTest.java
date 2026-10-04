package com.wok.commandersupport.airstrike;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrphanedShellCleanupTest {
    @Test
    void releasedJdamAndPavewayShellsAreRecognised() {
        CompoundTag jdam = new CompoundTag();
        jdam.putBoolean(MillenniumJdamProvider.JDAM_MARKER, true);
        CompoundTag paveway = new CompoundTag();
        paveway.putBoolean(F16PavewayProvider.MARKER, true);

        assertTrue(OrphanedShellCleanup.isSupportShell(jdam));
        assertTrue(OrphanedShellCleanup.isSupportShell(paveway));
        assertNotEquals(MillenniumJdamProvider.JDAM_MARKER, F16PavewayProvider.MARKER);
    }

    @Test
    void ordinaryCbcShellsAndUnrelatedEntitiesAreLeftAlone() {
        CompoundTag cannonShell = new CompoundTag();
        cannonShell.putBoolean("SomeOtherModFlag", true);
        CompoundTag cleared = new CompoundTag();
        cleared.putBoolean(MillenniumJdamProvider.JDAM_MARKER, false);

        assertFalse(OrphanedShellCleanup.isSupportShell(new CompoundTag()));
        assertFalse(OrphanedShellCleanup.isSupportShell(cannonShell));
        assertFalse(OrphanedShellCleanup.isSupportShell(cleared));
        assertFalse(OrphanedShellCleanup.isSupportShell(null));
    }
}
