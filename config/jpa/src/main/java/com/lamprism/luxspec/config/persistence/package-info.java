/**
 * Provides an optional JPA adapter for source-partitioned configuration storage.
 *
 * <p>Each source-local configuration key is stored in one row. Scalar and list values are kept in a
 * structured payload, while tombstones clear the payload columns. The package never enables entity
 * scanning, Hibernate DDL, or database migrations. Applications explicitly include {@code
 * META-INF/luxspec/liquibase/config/changelog.yaml} from their own Liquibase migration master before
 * using the mapping.</p>
 */
@NullMarked
package com.lamprism.luxspec.config.persistence;

import org.jspecify.annotations.NullMarked;
