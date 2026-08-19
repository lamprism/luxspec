package com.lamprism.luxspec.database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseTargetTest {
    @Test
    void parsesAndSerializesAnIpv6NetworkTarget() {
        DatabaseTarget target = DatabaseTarget.parse("network:[::1]:5432");

        assertEquals(DatabaseTarget.network("::1", 5432), target);
        assertEquals("network:[::1]:5432", target.toString());
    }

    @Test
    void rejectsAnUnbracketedIpv6NetworkTarget() {
        assertThrows(
                IllegalArgumentException.class,
                () -> DatabaseTarget.parse("network:::1:5432")
        );
    }

    @Test
    void rejectsAnInvalidNetworkPort() {
        assertThrows(
                IllegalArgumentException.class,
                () -> DatabaseTarget.network("localhost", 0)
        );
    }

    @Test
    void rejectsJdbcUrlDelimiterCharactersFromHosts() {
        assertThrows(
                IllegalArgumentException.class,
                () -> DatabaseTarget.network("localhost;ssl=true")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> DatabaseTarget.network("localhost;ssl")
        );
    }

    @Test
    void keepsFileTargetsProviderIndependent() {
        assertEquals(
                java.nio.file.Path.of("database;MODE=PostgreSQL"),
                DatabaseTarget.file(java.nio.file.Path.of("database;MODE=PostgreSQL")).getFile()
        );
    }
}
