package com.lamprism.luxspec.user.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lamprism.luxspec.user.security.EncodedPassword;
import com.lamprism.luxspec.user.security.PasswordScheme;
import java.util.Objects;
import org.junit.jupiter.api.Test;

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
