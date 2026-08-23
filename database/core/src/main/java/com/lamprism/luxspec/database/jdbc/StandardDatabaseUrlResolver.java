package com.lamprism.luxspec.database.jdbc;

import com.lamprism.luxspec.database.DatabaseConfig;
import com.lamprism.luxspec.database.DatabaseType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves database settings through the configured per-dialect URL builders.
 *
 * @author RollW
 */
public class StandardDatabaseUrlResolver implements DatabaseUrlResolver {
    private final Map<DatabaseType, DatabaseUrlBuilder> builders;

    /**
     * Creates the standard resolver with the built-in builders.
     */
    public StandardDatabaseUrlResolver() {
        this(new DefaultSslMaterializer(), new DefaultMySqlKeyStoreMaterializer());
    }

    /**
     * Creates the standard resolver with a shared SSL materializer.
     *
     * @param sslMaterializer the materializer used by PEM-based SSL builders
     */
    public StandardDatabaseUrlResolver(SslMaterializer sslMaterializer) {
        this(sslMaterializer, new DefaultMySqlKeyStoreMaterializer());
    }

    /**
     * Creates the standard resolver with selected SSL materializers.
     *
     * @param sslMaterializer      the materializer used by PEM-based SSL builders
     * @param keyStoreMaterializer the materializer used by the MySQL builder
     */
    public StandardDatabaseUrlResolver(
            SslMaterializer sslMaterializer,
            MySqlKeyStoreMaterializer keyStoreMaterializer
    ) {
        SslMaterializer materializer = Objects.requireNonNull(sslMaterializer, "sslMaterializer");
        MySqlKeyStoreMaterializer mySqlMaterializer = Objects.requireNonNull(
                keyStoreMaterializer,
                "keyStoreMaterializer"
        );
        this.builders = index(List.of(
                new SqliteDatabaseUrlBuilder(),
                new H2DatabaseUrlBuilder(),
                new MySqlDatabaseUrlBuilder(mySqlMaterializer),
                new MariaDbDatabaseUrlBuilder(materializer),
                new PostgresqlDatabaseUrlBuilder(materializer),
                new SqlServerDatabaseUrlBuilder(),
                new OracleDatabaseUrlBuilder()
        ));
    }

    /**
     * Creates a resolver from application-selected builders.
     *
     * @param builders builders indexed by their database type
     */
    public StandardDatabaseUrlResolver(List<? extends DatabaseUrlBuilder> builders) {
        this.builders = index(builders);
    }

    @Override
    public JdbcConnectionDetail resolve(DatabaseConfig settings) {
        DatabaseConfig nonNullSettings = Objects.requireNonNull(settings, "settings");
        DatabaseUrlBuilder builder = builders.get(nonNullSettings.getType());
        if (builder == null) {
            throw new IllegalArgumentException(
                    "No standard JDBC URL builder exists for database type: "
                            + nonNullSettings.getType()
            );
        }
        return builder.build(nonNullSettings);
    }

    private static Map<DatabaseType, DatabaseUrlBuilder> index(
            List<? extends DatabaseUrlBuilder> builders
    ) {
        Map<DatabaseType, DatabaseUrlBuilder> values = new LinkedHashMap<>();
        for (DatabaseUrlBuilder builder : Objects.requireNonNull(builders, "builders")) {
            DatabaseUrlBuilder nonNullBuilder = Objects.requireNonNull(builder, "builder");
            DatabaseUrlBuilder previous = values.putIfAbsent(
                    nonNullBuilder.getDatabaseType(),
                    nonNullBuilder
            );
            if (previous != null) {
                throw new IllegalArgumentException(
                        "Duplicate database URL builder for type: " + nonNullBuilder.getDatabaseType()
                );
            }
        }
        return Map.copyOf(values);
    }
}
