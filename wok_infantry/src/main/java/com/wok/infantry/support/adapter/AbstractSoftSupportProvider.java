package com.wok.infantry.support.adapter;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * Per-provider circuit breaker: one broken integration cannot destabilize the framework.
 *
 * <p>Subclasses report ordinary mission outcomes with {@link SupportSpawnException#endMission}
 * or {@link SupportSpawnException#notDelivered}; those never trip the circuit. Reflection,
 * runtime and linkage faults, and explicit {@link SupportSpawnException#providerBroken}
 * failures disable the provider until the server restarts. The trip is logged once by the
 * mandatory {@link GuardedSupportProvider} wrapper, not here, so one fault is one log entry.</p>
 */
public abstract class AbstractSoftSupportProvider implements SupportProvider {
    private final ResourceLocation supportId;
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
            throw SupportSpawnException.endMission("支援任务与适配器不匹配");
        }
        ProviderAvailability available = availability();
        if (!available.available()) {
            throw SupportSpawnException.endMission(available.reason());
        }
        runStep(() -> doExecuteStep(context));
    }

    /**
     * Runs one subclass step and applies the circuit policy. Ordinary
     * {@link SupportSpawnException}s pass through unchanged; reflection, runtime and linkage
     * faults become non-refunding provider-broken failures. Package-private so the policy can be
     * verified without a {@link net.minecraft.server.level.ServerLevel}.
     */
    final void runStep(StepAction action) throws SupportSpawnException {
        try {
            action.run();
        } catch (SupportSpawnException failure) {
            if (failure.providerBroken()) {
                String message = failure.getMessage();
                trip(message == null || message.isBlank() ? "执行失败" : message);
            }
            throw failure;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            trip(concise(failure));
            throw SupportSpawnException.providerBroken(
                    SupportSpawnException.PROVIDER_FAULT_MESSAGE, failure, false);
        }
    }

    protected abstract ProviderAvailability probeAvailability()
            throws ReflectiveOperationException;

    protected abstract void doExecuteStep(SupportSpawnContext context)
            throws ReflectiveOperationException, SupportSpawnException;

    /** Records the circuit state only; the rethrown failure carries the stack to the guard. */
    private void trip(String summary) {
        circuitReason = ProviderAvailability.unavailable("适配器已熔断: " + summary).reason();
    }

    private static String concise(Throwable failure) {
        String message = failure.getMessage();
        return failure.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : " - " + message);
    }

    @FunctionalInterface
    interface StepAction {
        void run() throws ReflectiveOperationException, SupportSpawnException;
    }
}
