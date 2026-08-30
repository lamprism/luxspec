/*
 * Copyright (C) Lamprism
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lamprism.luxspec.database.jdbc;

import com.lamprism.luxspec.database.DatabaseConfig;
import com.lamprism.luxspec.database.DatabaseTarget;
import com.lamprism.luxspec.database.DatabaseType;
import com.lamprism.luxspec.database.SslConfig;
import com.lamprism.luxspec.database.SslMaterial;
import com.lamprism.luxspec.database.SslMode;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

abstract class BuiltInDatabaseUrlBuilder implements DatabaseUrlBuilder {
    private final DatabaseType databaseType;
    private final String driverClassName;

    BuiltInDatabaseUrlBuilder(DatabaseType databaseType, String driverClassName) {
        this.databaseType = Objects.requireNonNull(databaseType, "databaseType");
        this.driverClassName = Objects.requireNonNull(driverClassName, "driverClassName");
    }

    @Override
    public final DatabaseType getDatabaseType() {
        return databaseType;
    }

    @Override
    public final JdbcConnectionDetail build(DatabaseConfig settings) {
        DatabaseConfig nonNullSettings = Objects.requireNonNull(settings, "settings");
        if (!nonNullSettings.getType().equals(databaseType)) {
            throw new IllegalArgumentException(
                    "Builder does not support database type: " + nonNullSettings.getType()
            );
        }
        List<AutoCloseable> resources = new ArrayList<>();
        try {
            return new JdbcConnectionDetail(
                    buildJdbcUrl(nonNullSettings),
                    driverClassName,
                    buildDriverProperties(nonNullSettings, resources),
                    resources
            );
        } catch (RuntimeException | Error failure) {
            closeResources(resources, failure);
            throw failure;
        }
    }

    protected abstract String buildJdbcUrl(DatabaseConfig settings);

    protected Map<String, String> buildDriverProperties(
            DatabaseConfig settings,
            List<AutoCloseable> resources
    ) {
        return driverProperties(settings);
    }

    protected final Map<String, String> driverProperties(DatabaseConfig settings) {
        return new LinkedHashMap<>(settings.getDriverProperties());
    }

    protected final DatabaseTarget requireNetworkTarget(DatabaseConfig settings) {
        DatabaseTarget target = settings.getTarget();
        if (target.getKind() != DatabaseTarget.Kind.NETWORK) {
            throw new IllegalArgumentException(settings.getType() + " requires a network target");
        }
        return target;
    }

    protected final String requireDatabaseName(DatabaseConfig settings) {
        return Objects.requireNonNull(settings.getDatabaseName(), "databaseName");
    }

    protected final String requireFile(DatabaseTarget target) {
        String value = Objects.requireNonNull(target.getFile(), "file")
                .toAbsolutePath()
                .normalize()
                .toString();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == ';' || character == '?' || character == '#') {
                throw new IllegalArgumentException("file contains a JDBC URL delimiter");
            }
        }
        return value;
    }

    protected final String formatHost(@Nullable String host) {
        String value = Objects.requireNonNull(host, "host");
        return value.indexOf(':') >= 0 && !value.startsWith("[")
                ? "[" + value + "]"
                : value;
    }

    protected final String formatPort(@Nullable Integer port) {
        return port == null ? "" : ":" + port;
    }

    protected final void requireSslDisabled(SslConfig ssl) {
        if (ssl.getMode() != SslMode.DISABLED || hasMaterial(ssl)) {
            throw new IllegalArgumentException(
                    databaseType + " does not support SSL in the standard resolver"
            );
        }
    }

    protected final boolean hasMaterial(SslConfig ssl) {
        return ssl.getServerCaCertificate() != null
                || ssl.getClientCertificate() != null
                || ssl.getClientPrivateKey() != null;
    }

    protected final void rejectManagedOptions(
            Map<String, String> properties,
            Set<String> managedNames,
            String category
    ) {
        for (String propertyName : properties.keySet()) {
            for (String managedName : managedNames) {
                if (propertyName.equalsIgnoreCase(managedName)) {
                    throw new IllegalArgumentException(
                            "database.options contains a managed " + category
                                    + " property: " + propertyName
                    );
                }
            }
        }
    }

    protected final void rejectManagedSslOptions(
            Map<String, String> properties,
            Set<String> managedNames
    ) {
        rejectManagedOptions(properties, managedNames, "SSL");
    }

    protected final void putMaterial(
            SslMaterializer materializer,
            Map<String, String> properties,
            List<AutoCloseable> resources,
            String propertyName,
            String artifactName,
            @Nullable SslMaterial material
    ) {
        if (material == null) {
            return;
        }
        SslMaterialArtifact artifact = materializer.materialize(artifactName, material);
        resources.add(artifact);
        properties.put(propertyName, artifact.getPath().toString());
    }

    private static void closeResources(List<AutoCloseable> resources, Throwable failure) {
        for (int index = resources.size() - 1; index >= 0; index--) {
            try {
                resources.get(index).close();
            } catch (Exception cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
        }
    }
}
