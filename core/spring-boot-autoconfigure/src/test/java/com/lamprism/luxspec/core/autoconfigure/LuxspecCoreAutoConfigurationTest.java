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

package com.lamprism.luxspec.core.autoconfigure;

import com.lamprism.luxspec.context.CorrelationId;
import com.lamprism.luxspec.context.CorrelationIdGenerator;
import com.lamprism.luxspec.context.ExecutionContextStorage;
import com.lamprism.luxspec.context.ThreadLocalExecutionContextStorage;
import com.lamprism.luxspec.context.UuidCorrelationIdGenerator;
import com.lamprism.luxspec.event.EventDispatcher;
import com.lamprism.luxspec.event.SynchronousEventDispatcher;
import com.lamprism.luxspec.resource.ResourceIdGenerator;
import com.lamprism.luxspec.resource.UuidResourceIdGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.TaskDecorator;

import static org.assertj.core.api.Assertions.assertThat;

class LuxspecCoreAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    LuxspecExecutionContextStorageAutoConfiguration.class,
                    LuxspecContextAutoConfiguration.class,
                    LuxspecEventAutoConfiguration.class,
                    LuxspecResourceAutoConfiguration.class
            ));

    @Test
    void createsUuidResourceIdGeneratorByDefault() {
        contextRunner.run(context -> assertThat(context.getBean(ResourceIdGenerator.class))
                .isInstanceOf(UuidResourceIdGenerator.class));
    }

    @Test
    void createsUuidCorrelationIdGeneratorByDefault() {
        contextRunner.run(context -> assertThat(context.getBean(CorrelationIdGenerator.class))
                .isInstanceOf(UuidCorrelationIdGenerator.class));
    }

    @Test
    void backsOffWhenApplicationProvidesCorrelationIdGenerator() {
        CorrelationIdGenerator applicationGenerator = () -> CorrelationId.of("application-id");

        contextRunner
                .withBean(CorrelationIdGenerator.class, () -> applicationGenerator)
                .run(context -> assertThat(context.getBean(CorrelationIdGenerator.class))
                        .isSameAs(applicationGenerator));
    }

    @Test
    void backsOffWhenApplicationProvidesResourceIdGenerator() {
        ResourceIdGenerator<String> applicationGenerator = resourceType -> "application-id";

        contextRunner
                .withBean(ResourceIdGenerator.class, () -> applicationGenerator)
                .run(context -> assertThat(context.getBean(ResourceIdGenerator.class))
                        .isSameAs(applicationGenerator));
    }

    @Test
    void createsEventDispatcherByDefault() {
        contextRunner.run(context -> assertThat(context.getBean(EventDispatcher.class))
                .isInstanceOf(SynchronousEventDispatcher.class));
    }

    @Test
    void backsOffWhenApplicationProvidesExecutionContextStorage() {
        ExecutionContextStorage applicationStorage = new ThreadLocalExecutionContextStorage();

        contextRunner
                .withBean(ExecutionContextStorage.class, () -> applicationStorage)
                .run(context -> {
                    assertThat(context).hasSingleBean(ExecutionContextStorage.class);
                    assertThat(context.getBean(ExecutionContextStorage.class))
                            .isSameAs(applicationStorage);
                    assertThat(context).hasSingleBean(TaskDecorator.class);
                });
    }

    @Test
    void createsDefaultExecutionContextStorageAndTaskDecorator() {
        contextRunner.run(context -> {
            assertThat(context.getBean(ExecutionContextStorage.class))
                    .isInstanceOf(ThreadLocalExecutionContextStorage.class);
            assertThat(context).hasSingleBean(TaskDecorator.class);
        });
    }

    @Test
    void allowsExecutionContextStorageAutoConfigurationToBeOmitted() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(LuxspecContextAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ExecutionContextStorage.class);
                    assertThat(context).doesNotHaveBean(TaskDecorator.class);
                });
    }

    @Test
    void allowsIndependentEventAutoConfiguration() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(LuxspecEventAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(EventDispatcher.class);
                    assertThat(context).doesNotHaveBean(ResourceIdGenerator.class);
                    assertThat(context).doesNotHaveBean(TaskDecorator.class);
                });
    }
}
