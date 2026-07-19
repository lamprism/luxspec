/**
 * Provides optional JPA adapters for the canonical Luxspec local-user schema.
 *
 * <p>The package never enables entity scanning, Hibernate DDL, or database migrations. Applications
 * explicitly include {@code META-INF/luxspec/liquibase/user/changelog.yaml} from their own Liquibase
 * migration master before using the mappings.</p>
 */
@NullMarked
package com.lamprism.luxspec.user.persistence;

import org.jspecify.annotations.NullMarked;
