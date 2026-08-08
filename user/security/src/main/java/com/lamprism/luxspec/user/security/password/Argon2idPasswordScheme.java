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

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

/**
 * Protects passwords with Argon2id version 1.3 and standard PHC representations.
 *
 * @author RollW
 */
public final class Argon2idPasswordScheme implements PasswordScheme {
    private static final String SCHEME_NAME = "argon2id";
    private static final String VERSION = "v=19";
    private static final int MAXIMUM_STORED_MEMORY_KIB = 262_144;
    private static final int MAXIMUM_STORED_ITERATIONS = 10;
    private static final int MAXIMUM_STORED_PARALLELISM = 16;
    private static final int MINIMUM_STORED_SALT_LENGTH = 8;
    private static final int MAXIMUM_STORED_SALT_LENGTH = 64;
    private static final int MINIMUM_STORED_HASH_LENGTH = 16;
    private static final int MAXIMUM_STORED_HASH_LENGTH = 64;

    private final Argon2idPasswordParameters parameters;
    private final SecureRandom secureRandom;

    /**
     * Creates the initial production Argon2id password scheme.
     */
    public Argon2idPasswordScheme() {
        this(Argon2idPasswordParameters.defaults());
    }

    /**
     * Creates an Argon2id password scheme with validated application parameters.
     *
     * @param parameters the current Argon2id parameters
     */
    public Argon2idPasswordScheme(Argon2idPasswordParameters parameters) {
        this.parameters = Objects.requireNonNull(parameters, "parameters");
        this.secureRandom = new SecureRandom();
    }

    @Override
    public EncodedPassword encode(CharSequence rawPassword) {
        byte[] salt = new byte[parameters.saltLength()];
        secureRandom.nextBytes(salt);
        try {
            byte[] derived = derive(
                    rawPassword,
                    salt,
                    parameters.memoryKiB(),
                    parameters.iterations(),
                    parameters.parallelism(),
                    parameters.hashLength()
            );
            try {
                return new EncodedPassword(format(salt, derived, parameters));
            } finally {
                clear(derived);
            }
        } finally {
            clear(salt);
        }
    }

    @Override
    public boolean verify(CharSequence rawPassword, EncodedPassword encodedPassword) {
        StoredPassword stored = parse(encodedPassword);
        try {
            byte[] derived = derive(
                    rawPassword,
                    stored.salt(),
                    stored.memoryKiB(),
                    stored.iterations(),
                    stored.parallelism(),
                    stored.hash().length
            );
            try {
                return MessageDigest.isEqual(derived, stored.hash());
            } finally {
                clear(derived);
            }
        } finally {
            stored.clear();
        }
    }

    @Override
    public boolean needsUpgrade(EncodedPassword encodedPassword) {
        StoredPassword stored = parse(encodedPassword);
        try {
            return stored.memoryKiB() != parameters.memoryKiB()
                    || stored.iterations() != parameters.iterations()
                    || stored.parallelism() != parameters.parallelism()
                    || stored.salt().length != parameters.saltLength()
                    || stored.hash().length != parameters.hashLength();
        } finally {
            stored.clear();
        }
    }

    private static byte[] derive(
            CharSequence rawPassword,
            byte[] salt,
            int memoryKiB,
            int iterations,
            int parallelism,
            int hashLength
    ) {
        byte[] rawBytes = Objects.requireNonNull(rawPassword, "rawPassword").toString().getBytes(StandardCharsets.UTF_8);
        Argon2Parameters argonParameters = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withMemoryAsKB(memoryKiB)
                .withIterations(iterations)
                .withParallelism(parallelism)
                .withSalt(salt)
                .build();
        byte[] derived = new byte[hashLength];
        try {
            Argon2BytesGenerator generator = new Argon2BytesGenerator();
            generator.init(argonParameters);
            generator.generateBytes(rawBytes, derived);
            return derived;
        } catch (RuntimeException exception) {
            clear(derived);
            throw new PasswordSchemeException(
                    PasswordErrorCode.INFRASTRUCTURE_FAILURE,
                    "Argon2id password processing failed",
                    exception
            );
        } finally {
            clear(rawBytes);
            argonParameters.clear();
        }
    }

    private static StoredPassword parse(EncodedPassword encodedPassword) {
        List<String> segments = split(Objects.requireNonNull(encodedPassword, "encodedPassword").getValue(), '$');
        if (segments.size() != 6 || !segments.get(0).isEmpty()) {
            throw malformed();
        }
        if (!SCHEME_NAME.equals(segments.get(1))) {
            throw new PasswordSchemeException(PasswordErrorCode.UNSUPPORTED_ENCODING, "Password scheme is unsupported");
        }
        if (!VERSION.equals(segments.get(2))) {
            throw new PasswordSchemeException(PasswordErrorCode.UNSUPPORTED_ENCODING, "Password scheme version is unsupported");
        }
        StoredParameters storedParameters = parseParameters(segments.get(3));
        byte[] salt = decode(segments.get(4));
        byte[] hash = decode(segments.get(5));
        validateLengths(salt, hash);
        return new StoredPassword(
                storedParameters.memoryKiB(),
                storedParameters.iterations(),
                storedParameters.parallelism(),
                salt,
                hash
        );
    }

