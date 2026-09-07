package com.wok.infantry.support.adapter;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Per-provider circuit breaker: one broken integration cannot destabilize the framework. */
public abstract class AbstractSoftSupportProvider implements SupportProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ResourceLocation supportId;
    private final AtomicBoolean failureLogged = new AtomicBoolean();
    private volatile ProviderAvailability probed;
    private volatile String circuitReason;

    protected AbstractSoftSupportProvider(ResourceLocation supportId) {
        this.supportId = Objects.requireNonNull(supportId, "supportId");
    }

    @Override
    public final ResourceLocation supportId() {
        return supportId;
    }

    @Override
    public final ProviderAvailability availability() {
        String tripped = circuitReason;
        if (tripped != null) {
            return ProviderAvailability.unavailable(tripped);
        }
        ProviderAvailability current = probed;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            current = probed;
            if (current == null) {
                try {
                    current = Objects.requireNonNull(probeAvailability(),
                            "probeAvailability");
                } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                    current = ProviderAvailability.unavailable(
                            "适配 API 校验失败: " + concise(failure));
                }
                probed = current;
            }
        }
        return current;
    }

    @Override
    public final void executeStep(SupportSpawnContext context) throws SupportSpawnException {
        if (context == null || !supportId.equals(context.definition().id())) {
            throw new SupportSpawnException("支援任务与适配器不匹配");
        }
        ProviderAvailability available = availability();
        if (!available.available()) {
            throw new SupportSpawnException(available.reason());
        }
        try {
            doExecuteStep(context);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            String reason = "适配器已熔断: " + concise(failure);
            circuitReason = reason;
            if (failureLogged.compareAndSet(false, true)) {
                LOGGER.error("Support provider {} tripped its circuit breaker",
                        supportId, failure);
            }
            throw new SupportSpawnException(reason, failure);
        }
    }

    protected abstract ProviderAvailability probeAvailability()
            throws ReflectiveOperationException;

    protected abstract void doExecuteStep(SupportSpawnContext context)
            throws ReflectiveOperationException;

    private static String concise(Throwable failure) {
        String message = failure.getMessage();
        return failure.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : " - " + message);
    }
}
