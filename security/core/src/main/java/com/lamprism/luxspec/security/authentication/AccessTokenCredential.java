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

import com.lamprism.luxspec.security.token.access.AccessToken;

import java.util.Objects;

/**
 * Carries one Access Token only for the duration of authentication.
 *
 * @author RollW
 */
public final class AccessTokenCredential implements Credential {
    private final AccessToken accessToken;

    /**
     * Creates credentials from one opaque Access Token.
     *
     * @param accessToken the presented Token
     */
    public AccessTokenCredential(AccessToken accessToken) {
        this.accessToken = Objects.requireNonNull(accessToken, "accessToken");
    }

    /**
     * Returns the token for an access-token authenticator.
     *
     * @return the opaque Access Token
     */
    public AccessToken getAccessToken() {
        return accessToken;
    }
}
