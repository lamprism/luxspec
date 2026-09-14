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

/**
 * Defines validated Argon2id work factors and generated value lengths.
 *
 * @author RollW
 */
public final class Argon2idPasswordConfiguration {
    private static final int MINIMUM_MEMORY_KIB = 8;
    private static final int MAXIMUM_MEMORY_KIB = 262_144;
    private static final int MINIMUM_ITERATIONS = 1;
    private static final int MAXIMUM_ITERATIONS = 10;
    private static final int MINIMUM_PARALLELISM = 1;
    private static final int MAXIMUM_PARALLELISM = 16;
    private static final int MINIMUM_SALT_LENGTH = 8;
    private static final int MAXIMUM_SALT_LENGTH = 64;
    private static final int MINIMUM_HASH_LENGTH = 16;
    private static final int MAXIMUM_HASH_LENGTH = 64;

    private final int memoryKiB;
    private final int iterations;
    private final int parallelism;
    private final int saltLength;
    private final int hashLength;

    /**
     * Creates validated Argon2id work factors and generated-value lengths.
     *
     * @param memoryKiB   the memory cost in kibibytes
     * @param iterations  the time cost
     * @param parallelism the number of parallel lanes
     * @param saltLength  the generated random salt length in bytes
     * @param hashLength  the derived value length in bytes
     */
    public Argon2idPasswordConfiguration(
            int memoryKiB,
            int iterations,
            int parallelism,
            int saltLength,
            int hashLength
    ) {
        requireRange(memoryKiB, MINIMUM_MEMORY_KIB, MAXIMUM_MEMORY_KIB, "memoryKiB");
        requireRange(iterations, MINIMUM_ITERATIONS, MAXIMUM_ITERATIONS, "iterations");
        requireRange(parallelism, MINIMUM_PARALLELISM, MAXIMUM_PARALLELISM, "parallelism");
        if (memoryKiB < parallelism * 8) {
            throw new IllegalArgumentException("memoryKiB must support the configured parallelism");
        }
        requireRange(saltLength, MINIMUM_SALT_LENGTH, MAXIMUM_SALT_LENGTH, "saltLength");
        requireRange(hashLength, MINIMUM_HASH_LENGTH, MAXIMUM_HASH_LENGTH, "hashLength");
        this.memoryKiB = memoryKiB;
        this.iterations = iterations;
        this.parallelism = parallelism;
        this.saltLength = saltLength;
        this.hashLength = hashLength;
    }

    /**
     * Returns the initial production Argon2id parameters.
     *
     * @return the initial password-protection parameters
     */
    public static Argon2idPasswordConfiguration defaults() {
        return new Argon2idPasswordConfiguration(65_536, 3, 1, 16, 32);
    }

    int memoryKiB() {
        return memoryKiB;
    }

    int iterations() {
        return iterations;
    }

    int parallelism() {
        return parallelism;
    }

    int saltLength() {
        return saltLength;
    }

    int hashLength() {
        return hashLength;
    }

    private static void requireRange(int value, int minimum, int maximum, String name) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " is outside the supported range");
        }
    }
}
