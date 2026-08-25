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

package com.lamprism.luxspec.security.authentication;

import com.lamprism.luxspec.context.ContextKey;

/**
 * Defines typed context keys owned by the Security domain.
 *
 * @author RollW
 */
public final class SecurityContextKeys {
    /**
     * Identifies the effective Luxspec Authentication for one execution context.
     */
    public static final ContextKey<Authentication> AUTHENTICATION = ContextKey.of(
            "security.authentication",
            Authentication.class
    );

    private SecurityContextKeys() {
    }
}
