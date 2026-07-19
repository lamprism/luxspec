package com.lamprism.luxspec.security.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CredentialTypeTest {
    @Test
    void acceptsCanonicalSegmentedNames() {
        CredentialType<TestCredentials> credentialType = CredentialType.of(
                "external:access-token",
                TestCredentials.class
        );

        assertEquals("external:access-token", credentialType.getName());
    }

    @Test
    void rejectsMalformedNames() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CredentialType.of("external::access-token", TestCredentials.class)
        );
    }

    private static final class TestCredentials implements Credentials {
    }
}
