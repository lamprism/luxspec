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

package com.lamprism.luxspec.observability.runtime.instrumentation;

import com.lamprism.luxspec.observability.observation.Observation;
import com.lamprism.luxspec.observability.observation.ObservationOutcome;
import com.lamprism.luxspec.observability.observation.ObservationRegistry;
import com.lamprism.luxspec.observability.observation.ObservationSpec;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.Objects;
import java.util.logging.Logger;

final class ObservedDataSource implements DataSource {
    private final DataSource delegate;
    private final ObservationRegistry registry;
    private final ObservationSpec spec;

    ObservedDataSource(DataSource delegate, ObservationRegistry registry, ObservationSpec spec) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.spec = Objects.requireNonNull(spec, "spec");
    }

    @Override
    public Connection getConnection() throws SQLException {
        return observe(() -> wrapConnection(delegate.getConnection()));
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return observe(() -> wrapConnection(delegate.getConnection(username, password)));
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this) || delegate.isWrapperFor(iface);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    private Connection wrapConnection(Connection connection) {
        return (Connection) proxy(
                Connection.class,
                connection,
                (method, target, arguments) -> {
                    Object result = invoke(method, target, arguments);
                    if (result instanceof Statement statement
                            && isStatementFactory(method.getName())) {
                        return wrapStatement(statement, method.getReturnType());
                    }
                    return result;
                }
        );
    }

    private Statement wrapStatement(Statement statement, Class<?> contract) {
        return (Statement) proxy(
                contract,
                statement,
                (method, target, arguments) -> {
                    if (!method.getName().startsWith("execute")) {
                        return invoke(method, target, arguments);
                    }
                    return observe(() -> invokeStatement(method, target, arguments));
                }
        );
    }

    private <R> R observe(ThrowingSupplier<R> operation) throws SQLException {
        Observation observation = registry.start(spec);
        try {
            R result = operation.get();
            observation.setOutcome(ObservationOutcome.SUCCESS);
            return result;
        } catch (SQLException | RuntimeException | Error failure) {
            observation.error(failure);
            throw failure;
        } finally {
            observation.stop();
        }
    }

    private static boolean isStatementFactory(String name) {
        return name.equals("createStatement")
                || name.equals("prepareStatement")
                || name.equals("prepareCall");
    }

    private static Object invokeStatement(Method method, Object target, Object[] arguments)
            throws SQLException {
        try {
            return invoke(method, target, arguments);
        } catch (SQLException exception) {
            throw exception;
        } catch (RuntimeException | Error failure) {
            throw failure;
        } catch (Throwable failure) {
            throw new SQLException("JDBC operation failed", failure);
        }
    }

    private static Object invoke(Method method, Object target, Object[] arguments) throws Throwable {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    private static Object proxy(
            Class<?> contract,
            Object target,
            JdbcInvocation handler
    ) {
        return Proxy.newProxyInstance(
                contract.getClassLoader(),
                new Class<?>[]{contract},
                (proxy, method, arguments) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return method.invoke(target, arguments);
                    }
                    return handler.invoke(method, target, arguments);
                }
        );
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws SQLException;
    }

    @FunctionalInterface
    private interface JdbcInvocation {
        Object invoke(Method method, Object target, Object[] arguments) throws Throwable;
    }
}
