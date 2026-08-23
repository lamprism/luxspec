/*
 * Copyright (C) Lamprism
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lamprism.luxspec.resource;

import com.lamprism.luxspec.data.pagination.QueryResult;
import com.lamprism.luxspec.data.pagination.QueryWindow;
import com.lamprism.luxspec.data.query.InMemoryQueryExecutor;
import com.lamprism.luxspec.data.query.QueryCriteria;
import com.lamprism.luxspec.data.query.QueryField;
import com.lamprism.luxspec.data.query.QueryFieldResolver;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A snapshot-based resource browser for tests, local applications, and small in-memory datasets.
 *
 * <p>The resource supplier is called once for each operation. Each returned collection must contain
 * uniquely referenced resources of the browser's declared type.</p>
 *
 * @param <ID> the resource ID type
 * @param <R>  the loaded resource type
 * @author RollW
 */
public class InMemoryResourceBrowser<ID, R extends Resource<ID>> implements ResourceBrowser<ID> {
    private final ResourceType<ID> resourceType;
    private final Supplier<? extends Collection<? extends R>> resourceSupplier;
    private final InMemoryQueryExecutor<R> queryExecutor;

    /**
     * Starts configuring an in-memory resource browser without exposing resolver assembly details.
     *
     * @param resourceType     the handled resource type
     * @param resourceSupplier the resource source evaluated once per operation
     * @param <ID>             the resource ID type
     * @param <R>              the loaded resource type
     * @return a new resource browser builder
     */
    public static <ID, R extends Resource<ID>> Builder<ID, R> builder(
            ResourceType<ID> resourceType,
            Supplier<? extends Collection<? extends R>> resourceSupplier
    ) {
        return new Builder<>(resourceType, resourceSupplier);
    }

    /**
     * Creates a browser with one resource snapshot supplier and an explicit query field resolver.
     *
     * <p>This constructor is an advanced extension point for custom field resolution. Use
     * {@link #builder(ResourceType, Supplier)} for the standard construction path.</p>
     *
     * @param resourceType     the handled resource type
     * @param resourceSupplier the resource source evaluated once per operation
     * @param fieldResolver    the query field resolver
     */
    public InMemoryResourceBrowser(
            ResourceType<ID> resourceType,
            Supplier<? extends Collection<? extends R>> resourceSupplier,
            QueryFieldResolver<R> fieldResolver
    ) {
        this.resourceType = Objects.requireNonNull(resourceType, "resourceType");
        this.resourceSupplier = Objects.requireNonNull(resourceSupplier, "resourceSupplier");
        QueryFieldResolver<R> nonNullFieldResolver = Objects.requireNonNull(
                fieldResolver,
                "fieldResolver"
        );
        this.queryExecutor = new InMemoryQueryExecutor<>(this::queryCandidates, nonNullFieldResolver);
    }

    /**
     * Configures an in-memory resource browser.
     *
     * @param <ID> the resource ID type
     * @param <R>  the loaded resource type
     */
    public static class Builder<ID, R extends Resource<ID>> {
        private final ResourceType<ID> resourceType;
        private final Supplier<? extends Collection<? extends R>> resourceSupplier;
        private final QueryFieldResolver.Builder<R> fieldResolverBuilder = QueryFieldResolver.builder();

        private Builder(
                ResourceType<ID> resourceType,
                Supplier<? extends Collection<? extends R>> resourceSupplier
        ) {
            this.resourceType = Objects.requireNonNull(resourceType, "resourceType");
            this.resourceSupplier = Objects.requireNonNull(resourceSupplier, "resourceSupplier");
        }

        /**
         * Registers a queryable field using its natural value ordering.
         *
         * @param field    the query field
         * @param accessor the resource value accessor
         * @param <V>      the field value type
         * @return this builder
         */
        public <V> Builder<ID, R> field(
                QueryField<V> field,
                Function<? super R, ? extends @Nullable V> accessor
        ) {
            fieldResolverBuilder.field(field, accessor);
            return this;
        }

        /**
         * Registers a queryable field with an explicit value comparator.
         *
         * @param field      the query field
         * @param accessor   the resource value accessor
         * @param comparator the field value comparator
         * @param <V>        the field value type
         * @return this builder
         */
        public <V> Builder<ID, R> field(
                QueryField<V> field,
                Function<? super R, ? extends @Nullable V> accessor,
                Comparator<? super V> comparator
        ) {
            fieldResolverBuilder.field(field, accessor, comparator);
            return this;
        }

        /**
         * Creates the configured resource browser.
         *
         * @return a new in-memory resource browser
         */
        public InMemoryResourceBrowser<ID, R> build() {
            return new InMemoryResourceBrowser<>(resourceType, resourceSupplier, fieldResolverBuilder.build());
        }
    }

