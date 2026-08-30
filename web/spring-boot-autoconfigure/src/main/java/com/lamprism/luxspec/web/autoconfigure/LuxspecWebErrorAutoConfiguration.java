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

import com.lamprism.luxspec.message.MessageResolver;
import com.lamprism.luxspec.message.spring.SpringMessageResolver;
import com.lamprism.luxspec.web.DefaultErrorHttpStatusResolver;
import com.lamprism.luxspec.web.ErrorHttpStatusMapping;
import com.lamprism.luxspec.web.ErrorHttpStatusResolver;
import com.lamprism.luxspec.web.spring.LuxspecExceptionHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Supplies the default Luxspec exception-to-HTTP response integration for Spring MVC.
 *
 * @author RollW
 */
@AutoConfiguration
@ConditionalOnClass(DispatcherServlet.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
public class LuxspecWebErrorAutoConfiguration {
    /**
     * Supplies the conservative default mapping from business errors to HTTP status codes.
     *
     * @param mappings ordered application mappings evaluated before foundation defaults
     * @return the default status resolver
     */
    @Bean
    @ConditionalOnMissingBean(ErrorHttpStatusResolver.class)
    public ErrorHttpStatusResolver errorHttpStatusResolver(
            ObjectProvider<ErrorHttpStatusMapping> mappings
    ) {
        return new DefaultErrorHttpStatusResolver(mappings.orderedStream().toList());
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
}
