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

package com.lamprism.luxspec.web.autoconfigure;

import com.lamprism.luxspec.config.ConfigReader;
import com.lamprism.luxspec.config.ConfigValue;
import com.lamprism.luxspec.config.catalog.ConfigSpecContributor;
import com.lamprism.luxspec.context.CorrelationIdGenerator;
import com.lamprism.luxspec.context.ExecutionContextStorage;
import com.lamprism.luxspec.context.UuidCorrelationIdGenerator;
import com.lamprism.luxspec.web.WebConfigSpec;
import com.lamprism.luxspec.web.spring.LuxspecRequestContextFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

import java.util.Objects;

/**
 * Supplies correlation identifiers and request-context integration for Spring MVC.
 *
 * @author RollW
 */
@AutoConfiguration(afterName =
        "com.lamprism.luxspec.core.autoconfigure.LuxspecExecutionContextStorageAutoConfiguration")
@ConditionalOnClass(DispatcherServlet.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
public class LuxspecWebRequestContextAutoConfiguration {
    /**
     * Registers the Web configuration definitions with the application catalog.
     *
     * @return the Web configuration contributor
     */
    @Bean
    @ConditionalOnMissingBean(name = "webConfigSpecContributor")
    public ConfigSpecContributor webConfigSpecContributor() {
        return registry -> registry.register(WebConfigSpec.CORRELATION_ID_HEADER);
    }

    /**
     * Supplies the request filter when an application has not supplied one.
     *
     * @param configReaders the optional Luxspec configuration reader
     * @param generators    the optional configured correlation ID generator
     * @param storage       the optional configured context storage
     * @return the request filter
     */
    @Bean
    @ConditionalOnMissingBean(LuxspecRequestContextFilter.class)
    public LuxspecRequestContextFilter luxspecRequestContextFilter(
            ObjectProvider<ConfigReader> configReaders,
            ObjectProvider<CorrelationIdGenerator> generators,
            ObjectProvider<ExecutionContextStorage> storage
    ) {
        CorrelationIdGenerator generator = generators.getIfAvailable(
                UuidCorrelationIdGenerator::new
        );
        return new LuxspecRequestContextFilter(
                correlationIdHeader(configReaders),
                generator,
                storage.getIfAvailable()
        );
    }

    private static String correlationIdHeader(ObjectProvider<ConfigReader> configReaders) {
        ConfigReader reader = configReaders.getIfAvailable();
        if (reader == null) {
            return defaultCorrelationIdHeader();
        }
        ConfigValue<String> value = reader.get(WebConfigSpec.CORRELATION_ID_HEADER);
        if (!value.hasValue()) {
            return defaultCorrelationIdHeader();
        }
        return value.requireValue();
    }

    private static String defaultCorrelationIdHeader() {
        return Objects.requireNonNull(WebConfigSpec.CORRELATION_ID_HEADER.getDefaultValue());
    }
}
