package com.lamprism.luxspec.database.jdbc;

import com.lamprism.luxspec.database.DatabaseConfig;

/**
 * Resolves provider-neutral database settings into JDBC connection details.
 */
public interface DatabaseUrlResolver {
    /**
     * Resolves one validated database configuration.
     *
     * @param settings the startup database settings
     * @return JDBC URL, driver identity, and driver properties
     */
    JdbcConnectionDetail resolve(DatabaseConfig settings);
}
