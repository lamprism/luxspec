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

package com.lamprism.luxspec.database.hikari;

import com.lamprism.luxspec.database.DatabaseConfig;
import com.lamprism.luxspec.database.DatabaseTarget;
import com.lamprism.luxspec.database.DatabaseType;
import com.lamprism.luxspec.database.jdbc.DataSourceFactory;
import com.lamprism.luxspec.database.jdbc.DatabaseUrlResolver;
import com.lamprism.luxspec.database.jdbc.JdbcConnectionDetail;
import com.lamprism.luxspec.database.jdbc.StandardDatabaseUrlResolver;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author RollW
 */
class HikariDataSourceFactoryTest {
    @Test
    void createsAWorkingH2DataSource() throws SQLException {
        DatabaseConfig config = DatabaseConfig.builder(
                        DatabaseType.H2,
                        DatabaseTarget.memory()
                )
                .databaseName("factory-test")
                .build();
        HikariDataSourceFactory factory = new HikariDataSourceFactory(
                new StandardDatabaseUrlResolver()
        );

        HikariDataSource dataSource = (HikariDataSource) factory.create(config);
        try {
            assertEquals(10, dataSource.getMaximumPoolSize());
            try (var connection = dataSource.getConnection()) {
                assertNotNull(connection);
            }
            assertTrue(dataSource.isRunning());
        } finally {
            dataSource.close();
        }
    }

    @Test
    void closesResourcesOwnedByConnectionDetails() {
        AtomicBoolean closed = new AtomicBoolean();
        DatabaseUrlResolver resolver = settings -> new JdbcConnectionDetail(
                "jdbc:h2:mem:resource-test",
                "org.h2.Driver",
                Map.of(),
                List.of(() -> closed.set(true))
        );
        DatabaseConfig config = DatabaseConfig.builder(
                        DatabaseType.H2,
                        DatabaseTarget.memory()
                )
                .databaseName("resource-test")
                .build();

        DataSourceFactory factory = new HikariDataSourceFactory(resolver);
        DataSource dataSource = factory.create(config);

        assertInstanceOf(ManagedHikariDataSource.class, dataSource);
        ((ManagedHikariDataSource) dataSource).close();

        assertTrue(closed.get());
    }

    @Test
    void mergesDatabasePropertiesBeforeResolvedDriverProperties() {
        DatabaseUrlResolver resolver = settings -> new JdbcConnectionDetail(
                "jdbc:h2:mem:resolved-properties",
                "org.h2.Driver",
                Map.of("resolvedProperty", "resolved"),
                List.of()
        );
        DatabaseConfig config = DatabaseConfig.builder(
                        DatabaseType.H2,
                        DatabaseTarget.memory()
                )
                .databaseName("resolved-properties")
                .property("rawProperty", "must-not-leak")
                .build();

        HikariDataSource dataSource = (HikariDataSource) new HikariDataSourceFactory(resolver).create(config);
        try {
            assertEquals("resolved", dataSource.getDataSourceProperties().getProperty("resolvedProperty"));
            assertEquals("must-not-leak", dataSource.getDataSourceProperties().getProperty("rawProperty"));
        } finally {
            dataSource.close();
        }
    }
}
