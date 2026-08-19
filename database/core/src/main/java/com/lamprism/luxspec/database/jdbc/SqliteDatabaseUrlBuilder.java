package com.lamprism.luxspec.database.jdbc;

import com.lamprism.luxspec.database.DatabaseConfig;
import com.lamprism.luxspec.database.DatabaseTarget;
import com.lamprism.luxspec.database.DatabaseType;

import java.util.List;
import java.util.Map;

/**
 * Builds SQLite JDBC connection details.
 *
 * @author RollW
 */
public class SqliteDatabaseUrlBuilder extends AbstractDatabaseUrlBuilder {
    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.SQLITE;
    }

    @Override
    protected String buildJdbcUrl(DatabaseConfig settings) {
        DatabaseTarget target = settings.getTarget();
        return switch (target.getKind()) {
            case MEMORY -> "jdbc:sqlite::memory:";
            case FILE -> "jdbc:sqlite:" + requireFile(target);
            case NETWORK -> throw new IllegalArgumentException(
                    "SQLite does not support network targets"
            );
        };
    }

    @Override
    protected String getDriverClassName() {
        return "org.sqlite.JDBC";
    }

    @Override
    protected Map<String, String> buildDriverProperties(
            DatabaseConfig settings,
            List<AutoCloseable> resources
    ) {
        Map<String, String> properties = baseProperties(settings, CharacterSetFlavor.SQLITE);
        requireSslDisabled(settings.getSsl(), DatabaseType.SQLITE);
        return properties;
    }
}
