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

package com.lamprism.luxspec.security.secret;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;

/**
 * Owned secret bytes with explicit masking and destruction semantics.
 *
 * <p>The value is copied on input and output. Callers should close it as soon as the consuming
 * operation completes. The class deliberately does not expose the material from {@link
 * #toString()}.</p>
 *
 * @author RollW
 */
public final class SecretValue implements AutoCloseable {
    private byte[] material;

    private SecretValue(byte[] material) {
        byte[] nonNullMaterial = Objects.requireNonNull(material, "material");
        if (nonNullMaterial.length == 0) {
            throw new IllegalArgumentException("Secret material must not be empty");
        }
        this.material = nonNullMaterial.clone();
    }

    /**
     * Creates a secret value from bytes.
     *
     * @param material the non-empty secret bytes
     * @return the owned secret value
     */
    public static SecretValue of(byte[] material) {
        return new SecretValue(material);
    }

    /**
     * Creates a secret value using an explicit charset.
     *
     * @param value   the non-empty secret text
     * @param charset the encoding charset
     * @return the owned secret value
     */
    public static SecretValue of(String value, Charset charset) {
        String nonNullValue = Objects.requireNonNull(value, "value");
        Charset nonNullCharset = Objects.requireNonNull(charset, "charset");
        return of(nonNullValue.getBytes(nonNullCharset));
    }

    /**
     * Returns a copy of the secret bytes.
     *
     * @return a copy of the secret bytes
     * @throws IllegalStateException when this value has been destroyed
     */
    public synchronized byte[] copy() {
        requireLive();
        return material.clone();
    }

    /**
     * Decodes a copy of the secret using an explicit charset.
     *
     * @param charset the decoding charset
     * @return the decoded secret text
     * @throws IllegalStateException when this value has been destroyed
     */
    public synchronized String asString(Charset charset) {
        return new String(copy(), Objects.requireNonNull(charset, "charset"));
    }

    /**
     * Returns the current secret length without exposing its contents.
     *
     * @return the secret length
     * @throws IllegalStateException when this value has been destroyed
     */
    public synchronized int length() {
        requireLive();
        return material.length;
    }

    /**
     * Reports whether the secret bytes have been destroyed.
     *
     * @return true after destruction
     */
    public synchronized boolean isDestroyed() {
        return material == null;
    }

    /**
     * Destroys the owned byte array. Repeated calls are harmless.
     */
    public synchronized void destroy() {
        if (material == null) {
            return;
        }
        Arrays.fill(material, (byte) 0);
        material = null;
    }

    @Override
    public void close() {
        destroy();
    }

    @Override
    public String toString() {
        return "SecretValue[REDACTED]";
    }

    private void requireLive() {
        if (material == null) {
            throw new IllegalStateException("Secret value has been destroyed");
        }
    }
}
