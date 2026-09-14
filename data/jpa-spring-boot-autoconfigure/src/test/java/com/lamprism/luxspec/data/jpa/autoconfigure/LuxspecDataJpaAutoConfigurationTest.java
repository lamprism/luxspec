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

package com.lamprism.luxspec.data.jpa.autoconfigure;

import com.lamprism.luxspec.data.jpa.JpaCriteriaTranslator;
import com.lamprism.luxspec.data.jpa.JpaFieldNamingStrategy;
import com.lamprism.luxspec.data.jpa.JpaQueryExecutorFactory;
import com.lamprism.luxspec.naming.CaseFormat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class LuxspecDataJpaAutoConfigurationTest {
    @Test
    void createsCriteriaTranslator() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(LuxspecDataJpaAutoConfiguration.class))
                .run(context -> assertThat(context)
                        .hasSingleBean(JpaCriteriaTranslator.class)
                        .hasSingleBean(JpaFieldNamingStrategy.class)
                        .doesNotHaveBean(JpaQueryExecutorFactory.class));
    }

    @Test
    void backsOffWhenTheApplicationProvidesANamingStrategy() {
        new ApplicationContextRunner()
                .withUserConfiguration(UserNamingStrategyConfiguration.class)
                .withConfiguration(AutoConfigurations.of(LuxspecDataJpaAutoConfiguration.class))
                .run(context -> assertThat(context.getBean(JpaFieldNamingStrategy.class)
                        .toAttributeName("display_name"))
                        .isEqualTo("displayName"));
    }

    @Configuration(proxyBeanMethods = false)
    static class UserNamingStrategyConfiguration {
        @Bean
        JpaFieldNamingStrategy jpaFieldNamingStrategy() {
            return JpaFieldNamingStrategy.from(
                    CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL)
            );
        }
    }
}
