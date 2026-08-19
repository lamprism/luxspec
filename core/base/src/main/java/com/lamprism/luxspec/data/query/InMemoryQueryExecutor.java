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

package com.lamprism.luxspec.data.query;

import com.lamprism.luxspec.data.pagination.CompleteResult;
import com.lamprism.luxspec.data.pagination.PageResult;
import com.lamprism.luxspec.data.pagination.PageWindow;
import com.lamprism.luxspec.data.pagination.QueryResult;
import com.lamprism.luxspec.data.pagination.QueryWindow;
import com.lamprism.luxspec.data.pagination.SliceResult;
import com.lamprism.luxspec.data.pagination.SliceWindow;
import com.lamprism.luxspec.data.pagination.UnboundedWindow;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Executes validated structured queries over a supplied in-memory candidate collection.
 *
 * <p>A supplied candidate filter is evaluated after structured criteria and before ordering,
 * counting, and windowing. This avoids expensive visibility checks for candidates that cannot
 * appear in the result. The candidate supplier is called once per operation.</p>
 *
 * @param <T> the queried candidate type
 * @author RollW
 */
public class InMemoryQueryExecutor<T> implements QueryExecutor<T> {
    private final Supplier<? extends Collection<? extends T>> candidateSupplier;
    private final QueryFieldResolver<T> fieldResolver;

    /**
     * Starts configuring an in-memory query executor without exposing resolver assembly details.
     *
     * @param candidateSupplier the in-memory candidate source evaluated once per operation
     * @param <T>               the queried candidate type
     * @return a new executor builder
     */
    public static <T> Builder<T> builder(
            Supplier<? extends Collection<? extends T>> candidateSupplier
    ) {
        return new Builder<>(candidateSupplier);
    }

    /**
     * Creates an executor using one candidate supplier and an explicit field resolver.
     *
     * <p>This constructor is an advanced extension point for custom field resolution. Use
     * {@link #builder(Supplier)} for the standard construction path.</p>
     *
     * @param candidateSupplier the in-memory candidate source evaluated once per operation
     * @param fieldResolver     the query field resolver
     */
    public InMemoryQueryExecutor(
            Supplier<? extends Collection<? extends T>> candidateSupplier,
            QueryFieldResolver<T> fieldResolver
    ) {
        this.candidateSupplier = Objects.requireNonNull(candidateSupplier, "candidateSupplier");
        this.fieldResolver = Objects.requireNonNull(fieldResolver, "fieldResolver");
    }

    /**
     * Configures an in-memory query executor.
     *
     * @param <T> the queried candidate type
     */
    public static class Builder<T> {
        private final Supplier<? extends Collection<? extends T>> candidateSupplier;
        private final QueryFieldResolver.Builder<T> fieldResolverBuilder = QueryFieldResolver.builder();

        private Builder(
                Supplier<? extends Collection<? extends T>> candidateSupplier
        ) {
            this.candidateSupplier = Objects.requireNonNull(candidateSupplier, "candidateSupplier");
        }

        /**
         * Registers a queryable field using its natural value ordering.
         *
         * @param field    the query field
         * @param accessor the candidate value accessor
         * @param <V>      the field value type
         * @return this builder
         */
        public <V> Builder<T> field(
                QueryField<V> field,
                Function<? super T, ? extends V> accessor
        ) {
            fieldResolverBuilder.field(field, accessor);
            return this;
        }

        /**
         * Registers a queryable field with an explicit value comparator.
         *
         * @param field      the query field
         * @param accessor   the candidate value accessor
         * @param comparator the field value comparator
         * @param <V>        the field value type
         * @return this builder
         */
        public <V> Builder<T> field(
                QueryField<V> field,
                Function<? super T, ? extends V> accessor,
                Comparator<? super V> comparator
        ) {
            fieldResolverBuilder.field(field, accessor, comparator);
            return this;
        }

        /**
         * Creates the configured executor.
         *
         * @return a new in-memory query executor
         */
        public InMemoryQueryExecutor<T> build() {
            return new InMemoryQueryExecutor<>(candidateSupplier, fieldResolverBuilder.build());
        }
    }

    /**
     * Executes criteria over every candidate supplied for this operation.
     *
     * @param criteria the validated filters and ordering
     * @param window   the requested result window
     * @return the complete, page, or slice result
     */
    @Override
    public QueryResult<T> query(
            QueryCriteria criteria,
            QueryWindow window
    ) {
        return query(candidate -> true, criteria, window);
    }

