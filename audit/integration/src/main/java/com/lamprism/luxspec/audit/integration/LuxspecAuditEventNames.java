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

package com.lamprism.luxspec.audit.integration;

/**
 * Names of the standard audit events emitted by Luxspec.
 *
 * <p>Names are stable lower-case dot-separated identifiers. They are used as
 * the event names passed to {@code AuditPublisher} and as the keys registered
 * in the standard audit registry.</p>
 *
 * @author RollW
 */
public final class LuxspecAuditEventNames {
    public static final String CONFIG_SOURCE_CHANGED = "config.source.changed";
    public static final String CONFIG_EFFECTIVE_CHANGED = "config.effective.changed";
    public static final String SECURITY_AUTHORIZATION_DECIDED = "security.authorization.decided";
    public static final String SECURITY_AUTHENTICATION = "security.authentication";
    public static final String SECURITY_TOKEN_LIFECYCLE = "security.token.lifecycle";
    public static final String SECURITY_FIREWALL_RULE_DENIED = "security.firewall.rule.denied";
    public static final String SECURITY_FIREWALL_RULE_FAILED = "security.firewall.rule.failed";
    public static final String USER_REGISTERED = "user.registered";
    public static final String USER_RENAMED = "user.renamed";
    public static final String USER_EMAIL_CHANGED = "user.email.changed";
    public static final String USER_ROLES_CHANGED = "user.roles.changed";
    public static final String USER_STATUS_CHANGED = "user.status.changed";
    public static final String USER_PASSWORD_CHANGED = "user.password.changed";

    private LuxspecAuditEventNames() {
    }
}
