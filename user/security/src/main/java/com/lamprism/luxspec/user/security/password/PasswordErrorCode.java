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

package com.lamprism.luxspec.user.security.password;

import com.lamprism.luxspec.ErrorCode;

/**
 * Identifies failures while processing protected password representations.
 *
 * @author RollW
 */
public enum PasswordErrorCode implements ErrorCode {
    /**
     * A stored password representation is malformed.
     */
    MALFORMED_ENCODING("user:password-encoding-malformed"),
    /**
     * A stored password representation uses an unsupported protection scheme.
     */
    UNSUPPORTED_ENCODING("user:password-encoding-unsupported"),
    /**
     * The configured password infrastructure could not process a valid representation.
     */
    INFRASTRUCTURE_FAILURE("user:password-infrastructure-failure");

    private final String code;

    PasswordErrorCode(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
