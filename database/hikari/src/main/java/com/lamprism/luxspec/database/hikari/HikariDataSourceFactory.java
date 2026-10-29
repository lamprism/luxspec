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

import com.lamprism.luxspec.database.ConnectionPoolPolicy;
import com.lamprism.luxspec.database.DatabaseConfig;
import com.lamprism.luxspec.database.jdbc.DataSourceFactory;
import com.lamprism.luxspec.database.jdbc.DatabaseUrlResolver;
import com.lamprism.luxspec.database.jdbc.JdbcConnectionDetail;
import com.zaxxer.hikari.HikariConfig;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.Objects;
import java.util.Properties;

/**
 * Creates Hikari-backed data sources from provider-neutral database configuration.
 *
 * @author RollW
 */
public class HikariDataSourceFactory implements DataSourceFactory {
    private final DatabaseUrlResolver urlResolver;

    public HikariDataSourceFactory(DatabaseUrlResolver urlResolver) {
        this.urlResolver = Objects.requireNonNull(urlResolver, "urlResolver");
    }

    @Override
    public DataSource create(DatabaseConfig settings) {
        DatabaseConfig nonNullSettings = Objects.requireNonNull(settings, "settings");
        JdbcConnectionDetail detail = urlResolver.resolve(nonNullSettings);
        try {
            HikariConfig configuration = createConfiguration(nonNullSettings, detail);
            return new ManagedHikariDataSource(configuration, detail);
        } catch (RuntimeException | Error exception) {
            closeDetailOnFailure(detail, exception);
            throw exception;
        }
    }

    private static HikariConfig createConfiguration(
            DatabaseConfig settings,
            JdbcConnectionDetail detail
    ) {
        HikariConfig configuration = new HikariConfig();
        configuration.setJdbcUrl(detail.getJdbcUrl());
        String driverClassName = detail.getDriverClassName();
        if (driverClassName != null) {
            configuration.setDriverClassName(driverClassName);
        }
        String username = settings.getUsername();
        if (username != null) {
            configuration.setUsername(username);
        }
        String password = settings.getPassword();
        if (password != null) {
            configuration.setPassword(password);
        }
        configuration.setDataSourceProperties(driverProperties(settings, detail));
        applyPoolPolicy(configuration, settings.getPool());
        return configuration;
    }

    private static Properties driverProperties(
            DatabaseConfig settings,
            JdbcConnectionDetail detail
    ) {
        Properties properties = new Properties();
        properties.putAll(settings.getDriverProperties());
        properties.putAll(detail.getDriverProperties());
        return properties;
    }

    private static void applyPoolPolicy(HikariConfig configuration, ConnectionPoolPolicy policy) {
        configuration.setMaximumPoolSize(policy.getMaximumPoolSize());
        configuration.setMinimumIdle(policy.getMinimumIdle());
        configuration.setConnectionTimeout(toMillis(policy.getConnectionTimeout(), "connectionTimeout"));
        configuration.setIdleTimeout(toMillis(policy.getIdleTimeout(), "idleTimeout"));
        configuration.setMaxLifetime(toMillis(policy.getMaximumLifetime(), "maximumLifetime"));
        configuration.setLeakDetectionThreshold(toMillis(
                policy.getLeakDetectionThreshold(),
                "leakDetectionThreshold"
        ));
    }

    private static long toMillis(Duration duration, String name) {
        try {
            return duration.toMillis();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(name + " is too large", exception);
        }
    }

    private static void closeDetailOnFailure(JdbcConnectionDetail detail, Throwable failure) {
        try {
            detail.close();
        } catch (Exception cleanupException) {
            failure.addSuppressed(cleanupException);
        }
    }
}