    private static List<String> split(String value, char separator) {
        List<String> segments = new ArrayList<>();
        int segmentStart = 0;
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) == separator) {
                segments.add(value.substring(segmentStart, index));
                segmentStart = index + 1;
            }
        }
        segments.add(value.substring(segmentStart));
        return segments;
    }

    private static StoredParameters parseParameters(String value) {
        List<String> values = split(value, ',');
        if (values.size() != 3) {
            throw malformed();
        }
        int memoryKiB = parseParameter(values.get(0), "m=");
        int iterations = parseParameter(values.get(1), "t=");
        int parallelism = parseParameter(values.get(2), "p=");
        if (memoryKiB < parallelism * 8
                || memoryKiB > MAXIMUM_STORED_MEMORY_KIB
                || iterations < 1
                || iterations > MAXIMUM_STORED_ITERATIONS
                || parallelism < 1
                || parallelism > MAXIMUM_STORED_PARALLELISM) {
            throw malformed();
        }
        return new StoredParameters(memoryKiB, iterations, parallelism);
    }

    private static int parseParameter(String value, String prefix) {
        if (!value.startsWith(prefix) || value.length() == prefix.length()) {
            throw malformed();
        }
        String number = value.substring(prefix.length());
        for (int index = 0; index < number.length(); index++) {
            char character = number.charAt(index);
            if (character < '0' || character > '9') {
                throw malformed();
            }
        }
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException exception) {
            throw malformed();
        }
    }

    private static byte[] decode(String value) {
        if (value.isEmpty()) {
            throw malformed();
        }
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException exception) {
            throw malformed();
        }
    }

    private static void validateLengths(byte[] salt, byte[] hash) {
        if (salt.length < MINIMUM_STORED_SALT_LENGTH || salt.length > MAXIMUM_STORED_SALT_LENGTH) {
            clear(salt);
            clear(hash);
            throw malformed();
        }
        if (hash.length < MINIMUM_STORED_HASH_LENGTH || hash.length > MAXIMUM_STORED_HASH_LENGTH) {
            clear(salt);
            clear(hash);
            throw malformed();
        }
    }

    private static String format(byte[] salt, byte[] hash, Argon2idPasswordParameters parameters) {
        Base64.Encoder encoder = Base64.getEncoder().withoutPadding();
        return "$" + SCHEME_NAME
                + "$" + VERSION
                + "$m=" + parameters.memoryKiB() + ",t=" + parameters.iterations() + ",p=" + parameters.parallelism()
                + "$" + encoder.encodeToString(salt)
                + "$" + encoder.encodeToString(hash);
    }

    private static PasswordSchemeException malformed() {
        return new PasswordSchemeException(PasswordErrorCode.MALFORMED_ENCODING, "Password encoding is malformed");
    }

    private static void clear(byte[] value) {
        java.util.Arrays.fill(value, (byte) 0);
    }

    private static final class StoredParameters {
        private final int memoryKiB;
        private final int iterations;
        private final int parallelism;

        private StoredParameters(int memoryKiB, int iterations, int parallelism) {
            this.memoryKiB = memoryKiB;
            this.iterations = iterations;
            this.parallelism = parallelism;
        }

        private int memoryKiB() {
            return memoryKiB;
        }

        private int iterations() {
            return iterations;
        }

        private int parallelism() {
            return parallelism;
        }
    }

    private static final class StoredPassword {
        private final int memoryKiB;
        private final int iterations;
        private final int parallelism;
        private final byte[] salt;
        private final byte[] hash;

        private StoredPassword(int memoryKiB, int iterations, int parallelism, byte[] salt, byte[] hash) {
            this.memoryKiB = memoryKiB;
            this.iterations = iterations;
            this.parallelism = parallelism;
            this.salt = salt;
            this.hash = hash;
        }

        private int memoryKiB() {
            return memoryKiB;
        }

        private int iterations() {
            return iterations;
        }

        private int parallelism() {
            return parallelism;
        }

        private byte[] salt() {
            return salt;
        }

        private byte[] hash() {
            return hash;
        }

        private void clear() {
            Argon2idPasswordScheme.clear(salt);
            Argon2idPasswordScheme.clear(hash);
        }
    }
}
