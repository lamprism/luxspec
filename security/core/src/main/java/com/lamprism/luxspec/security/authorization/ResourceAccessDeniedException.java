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

package com.lamprism.luxspec.security.authorization;

import com.lamprism.luxspec.ErrorCode;
import com.lamprism.luxspec.resource.ResourceException;

/**
 * Indicates that an otherwise supported resource action was denied.
 *
 * @author RollW
 */
public final class ResourceAccessDeniedException extends ResourceException {
    /**
     * Creates a denial exception with its stable authorization reason.
     *
     * @param reasonCode the stable denial reason
     */
    public ResourceAccessDeniedException(ErrorCode reasonCode) {
        super(reasonCode, "Resource action was denied");
    }
}
