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

package com.lamprism.luxspec.security.firewall.filtertable;

import com.lamprism.luxspec.security.authentication.SimpleSubject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileFilterTableTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void treatsMissingFileAsAnEmptyTable() {
        FileFilterTable table = new FileFilterTable(filePath());

        assertTrue(table.getEntries().isEmpty());
        assertFalse(Files.exists(table.getPath()));
    }

    @Test
    void persistsEntriesAndReloadsThemWithAllMetadata() throws Exception {
        Path path = filePath();
        FilterTarget target = FilterTarget.subject(new SimpleSubject("tenant:alpha", "user/42"));
        FilterEntry entry = new FilterEntry(
                target,
                FilterMode.DENY,
                Instant.parse("2026-09-21T01:02:03Z"),
                "manual\toperator block"
        );
        FileFilterTable table = new FileFilterTable(path);

        table.put(entry);

        String content = Files.readString(path);
        assertTrue(content.startsWith("# Luxspec firewall filter table v1\n"));
        assertTrue(content.contains("SUBJECT\ttenant%3Aalpha:user%2F42\tDENY"));
        FileFilterTable reloaded = new FileFilterTable(path);
        assertEquals(entry, reloaded.get(target));
    }

    @Test
    void removesAndClearsEntriesInTheFile() throws Exception {
        FileFilterTable table = new FileFilterTable(filePath());
        FilterEntry first = FilterEntry.deny(FilterTarget.clientAddress("192.0.2.1"));
        FilterEntry second = FilterEntry.allow(FilterTarget.clientAddress("192.0.2.0/24"));
        table.replaceAll(List.of(first, second));

        assertTrue(table.remove(first.target()));
        assertEquals(List.of(second), table.getEntries());
        FileFilterTable afterRemove = new FileFilterTable(table.getPath());
        assertEquals(List.of(second), afterRemove.getEntries());

        table.clear();
        assertTrue(table.getEntries().isEmpty());
        assertTrue(new FileFilterTable(table.getPath()).getEntries().isEmpty());
    }

    @Test
    void reloadsExplicitExternalEdits() throws Exception {
        Path path = filePath();
        FileFilterTable table = new FileFilterTable(path);
        FilterEntry entry = FilterEntry.allow(FilterTarget.clientAddress("198.51.100.4"));
        table.put(entry);
        Files.writeString(
                path,
                "# external edit\nADDRESS\t198.51.100.0%2F24\tDENY\t-\tchanged\n"
        );

        table.reload();

        assertEquals(FilterMode.DENY, table.get(FilterTarget.clientAddress("198.51.100.0/24")).mode());
    }

    @Test
    void rejectsMalformedContentAndKeepsPreviousSnapshot() throws Exception {
        Path path = filePath();
        FileFilterTable table = new FileFilterTable(path);
        FilterEntry entry = FilterEntry.allow(FilterTarget.clientAddress("203.0.113.4"));
        table.put(entry);
        Files.writeString(path, "ADDRESS\tbroken\tDENY\n");

        assertThrows(IllegalStateException.class, table::reload);
        assertEquals(entry, table.get(entry.target()));
    }

    private Path filePath() {
        return temporaryDirectory.resolve("firewall-filter-table.tsv");
    }
}