    /**
     * Executes criteria over candidates accepted by an in-memory post-condition filter.
     *
     * @param candidateFilter the restriction applied before ordering and windowing
     * @param criteria        the validated filters and ordering
     * @param window          the requested result window
     * @return the complete, page, or slice result
     */
    public QueryResult<T> query(
            Predicate<? super T> candidateFilter,
            QueryCriteria criteria,
            QueryWindow window
    ) {
        Predicate<? super T> nonNullCandidateFilter = Objects.requireNonNull(candidateFilter, "candidateFilter");
        QueryCriteria nonNullCriteria = Objects.requireNonNull(criteria, "criteria");
        QueryWindow nonNullWindow = Objects.requireNonNull(window, "window");
        Collection<? extends T> nonNullCandidates = candidates();
        return query(
                nonNullCandidates,
                nonNullCandidateFilter,
                nonNullCriteria,
                nonNullWindow
        );
    }

    private QueryResult<T> query(
            Collection<? extends T> candidates,
            Predicate<? super T> candidateFilter,
            QueryCriteria criteria,
            QueryWindow window
    ) {
        List<T> matchingCandidates = matchingCandidates(
                candidates,
                candidateFilter,
                criteria.getCondition()
        );
        order(matchingCandidates, criteria.getOrders());
        return window(matchingCandidates, window);
    }

    private Collection<? extends T> candidates() {
        return Objects.requireNonNull(candidateSupplier.get(), "candidateSupplier result");
    }

    private List<T> matchingCandidates(
            Collection<? extends T> candidates,
            Predicate<? super T> candidateFilter,
            QueryExpression condition
    ) {
        List<T> matches = new ArrayList<>();
        for (T candidate : candidates) {
            T nonNullCandidate = Objects.requireNonNull(candidate, "candidate");
            if (matches(nonNullCandidate, condition) && candidateFilter.test(nonNullCandidate)) {
                matches.add(nonNullCandidate);
            }
        }
        return matches;
    }

    private boolean matches(T candidate, QueryExpression condition) {
        return new CandidateMatcher<>(fieldResolver, candidate).matches(condition);
    }

    private void order(List<T> candidates, List<OrderBy<?>> orders) {
        Comparator<T> comparator = null;
        for (OrderBy<?> order : orders) {
            Comparator<T> nextComparator = comparator(order);
            if (comparator == null) {
                comparator = nextComparator;
            } else {
                comparator = comparator.thenComparing(nextComparator);
            }
        }
        if (comparator != null) {
            candidates.sort(comparator);
        }
    }

    private <V> Comparator<T> comparator(OrderBy<V> order) {
        OrderBy<V> nonNullOrder = Objects.requireNonNull(order, "order");
        if (nonNullOrder.getDirection() == OrderBy.Direction.ASCENDING) {
            return (left, right) -> fieldResolver.compare(left, right, nonNullOrder.getField());
        }
        return (left, right) -> fieldResolver.compare(right, left, nonNullOrder.getField());
    }

    private QueryResult<T> window(List<T> candidates, QueryWindow window) {
        if (window instanceof UnboundedWindow) {
            return new CompleteResult<>(candidates);
        }
        if (window instanceof PageWindow pageWindow) {
            return page(candidates, pageWindow);
        }
        if (window instanceof SliceWindow sliceWindow) {
            return slice(candidates, sliceWindow);
        }
        throw new IllegalArgumentException("Unsupported query window: " + window.getClass().getName());
    }

    private PageResult<T> page(List<T> candidates, PageWindow window) {
        int start = startIndex(candidates.size(), window.offset());
        int end = endIndex(start, window.limit(), candidates.size());
        return new PageResult<>(
                candidates.subList(start, end),
                window.offset(),
                window.limit(),
                candidates.size()
        );
    }

    private SliceResult<T> slice(List<T> candidates, SliceWindow window) {
        int start = startIndex(candidates.size(), window.offset());
        int end = endIndex(start, window.limit(), candidates.size());
        return new SliceResult<>(
                candidates.subList(start, end),
                window.offset(),
                window.limit(),
                end < candidates.size()
        );
    }

    private static int startIndex(int size, long offset) {
        if (offset >= size) {
            return size;
        }
        return (int) offset;
    }

    private static int endIndex(int start, int limit, int size) {
        long requestedEnd = (long) start + limit;
        return (int) Math.min(requestedEnd, size);
    }

