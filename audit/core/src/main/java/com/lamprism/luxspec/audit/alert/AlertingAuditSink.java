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

package com.lamprism.luxspec.audit.alert;

import com.lamprism.luxspec.audit.AuditEntry;
import com.lamprism.luxspec.audit.publish.AuditSink;
import com.lamprism.luxspec.failure.FailureHandler;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Evaluates an alert only after the delegate accepts an audit entry.
 *
 * <p>Audit acceptance and alert delivery are separate operations. A retry can deliver an alert
 * again even when the audit sink deduplicates entries; applications choose their own delivery and
 * deduplication policies. The default propagates alert failures. An explicit failure handler can
 * report a failure and let audit publication complete.</p>
 *
 * @param <A> the application alert type
 * @author RollW
 */
public final class AlertingAuditSink<A> implements AuditSink {
    private final AuditSink delegate;
    private final AuditAlertPolicy<A> policy;
    private final AuditAlertSink<? super A> alertSink;
    private final @Nullable FailureHandler<AuditEntry> failureHandler;

    /**
     * Creates an alerting sink that propagates alert failures.
     *
     * @param delegate  the audit acceptance boundary
     * @param policy    the alert selection and projection policy
     * @param alertSink the alert destination
     */
    public AlertingAuditSink(AuditSink delegate, AuditAlertPolicy<A> policy, AuditAlertSink<? super A> alertSink) {
        this(delegate, policy, alertSink, null);
    }

    /**
     * Creates an alerting sink with an explicit alert failure handler.
     *
     * @param delegate       the audit acceptance boundary
     * @param policy         the alert selection and projection policy
     * @param alertSink      the alert destination
     * @param failureHandler the handler, or {@code null} to propagate failures
     */
    public AlertingAuditSink(
            AuditSink delegate,
            AuditAlertPolicy<A> policy,
            AuditAlertSink<? super A> alertSink,
            @Nullable FailureHandler<AuditEntry> failureHandler
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.alertSink = Objects.requireNonNull(alertSink, "alertSink");
        this.failureHandler = failureHandler;
    }

    @Override
    public void accept(AuditEntry entry) {
        AuditEntry nonNullEntry = Objects.requireNonNull(entry, "entry");
        delegate.accept(nonNullEntry);
        try {
            Objects.requireNonNull(policy.evaluate(nonNullEntry), "alert policy result").ifPresent(alertSink::accept);
        } catch (RuntimeException failure) {
            if (failureHandler == null) {
                throw failure;
            }
            failureHandler.onFailure(nonNullEntry, failure);
        }
    }
}
