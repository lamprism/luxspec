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

package com.lamprism.luxspec.config.value;

import com.lamprism.luxspec.validation.ValidationException;

/**
 * Indicates that a decoded or explicitly written configuration value violates its definition.
 *
 * <p>The detail must not include the value itself because a value may be sensitive.</p>
 *
 * @author RollW
 */
public final class ConfigValueValidationException extends ValidationException {
    /**
     * Creates a value validation failure.
     *
     * @param detail a value-free explanation
     */
    public ConfigValueValidationException(String detail) {
        super(detail);
    }

    /**
     * Creates a value validation failure with an underlying cause.
     *
     * @param detail a value-free explanation
     * @param cause  the underlying validation failure
     */
    public ConfigValueValidationException(String detail, Throwable cause) {
        super(detail, cause);
    }
}
