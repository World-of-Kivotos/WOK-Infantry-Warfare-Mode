package com.wok.infantry.client.map;

import com.mojang.blaze3d.platform.NativeImage;
import com.wok.infantry.WokInfantryMod;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Terrain-provider boundary between WOK Infantry UI code and JourneyMap 6.
 *
 * <p>Core/client screen code only knows this registry. The required JourneyMap plugin registers
 * its API-backed provider when JourneyMap discovers it.</p>
 */
public final class TacticalMapTerrainRegistry {
    private static final int MAX_GLOBAL_PENDING_REQUESTS = 8;
    private static final Object PENDING_LOCK = new Object();
    private static final Map<TacticalMapTerrainProvider,
            Map<TacticalMapTerrainRequest, PendingRequest>> PENDING_BY_PROVIDER =
            new IdentityHashMap<>();
    /** Number of host API tasks that have started but whose guarded callback has not completed. */
    private static int activeHostTaskCount;

    private static final TacticalMapTerrainProvider NONE = new TacticalMapTerrainProvider() {
        @Override
        public String id() {
            return "grid";
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void requestTile(TacticalMapTerrainRequest request,
                                Consumer<NativeImage> callback) {
            callback.accept(null);
        }
    };

    private static final AtomicReference<ProviderState> ACTIVE =
            new AtomicReference<>(new ProviderState(NONE, 0L));

    private TacticalMapTerrainRegistry() {
    }

    public static void register(TacticalMapTerrainProvider provider) {
        TacticalMapTerrainProvider registered = Objects.requireNonNull(provider, "provider");
        ProviderState state = ACTIVE.updateAndGet(current ->
                new ProviderState(registered, current.generation() + 1L));
        WokInfantryMod.LOGGER.info("Registered tactical map terrain provider: {} (generation {})",
                provider.id(), state.generation());
    }

    public static void unregister(TacticalMapTerrainProvider provider) {
        if (provider == null) {
            return;
        }
        while (true) {
            ProviderState current = ACTIVE.get();
            if (current.provider() != provider) {
                return;
            }
            ProviderState replacement = new ProviderState(NONE, current.generation() + 1L);
            if (ACTIVE.compareAndSet(current, replacement)) {
                WokInfantryMod.LOGGER.info(
                        "Unregistered tactical map terrain provider: {} (generation {})",
                        provider.id(), replacement.generation());
                return;
            }
        }
    }

    public static String activeProviderId() {
        return ACTIVE.get().provider().id();
    }

    /**
     * Monotonic identity token for the active provider. Unlike {@link #activeProviderId()}, this
     * changes when a provider is replaced by another instance with the same public id.
     */
    public static long activeProviderGeneration() {
        return ACTIVE.get().generation();
    }

    public static boolean isReady() {
        TacticalMapTerrainProvider provider = ACTIVE.get().provider();
        return provider != NONE && provider.isReady();
    }

    public static void requestTile(TacticalMapTerrainRequest request,
                                   Consumer<NativeImage> callback) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(callback, "callback");
        ProviderState providerState = ACTIVE.get();
        TacticalMapTerrainProvider provider = providerState.provider();
        long providerGeneration = providerState.generation();
        if (provider == NONE || !provider.isReady()) {
            callback.accept(null);
            return;
        }

        PendingRequest pending = null;
        Consumer<NativeImage> displacedCallback = null;
        boolean rejected = false;
        synchronized (PENDING_LOCK) {
            Map<TacticalMapTerrainRequest, PendingRequest> providerRequests =
                    PENDING_BY_PROVIDER.get(provider);
            PendingRequest existing = providerRequests == null
                    ? null : providerRequests.get(request);
            if (existing != null) {
                if (existing.providerGeneration == providerGeneration) {
                    // Only one tactical map can be active in a client. Replacing the subscriber
                    // keeps a reopened/recentered screen from retaining the obsolete Screen
                    // instance while the provider continues the same host request.
                    existing.callback = callback;
                    return;
                }
                // The same provider instance may have been re-registered. Detach its old
                // generation so the current screen gets a fresh host request. The old guarded
                // callback will observe the pending mismatch and close any late image.
                providerRequests.remove(request);
                displacedCallback = existing.callback;
                if (providerRequests.isEmpty()) {
                    PENDING_BY_PROVIDER.remove(provider);
                    providerRequests = null;
                }
            }
            if (activeHostTaskCount >= MAX_GLOBAL_PENDING_REQUESTS) {
                rejected = true;
            } else {
                if (providerRequests == null) {
                    providerRequests = new HashMap<>();
                    PENDING_BY_PROVIDER.put(provider, providerRequests);
                }
                pending = new PendingRequest(callback, providerGeneration);
                providerRequests.put(request, pending);
                activeHostTaskCount++;
            }
        }
        if (displacedCallback != null) {
            try {
                displacedCallback.accept(null);
            } catch (RuntimeException exception) {
                WokInfantryMod.LOGGER.warn("Obsolete tactical terrain callback failed",
                        exception);
            }
        }
        if (rejected) {
            // Never invoke arbitrary subscribers while holding the global coordinator lock.
            callback.accept(null);
            return;
        }

        ProviderCallbackGuard guardedCallback = new ProviderCallbackGuard(
                provider, request, Objects.requireNonNull(pending));
        try {
            provider.requestTile(request, guardedCallback);
        } catch (RuntimeException exception) {
            WokInfantryMod.LOGGER.warn("Tactical terrain provider {} rejected a tile request",
                    provider.id(), exception);
            guardedCallback.accept(null);
        }
    }

