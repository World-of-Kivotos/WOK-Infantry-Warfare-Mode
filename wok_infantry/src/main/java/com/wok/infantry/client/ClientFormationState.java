package com.wok.infantry.client;

import com.wok.infantry.formation.selection.FormationSelectionSnapshot;

import java.util.Objects;
import java.util.Optional;

/** Client cache contains display data only and is never trusted by the server. */
public final class ClientFormationState {
    /**
     * A catalog request still unanswered after this long counts as lost (the server drops
     * requests over its rate limit silently); the waiting page then offers a retry.
     */
    public static final long CATALOG_TIMEOUT_NANOS = 3_000_000_000L;
    private static final long FEEDBACK_DURATION_NANOS = 3_000_000_000L;
    private static FormationSelectionSnapshot snapshot;
    private static String feedback = "";
    private static FeedbackKind feedbackKind = FeedbackKind.DANGER;
    private static long feedbackExpiresAt;
    private static LockTransition lockTransition;
    /** {@link System#nanoTime()} of the last unanswered catalog request, 0 = none pending. */
    private static long catalogRequestedAt;

    /**
     * Kind of the 3-second vote-page receipt: green confirmed success, red failure, or orange
     * "request sent, waiting for the server" (an administrator command, a join or a vote).
     */
    public enum FeedbackKind {
        SUCCESS,
        DANGER,
        PENDING
    }

    private ClientFormationState() {
    }

    public static synchronized FormationSelectionSnapshot snapshot() {
        return snapshot;
    }

    public static synchronized void update(FormationSelectionSnapshot replacement) {
        snapshot = replacement;
        catalogRequestedAt = 0L;
    }

    /** The client just asked the server for the catalog; any arriving catalog answers it. */
    public static synchronized void catalogRequested() {
        long now = System.nanoTime();
        catalogRequestedAt = now == 0L ? 1L : now;
    }

    /** Whether the last catalog request is still unanswered after {@link #CATALOG_TIMEOUT_NANOS}. */
    public static synchronized boolean catalogOverdue() {
        return catalogOverdue(catalogRequestedAt, System.nanoTime());
    }

    /**
     * Pure rule of {@link #catalogOverdue()}: a request sent at {@code requestedAtNanos}
     * (0 = none pending) is overdue at {@code nowNanos} once the timeout has passed.
     */
    public static boolean catalogOverdue(long requestedAtNanos, long nowNanos) {
        return requestedAtNanos != 0L && nowNanos - requestedAtNanos >= CATALOG_TIMEOUT_NANOS;
    }

    /** Server receipt: green on success, red on failure (kept for existing callers). */
    public static synchronized void feedback(boolean success, String message) {
        feedback(success ? FeedbackKind.SUCCESS : FeedbackKind.DANGER, message);
    }

    /** Receipt of the given kind for 3 seconds; a blank message clears it. */
    public static synchronized void feedback(FeedbackKind kind, String message) {
        feedbackKind = Objects.requireNonNullElse(kind, FeedbackKind.DANGER);
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
        return feedbackKind == FeedbackKind.SUCCESS;
    }

    public static synchronized FeedbackKind feedbackKind() {
        return feedbackKind;
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
        feedbackKind = FeedbackKind.DANGER;
        feedbackExpiresAt = 0L;
        lockTransition = null;
        catalogRequestedAt = 0L;
    }

    /** A formation lock observed by this client; {@code lockedAtNanos} uses {@link System#nanoTime()}. */
    public record LockTransition(String factionId, String formationId, String formationName, long lockedAtNanos) {
    }
}
