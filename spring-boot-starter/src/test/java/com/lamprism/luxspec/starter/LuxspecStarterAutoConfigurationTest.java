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

package com.lamprism.luxspec.starter;

import com.lamprism.luxspec.config.ConfigReader;
import com.lamprism.luxspec.config.autoconfigure.LuxspecConfigAutoConfiguration;
import com.lamprism.luxspec.config.autoconfigure.LuxspecProcessConfigAutoConfiguration;
import com.lamprism.luxspec.config.runtime.LayeredConfigReader;
import com.lamprism.luxspec.context.CorrelationIdGenerator;
import com.lamprism.luxspec.context.ExecutionContextStorage;
import com.lamprism.luxspec.context.ThreadLocalExecutionContextStorage;
import com.lamprism.luxspec.context.UuidCorrelationIdGenerator;
import com.lamprism.luxspec.core.autoconfigure.LuxspecContextAutoConfiguration;
import com.lamprism.luxspec.core.autoconfigure.LuxspecEventAutoConfiguration;
import com.lamprism.luxspec.core.autoconfigure.LuxspecExecutionContextStorageAutoConfiguration;
import com.lamprism.luxspec.core.autoconfigure.LuxspecResourceAutoConfiguration;
import com.lamprism.luxspec.event.EventDispatcher;
import com.lamprism.luxspec.resource.ResourceIdGenerator;
import com.lamprism.luxspec.web.autoconfigure.LuxspecWebErrorAutoConfiguration;
import com.lamprism.luxspec.web.autoconfigure.LuxspecWebRequestContextAutoConfiguration;
import com.lamprism.luxspec.web.spring.LuxspecExceptionHandler;
import com.lamprism.luxspec.web.spring.LuxspecRequestContextFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.core.task.TaskDecorator;

import static org.assertj.core.api.Assertions.assertThat;

class LuxspecStarterAutoConfigurationTest {
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    LuxspecConfigAutoConfiguration.class,
                    LuxspecProcessConfigAutoConfiguration.class,
                    LuxspecExecutionContextStorageAutoConfiguration.class,
                    LuxspecContextAutoConfiguration.class,
                    LuxspecEventAutoConfiguration.class,
                    LuxspecResourceAutoConfiguration.class,
                    LuxspecWebErrorAutoConfiguration.class,
                    LuxspecWebRequestContextAutoConfiguration.class
            ));

    @Test
    void assemblesTheStandardFoundationDefaults() {
        contextRunner.run(context -> {
            assertThat(context.getBean(ConfigReader.class))
                    .isInstanceOf(LayeredConfigReader.class);
            assertThat(context.getBean(CorrelationIdGenerator.class))
                    .isInstanceOf(UuidCorrelationIdGenerator.class);
            assertThat(context.getBean(ExecutionContextStorage.class))
                    .isInstanceOf(ThreadLocalExecutionContextStorage.class);
            assertThat(context).hasSingleBean(TaskDecorator.class);
            assertThat(context).hasSingleBean(EventDispatcher.class);
            assertThat(context).hasSingleBean(ResourceIdGenerator.class);
            assertThat(context).hasSingleBean(LuxspecExceptionHandler.class);
            assertThat(context).hasSingleBean(LuxspecRequestContextFilter.class);
        });
    }
}
