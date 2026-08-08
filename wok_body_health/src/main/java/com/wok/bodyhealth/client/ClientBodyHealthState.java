package com.wok.bodyhealth.client;

import com.wok.bodyhealth.health.BodyHealthSnapshot;
import com.wok.bodyhealth.health.BodyPart;
import net.minecraft.Util;

public final class ClientBodyHealthState {
    private static final long HIT_FLASH_MILLIS = 260L;
    private static volatile BodyHealthSnapshot snapshot;
    private static final long[] lastDamageMillis = new long[BodyPart.values().length];

    public static void accept(BodyHealthSnapshot newSnapshot) {
        BodyHealthSnapshot previous = snapshot;
        if (previous != null
                && previous.current().length == newSnapshot.current().length) {
            long now = Util.getMillis();
            for (int index = 0; index < newSnapshot.current().length; index++) {
                if (newSnapshot.current()[index] < previous.current()[index] - 0.001F) {
                    lastDamageMillis[index] = now;
                }
            }
        }
        snapshot = newSnapshot;
    }

    public static BodyHealthSnapshot get() {
        return snapshot;
    }

    public static float ratio(BodyPart part) {
        BodyHealthSnapshot currentSnapshot = snapshot;
        if (currentSnapshot == null) {
            return 1.0F;
        }
        int index = part.ordinal();
        float maximum = currentSnapshot.maximum()[index];
        return maximum <= 0.0F ? 0.0F
                : Math.max(0.0F, Math.min(1.0F, currentSnapshot.current()[index] / maximum));
    }

    public static int value(BodyPart part) {
        BodyHealthSnapshot currentSnapshot = snapshot;
        if (currentSnapshot == null) {
            return 0;
        }
        return Math.round(currentSnapshot.current()[part.ordinal()]);
    }

    public static float hitFlash(BodyPart part) {
        long elapsed = Util.getMillis() - lastDamageMillis[part.ordinal()];
        if (elapsed < 0L || elapsed >= HIT_FLASH_MILLIS) {
            return 0.0F;
        }
        return 1.0F - elapsed / (float) HIT_FLASH_MILLIS;
    }

    private ClientBodyHealthState() {
    }
}
