package com.wok.infantry.support.adapter;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mandatory circuit-breaker wrapper for every registered provider, including direct interface
 * implementations that do not extend {@link AbstractSoftSupportProvider}.
 *
 * <p>Only a {@link SupportSpawnException#providerBroken() provider-broken} failure or an
 * unchecked runtime/linkage fault trips the circuit. Ordinary mission failures (end mission,
 * not delivered) pass through unchanged and leave the provider usable.</p>
 *
 * <p>This wrapper is the only layer that logs a circuit trip, once per provider with the full
 * stack trace; {@link AbstractSoftSupportProvider} only records its own circuit state.</p>
 */
public final class GuardedSupportProvider implements SupportProvider {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final SupportProvider delegate;
    private final ResourceLocation supportId;
    private final AtomicBoolean failureLogged = new AtomicBoolean();
    private volatile String circuitReason;

    private GuardedSupportProvider(ResourceLocation supportId, SupportProvider delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.supportId = Objects.requireNonNull(supportId, "supportId");
    }

    public static SupportProvider guard(SupportProvider provider) {
        Objects.requireNonNull(provider, "provider");
        return guard(Objects.requireNonNull(provider.supportId(), "provider.supportId"), provider);
    }

    /** Uses the id captured during atomic registration instead of probing a mutable provider twice. */
    public static SupportProvider guard(ResourceLocation registeredId, SupportProvider provider) {
        Objects.requireNonNull(registeredId, "registeredId");
        Objects.requireNonNull(provider, "provider");
        if (provider instanceof GuardedSupportProvider guarded
                && guarded.supportId.equals(registeredId)) {
            return guarded;
        }
        return new GuardedSupportProvider(registeredId, provider);
    }

    @Override
    public ResourceLocation supportId() {
        return supportId;
    }

    @Override
    public ProviderAvailability availability() {
        String tripped = circuitReason;
        if (tripped != null) {
            return ProviderAvailability.unavailable(tripped);
        }
        try {
            ProviderAvailability availability = delegate.availability();
            if (availability == null) {
                return trip("可用性检查返回 null", null);
            }
            return availability;
        } catch (RuntimeException | LinkageError failure) {
            return trip("可用性检查失败", failure);
        }
    }

    /**
     * Forwards the acceptance cue unless the circuit has tripped. A failing cue is only logged:
     * it is cosmetic, so it never trips the circuit or reaches the scheduler.
     */
    @Override
    public void accepted(SupportSpawnContext context) {
        if (circuitReason != null) {
            return;
        }
        try {
            delegate.accepted(context);
        } catch (Exception | LinkageError failure) {
            // Exception also covers checked failures rethrown "sneakily" by a direct
            // implementation; none of them may escape into an already accepted request.
            LOGGER.warn("Support provider {} failed its acceptance cue", supportId, failure);
        }
    }

    @Override
    public void executeStep(SupportSpawnContext context) throws SupportSpawnException {
        ProviderAvailability available = availability();
        if (!available.available()) {
            // The delegate was not invoked. Whether an unstarted mission gets its cooldown back
            // is decided by the scheduler, which checks availability before every step.
            throw SupportSpawnException.endMission(available.reason());
        }
        try {
            delegate.executeStep(context);
        } catch (SupportSpawnException failure) {
            if (failure.providerBroken()) {
                trip(failure);
            }
            // Ordinary mission failures keep their message and refund flag untouched.
            throw failure;
        } catch (Exception | LinkageError failure) {
            // Exception also covers checked reflection failures rethrown "sneakily" by a direct
            // implementation; every such fault is treated as a defective integration.
            trip("执行异常", failure);
            throw SupportSpawnException.providerBroken(
                    SupportSpawnException.PROVIDER_FAULT_MESSAGE, failure, false);
        }
    }

    /** Cleanup runs even with a tripped circuit; a failing cleanup is only logged. */
    @Override
    public void abandon(SupportSpawnContext context) {
        try {
            delegate.abandon(context);
        } catch (RuntimeException | LinkageError failure) {
            LOGGER.warn("Support provider {} failed to clean up an abandoned mission",
                    supportId, failure);
        }
    }

    private void trip(SupportSpawnException failure) {
        String message = failure.getMessage();
        if (failure.getCause() != null
                && SupportSpawnException.PROVIDER_FAULT_MESSAGE.equals(message)) {
            // A provider-side conversion of an unchecked fault: show the root fault instead.
            trip("执行异常", failure.getCause());
            return;
        }
        String summary = message == null || message.isBlank() ? "执行失败" : message;
        circuitReason = ProviderAvailability.unavailable("适配器已熔断: " + summary).reason();
        if (failureLogged.compareAndSet(false, true)) {
            LOGGER.error("Support provider {} tripped its circuit breaker: {}",
                    supportId, summary, failure);
        }
    }

    private ProviderAvailability trip(String summary, Throwable failure) {
        String reason = "适配器已熔断: " + summary;
        if (failure != null) {
            String message = failure.getMessage();
            reason += " - " + failure.getClass().getSimpleName()
                    + (message == null || message.isBlank() ? "" : ": " + message);
        }
        reason = ProviderAvailability.unavailable(reason).reason();
        circuitReason = reason;
        if (failureLogged.compareAndSet(false, true)) {
            if (failure == null) {
                LOGGER.error("Support provider {} tripped its circuit breaker: {}",
                        supportId, summary);
            } else {
                LOGGER.error("Support provider {} tripped its circuit breaker: {}",
                        supportId, summary, failure);
            }
        }
        return ProviderAvailability.unavailable(reason);
    }
}
