package com.lamprism.luxspec.database.jdbc;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Immutable JDBC connection details produced by a URL strategy.
 *
 * @author RollW
 */
public final class JdbcConnectionDetail implements AutoCloseable {
    private final String jdbcUrl;
    private final @Nullable String driverClassName;
    private final Map<String, String> driverProperties;
    private final List<AutoCloseable> resources;
    private final AtomicBoolean resourcesClosed = new AtomicBoolean();

    public JdbcConnectionDetail(
            String jdbcUrl,
            @Nullable String driverClassName,
            Map<String, String> driverProperties,
            List<? extends AutoCloseable> resources
    ) {
        this.jdbcUrl = requireText(jdbcUrl, "jdbcUrl");
        this.driverClassName = optionalText(driverClassName, "driverClassName");
        this.driverProperties = immutableProperties(driverProperties);
        this.resources = List.copyOf(Objects.requireNonNull(resources, "resources"));
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public @Nullable String getDriverClassName() {
        return driverClassName;
    }

    public Map<String, String> getDriverProperties() {
        return driverProperties;
    }

    @Override
    public void close() throws Exception {
        if (!resourcesClosed.compareAndSet(false, true)) {
            return;
        }
        Exception failure = null;
        List<AutoCloseable> reversed = new ArrayList<>(resources);
        Collections.reverse(reversed);
        for (AutoCloseable resource : reversed) {
            try {
                resource.close();
            } catch (Exception exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    @Nullable
    private static String requireText(String value, String name) {
        return optionalText(Objects.requireNonNull(value, name), name);
    }

    @Nullable
    private static String optionalText(@Nullable String value, String name) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }

    private static Map<String, String> immutableProperties(Map<String, String> properties) {
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : Objects.requireNonNull(properties, "properties").entrySet()) {
            String key = requireText(entry.getKey(), "driver property key");
            String value = Objects.requireNonNull(entry.getValue(), "driver property value");
            if (key.indexOf('=') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
                throw new IllegalArgumentException("driver property contains an unsupported character");
            }
            values.put(key, value);
        }
        return Collections.unmodifiableMap(values);
    }

    @Override
    public String toString() {
        return "JdbcConnectionDetail[jdbcUrlConfigured=true"
                + ", driverClassName=" + driverClassName
                + ", driverPropertyNames=" + driverProperties.keySet()
                + "]";
    }
}
