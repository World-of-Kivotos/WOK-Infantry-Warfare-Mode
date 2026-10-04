package com.wok.infantry.support.adapter;

/**
 * Checked boundary prevents optional provider faults from escaping the server tick.
 *
 * <p>Every failure ends the current mission. The two flags tell the scheduler what else to do:
 * {@link #refundCooldown()} asks it to return the cooldown consumed at acceptance (honoured only
 * while no mission step has completed yet), and {@link #providerBroken()} trips the provider's
 * circuit breaker until the server restarts. The message is a short player-visible Chinese
 * sentence; the cause is only written to the server log.</p>
 */
public final class SupportSpawnException extends Exception {
    /** Player-visible message used when an unchecked provider fault is converted. */
    static final String PROVIDER_FAULT_MESSAGE = "支援适配器执行异常，已停用至服务器重启";

    private final boolean providerBroken;
    private final boolean refundCooldown;

    /** Ends only this mission: no circuit break and no cooldown refund. */
    public SupportSpawnException(String message) {
        this(message, null, false, false);
    }

    /** Ends only this mission: no circuit break and no cooldown refund. */
    public SupportSpawnException(String message, Throwable cause) {
        this(message, cause, false, false);
    }

    private SupportSpawnException(String message, Throwable cause,
                                  boolean providerBroken, boolean refundCooldown) {
        super(message, cause);
        this.providerBroken = providerBroken;
        this.refundCooldown = refundCooldown;
    }

    /** The mission ends; the accepted cooldown stays consumed and the provider stays usable. */
    public static SupportSpawnException endMission(String message) {
        return new SupportSpawnException(message, null, false, false);
    }

    /** The mission ends; the accepted cooldown stays consumed and the provider stays usable. */
    public static SupportSpawnException endMission(String message, Throwable cause) {
        return new SupportSpawnException(message, cause, false, false);
    }

    /** Nothing was delivered: the mission ends and the accepted cooldown is returned. */
    public static SupportSpawnException notDelivered(String message) {
        return new SupportSpawnException(message, null, false, true);
    }

    /** Nothing was delivered: the mission ends and the accepted cooldown is returned. */
    public static SupportSpawnException notDelivered(String message, Throwable cause) {
        return new SupportSpawnException(message, cause, false, true);
    }

    /**
     * The integration itself is defective: the provider is disabled until restart. The caller
     * states whether the current mission's cooldown should be returned. {@code cause} may be null.
     */
    public static SupportSpawnException providerBroken(String message, Throwable cause,
                                                       boolean refundCooldown) {
        return new SupportSpawnException(message, cause, true, refundCooldown);
    }

    public boolean providerBroken() {
        return providerBroken;
    }

    public boolean refundCooldown() {
        return refundCooldown;
    }
}