    /**
     * Returns the one resource type handled by this browser.
     *
     * @return the handled resource type
     */
    @Override
    public ResourceType<ID> getResourceType() {
        return resourceType;
    }

    /**
     * Resolves one resource from the current snapshot.
     *
     * @param reference the required resource reference
     * @return the resolved resource
     */
    @Override
    public R provide(ResourceReference<ID> reference) {
        ResourceReference<ID> nonNullReference = requireMatchingReference(reference);
        Snapshot<ID, R> snapshot = snapshot();
        R resource = snapshot.resourcesByReference.get(nonNullReference);
        if (resource == null) {
            throw new ResourceException(ResourceErrorCode.NOT_FOUND, "Resource was not found");
        }
        return resource;
    }

    /**
     * Resolves resources from one current snapshot while preserving input order and duplicates.
     *
     * @param references the required resource references
     * @return immutable resolved resources
     */
    @Override
    public List<R> provide(Collection<ResourceReference<ID>> references) {
        List<ResourceReference<ID>> requestedReferences = List.copyOf(
                Objects.requireNonNull(references, "references")
        );
        Snapshot<ID, R> snapshot = snapshot();
        List<R> resources = new ArrayList<>(requestedReferences.size());
        for (ResourceReference<ID> reference : requestedReferences) {
            ResourceReference<ID> nonNullReference = requireMatchingReference(reference);
            R resource = snapshot.resourcesByReference.get(nonNullReference);
            if (resource == null) {
                throw new ResourceException(ResourceErrorCode.NOT_FOUND, "Resource was not found");
            }
            resources.add(resource);
        }
        return List.copyOf(resources);
    }

    /**
     * Browses resources from one current snapshot.
     *
     * @param criteria the validated filters and ordering
     * @param window   the allowed result window
     * @return the typed resource query result
     */
    @Override
    public QueryResult<R> browse(QueryCriteria criteria, QueryWindow window) {
        QueryCriteria nonNullCriteria = Objects.requireNonNull(criteria, "criteria");
        QueryWindow nonNullWindow = Objects.requireNonNull(window, "window");
        return queryExecutor.query(nonNullCriteria, nonNullWindow);
    }

    /**
     * Browses resources accepted by a restriction after criteria and before ordering and windowing.
     *
     * @param candidateFilter the resource restriction
     * @param criteria        the validated filters and ordering
     * @param window          the allowed result window
     * @return the typed resource query result
     */
    public QueryResult<R> browse(
            Predicate<? super R> candidateFilter,
            QueryCriteria criteria,
            QueryWindow window
    ) {
        Predicate<? super R> nonNullCandidateFilter = Objects.requireNonNull(candidateFilter, "candidateFilter");
        QueryCriteria nonNullCriteria = Objects.requireNonNull(criteria, "criteria");
        QueryWindow nonNullWindow = Objects.requireNonNull(window, "window");
        return queryExecutor.query(
                nonNullCandidateFilter,
                nonNullCriteria,
                nonNullWindow
        );
    }

    private Collection<? extends R> queryCandidates() {
        return snapshot().resources;
    }

    private ResourceReference<ID> requireMatchingReference(ResourceReference<ID> reference) {
        ResourceReference<ID> nonNullReference = Objects.requireNonNull(reference, "reference");
        if (!resourceType.equals(nonNullReference.resourceType())) {
            throw new ResourceException(
                    ResourceErrorCode.INVALID_REFERENCE,
                    "Reference does not match the browser type"
            );
        }
        return nonNullReference;
    }

    private Snapshot<ID, R> snapshot() {
        Collection<? extends R> suppliedResources = Objects.requireNonNull(
                resourceSupplier.get(),
                "resourceSupplier result"
        );
        Map<ResourceReference<ID>, R> resourcesByReference = new LinkedHashMap<>();
        for (R resource : suppliedResources) {
            R nonNullResource = Objects.requireNonNull(resource, "resource");
            ResourceReference<ID> reference = Objects.requireNonNull(
                    nonNullResource.getReference(),
                    "resource.reference"
            );
            if (!resourceType.equals(reference.resourceType())) {
                throw new IllegalStateException("Supplied resource does not match the browser type");
            }
            if (resourcesByReference.putIfAbsent(reference, nonNullResource) != null) {
                throw new IllegalStateException("Supplied resource snapshot contains duplicate references");
            }
        }
        return new Snapshot<>(List.copyOf(resourcesByReference.values()), Map.copyOf(resourcesByReference));
    }

    private static final class Snapshot<ID, R> {
        private final List<R> resources;
        private final Map<ResourceReference<ID>, R> resourcesByReference;

        private Snapshot(List<R> resources, Map<ResourceReference<ID>, R> resourcesByReference) {
            this.resources = resources;
            this.resourcesByReference = resourcesByReference;
        }
    }
}
