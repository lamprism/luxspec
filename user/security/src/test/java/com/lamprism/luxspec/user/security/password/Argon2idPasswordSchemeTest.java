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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Argon2idPasswordSchemeTest {
    private static final Argon2idPasswordConfiguration TEST_PARAMETERS = new Argon2idPasswordConfiguration(
            8_192,
            1,
            1,
            16,
            32
    );

    @Test
    void encodesAndVerifiesPasswordsUsingAStandardPhcRepresentation() {
        Argon2idPasswordScheme scheme = new Argon2idPasswordScheme(TEST_PARAMETERS);

        EncodedPassword encodedPassword = scheme.encode("correct horse");

        assertTrue(encodedPassword.getValue().startsWith("$argon2id$v=19$m=8192,t=1,p=1$"));
        assertTrue(scheme.verify("correct horse", encodedPassword));
        assertFalse(scheme.verify("incorrect horse", encodedPassword));
        assertFalse(scheme.needsUpgrade(encodedPassword));
    }

    @Test
    void reportsWhenStoredParametersNeedAnUpgrade() {
        Argon2idPasswordScheme oldScheme = new Argon2idPasswordScheme(TEST_PARAMETERS);
        Argon2idPasswordScheme currentScheme = new Argon2idPasswordScheme(
                new Argon2idPasswordConfiguration(8_192, 2, 1, 16, 32)
        );

        EncodedPassword encodedPassword = oldScheme.encode("secret");

        assertTrue(currentScheme.needsUpgrade(encodedPassword));
        assertTrue(currentScheme.verify("secret", encodedPassword));
    }

    @Test
    void rejectsMalformedAndUnsupportedStoredValuesExplicitly() {
        Argon2idPasswordScheme scheme = new Argon2idPasswordScheme(TEST_PARAMETERS);

        PasswordSchemeException malformed = assertThrows(
                PasswordSchemeException.class,
                () -> scheme.verify("secret", new EncodedPassword("not-a-phc-value"))
        );
        PasswordSchemeException unsupported = assertThrows(
                PasswordSchemeException.class,
                () -> scheme.needsUpgrade(new EncodedPassword(
                        "$bcrypt$v=19$m=8192,t=1,p=1$AQ$Ag"
                ))
        );

        assertEquals(PasswordErrorCode.MALFORMED_ENCODING, malformed.getErrorCode());
        assertEquals(PasswordErrorCode.UNSUPPORTED_ENCODING, unsupported.getErrorCode());
    }

    @Test
    void redactsEncodedPasswordsFromStringRepresentations() {
        EncodedPassword encodedPassword = new EncodedPassword("secret-representation");

        assertTrue(encodedPassword.toString().contains("redacted"));
        assertFalse(encodedPassword.toString().contains("secret-representation"));
    }
}
