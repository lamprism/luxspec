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

package com.lamprism.luxspec.web;

import com.lamprism.luxspec.config.ConfigCodec;
import com.lamprism.luxspec.config.ConfigSpec;
import com.lamprism.luxspec.validation.Validator;

/**
 * Typed configuration definitions owned by the Web capability.
 *
 * @author RollW
 */
public final class WebConfigSpec {
    /**
     * The request and response header used for the correlation identifier.
     */
    public static final ConfigSpec<String> CORRELATION_ID_HEADER = ConfigSpec.of(
            "web.correlation-id-header",
            ConfigCodec.string(),
            "X-Request-ID",
            false,
            Validator.of(
                    value -> !value.isBlank(),
                    "Correlation ID header must not be blank"
            )
    );

    private WebConfigSpec() {
    }
}
