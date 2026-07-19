package com.lamprism.luxspec.resource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Locates authoritative resource providers and optional browsing capabilities by resource type.
 *
 * @author RollW
 */
public final class ResourceRegistry {
    private final Map<String, ResourceProvider<?>> providers;
    private final Map<String, ResourceBrowser<?>> browsers;

    /**
     * Creates an immutable registry from authoritative providers.
     *
     * @param providers the providers to register
     */
    public ResourceRegistry(Iterable<? extends ResourceProvider<?>> providers) {
        Objects.requireNonNull(providers, "providers");
        Map<String, ResourceProvider<?>> registeredProviders = new LinkedHashMap<>();
        Map<String, ResourceBrowser<?>> registeredBrowsers = new LinkedHashMap<>();
        for (ResourceProvider<?> provider : providers) {
            register(provider, registeredProviders, registeredBrowsers);
        }
        this.providers = Map.copyOf(registeredProviders);
        this.browsers = Map.copyOf(registeredBrowsers);
    }

    /**
     * Returns the authoritative provider for a typed resource type.
     *
     * @param resourceType the resource type
     * @param <ID> the resource ID type
     * @return the typed provider
     * @throws ResourceProviderNotRegisteredException when no provider is registered
     * @throws ResourceProviderConflictException when the normalized name has a conflicting ID type
     */
    public <ID> ResourceProvider<ID> getProvider(ResourceType<ID> resourceType) {
        ResourceType<ID> nonNullResourceType = Objects.requireNonNull(resourceType, "resourceType");
        ResourceProvider<?> provider = providers.get(nonNullResourceType.getName());
        if (provider == null) {
            throw new ResourceProviderNotRegisteredException(nonNullResourceType);
        }
        requireMatchingType(provider.getResourceType(), nonNullResourceType);
        return castProvider(provider);
    }

    /**
     * Returns the optional browsing capability for a typed resource type.
     *
     * @param resourceType the resource type
     * @param <ID> the resource ID type
     * @return the browser when the registered provider supports browsing
     * @throws ResourceProviderConflictException when the normalized name has a conflicting ID type
     */
    public <ID> Optional<ResourceBrowser<ID>> findBrowser(ResourceType<ID> resourceType) {
        ResourceType<ID> nonNullResourceType = Objects.requireNonNull(resourceType, "resourceType");
        ResourceBrowser<?> browser = browsers.get(nonNullResourceType.getName());
        if (browser == null) {
            return Optional.empty();
        }
        requireMatchingType(browser.getResourceType(), nonNullResourceType);
        return Optional.of(castBrowser(browser));
    }

    /**
     * Dispatches one typed reference to its authoritative provider.
     *
     * @param reference the reference to resolve
     * @param <ID> the resource ID type
     * @return the resolved resource
     */
    public <ID> Resource<ID> provide(ResourceReference<ID> reference) {
        ResourceReference<ID> nonNullReference = Objects.requireNonNull(reference, "reference");
        return getProvider(nonNullReference.resourceType()).provide(nonNullReference);
    }

    private static void register(
            ResourceProvider<?> provider,
            Map<String, ResourceProvider<?>> registeredProviders,
            Map<String, ResourceBrowser<?>> registeredBrowsers
    ) {
        ResourceProvider<?> nonNullProvider = Objects.requireNonNull(provider, "provider");
        ResourceType<?> resourceType = Objects.requireNonNull(nonNullProvider.getResourceType(), "provider.resourceType");
        ResourceProvider<?> existingProvider = registeredProviders.putIfAbsent(resourceType.getName(), nonNullProvider);
        if (existingProvider != null) {
            throw new ResourceProviderConflictException(existingProvider.getResourceType(), resourceType);
        }
        if (nonNullProvider instanceof ResourceBrowser<?> browser) {
            registeredBrowsers.put(resourceType.getName(), browser);
        }
    }

    private static void requireMatchingType(ResourceType<?> registeredType, ResourceType<?> requestedType) {
        if (!Objects.requireNonNull(registeredType, "registeredType").equals(requestedType)) {
            throw new ResourceProviderConflictException(registeredType, requestedType);
        }
    }

    @SuppressWarnings("unchecked")
    private static <ID> ResourceProvider<ID> castProvider(ResourceProvider<?> provider) {
        // Registration and lookup both verify the ResourceType name and ID class before this cast.
        return (ResourceProvider<ID>) provider;
    }

    @SuppressWarnings("unchecked")
    private static <ID> ResourceBrowser<ID> castBrowser(ResourceBrowser<?> browser) {
        // Registration and lookup both verify the ResourceType name and ID class before this cast.
        return (ResourceBrowser<ID>) browser;
    }
}
