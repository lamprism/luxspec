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

package com.lamprism.luxspec.security.crypto;

/**
 * Resolves immutable cryptographic key-set snapshots by provider-owned reference.
 *
 * <p>This is the single loading boundary used by cryptographic consumers. Implementations own the
 * representation and loading policy behind the reference. Consumers must use the resulting
 * {@link KeySet} and must not parse source-specific key material.</p>
 *
 * @author RollW
 */
@FunctionalInterface
public interface KeySetProvider {
    /**
     * Returns the current snapshot for one provider-owned key-set reference.
     *
     * @param keySetReference the provider-owned key-set reference
     * @return the matching non-null key set
     * @throws IllegalArgumentException when the provider does not define the name
     */
    KeySet get(String keySetReference);
}
