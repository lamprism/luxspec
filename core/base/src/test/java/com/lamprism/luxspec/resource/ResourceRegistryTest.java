package com.lamprism.luxspec.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lamprism.luxspec.data.CompleteResult;
import com.lamprism.luxspec.data.QueryCriteria;
import com.lamprism.luxspec.data.QueryResult;
import com.lamprism.luxspec.data.QueryWindow;
import com.lamprism.luxspec.data.UnboundedWindow;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ResourceRegistryTest {
    private static final ResourceType<Long> USER = ResourceType.of("userProfile", Long.class);
    private static final ResourceType<String> FILE = ResourceType.of("file", String.class);

    @Test
    void normalizesEquivalentNamesAndRejectsProviderConflicts() {
        ResourceType<Long> snakeCase = ResourceType.of("USER_PROFILE", Long.class);
        ResourceType<Long> kebabCase = ResourceType.of("user-profile", Long.class);

        assertEquals(USER, snakeCase);
        assertEquals(USER, kebabCase);
        assertThrows(
                ResourceProviderConflictException.class,
                () -> new ResourceRegistry(List.of(new EmptyProvider<>(USER), new EmptyProvider<>(kebabCase)))
        );
        assertThrows(
                ResourceProviderConflictException.class,
                () -> new ResourceRegistry(List.of(new EmptyProvider<>(USER), new EmptyProvider<>(ResourceType.of("user-profile", Integer.class))))
        );
    }

    @Test
    void dispatchesTypedReferencesAndExposesOptionalBrowsers() {
        TestResource first = new TestResource(1L);
        TestResource second = new TestResource(2L);
        TestBrowser browser = new TestBrowser(USER, List.of(first, second));
        ResourceRegistry registry = new ResourceRegistry(List.of(browser, new EmptyProvider<>(FILE)));

        Resource<Long> resolved = registry.provide(new ResourceReference<>(USER, 2L));
        ResourceBrowser<Long> registeredBrowser = registry.findBrowser(USER).orElseThrow();
        QueryResult<? extends Resource<Long>> browsed = registeredBrowser.browse(QueryCriteria.empty(), UnboundedWindow.getInstance());

        assertEquals(second, resolved);
        assertEquals(List.of(first, second), browsed.getItems());
        assertSame(browser, registeredBrowser);
        assertTrue(registry.findBrowser(FILE).isEmpty());
    }

    @Test
    void distinguishesMissingProvidersFromTypeConflicts() {
        ResourceRegistry registry = new ResourceRegistry(List.of(new EmptyProvider<>(USER)));

        assertThrows(ResourceProviderNotRegisteredException.class, () -> registry.getProvider(FILE));
        assertThrows(
                ResourceProviderConflictException.class,
                () -> registry.getProvider(ResourceType.of("user-profile", Integer.class))
        );
    }

    private static final class TestResource implements Resource<Long> {
        private final long id;

        private TestResource(long id) {
            this.id = id;
        }

        @Override
        public ResourceReference<Long> getReference() {
            return new ResourceReference<>(USER, id);
        }
    }

    private static final class TestBrowser implements ResourceBrowser<Long> {
        private final ResourceType<Long> resourceType;
        private final Map<Long, TestResource> resources;

        private TestBrowser(ResourceType<Long> resourceType, List<TestResource> resources) {
            this.resourceType = resourceType;
            this.resources = new LinkedHashMap<>();
            for (TestResource resource : resources) {
                this.resources.put(resource.getReference().id(), resource);
            }
        }

        @Override
        public ResourceType<Long> getResourceType() {
            return resourceType;
        }

        @Override
        public TestResource provide(ResourceReference<Long> reference) {
            requireType(reference);
            TestResource resource = resources.get(reference.id());
            if (resource == null) {
                throw new ResourceException(ResourceErrorCode.NOT_FOUND, "Test resource was not found");
            }
            return resource;
        }

        @Override
        public List<TestResource> provide(Collection<ResourceReference<Long>> references) {
            List<TestResource> resolved = new ArrayList<>();
            for (ResourceReference<Long> reference : references) {
                resolved.add(provide(reference));
            }
            return List.copyOf(resolved);
        }

        @Override
        public QueryResult<TestResource> browse(QueryCriteria criteria, QueryWindow window) {
            return new CompleteResult<>(List.copyOf(resources.values()));
        }

        private void requireType(ResourceReference<Long> reference) {
            if (!resourceType.equals(reference.resourceType())) {
                throw new ResourceException(ResourceErrorCode.INVALID_REFERENCE, "Reference does not match the provider type");
            }
        }
    }

    private static final class EmptyProvider<ID> implements ResourceProvider<ID> {
        private final ResourceType<ID> resourceType;

        private EmptyProvider(ResourceType<ID> resourceType) {
            this.resourceType = resourceType;
        }

        @Override
        public ResourceType<ID> getResourceType() {
            return resourceType;
        }

        @Override
        public Resource<ID> provide(ResourceReference<ID> reference) {
            throw new ResourceException(ResourceErrorCode.NOT_FOUND, "Test resource was not found");
        }

        @Override
        public List<Resource<ID>> provide(Collection<ResourceReference<ID>> references) {
            return List.of();
        }
    }
}
