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

package com.lamprism.luxspec.audit.retention;

import java.time.Instant;

/**
 * Removes entries selected by a retention policy in a bounded batch.
 *
 * <p>Implementations must not remove entries that the policy did not select. The caller owns
 * scheduling and the operational authorization to execute retention.</p>
 *
 * @author RollW
 */
public interface AuditRetentionStore {
    /**
     * Removes at most the specified number of eligible entries.
     *
     * @param policy the entry retention policy
     * @param now    the evaluation instant
     * @param limit  the positive maximum number of removals
     * @return the number of removed entries
     */
    int remove(AuditRetentionPolicy policy, Instant now, int limit);
}
