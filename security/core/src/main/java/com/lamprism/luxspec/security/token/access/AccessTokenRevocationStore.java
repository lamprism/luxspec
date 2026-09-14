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

/**
 * Stores explicit revocations for already verified access-token identities.
 *
 * <p>Implementations retain revocation state only until the token's natural expiration and
 * never receive an encoded access-token body.</p>
 *
 * @author RollW
 */
public interface AccessTokenRevocationStore {
    /**
     * Records an explicit revocation for a verified access token.
     *
     * @param accessToken the verified token metadata containing its stable identifier and expiration
     */
    void revoke(VerifiedAccessToken accessToken);

    /**
     * Reports whether a verified access token has been explicitly revoked.
     *
     * @param accessToken the verified token metadata to check
     * @return whether the token must be rejected
     */
    boolean isRevoked(VerifiedAccessToken accessToken);
}
