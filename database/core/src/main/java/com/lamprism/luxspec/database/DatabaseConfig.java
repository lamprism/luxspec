package com.lamprism.luxspec.database;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable startup settings for assembling one database connection pool.
 */
public final class DatabaseConfig {
    private final DatabaseType type;
    private final DatabaseTarget target;
    private final @Nullable String databaseName;
    private final @Nullable String username;
    private final @Nullable String password;
    private final @Nullable String charset;
    private final Map<String, String> driverProperties;
    private final SslConfig ssl;
    private final ConnectionPoolPolicy pool;

    private DatabaseConfig(Builder builder) {
        this.type = Objects.requireNonNull(builder.type, "type");
        this.target = Objects.requireNonNull(builder.target, "target");
        this.databaseName = databaseName(builder.databaseName);
        this.username = optionalText(builder.username, "username");
        this.password = builder.password;
        this.charset = optionalText(builder.characterSet, "characterSet");
        this.driverProperties = immutableProperties(builder.driverProperties);
        this.ssl = Objects.requireNonNull(builder.ssl, "ssl");
        this.pool = Objects.requireNonNull(builder.pool, "pool");
        validateTarget();
    }

    public static Builder builder(DatabaseType type, DatabaseTarget target) {
        return new Builder(type, target);
    }

    public DatabaseType getType() {
        return type;
    }

    public DatabaseTarget getTarget() {
        return target;
    }

    public @Nullable String getDatabaseName() {
        return databaseName;
    }

    public @Nullable String getUsername() {
        return username;
    }

    public @Nullable String getPassword() {
        return password;
    }

    public @Nullable String getCharset() {
        return charset;
    }

    public Map<String, String> getDriverProperties() {
        return driverProperties;
    }

    public SslConfig getSsl() {
        return ssl;
    }

    public ConnectionPoolPolicy getPool() {
        return pool;
    }

    private void validateTarget() {
        if (type.equals(DatabaseType.SQLITE)) {
            requireTarget(target.getKind() == DatabaseTarget.Kind.MEMORY
                            || target.getKind() == DatabaseTarget.Kind.FILE,
                    "SQLite supports only memory and file targets");
            return;
        }
        if (type.equals(DatabaseType.H2)) {
            if (target.getKind() == DatabaseTarget.Kind.NETWORK) {
                requireDatabaseName();
            }
            return;
        }
        if (isNetworkDatabase(type)) {
            requireTarget(target.getKind() == DatabaseTarget.Kind.NETWORK,
                    type + " requires a network target");
            requireDatabaseName();
        }
    }

    private void requireDatabaseName() {
        if (databaseName == null) {
            throw new IllegalArgumentException("databaseName is required for a network target");
        }
    }

    private static boolean isNetworkDatabase(DatabaseType type) {
        return type.equals(DatabaseType.MYSQL)
                || type.equals(DatabaseType.MARIADB)
                || type.equals(DatabaseType.POSTGRESQL)
                || type.equals(DatabaseType.SQL_SERVER)
                || type.equals(DatabaseType.ORACLE);
    }

    private static void requireTarget(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private static @Nullable String optionalText(@Nullable String value, String name) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        for (int index = 0; index < trimmed.length(); index++) {
            if (Character.isISOControl(trimmed.charAt(index))) {
                throw new IllegalArgumentException(name + " contains a control character");
            }
        }
        return trimmed;
    }

    private static @Nullable String databaseName(@Nullable String value) {
        String normalized = optionalText(value, "databaseName");
        if (normalized == null) {
            return null;
        }
        for (int index = 0; index < normalized.length(); index++) {
            char character = normalized.charAt(index);
            if (character == '/' || character == '?' || character == '#'
                    || character == '='
                    || character == ';' || character == '\\') {
                throw new IllegalArgumentException("databaseName contains an unsupported character");
            }
        }
        return normalized;
    }

    private static Map<String, String> immutableProperties(Map<String, String> properties) {
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : Objects.requireNonNull(properties, "properties").entrySet()) {
            String key = optionalText(entry.getKey(), "driver property key");
            String value = Objects.requireNonNull(entry.getValue(), "driver property value");
            if (key == null) {
                throw new IllegalArgumentException("driver property key must not be blank");
            }
            if (key.indexOf('=') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
                throw new IllegalArgumentException("driver property contains an unsupported character");
            }
            values.put(key, value);
        }
        if (values.size() > 128) {
            throw new IllegalArgumentException("Too many driver properties");
        }
        return Collections.unmodifiableMap(values);
    }

    @Override
    public String toString() {
        return "DatabaseConfig[type=" + type
                + ", target=" + target
                + ", databaseName=" + databaseName
                + ", usernameConfigured=" + (username != null)
                + ", passwordConfigured=" + (password != null)
                + ", characterSet=" + charset
                + ", driverPropertyCount=" + driverProperties.size()
                + ", ssl=" + ssl
                + ", pool=" + pool
                + "]";
    }

    public static final class Builder {
        private final DatabaseType type;
        private final DatabaseTarget target;
        private @Nullable String databaseName;
        private @Nullable String username;
        private @Nullable String password;
        private @Nullable String characterSet;
        private final Map<String, String> driverProperties = new LinkedHashMap<>();
        private SslConfig ssl = SslConfig.disabled();
        private ConnectionPoolPolicy pool = ConnectionPoolPolicy.defaults();

        private Builder(DatabaseType type, DatabaseTarget target) {
            this.type = Objects.requireNonNull(type, "type");
            this.target = Objects.requireNonNull(target, "target");
        }

        public Builder databaseName(@Nullable String databaseName) {
            this.databaseName = databaseName;
            return this;
        }

        public Builder username(@Nullable String username) {
            this.username = username;
            return this;
        }

        public Builder password(@Nullable String password) {
            this.password = password;
            return this;
        }

        public Builder characterSet(@Nullable String characterSet) {
            this.characterSet = characterSet;
            return this;
        }

        public Builder property(String name, String value) {
            driverProperties.put(
                    Objects.requireNonNull(name, "name"),
                    Objects.requireNonNull(value, "value")
            );
            return this;
        }

        public Builder properties(Map<String, String> properties) {
            for (Map.Entry<String, String> entry : Objects.requireNonNull(properties, "properties").entrySet()) {
                property(entry.getKey(), entry.getValue());
            }
            return this;
        }

        public Builder ssl(SslConfig ssl) {
            this.ssl = Objects.requireNonNull(ssl, "ssl");
            return this;
        }

        public Builder pool(ConnectionPoolPolicy pool) {
            this.pool = Objects.requireNonNull(pool, "pool");
            return this;
        }

        public DatabaseConfig build() {
            return new DatabaseConfig(this);
        }
    }
}
