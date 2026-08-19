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

package com.lamprism.luxspec.observability.runtime.observation;

import com.lamprism.luxspec.observability.observation.ObservationKind;
import com.lamprism.luxspec.observability.observation.ObservationSpec;

/**
 * Stable built-in operation observation declarations grouped by domain.
 *
 * @author RollW
 */
public final class StandardObservationCatalog {
    public static final ObservationSpec WEB_REQUEST = spec(
            "web.request",
            ObservationKind.SERVER,
            "Inbound web request"
    );
    public static final ObservationSpec DATABASE_OPERATION = spec(
            "database.operation",
            ObservationKind.CLIENT,
            "Database operation"
    );
    public static final ObservationSpec SECURITY_AUTHENTICATION = spec(
            "security.authentication",
            ObservationKind.INTERNAL,
            "Security authentication"
    );
    public static final ObservationSpec SECURITY_AUTHORIZATION = spec(
            "security.authorization",
            ObservationKind.INTERNAL,
            "Security authorization decision"
    );
    public static final ObservationSpec SECURITY_TOKEN = spec(
            "security.token",
            ObservationKind.INTERNAL,
            "Security token operation"
    );
    public static final ObservationSpec CONFIG_READ = spec(
            "config.read",
            ObservationKind.INTERNAL,
            "Configuration read"
    );
    public static final ObservationSpec CONFIG_WRITE = spec(
            "config.write",
            ObservationKind.INTERNAL,
            "Configuration write"
    );
    public static final ObservationSpec AUDIT_PUBLISH = spec(
            "audit.publish",
            ObservationKind.PRODUCER,
            "Audit publication"
    );
    public static final ObservationSpec USER_REGISTER = spec(
            "user.register",
            ObservationKind.INTERNAL,
            "User registration"
    );
    public static final ObservationSpec USER_AUTHENTICATION = spec(
            "user.authentication",
            ObservationKind.INTERNAL,
            "User authentication"
    );
    public static final ObservationSpec USER_PASSWORD_CHANGE = spec(
            "user.password.change",
            ObservationKind.INTERNAL,
            "User password change"
    );
    public static final ObservationSpec USER_STATUS_CHANGE = spec(
            "user.status.change",
            ObservationKind.INTERNAL,
            "User status change"
    );

    private StandardObservationCatalog() {
    }

    private static ObservationSpec spec(String name, ObservationKind kind, String description) {
        return ObservationSpec.builder(name)
                .kind(kind)
                .description(description)
                .build();
    }
}
