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

package com.lamprism.luxspec.config.persistence.autoconfigure;

import com.lamprism.luxspec.config.source.ConfigSourceId;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for the optional JPA-backed Config source.
 *
 * @author RollW
 */
@ConfigurationProperties("luxspec.config.jpa")
public class LuxspecConfigJpaConfiguration {
    private String sourceId = "jpa";

    /**
     * Returns the source instance identifier.
     *
     * @return the source identifier
     */
    public String getSourceId() {
        return sourceId;
    }

    /**
     * Sets the source instance identifier.
     *
     * @param sourceId the source identifier
     */
    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    /**
     * Creates the provider-neutral source identifier.
     *
     * @return the configured source identifier
     */
    public ConfigSourceId toSourceId() {
        return ConfigSourceId.of(sourceId);
    }
}
