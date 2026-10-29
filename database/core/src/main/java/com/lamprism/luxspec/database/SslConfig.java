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

package com.lamprism.luxspec.database;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Immutable SSL policy and material references for a database connection.
 *
 * @author RollW
 */
public final class SslConfig {
    private final SslMode mode;
    private final @Nullable SslMaterial serverCaCertificate;
    private final @Nullable SslMaterial clientCertificate;
    private final @Nullable SslMaterial clientPrivateKey;

    private SslConfig(Builder builder) {
        this.mode = Objects.requireNonNull(builder.mode, "mode");
        this.serverCaCertificate = builder.serverCaCertificate;
        this.clientCertificate = builder.clientCertificate;
        this.clientPrivateKey = builder.clientPrivateKey;
        validate();
    }

    public static SslConfig disabled() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public SslMode getMode() {
        return mode;
    }

    public @Nullable SslMaterial getServerCaCertificate() {
        return serverCaCertificate;
    }

    public @Nullable SslMaterial getClientCertificate() {
        return clientCertificate;
    }

    public @Nullable SslMaterial getClientPrivateKey() {
        return clientPrivateKey;
    }

    private void validate() {
        boolean hasClientCertificate = clientCertificate != null;
        boolean hasClientPrivateKey = clientPrivateKey != null;
        if (hasClientCertificate != hasClientPrivateKey) {
            throw new IllegalArgumentException("Client certificate and private key must be configured together");
        }
        if (mode == SslMode.DISABLED && (serverCaCertificate != null || hasClientCertificate)) {
            throw new IllegalArgumentException("SSL material cannot be configured when SSL is disabled");
        }
    }

    @Override
    public String toString() {
        return "SslConfig[mode=" + mode
                + ", serverCaCertificateConfigured=" + (serverCaCertificate != null)
                + ", clientCertificateConfigured=" + (clientCertificate != null)
                + "]";
    }

    public static final class Builder {
        private SslMode mode = SslMode.DISABLED;
        private @Nullable SslMaterial serverCaCertificate;
        private @Nullable SslMaterial clientCertificate;
        private @Nullable SslMaterial clientPrivateKey;

        private Builder() {
        }

        public Builder mode(SslMode mode) {
            this.mode = Objects.requireNonNull(mode, "mode");
            return this;
        }

        public Builder serverCaCertificate(@Nullable SslMaterial serverCaCertificate) {
            this.serverCaCertificate = serverCaCertificate;
            return this;
        }

        public Builder clientCertificate(@Nullable SslMaterial clientCertificate) {
            this.clientCertificate = clientCertificate;
            return this;
        }

        public Builder clientPrivateKey(@Nullable SslMaterial clientPrivateKey) {
            this.clientPrivateKey = clientPrivateKey;
            return this;
        }

        public SslConfig build() {
            return new SslConfig(this);
        }
    }
}
