package com.wok.infantry.support.adapter;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mandatory circuit-breaker wrapper for every registered provider, including direct interface
 * implementations that do not extend {@link AbstractSoftSupportProvider}.
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

    @Override
    public void executeStep(SupportSpawnContext context) throws SupportSpawnException {
        ProviderAvailability available = availability();
        if (!available.available()) {
            throw new SupportSpawnException(available.reason());
        }
        try {
            delegate.executeStep(context);
        } catch (SupportSpawnException failure) {
            String reason = trip("执行失败", failure).reason();
            throw new SupportSpawnException(reason, failure);
        } catch (RuntimeException | LinkageError failure) {
            String reason = trip("执行异常", failure).reason();
            throw new SupportSpawnException(reason, failure);
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
