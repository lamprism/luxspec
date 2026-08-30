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

package com.lamprism.luxspec.audit.integration.security;

/**
 * Stable audit event names for security events.
 *
 * <p>Names are explicit protocol identifiers rather than values generated from
 * Java class names. Refactoring an event class must not rename persisted audit
 * records. Built-in names use lowercase dot-separated domain and event
 * segments.</p>
 *
 * @author RollW
 */
public final class SecurityAuditEventNames {
    /**
     * A resource authorization decision was made.
     */
    public static final String AUTHORIZATION_DECIDED = "security.authorization.decided";
    /** An authentication attempt completed. */
    public static final String AUTHENTICATION = "security.authentication";
    /** A security token lifecycle operation completed. */
    public static final String TOKEN_LIFECYCLE = "security.token.lifecycle";
    /** A firewall rule denied a request. */
    public static final String FIREWALL_RULE_DENIED = "security.firewall.rule.denied";
    /** A firewall rule failed while evaluating a request. */
    public static final String FIREWALL_RULE_FAILED = "security.firewall.rule.failed";

    private SecurityAuditEventNames() {
    }
}
