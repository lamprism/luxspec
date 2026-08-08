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

import com.lamprism.luxspec.ErrorCode;
import com.lamprism.luxspec.message.MessageResolver;
import com.lamprism.luxspec.message.spring.SpringMessageResolver;
import com.lamprism.luxspec.web.spring.ErrorHttpStatusResolver;
import com.lamprism.luxspec.web.spring.LuxspecExceptionHandler;
import com.lamprism.luxspec.web.spring.LuxspecRequestContextFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class LuxspecWebAutoConfigurationTest {
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LuxspecWebAutoConfiguration.class));

    @Test
    void createsTheDefaultMvcSupport() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ErrorHttpStatusResolver.class);
            assertThat(context).hasSingleBean(LuxspecExceptionHandler.class);
            assertThat(context).hasSingleBean(LuxspecRequestContextFilter.class);
        });
    }

    @Test
    void backsOffFromApplicationOwnedBeans() {
        contextRunner
                .withUserConfiguration(CustomWebConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ErrorHttpStatusResolver.class);
                    assertThat(context).hasSingleBean(LuxspecExceptionHandler.class);
                    assertThat(context).hasSingleBean(LuxspecRequestContextFilter.class);
                    assertThat(context.getBean(ErrorHttpStatusResolver.class))
                            .isInstanceOf(CustomErrorHttpStatusResolver.class);
                    assertThat(context.getBean(LuxspecExceptionHandler.class))
                            .isSameAs(CustomWebConfiguration.CUSTOM_EXCEPTION_HANDLER);
                    assertThat(context.getBean(LuxspecRequestContextFilter.class))
                            .isSameAs(CustomWebConfiguration.CUSTOM_REQUEST_CONTEXT_FILTER);
                });
    }

    @Test
    void createsTheSpringMessageResolverWhenAnApplicationMessageSourceExists() {
        contextRunner
                .withUserConfiguration(MessageSourceConfiguration.class)
                .run(context -> assertThat(context.getBean(MessageResolver.class))
                        .isInstanceOf(SpringMessageResolver.class));
    }

    @Test
    void backsOffFromAnApplicationMessageResolver() {
        contextRunner
                .withUserConfiguration(CustomMessageResolverConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(MessageResolver.class);
                    assertThat(context.getBean(MessageResolver.class))
                            .isInstanceOf(CustomMessageResolver.class);
                    assertThat(context.getBeansOfType(SpringMessageResolver.class)).isEmpty();
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomWebConfiguration {
        private static final LuxspecExceptionHandler CUSTOM_EXCEPTION_HANDLER =
                new LuxspecExceptionHandler(new CustomErrorHttpStatusResolver());
        private static final LuxspecRequestContextFilter CUSTOM_REQUEST_CONTEXT_FILTER =
                new LuxspecRequestContextFilter();

        @Bean
        ErrorHttpStatusResolver errorHttpStatusResolver() {
            return new CustomErrorHttpStatusResolver();
        }

        @Bean
        LuxspecExceptionHandler luxspecExceptionHandler() {
            return CUSTOM_EXCEPTION_HANDLER;
        }

        @Bean
        LuxspecRequestContextFilter luxspecRequestContextFilter() {
            return CUSTOM_REQUEST_CONTEXT_FILTER;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class MessageSourceConfiguration {
        @Bean
        StaticMessageSource messageSource() {
            return new StaticMessageSource();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomMessageResolverConfiguration {
        @Bean
        MessageResolver messageResolver() {
            return new CustomMessageResolver();
        }
    }

    private static final class CustomErrorHttpStatusResolver implements ErrorHttpStatusResolver {
        @Override
        public HttpStatusCode resolve(ErrorCode errorCode) {
            return HttpStatus.I_AM_A_TEAPOT;
        }
    }

    private static final class CustomMessageResolver implements MessageResolver {
        @Override
        public String resolve(String key, Locale locale, Object... arguments) {
            return "custom";
        }
    }

}
