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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Builds PostgreSQL JDBC connection details.
 *
 * @author RollW
 */
public class PostgresqlDatabaseUrlBuilder extends BuiltInDatabaseUrlBuilder {
    private final SslMaterializer sslMaterializer;

    public PostgresqlDatabaseUrlBuilder() {
        this(new DefaultSslMaterializer());
    }

    public PostgresqlDatabaseUrlBuilder(SslMaterializer sslMaterializer) {
        super(DatabaseType.POSTGRESQL, "org.postgresql.Driver");
        this.sslMaterializer = Objects.requireNonNull(sslMaterializer, "sslMaterializer");
    }

    @Override
    protected String buildJdbcUrl(DatabaseConfig settings) {
        DatabaseTarget target = requireNetworkTarget(settings);
        return "jdbc:postgresql://"
                + formatHost(target.getHost())
                + formatPort(target.getPort())
                + "/"
                + requireDatabaseName(settings);
    }

    @Override
    protected Map<String, String> buildDriverProperties(
            DatabaseConfig settings,
            List<AutoCloseable> resources
    ) {
        Map<String, String> properties = driverProperties(settings);
        applyCharacterSet(properties, settings.getCharset());
        rejectManagedSslOptions(properties, Set.of(
                "sslmode",
                "sslrootcert",
                "sslcert",
                "sslkey",
                "sslkeytype",
                "sslpassword"
        ));
        SslConfig ssl = settings.getSsl();
        requireCaForVerification(ssl);
        properties.put("sslmode", sslMode(ssl.getMode()));
        putMaterial(
                sslMaterializer,
                properties,
                resources,
                "sslrootcert",
                "postgres-ca",
                ssl.getServerCaCertificate()
        );
        putClientKeyStore(
                properties,
                resources,
                ssl.getClientCertificate(),
                ssl.getClientPrivateKey()
        );
        return properties;
    }

    private void putClientKeyStore(
            Map<String, String> properties,
            List<AutoCloseable> resources,
            @Nullable SslMaterial clientCertificate,
            @Nullable SslMaterial clientPrivateKey
    ) {
        if (clientCertificate == null) {
            return;
        }
        KeyStoreArtifact artifact = Objects.requireNonNull(
                sslMaterializer.materializeClientKeyStore(
                        "postgres-client",
                        clientCertificate,
                        Objects.requireNonNull(clientPrivateKey, "clientPrivateKey")
                ),
                "client key store artifact"
        );
        resources.add(artifact);
        properties.put("sslkey", artifact.getPath().toAbsolutePath().normalize().toString());
        properties.put("sslkeytype", artifact.getKeyStoreType());
        properties.put("sslpassword", artifact.getPassword());
    }

    private static void requireCaForVerification(SslConfig ssl) {
        if ((ssl.getMode() == SslMode.VERIFY_CA || ssl.getMode() == SslMode.VERIFY_IDENTITY)
                && ssl.getServerCaCertificate() == null) {
            throw new IllegalArgumentException(
                    "PostgreSQL SSL verification requires a server CA certificate"
            );
        }
    }

    private void applyCharacterSet(
            Map<String, String> properties,
            @Nullable String characterSet
    ) {
        if (characterSet == null) {
            return;
        }
        rejectManagedOptions(properties, Set.of("characterEncoding"), "character set");
        boolean utf8 = characterSet.equalsIgnoreCase("utf8")
                || characterSet.equalsIgnoreCase("utf-8")
                || characterSet.equalsIgnoreCase("utf8mb4");
        properties.put(
                "characterEncoding",
                utf8 ? "UTF8" : characterSet.toUpperCase(Locale.ROOT)
        );
    }

    private String sslMode(SslMode mode) {
        return switch (mode) {
            case DISABLED -> "disable";
            case REQUIRED -> "require";
            case VERIFY_CA -> "verify-ca";
            case VERIFY_IDENTITY -> "verify-full";
        };
    }
}
