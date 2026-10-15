package com.wok.infantry.support;

import com.wok.infantry.support.adapter.ProviderAvailability;
import com.wok.infantry.support.adapter.SupportProvider;
import com.wok.infantry.support.adapter.SupportSpawnContext;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportRegistryTest {
    @Test
    void emptyRegistryIsFrozenAndContainsNoConcreteCapabilities() {
        SupportRegistry.Builder builder = SupportRegistry.builder();
        SupportRegistry registry = builder.freeze();

        assertTrue(builder.frozen());
        assertTrue(registry.isEmpty());
        assertTrue(registry.definitions().isEmpty());
        assertThrows(IllegalStateException.class, () -> builder.registerDefinition(
                SupportDefinitionAndTargetTest.definition("test", "late",
                        SupportTargetMode.POINT)));
    }

    @Test
    void definitionsAndProvidersUseTheSameDynamicIdAndFreezeDeterministically() {
        SupportDefinition zeta = SupportDefinitionAndTargetTest.definition(
                "test", "zeta", SupportTargetMode.POINT);
        SupportDefinition alpha = SupportDefinitionAndTargetTest.definition(
                "addon", "alpha", SupportTargetMode.DIRECTIONAL);
        FakeProvider provider = new FakeProvider(alpha.id());

        SupportRegistry registry = SupportRegistry.builder()
                .registerDefinition(zeta)
                .register(alpha, provider)
                .freeze();

        assertEquals(java.util.List.of(alpha, zeta), registry.definitions());
        SupportProvider guarded = registry.provider(alpha.id()).orElseThrow();
        assertNotSame(provider, guarded);
        assertEquals(alpha.id(), guarded.supportId());
        assertTrue(guarded.availability().available());
        assertTrue(registry.provider(zeta.id()).isEmpty());
    }

    @Test
    void duplicateAndDanglingProvidersFailClosed() {
        SupportDefinition definition = SupportDefinitionAndTargetTest.definition(
                "test", "support", SupportTargetMode.POINT);
        SupportRegistry.Builder duplicate = SupportRegistry.builder()
                .registerDefinition(definition);
        assertThrows(IllegalStateException.class,
                () -> duplicate.registerDefinition(definition));

        ResourceLocation danglingId = ResourceLocation.fromNamespaceAndPath("test", "missing");
        SupportRegistry.Builder dangling = SupportRegistry.builder()
                .registerProvider(new FakeProvider(danglingId));
        assertThrows(IllegalStateException.class, dangling::freeze);
    }

    @Test
    void pairedRegistrationCommitsNothingWhenProviderPreflightFails() {
        SupportDefinition definition = SupportDefinitionAndTargetTest.definition(
                "test", "atomic", SupportTargetMode.POINT);
        FakeProvider existingProvider = new FakeProvider(definition.id());
        SupportRegistry.Builder builder = SupportRegistry.builder()
                .registerProvider(existingProvider);

        assertThrows(IllegalStateException.class,
                () -> builder.register(definition, new FakeProvider(definition.id())));

        // If the failed paired call had inserted the definition, this recovery would fail.
        builder.registerDefinition(definition);
        SupportRegistry registry = builder.freeze();
        assertEquals(definition, registry.definition(definition.id()).orElseThrow());
        assertEquals(definition.id(), registry.provider(definition.id()).orElseThrow().supportId());
    }

    @Test
    void providerOnlyRegistrationUsesTheWireIdLimit() {
        ResourceLocation oversizedId = ResourceLocation.fromNamespaceAndPath(
                "test", "a".repeat(SupportDefinition.MAX_ID_LENGTH - "test:".length() + 1));

        assertThrows(IllegalArgumentException.class,
                () -> SupportRegistry.builder().registerProvider(new FakeProvider(oversizedId)));
    }

    private record FakeProvider(ResourceLocation supportId) implements SupportProvider {
        @Override
        public ProviderAvailability availability() {
            return ProviderAvailability.present();
        }

        @Override
        public void executeStep(SupportSpawnContext context) {
        }
    }
}