    private static void completeRequest(TacticalMapTerrainProvider provider,
                                        TacticalMapTerrainRequest request,
                                        PendingRequest pending,
                                        NativeImage image) {
        Consumer<NativeImage> callback;
        boolean pendingStillCurrent;
        synchronized (PENDING_LOCK) {
            // A detached old-generation subscriber no longer occupies the request map, but its
            // host API task remains real work until this first guarded completion. Returning the
            // slot here (and only here) keeps the global cap truthful across provider reloads.
            activeHostTaskCount--;
            Map<TacticalMapTerrainRequest, PendingRequest> providerRequests =
                    PENDING_BY_PROVIDER.get(provider);
            pendingStillCurrent = providerRequests != null
                    && providerRequests.get(request) == pending;
            if (pendingStillCurrent) {
                providerRequests.remove(request);
                if (providerRequests.isEmpty()) {
                    PENDING_BY_PROVIDER.remove(provider);
                }
                callback = pending.callback;
            } else {
                callback = null;
            }
        }
        if (!pendingStillCurrent) {
            if (image != null) {
                image.close();
            }
            return;
        }

        ProviderState activeState = ACTIVE.get();
        boolean providerCurrentAndReady = activeState.provider() == provider
                && activeState.generation() == pending.providerGeneration;
        if (providerCurrentAndReady) {
            try {
                providerCurrentAndReady = provider.isReady();
            } catch (RuntimeException exception) {
                providerCurrentAndReady = false;
                WokInfantryMod.LOGGER.warn(
                        "Tactical terrain provider {} failed its readiness check",
                        provider.id(), exception);
            }
        }
        if (!providerCurrentAndReady) {
            if (image != null) {
                image.close();
                image = null;
            }
        }
        try {
            callback.accept(image);
        } catch (RuntimeException exception) {
            if (image != null) {
                image.close();
            }
            WokInfantryMod.LOGGER.warn("Tactical terrain callback failed", exception);
        }
    }

    /**
     * A host API is allowed to complete synchronously and, defensively, may even invoke its
     * callback more than once. The first image transfers to the subscriber. A repeated callback
     * only closes a different image; closing the exact same instance would invalidate the image
     * already transferred to (or queued by) the client screen.
     */
    private static final class ProviderCallbackGuard implements Consumer<NativeImage> {
        private final TacticalMapTerrainProvider provider;
        private final TacticalMapTerrainRequest request;
        private final PendingRequest pending;
        private boolean completed;
        private NativeImage firstImage;

        private ProviderCallbackGuard(TacticalMapTerrainProvider provider,
                                      TacticalMapTerrainRequest request,
                                      PendingRequest pending) {
            this.provider = provider;
            this.request = request;
            this.pending = pending;
        }

        @Override
        public void accept(NativeImage image) {
            boolean firstCompletion;
            boolean closeDuplicate;
            synchronized (this) {
                firstCompletion = !completed;
                if (firstCompletion) {
                    completed = true;
                    firstImage = image;
                }
                closeDuplicate = !firstCompletion && image != null && image != firstImage;
            }
            if (firstCompletion) {
                completeRequest(provider, request, pending, image);
            } else if (closeDuplicate) {
                image.close();
            }
        }
    }

    static void resetForTests() {
        ACTIVE.set(new ProviderState(NONE, 0L));
        synchronized (PENDING_LOCK) {
            PENDING_BY_PROVIDER.clear();
            activeHostTaskCount = 0;
        }
    }

    private static final class PendingRequest {
        private Consumer<NativeImage> callback;
        private final long providerGeneration;

        private PendingRequest(Consumer<NativeImage> callback, long providerGeneration) {
            this.callback = callback;
            this.providerGeneration = providerGeneration;
        }
    }

    private record ProviderState(TacticalMapTerrainProvider provider, long generation) {
    }
}
