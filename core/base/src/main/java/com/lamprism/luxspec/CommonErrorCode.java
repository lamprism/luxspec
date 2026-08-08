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

package com.lamprism.luxspec;

/**
 * Common errors used by provider-independent foundation contracts.
 *
 * @author RollW
 */
public enum CommonErrorCode implements ErrorCode {
    /**
     * An unexpected internal failure occurred.
     */
    INTERNAL("common:internal"),
    /**
     * A caller supplied an invalid argument.
     */
    INVALID_ARGUMENT("common:invalid-argument"),
    /**
     * The requested operation is invalid for the current state.
     */
    ILLEGAL_STATE("common:illegal-state"),
    /**
     * A required domain object could not be found.
     */
    NOT_FOUND("common:not-found"),
    /**
     * The requested capability is not implemented or available.
     */
    UNSUPPORTED_OPERATION("common:unsupported-operation");

    private final String code;

    CommonErrorCode(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
