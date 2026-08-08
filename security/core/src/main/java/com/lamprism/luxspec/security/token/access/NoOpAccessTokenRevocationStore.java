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

package com.lamprism.luxspec.security.token.access;

import java.util.Objects;

/**
 * Disables optional access-token revocation lookups without retaining token state.
 *
 * @author RollW
 */
public final class NoOpAccessTokenRevocationStore implements AccessTokenRevocationStore {
    private static final NoOpAccessTokenRevocationStore INSTANCE = new NoOpAccessTokenRevocationStore();

    private NoOpAccessTokenRevocationStore() {
    }

    /**
     * Returns the shared disabled revocation store.
     *
     * @return the no-op store
     */
    public static NoOpAccessTokenRevocationStore getInstance() {
        return INSTANCE;
    }

    /**
     * Accepts a revocation request without retaining state.
     *
     * @param accessToken the verified access token
     */
    @Override
    public void revoke(VerifiedAccessToken accessToken) {
        Objects.requireNonNull(accessToken, "accessToken");
    }

    /**
     * Always reports no explicit revocation.
     *
     * @param accessToken the verified access token
     * @return false because this store is disabled
     */
    @Override
    public boolean isRevoked(VerifiedAccessToken accessToken) {
        Objects.requireNonNull(accessToken, "accessToken");
        return false;
    }
}
