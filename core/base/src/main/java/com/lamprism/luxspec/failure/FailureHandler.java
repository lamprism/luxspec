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

package com.lamprism.luxspec.failure;

/**
 * Handles a failure with boundary-specific context.
 *
 * <p>The owning boundary decides which failures it catches, when the handler is invoked, and what
 * happens if the handler itself fails. Accepting {@link Throwable} does not require a boundary to
 * catch JVM {@link Error} values. This role does not define retry, suppression, logging, or fallback
 * policy.</p>
 *
 * @param <C> the typed failure context
 * @author RollW
 */
@FunctionalInterface
public interface FailureHandler<C> {
    /**
     * Handles one failure.
     *
     * @param context the boundary-specific failure context
     * @param failure the failure selected by the owning boundary
     */
    void onFailure(C context, Throwable failure);
}
