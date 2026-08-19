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

import com.lamprism.luxspec.database.jdbc.JdbcConnectionDetail;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.Objects;

/**
 * Hikari data source that owns resources created by JDBC detail resolution.
 *
 * @author RollW
 */
public class ManagedHikariDataSource extends HikariDataSource {
    private final JdbcConnectionDetail connectionDetail;

    /**
     * Creates a managed Hikari data source.
     *
     * @param configuration    the Hikari configuration
     * @param connectionDetail the resolved connection detail and owned resources
     */
    public ManagedHikariDataSource(
            HikariConfig configuration,
            JdbcConnectionDetail connectionDetail
    ) {
        super(Objects.requireNonNull(configuration, "configuration"));
        this.connectionDetail = Objects.requireNonNull(connectionDetail, "connectionDetail");
    }

    @Override
    public void close() {
        Throwable failure = null;
        try {
            super.close();
        } catch (RuntimeException | Error exception) {
            failure = exception;
        }
        try {
            connectionDetail.close();
        } catch (Exception exception) {
            if (failure == null) {
                failure = new IllegalStateException(
                        "Failed to release JDBC connection resources",
                        exception
                );
            } else {
                failure.addSuppressed(exception);
            }
        }
        rethrow(failure);
    }

    private static void rethrow(Throwable failure) {
        if (failure == null) {
            return;
        }
        if (failure instanceof RuntimeException exception) {
            throw exception;
        }
        if (failure instanceof Error error) {
            throw error;
        }
        throw new IllegalStateException(failure);
    }
}
