package com.lamprism.luxspec.database.jdbc;

import com.lamprism.luxspec.database.DatabaseConfig;
import com.lamprism.luxspec.database.DatabaseTarget;
import com.lamprism.luxspec.database.DatabaseType;
import com.lamprism.luxspec.database.SslConfig;
import com.lamprism.luxspec.database.SslMaterial;
import com.lamprism.luxspec.database.SslMode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandardDatabaseUrlResolverTest {
    private final StandardDatabaseUrlResolver resolver = new StandardDatabaseUrlResolver();

    @Test
    void resolvesH2MemoryWithAnExplicitDatabaseName() {
        JdbcConnectionDetail detail = resolver.resolve(DatabaseConfig.builder(
                        DatabaseType.H2,
                        DatabaseTarget.memory()
                )
                .databaseName("test")
                .build());

        assertEquals("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", detail.getJdbcUrl());
        assertEquals("org.h2.Driver", detail.getDriverClassName());
    }

    @Test
    void preservesTheH2FileTargetWithoutDuplicatingTheDbSuffix() {
        JdbcConnectionDetail detail = resolver.resolve(DatabaseConfig.builder(
                        DatabaseType.H2,
                        DatabaseTarget.file(Path.of("application.db"))
                )
                .build());

        assertEquals(
                "jdbc:h2:file:" + Path.of("application").toAbsolutePath().normalize(),
                detail.getJdbcUrl()
        );
    }

    @Test
    void resolvesPostgresqlIpv6NetworkTarget() {
        JdbcConnectionDetail detail = resolver.resolve(DatabaseConfig.builder(
                        DatabaseType.POSTGRESQL,
                        DatabaseTarget.network("::1", 5432)
                )
                .databaseName("application")
                .build());

        assertEquals("jdbc:postgresql://[::1]:5432/application", detail.getJdbcUrl());
    }

    @Test
    void mapsPostgresqlSslSettingsToDriverProperties() {
        JdbcConnectionDetail detail = resolver.resolve(DatabaseConfig.builder(
                        DatabaseType.POSTGRESQL,
                        DatabaseTarget.network("localhost")
                )
                .databaseName("application")
                .ssl(SslConfig.builder()
                        .mode(SslMode.VERIFY_IDENTITY)
                        .serverCaCertificate(SslMaterial.file(Path.of("ca.pem")))
                        .build())
                .build());

        assertEquals("verify-full", detail.getDriverProperties().get("sslmode"));
        assertEquals(
                Path.of("ca.pem").toAbsolutePath().normalize().toString(),
                detail.getDriverProperties().get("sslrootcert")
        );
    }

    @Test
    void usesTheH2SslProtocolForNetworkSsl() {
        JdbcConnectionDetail detail = resolver.resolve(DatabaseConfig.builder(
                        DatabaseType.H2,
                        DatabaseTarget.network("localhost", 9092)
                )
                .databaseName("application")
                .ssl(SslConfig.builder().mode(SslMode.REQUIRED).build())
                .build());

        assertEquals("jdbc:h2:ssl://localhost:9092/application", detail.getJdbcUrl());
        assertTrue(detail.getDriverProperties().isEmpty());
    }

    @Test
    void mapsMariaDbSslModesSeparatelyFromMySql() {
        JdbcConnectionDetail detail = resolver.resolve(DatabaseConfig.builder(
                        DatabaseType.MARIADB,
                        DatabaseTarget.network("localhost", 3306)
                )
                .databaseName("application")
                .ssl(SslConfig.builder().mode(SslMode.REQUIRED).build())
                .build());

        assertEquals("trust", detail.getDriverProperties().get("sslMode"));
    }

    @Test
    void mapsCharacterSetsPerDatabaseDialect() {
        JdbcConnectionDetail sqlite = resolver.resolve(DatabaseConfig.builder(
                        DatabaseType.SQLITE,
                        DatabaseTarget.memory()
                )
                .characterSet("utf8")
                .build());
        JdbcConnectionDetail h2 = resolver.resolve(DatabaseConfig.builder(
                        DatabaseType.H2,
                        DatabaseTarget.memory()
                )
                .characterSet("utf8")
                .build());
        JdbcConnectionDetail postgres = resolver.resolve(DatabaseConfig.builder(
                        DatabaseType.POSTGRESQL,
                        DatabaseTarget.network("localhost")
                )
                .databaseName("application")
                .characterSet("utf8mb4")
                .build());
        JdbcConnectionDetail oracle = resolver.resolve(DatabaseConfig.builder(
                        DatabaseType.ORACLE,
                        DatabaseTarget.network("localhost")
                )
                .databaseName("application")
                .characterSet("utf8")
                .build());

        assertEquals("UTF-8", sqlite.getDriverProperties().get("encoding"));
        assertTrue(h2.getDriverProperties().isEmpty());
        assertEquals("UTF8", postgres.getDriverProperties().get("characterEncoding"));
        assertEquals("true", oracle.getDriverProperties().get("oracle.jdbc.defaultNChar"));
    }

    @Test
    void rejectsRawPropertiesThatConflictWithTypedCharacterSet() {
        assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve(DatabaseConfig.builder(
                                DatabaseType.MYSQL,
                                DatabaseTarget.network("localhost")
                        )
                        .databaseName("application")
                        .characterSet("utf8")
                        .property("characterEncoding", "latin1")
                        .build())
        );
    }

    @Test
    void requiresCaMaterialForPostgresqlVerification() {
        assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve(DatabaseConfig.builder(
                                DatabaseType.POSTGRESQL,
                                DatabaseTarget.network("localhost")
                        )
                        .databaseName("application")
                        .ssl(SslConfig.builder().mode(SslMode.VERIFY_CA).build())
                        .build())
        );
    }

    @Test
    void rejectsManagedSslOptionsEvenWhenSslIsDisabled() {
        assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve(DatabaseConfig.builder(
                                DatabaseType.H2,
                                DatabaseTarget.network("localhost")
                        )
                        .databaseName("application")
                        .property("SSL", "TRUE")
                        .build())
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve(DatabaseConfig.builder(
                                DatabaseType.SQL_SERVER,
                                DatabaseTarget.network("localhost")
                        )
                        .databaseName("application")
                        .property("encrypt", "true")
                        .build())
        );
    }

    @Test
    void rejectsJdbcUrlDelimitersFromFileTargetsAtTheJdbcBoundary() {
        assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve(DatabaseConfig.builder(
                                DatabaseType.H2,
                                DatabaseTarget.file(Path.of("database;MODE=PostgreSQL"))
                        )
                        .build())
        );
    }

    @Test
    void doesNotExposeTheJdbcUrlInConnectionDetailDiagnostics() {
        JdbcConnectionDetail detail = new JdbcConnectionDetail(
                "jdbc:postgresql://db.internal/application?user=secret",
                "org.postgresql.Driver",
                java.util.Map.of(),
                java.util.List.of()
        );

        assertFalse(detail.toString().contains("db.internal"));
        assertFalse(detail.toString().contains("secret"));
    }

    @Test
    void rejectsUnsupportedVerifyCaModesForSqlServerAndOracle() {
        assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve(DatabaseConfig.builder(
                                DatabaseType.SQL_SERVER,
                                DatabaseTarget.network("localhost")
                        )
                        .databaseName("application")
                        .ssl(SslConfig.builder().mode(SslMode.VERIFY_CA).build())
                        .build())
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve(DatabaseConfig.builder(
                                DatabaseType.ORACLE,
                                DatabaseTarget.network("localhost")
                        )
                        .databaseName("application")
                        .ssl(SslConfig.builder().mode(SslMode.VERIFY_CA).build())
                        .build())
        );
    }

    @Test
    void configuresTheSqlServerCertificateHostForIdentityVerification() {
        JdbcConnectionDetail detail = resolver.resolve(DatabaseConfig.builder(
                        DatabaseType.SQL_SERVER,
                        DatabaseTarget.network("db.internal", 1433)
                )
                .databaseName("application")
                .ssl(SslConfig.builder().mode(SslMode.VERIFY_IDENTITY).build())
                .build());

        assertEquals("db.internal", detail.getDriverProperties().get("hostNameInCertificate"));
        assertEquals("false", detail.getDriverProperties().get("trustServerCertificate"));
    }

    @Test
    void rejectsAnUnknownDatabaseTypeWithoutACustomResolver() {
        assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve(DatabaseConfig.builder(
                        DatabaseType.of("custom"),
                        DatabaseTarget.network("localhost")
                ).databaseName("application").build())
        );
    }

    @Test
    void rejectsCustomOptionsThatOverrideManagedPostgresqlSsl() {
        assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve(DatabaseConfig.builder(
                                DatabaseType.POSTGRESQL,
                                DatabaseTarget.network("localhost")
                        )
                        .databaseName("application")
                        .property("sslmode", "disable")
                        .ssl(SslConfig.builder().mode(SslMode.REQUIRED).build())
                        .build())
        );
    }

    @Test
    void cleansInlinePostgresqlSslMaterialWhenConnectionDetailsClose() throws Exception {
        JdbcConnectionDetail detail = resolver.resolve(DatabaseConfig.builder(
                        DatabaseType.POSTGRESQL,
                        DatabaseTarget.network("localhost")
                )
                .databaseName("application")
                .ssl(SslConfig.builder()
                        .mode(SslMode.REQUIRED)
                        .serverCaCertificate(SslMaterial.value(
                                "-----BEGIN CERTIFICATE-----\nvalue\n"
                        ))
                        .build())
                .build());

        Path materialPath = Path.of(detail.getDriverProperties().get("sslrootcert"));
        try {
            assertTrue(Files.exists(materialPath));
        } finally {
            detail.close();
        }
        assertFalse(Files.exists(materialPath));
    }
}
