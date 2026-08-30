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

package com.lamprism.luxspec.audit.integration.config;

/**
 * Stable audit event names for configuration events.
 *
 * <p>Names are explicit protocol identifiers rather than values generated from
 * Java class names. Refactoring an event class must not rename persisted audit
 * records. Built-in names use lowercase dot-separated domain and event
 * segments.</p>
 *
 * @author RollW
 */
public final class ConfigAuditEventNames {
    /**
     * A raw configuration source entry changed.
     */
    public static final String SOURCE_CHANGED = "config.source.changed";
    /** The effective resolved configuration value or origin changed. */
    public static final String EFFECTIVE_CHANGED = "config.effective.changed";

    private ConfigAuditEventNames() {
    }
}
