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
import com.lamprism.luxspec.message.MessageResolver;
import com.lamprism.luxspec.message.spring.SpringMessageResolver;
import com.lamprism.luxspec.web.WebConfigSpec;
import com.lamprism.luxspec.web.spring.CorrelationIdGenerator;
import com.lamprism.luxspec.web.spring.DefaultErrorHttpStatusResolver;
import com.lamprism.luxspec.web.spring.ErrorHttpStatusResolver;
import com.lamprism.luxspec.web.spring.LuxspecExceptionHandler;
import com.lamprism.luxspec.web.spring.LuxspecRequestContextFilter;
import com.lamprism.luxspec.web.spring.UuidCorrelationIdGenerator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

import java.util.Objects;

/**
 * Registers default MVC error-envelope support when an application has not supplied replacements.
 *
 * @author RollW
 */
@AutoConfiguration
@ConditionalOnClass(DispatcherServlet.class)
public class LuxspecWebAutoConfiguration {
    /**
     * Supplies the conservative default mapping from business errors to HTTP status codes.
     *
     * @return the default status resolver
     */
    @Bean
    @ConditionalOnMissingBean(ErrorHttpStatusResolver.class)
    public ErrorHttpStatusResolver errorHttpStatusResolver() {
        return new DefaultErrorHttpStatusResolver();
    }

    /**
     * Supplies the default MVC exception advice for Luxspec exceptions.
     *
     * @param statusResolver  the configured error status resolver
     * @param messageResolver the optional message resolver
     * @return the exception advice
     */
    @Bean
    @ConditionalOnMissingBean(LuxspecExceptionHandler.class)
    public LuxspecExceptionHandler luxspecExceptionHandler(
            ErrorHttpStatusResolver statusResolver,
            ObjectProvider<MessageResolver> messageResolver
    ) {
        MessageResolver optionalMessageResolver = messageResolver.getIfAvailable();
        return new LuxspecExceptionHandler(statusResolver, optionalMessageResolver);
    }

    /**
     * Adapts the application message source when no provider-independent resolver was supplied.
     *
     * @param messageSource the application message source
     * @return the safe Spring message resolver
     */
    @Bean
    @ConditionalOnMissingBean(MessageResolver.class)
    @ConditionalOnBean(MessageSource.class)
    public MessageResolver messageResolver(MessageSource messageSource) {
        return new SpringMessageResolver(messageSource, "[message unavailable]");
    }

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
     * Supplies the request-scope filter when an application has not supplied one.
     *
     * @param configReaders the optional Luxspec configuration reader
     * @param generators    the optional correlation ID generators
     * @return the request context filter
     */
    @Bean
    @ConditionalOnMissingBean(LuxspecRequestContextFilter.class)
    public LuxspecRequestContextFilter luxspecRequestContextFilter(
            ObjectProvider<ConfigReader> configReaders,
            ObjectProvider<CorrelationIdGenerator> generators
    ) {
        CorrelationIdGenerator generator = generators.getIfAvailable(UuidCorrelationIdGenerator::new);
        return new LuxspecRequestContextFilter(correlationIdHeader(configReaders), generator);
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
