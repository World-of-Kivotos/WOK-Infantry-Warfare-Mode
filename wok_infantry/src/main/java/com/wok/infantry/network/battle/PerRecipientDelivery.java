package com.wok.infantry.network.battle;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Fan-out helper for viewer-filtered packets: every recipient gets its own build and send, and a
 * runtime failure for one recipient never stops the others. Failures are reported once per
 * distinct signature so a fault that repeats on the one-second heartbeat cannot flood the log.
 */
final class PerRecipientDelivery {
    static final int MAX_REPORTED_SIGNATURES = 64;
    private static final int MAX_SIGNATURE_LENGTH = 240;

    private final Set<String> reportedSignatures = ConcurrentHashMap.newKeySet();

    /**
     * Runs {@code delivery} for each recipient in order and hands every {@link RuntimeException}
     * to {@code onFailure} instead of propagating it. Returns the number of failed recipients.
     */
    static <T> int deliverEach(Iterable<? extends T> recipients,
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
     * True the first time this failure signature is seen, false for repeats. After
     * {@link #MAX_REPORTED_SIGNATURES} distinct signatures every further one counts as a repeat.
     */
    boolean firstReport(Throwable failure) {
        if (failure == null) {
            return false;
        }
        String signature = signature(failure);
        if (reportedSignatures.contains(signature)
                || reportedSignatures.size() >= MAX_REPORTED_SIGNATURES) {
            return false;
        }
        return reportedSignatures.add(signature);
    }

    static String signature(Throwable failure) {
        String signature = failure.getClass().getName() + ": "
                + Objects.requireNonNullElse(failure.getMessage(), "");
        return signature.length() <= MAX_SIGNATURE_LENGTH ? signature
                : signature.substring(0, MAX_SIGNATURE_LENGTH);
    }
}
