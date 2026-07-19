package com.lamprism.luxspec.data.jpa;

import com.lamprism.luxspec.data.QueryField;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;

/**
 * Resolves explicitly allowed typed query fields to JPA criteria expressions.
 *
 * @param <T> the persistence entity type
 * @author RollW
 */
public interface JpaFieldResolver<T> {
    /**
     * Resolves one application-defined typed field for an entity root.
     *
     * @param root the query entity root
     * @param field the explicit allowed typed field
     * @param <V> the field value type
     * @return the JPA expression for that typed field
     */
    <V> Expression<V> resolve(Root<T> root, QueryField<V> field);
}
