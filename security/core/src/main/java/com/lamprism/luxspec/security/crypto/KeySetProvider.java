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
 * Resolves immutable cryptographic key-set snapshots by provider-owned name.
 *
 * @author RollW
 */
@FunctionalInterface
public interface KeySetProvider {
    /**
     * Returns the current snapshot for one key-set name.
     *
     * @param keySetName the provider-local key-set name
     * @return the matching non-null key set
     * @throws IllegalArgumentException when the provider does not define the name
     */
    KeySet get(String keySetName);
}
