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


/**
 * Authenticates one exact credential type.
 *
 * @param <C> the credential type
 * @author RollW
 */
public interface Authenticator<C extends Credentials> {
    /**
     * Returns the exact credential type this authenticator owns.
     *
     * @return the credential type
     */
    CredentialType<C> getCredentialType();

    /**
     * Authenticates one credential instance or throws a stable authentication failure.
     *
     * @param credentials the short-lived credential input
     * @return the authenticated subject and effective grants
     */
    Authentication authenticate(C credentials);
}
