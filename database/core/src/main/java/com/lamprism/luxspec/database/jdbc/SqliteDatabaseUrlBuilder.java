package com.lamprism.luxspec.database.jdbc;

import com.lamprism.luxspec.database.DatabaseConfig;
import com.lamprism.luxspec.database.DatabaseTarget;
import com.lamprism.luxspec.database.DatabaseType;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds SQLite JDBC connection details.
 *
 * @author RollW
 */
public class SqliteDatabaseUrlBuilder extends BuiltInDatabaseUrlBuilder {
    public SqliteDatabaseUrlBuilder() {
        super(DatabaseType.SQLITE, "org.sqlite.JDBC");
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
    protected Map<String, String> buildDriverProperties(
            DatabaseConfig settings,
            List<AutoCloseable> resources
    ) {
        Map<String, String> properties = driverProperties(settings);
        applyCharacterSet(properties, settings.getCharset());
        requireSslDisabled(settings.getSsl());
        return properties;
    }

    private void applyCharacterSet(
            Map<String, String> properties,
            @Nullable String characterSet
    ) {
        if (characterSet == null) {
            return;
        }
        rejectManagedOptions(properties, Set.of("encoding"), "character set");
        boolean utf8 = characterSet.equalsIgnoreCase("utf8")
                || characterSet.equalsIgnoreCase("utf-8")
                || characterSet.equalsIgnoreCase("utf8mb4");
        properties.put("encoding", utf8 ? "UTF-8" : characterSet);
    }
}
