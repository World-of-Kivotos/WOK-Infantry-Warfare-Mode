package com.wok.infantry.network.battle;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Fan-out helper for viewer-filtered packets: every recipient gets its own build and send, and a
 * runtime failure for one recipient never stops the others. Failures are reported at most once
 * per signature and {@link #REPORT_WINDOW_MILLIS} so a fault that repeats on the one-second
 * heartbeat cannot flood the log, while a fault that keeps happening (or a new one) is reported
 * again in the next window instead of being silenced for the rest of the server's lifetime.
 * Numbers and UUIDs in the message do not make a new signature.
 */
public final class PerRecipientDelivery {
    /** Distinct signatures tracked inside one window; beyond it new ones share one slot. */
    static final int MAX_REPORTED_SIGNATURES = 64;
    /** A signature is reported again once this long has passed since its last report. */
    static final long REPORT_WINDOW_MILLIS = 5L * 60L * 1000L;
    private static final int MAX_SIGNATURE_LENGTH = 240;
    private static final String OVERFLOW_SIGNATURE = "<other distinct failures>";
    private static final Pattern UUID_TEXT = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern NUMBER_TEXT = Pattern.compile("-?\\d+(?:[.,]\\d+)*");

    /** Signature to the time it was last reported, oldest first. */
    private final Map<String, Long> lastReported = new LinkedHashMap<>();

    /**
     * Runs {@code delivery} for each recipient in order and hands every {@link RuntimeException}
     * to {@code onFailure} instead of propagating it. Returns the number of failed recipients.
     */
    public static <T> int deliverEach(Iterable<? extends T> recipients,
                                      Consumer<? super T> delivery,
                                      BiConsumer<? super T, ? super RuntimeException> onFailure) {
        Objects.requireNonNull(recipients, "recipients");
        Objects.requireNonNull(delivery, "delivery");
        Objects.requireNonNull(onFailure, "onFailure");
        int failures = 0;
        for (T recipient : recipients) {
            try {
                delivery.accept(recipient);
            } catch (RuntimeException failure) {
                failures++;
                try {
                    onFailure.accept(recipient, failure);
                } catch (RuntimeException ignored) {
                    // Reporting must never be the reason the remaining recipients are skipped.
                }
            }
        }
        return failures;
    }

    /**
     * True when this failure should be logged in full now: the first time its signature is seen
     * in the current window, false for repeats inside the window.
     */
    public boolean firstReport(Throwable failure) {
        return shouldReport(failure, System.currentTimeMillis());
    }

    /**
     * Window logic of {@link #firstReport}. At most {@link #MAX_REPORTED_SIGNATURES} signatures
     * are tracked; when that many are still live, any further distinct signature shares one
     * overflow slot, so a burst of distinct faults still logs at most one extra line per window.
     */
    synchronized boolean shouldReport(Throwable failure, long nowMillis) {
        if (failure == null) {
            return false;
        }
        String key = signature(failure);
        if (!lastReported.containsKey(key) && lastReported.size() >= MAX_REPORTED_SIGNATURES) {
            lastReported.values().removeIf(reportedAt -> expired(reportedAt, nowMillis));
            if (lastReported.size() >= MAX_REPORTED_SIGNATURES) {
                key = OVERFLOW_SIGNATURE;
            }
        }
        Long reportedAt = lastReported.get(key);
        if (reportedAt != null && !expired(reportedAt, nowMillis)) {
            return false;
        }
        lastReported.remove(key);
        if (lastReported.size() >= MAX_REPORTED_SIGNATURES) {
            // Only the overflow slot reaches this with a full map: keep the bound strict.
            lastReported.remove(lastReported.keySet().iterator().next());
        }
        lastReported.put(key, nowMillis);
        return true;
    }

    private static boolean expired(long reportedAt, long nowMillis) {
        return nowMillis < reportedAt || nowMillis - reportedAt >= REPORT_WINDOW_MILLIS;
    }

    /** Exception type and message with UUIDs and numbers masked, bounded in length. */
    public static String signature(Throwable failure) {
        String message = Objects.requireNonNullElse(failure.getMessage(), "");
        message = NUMBER_TEXT.matcher(UUID_TEXT.matcher(message).replaceAll("<uuid>"))
                .replaceAll("#");
        String signature = failure.getClass().getName() + ": " + message;
        return signature.length() <= MAX_SIGNATURE_LENGTH ? signature
                : signature.substring(0, MAX_SIGNATURE_LENGTH);
    }
}
