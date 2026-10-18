package com.wok.infantry.network.battle.client;

import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.network.battle.BattleOpenTarget;

import java.util.Objects;

/**
 * Dist-safe handoff from S2C packets to the optional client state and screens.
 * Client bootstrap installs the real handler without making common packet classes load UI code.
 */
public final class BattleClientPacketBridge {
    private static final Handler NOOP = new Handler() {
    };

    private static volatile Handler handler = NOOP;
    private static BattleSnapshot pendingSnapshot;
    private static BattleOpenTarget pendingOpenTarget = BattleOpenTarget.NONE;
    private static String pendingFeedback;
    private static boolean pendingFeedbackSuccess;

    private BattleClientPacketBridge() {
    }

    public static synchronized void install(Handler replacement) {
        handler = Objects.requireNonNull(replacement, "replacement");
        if (pendingSnapshot != null) {
            BattleSnapshot snapshot = pendingSnapshot;
            BattleOpenTarget openTarget = pendingOpenTarget;
            pendingSnapshot = null;
            pendingOpenTarget = BattleOpenTarget.NONE;
            replacement.applySnapshot(snapshot, openTarget);
        }
        if (pendingFeedback != null) {
            String message = pendingFeedback;
            boolean success = pendingFeedbackSuccess;
            pendingFeedback = null;
            replacement.feedback(success, message);
        }
    }

    public static synchronized void reset() {
        handler = NOOP;
        pendingSnapshot = null;
        pendingOpenTarget = BattleOpenTarget.NONE;
        pendingFeedback = null;
    }

    public static synchronized void applySnapshot(BattleSnapshot snapshot,
                                                   BattleOpenTarget openTarget) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(openTarget, "openTarget");
        Handler current = handler;
        if (current == NOOP) {
            pendingSnapshot = snapshot;
            pendingOpenTarget = openTarget;
            return;
        }
        current.applySnapshot(snapshot, openTarget);
    }

    public static synchronized void clear() {
        pendingSnapshot = null;
        pendingOpenTarget = BattleOpenTarget.NONE;
        handler.clear();
    }

    public static synchronized void feedback(boolean success, String message) {
        Handler current = handler;
        if (current == NOOP) {
            pendingFeedbackSuccess = success;
            pendingFeedback = Objects.requireNonNullElse(message, "");
            return;
        }
        current.feedback(success, Objects.requireNonNullElse(message, ""));
    }

    public interface Handler {
        default void applySnapshot(BattleSnapshot snapshot, BattleOpenTarget openTarget) {
        }

        default void clear() {
        }

        default void feedback(boolean success, String message) {
        }
    }
}
