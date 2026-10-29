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

package com.lamprism.luxspec.config;

import com.lamprism.luxspec.ErrorCode;

/**
 * Defines stable configuration-domain error identifiers.
 *
 * @author RollW
 */
public enum ConfigErrorCode implements ErrorCode {
    /**
     * The source value cannot be decoded for the requested type.
     */
    INVALID_VALUE("config:invalid-value"),
    /**
     * A source failed while applying a typed write.
     */
    SOURCE_WRITE_FAILED("config:source-write-failed");

    private final String code;

    ConfigErrorCode(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
