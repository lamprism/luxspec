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

package com.lamprism.luxspec.database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author RollW
 */
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
