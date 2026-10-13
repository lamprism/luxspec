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

import com.lamprism.luxspec.ErrorCode;

/**
 * HTTP protocol errors used by provider-independent Web adapters.
 *
 * @author RollW
 */
public enum WebErrorCode implements ErrorCode {
    /**
     * The request method is not supported for the target resource.
     */
    METHOD_NOT_ALLOWED("web:method-not-allowed"),
    /**
     * The response representation cannot satisfy the request accept header.
     */
    NOT_ACCEPTABLE("web:not-acceptable"),
    /**
     * The request representation uses an unsupported media type.
     */
    UNSUPPORTED_MEDIA_TYPE("web:unsupported-media-type");

    private final String code;

    WebErrorCode(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
