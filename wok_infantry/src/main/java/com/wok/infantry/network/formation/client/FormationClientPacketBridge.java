package com.wok.infantry.network.formation.client;

import com.wok.infantry.formation.selection.FormationSelectionSnapshot;

import java.util.Objects;

/** Dist-safe bridge so common packet classes never directly load client screen classes. */
public final class FormationClientPacketBridge {
    private static final Handler NOOP = new Handler() {
    };
    private static Handler handler = NOOP;
    private static FormationSelectionSnapshot pending;
    private static boolean pendingOpen;
    private static boolean pendingLockNotice;
    private static String pendingMessage;
    private static boolean pendingSuccess;

    private FormationClientPacketBridge() {
    }

    public static synchronized void install(Handler replacement) {
        handler = Objects.requireNonNull(replacement, "replacement");
        if (pending != null) {
            FormationSelectionSnapshot snapshot = pending;
            boolean open = pendingOpen;
            boolean lockNotice = pendingLockNotice;
            pending = null;
            pendingOpen = false;
            pendingLockNotice = false;
            replacement.apply(snapshot, open, lockNotice);
        }
        if (pendingMessage != null) {
            String message = pendingMessage;
            boolean success = pendingSuccess;
            pendingMessage = null;
            replacement.feedback(success, message);
        }
    }

    public static synchronized void apply(FormationSelectionSnapshot snapshot,
                                          boolean openScreen) {
        apply(snapshot, openScreen, false);
    }

    /**
     * Delivers a catalog; {@code lockNotice} marks that the faction's shared formation was just
     * applied to the viewer. Before the client handler is installed only the latest catalog is
     * kept (a buffered lock notice survives a later catalog without one).
     */
    public static synchronized void apply(FormationSelectionSnapshot snapshot,
                                          boolean openScreen, boolean lockNotice) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (handler == NOOP) {
            pending = snapshot;
            pendingOpen = openScreen;
            pendingLockNotice = pendingLockNotice || lockNotice;
        } else {
            handler.apply(snapshot, openScreen, lockNotice);
        }
    }

    public static synchronized void feedback(boolean success, String message) {
        if (handler == NOOP) {
            pendingSuccess = success;
            pendingMessage = Objects.requireNonNullElse(message, "");
        } else {
            handler.feedback(success, Objects.requireNonNullElse(message, ""));
        }
    }

    public interface Handler {
        default void apply(FormationSelectionSnapshot snapshot, boolean openScreen) {
        }

        /** Catalog with the lock notice flag; defaults to {@link #apply(FormationSelectionSnapshot, boolean)}. */
        default void apply(FormationSelectionSnapshot snapshot, boolean openScreen,
                           boolean lockNotice) {
            apply(snapshot, openScreen);
        }

        default void feedback(boolean success, String message) {
        }
    }
}
