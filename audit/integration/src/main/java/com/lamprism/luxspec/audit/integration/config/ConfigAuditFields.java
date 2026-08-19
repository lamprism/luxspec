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

import com.lamprism.luxspec.data.query.QueryField;

/**
 * Typed fields emitted by configuration audit translations.
 *
 * <p>Translations publish state and origin metadata, never configuration values.</p>
 *
 * @author RollW
 */
public final class ConfigAuditFields {
    public static final QueryField<String> CONFIG_KEY = QueryField.of("config.key", String.class);
    public static final QueryField<String> CONFIG_SOURCE = QueryField.of("config.source", String.class);
    public static final QueryField<String> CONFIG_CHANGE_TYPE = QueryField.of("config.changeType", String.class);
    public static final QueryField<Boolean> CONFIG_SENSITIVE = QueryField.of("config.sensitive", Boolean.class);
    public static final QueryField<String> CONFIG_PREVIOUS_STATE = QueryField.of("config.previousState", String.class);
    public static final QueryField<String> CONFIG_CURRENT_STATE = QueryField.of("config.currentState", String.class);
    public static final QueryField<String> CONFIG_PREVIOUS_ORIGIN = QueryField.of("config.previousOrigin", String.class);
    public static final QueryField<String> CONFIG_CURRENT_ORIGIN = QueryField.of("config.currentOrigin", String.class);

    private ConfigAuditFields() {
    }
}