    private static boolean matchesLike(String value, String pattern) {
        int valueIndex = 0;
        int patternIndex = 0;
        int wildcardIndex = -1;
        int wildcardValueIndex = -1;
        while (valueIndex < value.length()) {
            if (patternIndex < pattern.length()) {
                char patternCharacter = pattern.charAt(patternIndex);
                if (patternCharacter == LikeCondition.ESCAPE_CHARACTER) {
                    if (value.charAt(valueIndex) == pattern.charAt(patternIndex + 1)) {
                        valueIndex++;
                        patternIndex += 2;
                        continue;
                    }
                } else if (patternCharacter == '_') {
                    valueIndex++;
                    patternIndex++;
                    continue;
                } else if (patternCharacter == '%') {
                    wildcardIndex = patternIndex;
                    wildcardValueIndex = valueIndex;
                    patternIndex++;
                    continue;
                } else if (value.charAt(valueIndex) == patternCharacter) {
                    valueIndex++;
                    patternIndex++;
                    continue;
                }
            }
            if (wildcardIndex < 0) {
                return false;
            }
            wildcardValueIndex++;
            valueIndex = wildcardValueIndex;
            patternIndex = wildcardIndex + 1;
        }
        while (patternIndex < pattern.length() && pattern.charAt(patternIndex) == '%') {
            patternIndex++;
        }
        return patternIndex == pattern.length();
    }

    private static final class CandidateMatcher<T> implements QueryExpressionVisitor<Boolean> {
        private final QueryFieldResolver<T> fieldResolver;
        private final T candidate;

        private CandidateMatcher(QueryFieldResolver<T> fieldResolver, T candidate) {
            this.fieldResolver = Objects.requireNonNull(fieldResolver, "fieldResolver");
            this.candidate = Objects.requireNonNull(candidate, "candidate");
        }

        private boolean matches(QueryExpression expression) {
            return Objects.requireNonNull(expression, "expression").accept(this);
        }

        @Override
        public Boolean visitMatchAll() {
            return true;
        }

        @Override
        public <V> Boolean visitCondition(QueryCondition<V> condition) {
            @Nullable V candidateValue = fieldResolver.resolve(candidate, condition.getField());
            return switch (condition.getOperator()) {
                case EQUAL -> Objects.equals(candidateValue, condition.getValues().get(0));
                case NOT_EQUAL -> !Objects.equals(candidateValue, condition.getValues().get(0));
                case IN -> condition.getValues().contains(candidateValue);
                case NOT_IN -> !condition.getValues().contains(candidateValue);
                default -> throw new IllegalArgumentException("Unsupported simple query operator");
            };
        }

        @Override
        public <V extends Comparable<? super V>> Boolean visitComparison(ComparisonCondition<V> condition) {
            @Nullable V candidateValue = fieldResolver.resolve(candidate, condition.getField());
            if (candidateValue == null) {
                return false;
            }
            int comparison = candidateValue.compareTo(condition.getValue());
            return switch (condition.getOperator()) {
                case GREATER_THAN -> comparison > 0;
                case GREATER_THAN_OR_EQUAL -> comparison >= 0;
                case LESS_THAN -> comparison < 0;
                case LESS_THAN_OR_EQUAL -> comparison <= 0;
                default -> throw new IllegalArgumentException("Unsupported ordered query operator");
            };
        }

        @Override
        public Boolean visitLike(LikeCondition condition) {
            @Nullable String candidateValue = fieldResolver.resolve(candidate, condition.getField());
            return candidateValue != null && matchesLike(candidateValue, condition.getPattern());
        }

        @Override
        public Boolean visitLogical(LogicalCondition condition) {
            if (condition.getOperator() == LogicalOperator.AND) {
                return matchesAll(condition.getExpressions());
            }
            return matchesAny(condition.getExpressions());
        }

        @Override
        public Boolean visitNegated(NegatedQueryExpression condition) {
            return !matches(condition.getExpression());
        }

        private boolean matchesAll(List<QueryExpression> expressions) {
            for (QueryExpression expression : expressions) {
                if (!matches(expression)) {
                    return false;
                }
            }
            return true;
        }

        private boolean matchesAny(List<QueryExpression> expressions) {
            for (QueryExpression expression : expressions) {
                if (matches(expression)) {
                    return true;
                }
            }
            return false;
        }
    }
}
