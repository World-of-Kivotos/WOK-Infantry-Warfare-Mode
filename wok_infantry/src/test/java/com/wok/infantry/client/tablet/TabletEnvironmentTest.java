package com.wok.infantry.client.tablet;

import com.wok.infantry.client.tablet.TabletEnvironment.Watch;
import com.wok.infantry.client.tablet.TabletMotion.Interrupt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Held item, hand pass prediction and interrupts (IMPL_PLAN D16, D17; DESIGN 3.4, 3.7). */
class TabletEnvironmentTest {
    /** Stand-ins named like Superb Warfare's item classes. */
    static class GunItem {
    }

    static class RifleGunItem extends GunItem {
    }

    static class MortarShellItem {
    }

    @Test
    void superbWarfareGunsAreRecognisedByNameOnly() {
        assertTrue(TabletEnvironment.sbwGun("superbwarfare", GunItem.class));
        assertTrue(TabletEnvironment.sbwGun("superbwarfare", RifleGunItem.class), "a subclass");
        assertFalse(TabletEnvironment.sbwGun("superbwarfare", MortarShellItem.class));
        assertFalse(TabletEnvironment.sbwGun("minecraft", GunItem.class), "another namespace");
        assertFalse(TabletEnvironment.sbwGun("superbwarfare", Object.class));
    }

    @Test
    void schemeANeedsTheFirstPersonHandPass() {
        assertFalse(TabletEnvironment.predictNoHands(false, true, false, false, true, false),
                "first person on foot");
        assertTrue(TabletEnvironment.predictNoHands(true, true, false, false, true, false), "F1");
        assertTrue(TabletEnvironment.predictNoHands(false, false, false, false, true, false),
                "third person");
        assertTrue(TabletEnvironment.predictNoHands(false, true, true, false, true, false),
                "spectator");
        assertTrue(TabletEnvironment.predictNoHands(false, true, false, true, true, false),
                "sleeping");
        assertTrue(TabletEnvironment.predictNoHands(false, true, false, false, false, false),
                "a drone's view");
        assertTrue(TabletEnvironment.predictNoHands(false, true, false, false, true, true),
                "in a vehicle");
    }

    private static Watch watch(Object level, Object vehicle, Object camera, Object player, int slot,
                               boolean alive) {
        return new Watch(level, vehicle, camera, player, slot, alive);
    }

    @Test
    void interruptsBetweenTwoTicks() {
        Object level = new Object();
        Object player = new Object();
        Watch base = watch(level, null, player, player, 2, true);

        assertNull(TabletEnvironment.detect(base, base, true, false, false, false), "nothing");
        assertNull(TabletEnvironment.detect(base, null, true, true, true, true), "no player");
        assertEquals(Interrupt.DEATH, TabletEnvironment.detect(base,
                watch(level, null, player, player, 2, false), false, false, false, false));
        assertEquals(Interrupt.DEATH, TabletEnvironment.detect(null,
                watch(level, null, player, player, 2, false), false, false, false, false),
                "dead on the first tick");
        assertEquals(Interrupt.DIMENSION, TabletEnvironment.detect(base,
                watch(new Object(), null, player, player, 2, true), false, false, false, false));
        assertEquals(Interrupt.VEHICLE, TabletEnvironment.detect(base,
                watch(level, new Object(), player, player, 2, true), false, false, false, false));
        assertNull(TabletEnvironment.detect(watch(level, "boat", player, player, 2, true),
                watch(level, null, player, player, 2, true), false, false, false, false),
                "getting off is no interrupt");
        assertEquals(Interrupt.CAMERA, TabletEnvironment.detect(base,
                watch(level, null, new Object(), player, 2, true), false, false, false, false));
    }

    @Test
    void firingAimingAndTheHotbarOnlyCountWhileClosing() {
        Object level = new Object();
        Object player = new Object();
        Watch base = watch(level, null, player, player, 2, true);
        Watch slot = watch(level, null, player, player, 5, true);

        assertEquals(Interrupt.HOTBAR, TabletEnvironment.detect(base, slot, true, false, false, false));
        assertEquals(Interrupt.FIRE, TabletEnvironment.detect(base, base, true, true, false, false));
        assertEquals(Interrupt.AIM, TabletEnvironment.detect(base, base, true, false, true, false));
        assertEquals(Interrupt.AIM, TabletEnvironment.detect(base, base, true, false, false, true),
                "TaCZ aiming");
        assertNull(TabletEnvironment.detect(base, slot, false, true, true, true),
                "an opening ignores them: input goes to the screen");
    }

    @Test
    void anOpeningOnlyEndsForTheFourHardInterrupts() {
        for (Interrupt cause : Interrupt.values()) {
            TabletMotion motion = new TabletMotion(TabletMotion.Config.DEFAULT
                    .withDegrade(true));
            motion.open(0.0D);
            motion.update(50.0D);
            boolean ended = motion.interrupt(60.0D, cause);
            assertEquals(cause.whileOpening(), ended, cause.name());
        }
    }
}
