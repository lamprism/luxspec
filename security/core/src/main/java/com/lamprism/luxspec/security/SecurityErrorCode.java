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

package com.lamprism.luxspec.security;

import com.lamprism.luxspec.ErrorCode;

/**
 * Defines stable security-domain errors not owned by authentication credentials.
 *
 * @author RollW
 */
public enum SecurityErrorCode implements ErrorCode {
    /**
     * A configured firewall rule failed unexpectedly.
     */
    FIREWALL_RULE_FAILURE("security:firewall-rule-failure"),
    /**
     * A request did not satisfy a configured HTTP method rule.
     */
    FIREWALL_METHOD_DENIED("security:firewall-method-denied"),
    /**
     * A request did not satisfy a configured path rule.
     */
    FIREWALL_PATH_DENIED("security:firewall-path-denied"),
    /**
     * A request did not satisfy a configured client-address rule.
     */
    FIREWALL_CLIENT_ADDRESS_DENIED("security:firewall-client-address-denied"),
    /**
     * An authenticated subject did not satisfy a configured subject rule.
     */
    FIREWALL_SUBJECT_DENIED("security:firewall-subject-denied"),
    /**
     * A subject was denied access to a resource action.
     */
    RESOURCE_ACCESS_DENIED("security:resource-access-denied"),
    /**
     * An action does not apply to the referenced resource type.
     */
    UNSUPPORTED_RESOURCE_ACTION("security:unsupported-resource-action");

    private final String code;

    SecurityErrorCode(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
