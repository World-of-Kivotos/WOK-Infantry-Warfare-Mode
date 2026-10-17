package com.wok.infantry.support;

import com.wok.infantry.support.adapter.SupportProvider;
import com.wok.infantry.support.adapter.GuardedSupportProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable per-server catalog of support definitions and their optional execution providers. */
public final class SupportRegistry {
    /** Shared with the bounded battle snapshot codec. */
    public static final int MAX_DEFINITIONS = 32;
    private static final SupportRegistry EMPTY = new SupportRegistry(Map.of(), Map.of());

    private final Map<ResourceLocation, SupportDefinition> definitions;
    private final Map<ResourceLocation, SupportProvider> providers;
    private final List<SupportDefinition> orderedDefinitions;

    private SupportRegistry(Map<ResourceLocation, SupportDefinition> definitions,
                            Map<ResourceLocation, SupportProvider> providers) {
        this.definitions = Map.copyOf(definitions);
        LinkedHashMap<ResourceLocation, SupportProvider> guardedProviders =
                new LinkedHashMap<>();
        providers.forEach((id, provider) ->
                guardedProviders.put(id, GuardedSupportProvider.guard(id, provider)));
        this.providers = Map.copyOf(guardedProviders);
        this.orderedDefinitions = this.definitions.values().stream()
                .sorted(Comparator.comparing(definition -> definition.id().toString()))
                .toList();
    }

    public static SupportRegistry empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<SupportDefinition> definitions() {
        return orderedDefinitions;
    }

    public Optional<SupportDefinition> definition(ResourceLocation id) {
        return Optional.ofNullable(id == null ? null : definitions.get(id));
    }

    public Optional<SupportProvider> provider(ResourceLocation supportId) {
        return Optional.ofNullable(supportId == null ? null : providers.get(supportId));
    }

    public boolean isEmpty() {
        return definitions.isEmpty();
    }

    /** Mutable bootstrap surface. Calling {@link #freeze()} permanently closes the builder. */
    public static final class Builder {
        private final LinkedHashMap<ResourceLocation, SupportDefinition> definitions =
                new LinkedHashMap<>();
        private final LinkedHashMap<ResourceLocation, SupportProvider> providers =
                new LinkedHashMap<>();
        private boolean frozen;

        public synchronized Builder registerDefinition(SupportDefinition definition) {
            ensureOpen();
            Objects.requireNonNull(definition, "definition");
            if (definitions.size() >= MAX_DEFINITIONS
                    && !definitions.containsKey(definition.id())) {
                throw new IllegalStateException("Support definition limit exceeded");
            }
            if (definitions.putIfAbsent(definition.id(), definition) != null) {
                throw new IllegalStateException("Duplicate support definition "
                        + definition.id());
            }
            return this;
        }

        public synchronized Builder registerProvider(SupportProvider provider) {
            ensureOpen();
            Objects.requireNonNull(provider, "provider");
            ResourceLocation id = Objects.requireNonNull(provider.supportId(),
                    "provider.supportId");
            SupportDefinition.requireValidId(id);
            if (providers.putIfAbsent(id, provider) != null) {
                throw new IllegalStateException("Duplicate support provider " + id);
            }
            return this;
        }

        public synchronized Builder register(SupportDefinition definition,
                                             SupportProvider provider) {
            ensureOpen();
            Objects.requireNonNull(definition, "definition");
            Objects.requireNonNull(provider, "provider");
            ResourceLocation providerId = Objects.requireNonNull(provider.supportId(),
                    "provider.supportId");
            SupportDefinition.requireValidId(providerId);
            if (!definition.id().equals(providerId)) {
                throw new IllegalArgumentException("Support definition/provider id mismatch");
            }
            if (definitions.size() >= MAX_DEFINITIONS
                    && !definitions.containsKey(definition.id())) {
                throw new IllegalStateException("Support definition limit exceeded");
            }
            if (definitions.containsKey(definition.id())) {
                throw new IllegalStateException("Duplicate support definition "
                        + definition.id());
            }
            if (providers.containsKey(providerId)) {
                throw new IllegalStateException("Duplicate support provider " + providerId);
            }
            // Commit only after every validation has passed: paired registration is atomic.
            definitions.put(definition.id(), definition);
            providers.put(providerId, provider);
            return this;
        }

        public synchronized SupportRegistry freeze() {
            ensureOpen();
            List<ResourceLocation> danglingProviders = new ArrayList<>();
            for (ResourceLocation id : providers.keySet()) {
                if (!definitions.containsKey(id)) {
                    danglingProviders.add(id);
                }
            }
            if (!danglingProviders.isEmpty()) {
                throw new IllegalStateException("Support providers without definitions: "
                        + danglingProviders);
            }
            frozen = true;
            if (definitions.isEmpty() && providers.isEmpty()) {
                return SupportRegistry.empty();
            }
            return new SupportRegistry(definitions, providers);
        }

        public synchronized boolean frozen() {
            return frozen;
        }

        private void ensureOpen() {
            if (frozen) {
                throw new IllegalStateException("Support registry builder is already frozen");
            }
        }
    }
}
