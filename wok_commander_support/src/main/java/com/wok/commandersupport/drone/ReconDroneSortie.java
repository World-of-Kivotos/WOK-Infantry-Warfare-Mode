package com.wok.commandersupport.drone;

import java.lang.ref.WeakReference;

/**
 * Link between one drone mission and its airframe. The provider keeps it between steps; the
 * drone reports its shoot-down here, so the next step still knows why the drone is gone after
 * the wreck has been removed. Only a weak reference to the entity is held, so a mission that
 * vanished without a callback never keeps a level alive.
 */
final class ReconDroneSortie {
    private final WeakReference<ReconDroneEntity> drone;
    private volatile boolean shotDown;

    ReconDroneSortie(ReconDroneEntity drone) {
        this.drone = new WeakReference<>(drone);
    }

    /** The airframe while it is still referenced anywhere, otherwise null. */
    ReconDroneEntity drone() {
        return drone.get();
    }

    void markShotDown() {
        shotDown = true;
    }

    boolean shotDown() {
        return shotDown;
    }
}
