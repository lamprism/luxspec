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

package com.lamprism.luxspec.audit.autoconfigure;

import com.lamprism.luxspec.audit.AuditEntry;
import com.lamprism.luxspec.audit.AuditOutcome;
import com.lamprism.luxspec.audit.integration.LuxspecAuditEventNames;
import com.lamprism.luxspec.audit.integration.StandardAuditEventRegistration;
import com.lamprism.luxspec.audit.publish.AuditPublisher;
import com.lamprism.luxspec.audit.publish.AuditRegistry;
import com.lamprism.luxspec.audit.publish.AuditSink;
import com.lamprism.luxspec.event.EventDispatcher;
import com.lamprism.luxspec.security.firewall.FirewallRuleFailureEvent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LuxspecAuditAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LuxspecAuditAutoConfiguration.class));

    @Test
    void createsThePublisherAndSubscribesBuiltInEventsWhenAnApplicationSuppliesASink() {
        List<AuditEntry> entries = new ArrayList<>();

        contextRunner
                .withBean(AuditSink.class, () -> entries::add)
                .run(context -> {
                    assertThat(context).hasSingleBean(AuditRegistry.class);
                    assertThat(context).hasSingleBean(AuditPublisher.class);
                    assertThat(context).hasSingleBean(EventDispatcher.class);
                    assertThat(context).hasSingleBean(StandardAuditEventRegistration.class);

                    context.getBean(EventDispatcher.class)
                            .publish(new FirewallRuleFailureEvent("example.FirewallRule"));

                    assertThat(entries).hasSize(1);
                    assertThat(entries.get(0).eventName())
                            .isEqualTo(LuxspecAuditEventNames.SECURITY_FIREWALL_RULE_FAILED);
                    assertThat(entries.get(0).outcome()).isEqualTo(AuditOutcome.FAILURE);
                });
    }

    @Test
    void doesNotCreateAuditRuntimeWithoutAnApplicationSink() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(AuditPublisher.class);
            assertThat(context).doesNotHaveBean(AuditRegistry.class);
            assertThat(context).doesNotHaveBean(EventDispatcher.class);
            assertThat(context).doesNotHaveBean(StandardAuditEventRegistration.class);
        });
    }
}
