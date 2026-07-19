/**
 * Provides optional JPA adapters for source-partitioned configuration storage.
 *
 * <p>The package never enables entity scanning, Hibernate DDL, or database migrations. Applications
 * explicitly include {@code META-INF/luxspec/liquibase/config/changelog.yaml} from their own Liquibase
 * migration master before using the mappings.</p>
 */
@NullMarked
package com.lamprism.luxspec.config.persistence;

import org.jspecify.annotations.NullMarked;
