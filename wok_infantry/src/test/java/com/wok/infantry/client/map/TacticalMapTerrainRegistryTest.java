package com.wok.infantry.client.map;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TacticalMapTerrainRegistryTest {
    private static final ResourceLocation OVERWORLD =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");

    @AfterEach
    void resetRegistry() {
        TacticalMapTerrainRegistry.resetForTests();
    }

    @Test
    void coalescesSameHostRequestAndDeliversOnlyToNewestScreen() {
        FakeProvider provider = new FakeProvider();
        TacticalMapTerrainRegistry.register(provider);
        TacticalMapTerrainRequest request = request(0);
        AtomicInteger obsoleteCallbacks = new AtomicInteger();
        AtomicInteger currentCallbacks = new AtomicInteger();

        TacticalMapTerrainRegistry.requestTile(request,
                image -> obsoleteCallbacks.incrementAndGet());
        TacticalMapTerrainRegistry.requestTile(request,
                image -> currentCallbacks.incrementAndGet());

        assertEquals(1, provider.requestCount);
        provider.complete(request);
        assertEquals(0, obsoleteCallbacks.get());
        assertEquals(1, currentCallbacks.get());
    }

    @Test
    void globalPendingBudgetRejectsNewWorkWithoutStartingMoreHostTasks() {
        FakeProvider provider = new FakeProvider();
        TacticalMapTerrainRegistry.register(provider);
        AtomicInteger callbacks = new AtomicInteger();

        for (int index = 0; index < 9; index++) {
            TacticalMapTerrainRegistry.requestTile(request(index),
                    image -> callbacks.incrementAndGet());
        }

        assertEquals(8, provider.requestCount);
        assertEquals(1, callbacks.get());
        for (int index = 0; index < 8; index++) {
            provider.complete(request(index));
        }
        assertEquals(9, callbacks.get());
    }

    @Test
    void synchronousCompletionReleasesTheGlobalBudgetImmediately() {
        AtomicInteger hostRequests = new AtomicInteger();
        AtomicInteger callbacks = new AtomicInteger();
        TacticalMapTerrainRegistry.register(new TacticalMapTerrainProvider() {
            @Override
            public String id() {
                return "synchronous";
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void requestTile(TacticalMapTerrainRequest request,
                                    Consumer<NativeImage> callback) {
                hostRequests.incrementAndGet();
                callback.accept(null);
            }
        });

        for (int index = 0; index < 16; index++) {
            TacticalMapTerrainRegistry.requestTile(request(index),
                    image -> callbacks.incrementAndGet());
        }

        assertEquals(16, hostRequests.get());
        assertEquals(16, callbacks.get());
    }

    @Test
    void repeatedCallbackWithSameImageDoesNotCloseTransferredImage() {
        FakeProvider provider = new FakeProvider();
        TacticalMapTerrainRegistry.register(provider);
        TacticalMapTerrainRequest request = request(0);
        AtomicReference<NativeImage> received = new AtomicReference<>();
        NativeImage image = new NativeImage(1, 1, true);
        try {
            TacticalMapTerrainRegistry.requestTile(request, received::set);

            provider.invoke(request, image);
            provider.invoke(request, image);

            assertSame(image, received.get());
            assertDoesNotThrow(() -> image.getPixelRGBA(0, 0));
        } finally {
            image.close();
        }
    }

    @Test
    void lateResultFromReplacedProviderIsDroppedAndReleasesSubscriber() {
        FakeProvider oldProvider = new FakeProvider();
        FakeProvider replacement = new FakeProvider();
        TacticalMapTerrainRegistry.register(oldProvider);
        TacticalMapTerrainRequest request = request(0);
        AtomicReference<NativeImage> received = new AtomicReference<>();
        NativeImage image = new NativeImage(1, 1, true);
        TacticalMapTerrainRegistry.requestTile(request, received::set);

        TacticalMapTerrainRegistry.register(replacement);
        oldProvider.invoke(request, image);

        assertNull(received.get());
    }

    @Test
    void generationChangesEvenWhenProviderPublicIdOrInstanceIsReused() {
        FakeProvider provider = new FakeProvider();
        TacticalMapTerrainRegistry.register(provider);
        long firstGeneration = TacticalMapTerrainRegistry.activeProviderGeneration();

        TacticalMapTerrainRegistry.register(provider);

        assertTrue(TacticalMapTerrainRegistry.activeProviderGeneration() > firstGeneration);
    }

    @Test
    void sameInstanceReregistrationStartsFreshHostRequest() {
        FakeProvider provider = new FakeProvider();
        TacticalMapTerrainRegistry.register(provider);
        TacticalMapTerrainRequest request = request(0);
        AtomicInteger obsoleteCallbacks = new AtomicInteger();
        AtomicInteger currentCallbacks = new AtomicInteger();
        TacticalMapTerrainRegistry.requestTile(request,
                image -> obsoleteCallbacks.incrementAndGet());

        TacticalMapTerrainRegistry.register(provider);
        TacticalMapTerrainRegistry.requestTile(request,
                image -> currentCallbacks.incrementAndGet());

        assertEquals(2, provider.requestCount);
        assertEquals(1, obsoleteCallbacks.get());
        provider.invoke(request, 0, null);
        assertEquals(0, currentCallbacks.get());
        provider.invoke(request, 1, null);
        assertEquals(1, currentCallbacks.get());
    }

    @Test
    void detachedGenerationDoesNotReturnHostBudgetUntilOldTasksComplete() {
        FakeProvider provider = new FakeProvider();
        TacticalMapTerrainRegistry.register(provider);
        AtomicInteger obsoleteCallbacks = new AtomicInteger();
        AtomicInteger rejectedCurrentCallbacks = new AtomicInteger();
        for (int index = 0; index < 8; index++) {
            TacticalMapTerrainRegistry.requestTile(request(index),
                    image -> obsoleteCallbacks.incrementAndGet());
        }
        assertEquals(8, provider.requestCount);

        TacticalMapTerrainRegistry.register(provider);
        for (int index = 0; index < 8; index++) {
            TacticalMapTerrainRegistry.requestTile(request(index),
                    image -> rejectedCurrentCallbacks.incrementAndGet());
        }

        assertEquals(8, provider.requestCount);
        assertEquals(8, obsoleteCallbacks.get());
        assertEquals(8, rejectedCurrentCallbacks.get());

        for (int index = 0; index < 8; index++) {
            provider.invoke(request(index), 0, null);
        }
        for (int index = 0; index < 8; index++) {
            TacticalMapTerrainRegistry.requestTile(request(index), image -> {
            });
        }
        assertEquals(16, provider.requestCount);
    }

    private static TacticalMapTerrainRequest request(int x) {
        return new TacticalMapTerrainRequest(OVERWORLD,
                TacticalMapTerrainRequest.Style.DAY, x, 0, x + 1, 1, 0, false);
    }

    private static final class FakeProvider implements TacticalMapTerrainProvider {
        private final Map<TacticalMapTerrainRequest, List<Consumer<NativeImage>>> callbacks =
                new HashMap<>();
        private int requestCount;

        @Override
        public String id() {
            return "fake";
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void requestTile(TacticalMapTerrainRequest request,
                                Consumer<NativeImage> callback) {
            requestCount++;
            callbacks.computeIfAbsent(request, ignored -> new ArrayList<>()).add(callback);
        }

        private void complete(TacticalMapTerrainRequest request) {
            callbacks.remove(request).get(0).accept(null);
        }

        private void invoke(TacticalMapTerrainRequest request, NativeImage image) {
            invoke(request, 0, image);
        }

        private void invoke(TacticalMapTerrainRequest request, int index, NativeImage image) {
            callbacks.get(request).get(index).accept(image);
        }
    }
}
