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

package com.lamprism.luxspec.security.crypto.config;

/**
 * Identifies the supported configuration representations for one cryptographic key entry.
 *
 * @author RollW
 */
public enum ConfigKeyMaterialType {
    /**
     * A symmetric secret used for both signing and verification.
     */
    SECRET,
    /**
     * A private key with a public verification key.
     */
    KEY_PAIR,
    /**
     * A public verification key without signing material.
     */
    PUBLIC_KEY
}
