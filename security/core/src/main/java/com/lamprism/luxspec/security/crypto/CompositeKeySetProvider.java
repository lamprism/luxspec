package com.lamprism.luxspec.security.crypto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Combines same-name snapshots supplied by multiple key-set providers.
 *
 * @author RollW
 */
public final class CompositeKeySetProvider implements KeySetProvider {
    private final List<KeySetProvider> providers;

    /**
     * Creates a composite provider from one or more named key-set providers.
     *
     * @param providers the non-empty providers to combine
     */
    public CompositeKeySetProvider(Iterable<? extends KeySetProvider> providers) {
        Objects.requireNonNull(providers, "providers");
        List<KeySetProvider> collectedProviders = new ArrayList<>();
        for (KeySetProvider provider : providers) {
            collectedProviders.add(Objects.requireNonNull(provider, "provider"));
        }
        if (collectedProviders.isEmpty()) {
            throw new IllegalArgumentException("At least one key-set provider is required");
        }
        this.providers = List.copyOf(collectedProviders);
    }

    /**
     * Resolves and combines all provider snapshots for one key-set name.
     *
     * @param keySetName the provider-local key-set name
     * @return the combined non-null key set
     * @throws IllegalArgumentException when a provider does not define the name
     */
    @Override
    public KeySet get(String keySetName) {
        String nonNullKeySetName = Objects.requireNonNull(keySetName, "keySetName");
        List<KeySet> keySets = new ArrayList<>();
        for (KeySetProvider provider : providers) {
            keySets.add(Objects.requireNonNull(provider.get(nonNullKeySetName), "provider key set"));
        }
        return KeySet.combine(keySets);
    }
}
