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

package com.lamprism.luxspec.config.autoconfigure;

import com.lamprism.luxspec.config.catalog.ConfigCatalog;
import com.lamprism.luxspec.config.catalog.ConfigSpecContributor;
import com.lamprism.luxspec.config.catalog.InMemoryConfigCatalog;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Assembles a read-only configuration catalog from application contributors.
 *
 * @author RollW
 */
@AutoConfiguration
@ConditionalOnBean(ConfigSpecContributor.class)
public class LuxspecConfigCatalogAutoConfiguration {
    /**
     * Creates the default catalog when the application has not supplied one.
     *
     * @param contributors the ordered application configuration contributors
     * @return the assembled configuration catalog
     */
    @Bean
    @ConditionalOnMissingBean(ConfigCatalog.class)
    public ConfigCatalog configCatalog(ObjectProvider<ConfigSpecContributor> contributors) {
        InMemoryConfigCatalog catalog = new InMemoryConfigCatalog();
        contributors.orderedStream().forEach(contributor -> contributor.contribute(catalog));
        return catalog;
    }
}
