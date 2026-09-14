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

import com.lamprism.luxspec.CommonErrorCode;
import com.lamprism.luxspec.ErrorCode;
import com.lamprism.luxspec.config.ConfigReader;
import com.lamprism.luxspec.config.runtime.LayeredConfigReader;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.ConfigSourceScope;
import com.lamprism.luxspec.config.source.InMemoryConfigSource;
import com.lamprism.luxspec.context.CorrelationId;
import com.lamprism.luxspec.context.CorrelationIdGenerator;
import com.lamprism.luxspec.message.MessageResolver;
import com.lamprism.luxspec.message.spring.SpringMessageResolver;
import com.lamprism.luxspec.web.ErrorHttpStatusMapping;
import com.lamprism.luxspec.web.ErrorHttpStatusResolver;
import com.lamprism.luxspec.web.HttpStatusCode;
import com.lamprism.luxspec.web.spring.LuxspecExceptionHandler;
import com.lamprism.luxspec.web.spring.LuxspecRequestContextFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LuxspecWebAutoConfigurationTest {
    private static final HttpStatusCode CONFLICT = new HttpStatusCode(409);
    private static final HttpStatusCode GONE = new HttpStatusCode(410);

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    LuxspecWebErrorAutoConfiguration.class,
                    LuxspecWebRequestContextAutoConfiguration.class
            ));

    @Test
    void createsTheDefaultMvcSupport() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ErrorHttpStatusResolver.class);
            assertThat(context).hasSingleBean(LuxspecExceptionHandler.class);
            assertThat(context).hasSingleBean(LuxspecRequestContextFilter.class);
        });
    }

    @Test
    void doesNotSelectServletDefaultsForANonWebApplication() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        LuxspecWebErrorAutoConfiguration.class,
                        LuxspecWebRequestContextAutoConfiguration.class
                ))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(LuxspecRequestContextFilter.class);
                    assertThat(context).doesNotHaveBean(LuxspecExceptionHandler.class);
                });
    }

    @Test
    void configuresTheCorrelationIdHeaderAndGenerator() throws Exception {
        contextRunner
                .withBean(ConfigReader.class, () -> new LayeredConfigReader(List.of(
                        InMemoryConfigSource.fromStrings(
                                ConfigSourceId.of("test"),
                                ConfigSourceScope.RUNTIME,
                                Map.of("web.correlation-id-header", "X-Correlation-ID")
                        )
                )))
                .withBean(CorrelationIdGenerator.class, () -> () -> CorrelationId.of("generated-id"))
                .run(context -> {
                    MockHttpServletResponse response = new MockHttpServletResponse();
                    context.getBean(LuxspecRequestContextFilter.class).doFilter(
                            new MockHttpServletRequest("GET", "/accounts"),
                            response,
                            (request, servletResponse) -> {
                            }
                    );
                    assertThat(response.getHeader("X-Correlation-ID")).isEqualTo("generated-id");
                });
    }

    @Test
    void allowsRequestContextSupportToBeDisabledIndependently() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(LuxspecWebErrorAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(LuxspecRequestContextFilter.class);
                    assertThat(context).hasSingleBean(LuxspecExceptionHandler.class);
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
    void composesApplicationMappingsWithFoundationDefaults() {
        contextRunner
                .withUserConfiguration(ErrorHttpStatusMappingConfiguration.class)
                .run(context -> {
                    ErrorHttpStatusResolver resolver = context.getBean(
                            ErrorHttpStatusResolver.class
                    );

                    assertThat(resolver.resolve(ErrorCode.of("application:conflict")))
                            .isEqualTo(CONFLICT);
                    assertThat(resolver.resolve(CommonErrorCode.NOT_FOUND))
                            .isEqualTo(CONFLICT);
                    assertThat(resolver.resolve(CommonErrorCode.INVALID_ARGUMENT))
                            .isEqualTo(HttpStatusCode.BAD_REQUEST);
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
    static class ErrorHttpStatusMappingConfiguration {
        @Bean
        @Order(10)
        ErrorHttpStatusMapping applicationErrorHttpStatusMapping() {
            return ErrorHttpStatusMapping.forErrors(
                    CONFLICT,
                    ErrorCode.of("application:conflict"),
                    CommonErrorCode.NOT_FOUND
            );
        }

        @Bean
        @Order(20)
        ErrorHttpStatusMapping lowerPriorityErrorHttpStatusMapping() {
            return ErrorHttpStatusMapping.forErrors(
                    GONE,
                    ErrorCode.of("application:conflict"),
                    CommonErrorCode.NOT_FOUND
            );
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
            return new HttpStatusCode(418);
        }
    }

    private static final class CustomMessageResolver implements MessageResolver {
        @Override
        public String resolve(String key, Locale locale, Object... arguments) {
            return "custom";
        }
    }

}
