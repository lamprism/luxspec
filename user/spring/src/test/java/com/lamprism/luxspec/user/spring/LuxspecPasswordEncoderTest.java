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

package com.lamprism.luxspec.user.spring;

import com.lamprism.luxspec.user.security.password.EncodedPassword;
import com.lamprism.luxspec.user.security.password.PasswordScheme;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuxspecPasswordEncoderTest {
    @Test
    void delegatesSpringOperationsToTheLuxspecScheme() {
        RecordingPasswordScheme scheme = new RecordingPasswordScheme();
        LuxspecPasswordEncoder encoder = new LuxspecPasswordEncoder(scheme);

        assertEquals("encoded", encoder.encode("raw"));
        assertTrue(encoder.matches("raw", "encoded"));
        assertTrue(encoder.upgradeEncoding("encoded"));
        assertEquals(1, scheme.encodeCalls);
        assertEquals(1, scheme.verifyCalls);
        assertEquals(1, scheme.upgradeCalls);
    }

    private static final class RecordingPasswordScheme implements PasswordScheme {
        private int encodeCalls;
        private int verifyCalls;
        private int upgradeCalls;

        @Override
        public EncodedPassword encode(CharSequence rawPassword) {
            encodeCalls++;
            if (!Objects.equals("raw", rawPassword.toString())) {
                throw new AssertionError("Unexpected raw password");
            }
            return new EncodedPassword("encoded");
        }

        @Override
        public boolean verify(CharSequence rawPassword, EncodedPassword encodedPassword) {
            verifyCalls++;
            return "raw".contentEquals(rawPassword) && "encoded".equals(encodedPassword.getValue());
        }

        @Override
        public boolean needsUpgrade(EncodedPassword encodedPassword) {
            upgradeCalls++;
            return "encoded".equals(encodedPassword.getValue());
        }
    }
}
