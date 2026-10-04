package com.wok.infantry.client;

import com.wok.infantry.formation.selection.FormationSelectionSnapshot;

import java.util.Objects;
import java.util.Optional;

/** Client cache contains display data only and is never trusted by the server. */
public final class ClientFormationState {
    private static final long FEEDBACK_DURATION_NANOS = 3_000_000_000L;
    private static FormationSelectionSnapshot snapshot;
    private static String feedback = "";
    private static boolean feedbackSuccess;
    private static long feedbackExpiresAt;
    private static LockTransition lockTransition;

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

    /**
     * Records that the viewer's faction formation just became locked. The formation batch calls this when a
     * snapshot first reports the lock; HUD overlays only read it through {@link #lockTransition()}.
     */
    public static synchronized void recordLock(String factionId, String formationId, String formationName) {
        lockTransition = new LockTransition(Objects.requireNonNullElse(factionId, ""),
                Objects.requireNonNullElse(formationId, ""), Objects.requireNonNullElse(formationName, ""),
                System.nanoTime());
    }

    /** Most recent lock seen this session, or empty; consumers decide how long to show it from {@code lockedAtNanos}. */
    public static synchronized Optional<LockTransition> lockTransition() {
        return Optional.ofNullable(lockTransition);
    }

    public static synchronized void clear() {
        snapshot = null;
        feedback = "";
        feedbackSuccess = false;
        feedbackExpiresAt = 0L;
        lockTransition = null;
    }

    /** A formation lock observed by this client; {@code lockedAtNanos} uses {@link System#nanoTime()}. */
    public record LockTransition(String factionId, String formationId, String formationName, long lockedAtNanos) {
    }
}
