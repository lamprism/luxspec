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

import com.lamprism.luxspec.audit.integration.config.ConfigAuditEventNames;
import com.lamprism.luxspec.audit.integration.security.SecurityAuditEventNames;
import com.lamprism.luxspec.audit.integration.user.UserAuditEventNames;

/**
 * Compatibility aliases for the standard Luxspec audit event names.
 *
 * <p>Canonical names are owned by the feature-specific integration packages.
 * Use {@link ConfigAuditEventNames}, {@link SecurityAuditEventNames}, and
 * {@link UserAuditEventNames} for new code.</p>
 *
 * @author RollW
 */
@Deprecated
public final class LuxspecAuditEventNames {
    public static final String CONFIG_SOURCE_CHANGED = ConfigAuditEventNames.SOURCE_CHANGED;
    public static final String CONFIG_EFFECTIVE_CHANGED = ConfigAuditEventNames.EFFECTIVE_CHANGED;
    public static final String SECURITY_AUTHORIZATION_DECIDED = SecurityAuditEventNames.AUTHORIZATION_DECIDED;
    public static final String SECURITY_AUTHENTICATION = SecurityAuditEventNames.AUTHENTICATION;
    public static final String SECURITY_TOKEN_LIFECYCLE = SecurityAuditEventNames.TOKEN_LIFECYCLE;
    public static final String SECURITY_FIREWALL_RULE_DENIED = SecurityAuditEventNames.FIREWALL_RULE_DENIED;
    public static final String SECURITY_FIREWALL_RULE_FAILED = SecurityAuditEventNames.FIREWALL_RULE_FAILED;
    public static final String USER_REGISTERED = UserAuditEventNames.REGISTERED;
    public static final String USER_RENAMED = UserAuditEventNames.RENAMED;
    public static final String USER_EMAIL_CHANGED = UserAuditEventNames.EMAIL_CHANGED;
    public static final String USER_ROLES_CHANGED = UserAuditEventNames.ROLES_CHANGED;
    public static final String USER_STATUS_CHANGED = UserAuditEventNames.STATUS_CHANGED;
    public static final String USER_PASSWORD_CHANGED = UserAuditEventNames.PASSWORD_CHANGED;

    private LuxspecAuditEventNames() {
    }
}
