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

import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.resolution.ConfigValueOrigin;
import com.lamprism.luxspec.resource.ResourceReference;
import com.lamprism.luxspec.resource.ResourceType;

import java.util.Objects;

/**
 * Shared resource and origin projection for configuration audit translators.
 *
 * @author RollW
 */
final class ConfigAuditTranslationSupport {
    private static final ResourceType<String> CONFIGURATION = ResourceType.of("configuration", String.class);

    private ConfigAuditTranslationSupport() {
    }

    static ResourceReference<String> configuration(ConfigKey key) {
        return new ResourceReference<>(CONFIGURATION, key.getValue());
    }

    static String origin(ConfigValueOrigin origin) {
        ConfigValueOrigin nonNullOrigin = Objects.requireNonNull(origin, "origin");
        if (nonNullOrigin instanceof ConfigValueOrigin.SourceOrigin sourceOrigin) {
            return "source:" + sourceOrigin.sourceId().getValue();
        }
        if (nonNullOrigin instanceof ConfigValueOrigin.TombstoneOrigin tombstoneOrigin) {
            return "tombstone:" + tombstoneOrigin.sourceId().getValue();
        }
        if (nonNullOrigin == ConfigValueOrigin.DefaultOrigin.INSTANCE) {
            return "default";
        }
        if (nonNullOrigin == ConfigValueOrigin.AbsentOrigin.INSTANCE) {
            return "absent";
        }
        return "unknown";
    }
}
