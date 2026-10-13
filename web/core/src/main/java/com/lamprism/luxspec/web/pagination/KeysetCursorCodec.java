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

package com.lamprism.luxspec.web.pagination;

import com.lamprism.luxspec.data.pagination.KeysetPosition;
import com.lamprism.luxspec.data.query.OrderBy;

import javax.crypto.SecretKey;
import java.util.List;

/**
 * Encodes and decodes a keyset continuation position for one ordered query.
 *
 * <p>The codec is deliberately a transport boundary. Implementations must authenticate untrusted
 * cursor text and bind it to the exact ordering before returning a position.</p>
 *
 * @author RollW
 */
public interface KeysetCursorCodec {
    /**
     * Encodes a continuation position.
     *
     * @param orders   the exact query ordering
     * @param position the continuation position
     * @return the opaque cursor text
     */
    String encode(List<? extends OrderBy<?>> orders, KeysetPosition position);

    /**
     * Verifies and decodes a continuation position.
     *
     * @param orders the exact query ordering
     * @param token  the untrusted cursor text
     * @return the verified position
     * @throws IllegalArgumentException when the cursor is invalid
     */
    KeysetPosition decode(List<? extends OrderBy<?>> orders, String token);

    /**
     * Creates the standard signed cursor codec.
     *
     * @param purpose    endpoint-specific purpose
     * @param signingKey the application-managed signing key
     * @return the default codec builder
     */
    static DefaultKeysetCursorCodec.Builder signed(String purpose, SecretKey signingKey) {
        return DefaultKeysetCursorCodec.builder(purpose, signingKey);
    }
}
