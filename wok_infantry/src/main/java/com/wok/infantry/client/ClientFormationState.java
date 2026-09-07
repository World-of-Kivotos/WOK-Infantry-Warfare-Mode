package com.wok.infantry.client;

import com.wok.infantry.formation.selection.FormationSelectionSnapshot;

import java.util.Objects;

/** Client cache contains display data only and is never trusted by the server. */
public final class ClientFormationState {
    private static final long FEEDBACK_DURATION_NANOS = 3_000_000_000L;
    private static FormationSelectionSnapshot snapshot;
    private static String feedback = "";
    private static boolean feedbackSuccess;
    private static long feedbackExpiresAt;

    private ClientFormationState() {
    }

    public static synchronized FormationSelectionSnapshot snapshot() {
        return snapshot;
    }

    public static synchronized void update(FormationSelectionSnapshot replacement) {
        snapshot = replacement;
    }

    public static synchronized void feedback(boolean success, String message) {
        feedbackSuccess = success;
        feedback = Objects.requireNonNullElse(message, "");
        feedbackExpiresAt = feedback.isBlank() ? 0L
                : System.nanoTime() + FEEDBACK_DURATION_NANOS;
    }

    public static synchronized String feedback() {
        if (feedbackExpiresAt != 0L && System.nanoTime() >= feedbackExpiresAt) {
            feedback = "";
            feedbackExpiresAt = 0L;
        }
        return feedback;
    }

    public static synchronized boolean feedbackSuccess() {
        return feedbackSuccess;
    }

    public static synchronized void clear() {
        snapshot = null;
        feedback = "";
        feedbackSuccess = false;
        feedbackExpiresAt = 0L;
    }
}
