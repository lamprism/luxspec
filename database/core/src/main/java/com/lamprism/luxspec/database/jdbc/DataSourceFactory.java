package com.lamprism.luxspec.database.jdbc;

import com.lamprism.luxspec.database.DatabaseConfig;

import javax.sql.DataSource;

/**
 * Creates a managed JDBC data source from startup database settings.
 */
public interface DataSourceFactory {
    /**
     * Creates one data source for the supplied startup settings.
     *
     * @param settings the database settings
     * @return the managed data source
     */
    DataSource create(DatabaseConfig settings);
}
