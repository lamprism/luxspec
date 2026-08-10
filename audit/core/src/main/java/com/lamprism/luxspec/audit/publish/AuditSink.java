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

package com.lamprism.luxspec.audit.publish;

import com.lamprism.luxspec.audit.AuditEntry;

/**
 * Synchronously accepts an audit entry at a sink-owned reliability boundary.
 *
 * <p>Normal return means that the sink reached its declared acceptance
 * boundary. An exception means delivery failed. An asynchronous sink owns its
 * own queue, retries, backpressure, and lifecycle behind this method.
 *
 * @author RollW
 */
@FunctionalInterface
public interface AuditSink {
    void accept(AuditEntry entry);
}
