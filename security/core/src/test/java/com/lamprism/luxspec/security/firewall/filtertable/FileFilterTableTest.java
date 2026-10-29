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

import com.lamprism.luxspec.network.ip.IpNetwork;

import com.lamprism.luxspec.security.authentication.SimpleSubject;
import com.lamprism.luxspec.security.firewall.filtertable.format.FilterTableFileFormat;
import com.lamprism.luxspec.security.firewall.filtertable.format.LineFilterTableFileFormat;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author RollW
 */
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
        FilterTarget target = FilterTarget.subject(new SimpleSubject("tenant:\u4e2d", "user/42"));
        FilterEntry entry = new FilterEntry(
                target,
                FilterMode.DENY,
                Instant.parse("2026-09-21T01:02:03Z"),
                "manual\toperator \"block\" \u4e2d"
        );
        FileFilterTable table = new FileFilterTable(path);

        table.put(entry);

        String content = Files.readString(path);
        assertTrue(content.startsWith("# Luxspec firewall filter table\n"));
        assertTrue(content.contains("deny subject \"tenant:\u4e2d\" \"user/42\""));
        assertTrue(content.contains("reason \"manual\\toperator \\\"block\\\" \u4e2d\""));
        FileFilterTable reloaded = new FileFilterTable(path);
        assertEquals(entry, reloaded.get(target));
    }

    @Test
    void removesAndClearsEntriesInTheFile() throws Exception {
        FileFilterTable table = new FileFilterTable(filePath());
        FilterEntry first = FilterEntry.deny(FilterTarget.clientAddress(IpNetwork.parse("192.0.2.1")));
        FilterEntry second = FilterEntry.allow(FilterTarget.clientAddress(IpNetwork.parse("192.0.2.0/24")));
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
        FilterEntry entry = FilterEntry.allow(FilterTarget.clientAddress(IpNetwork.parse("198.51.100.4")));
        table.put(entry);
        Files.writeString(
                path,
                "# external edit\ndeny address 198.51.100.0/24 reason \"changed\"\n"
        );

        table.reload();

        assertEquals(FilterMode.DENY, table.get(FilterTarget.clientAddress(IpNetwork.parse("198.51.100.0/24"))).mode());
    }

    @Test
    void rejectsMalformedContentAndKeepsPreviousSnapshot() throws Exception {
        Path path = filePath();
        FileFilterTable table = new FileFilterTable(path);
        FilterEntry entry = FilterEntry.allow(FilterTarget.clientAddress(IpNetwork.parse("203.0.113.4")));
        table.put(entry);
        Files.writeString(path, "deny address broken\n");

        assertThrows(IllegalStateException.class, table::reload);
        assertEquals(entry, table.get(entry.target()));
    }

    @Test
    void allowsConcurrentPreparationWithoutLosingEitherUpdate() throws Exception {
        CountDownLatch preparing = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        FilterTableFileFormat delegate = new LineFilterTableFileFormat();
        FilterTableFileFormat format = new FilterTableFileFormat() {
            @Override
            public FilterTableSnapshot read(@NonNull BufferedReader reader, @NonNull String sourceName) throws IOException {
                return delegate.read(reader, sourceName);
            }

            @Override
            public void write(@NonNull Writer writer, @NonNull FilterTableSnapshot snapshot) throws IOException {
                preparing.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) {
                        throw new IOException("Timed out waiting for concurrent preparation");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while preparing a filter table", exception);
                }
                delegate.write(writer, snapshot);
            }
        };
        FileFilterTable table = new FileFilterTable(filePath(), format);
        FilterEntry first = FilterEntry.allow(FilterTarget.clientAddress(IpNetwork.parse("192.0.2.1")));
        FilterEntry second = FilterEntry.deny(FilterTarget.clientAddress(IpNetwork.parse("192.0.2.2")));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> firstUpdate = executor.submit(() -> table.put(first));
            Future<?> secondUpdate = executor.submit(() -> table.put(second));
            assertTrue(preparing.await(5, TimeUnit.SECONDS));
            release.countDown();
            firstUpdate.get(10, TimeUnit.SECONDS);
            secondUpdate.get(10, TimeUnit.SECONDS);

            assertEquals(first, table.get(first.target()));
            assertEquals(second, table.get(second.target()));
            FileFilterTable reloaded = new FileFilterTable(table.getPath());
            assertEquals(first, reloaded.get(first.target()));
            assertEquals(second, reloaded.get(second.target()));
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void keepsThePreviousFileAndSnapshotWhenPreparationFails() throws Exception {
        AtomicBoolean fail = new AtomicBoolean();
        FilterTableFileFormat delegate = new LineFilterTableFileFormat();
        FilterTableFileFormat format = new FilterTableFileFormat() {
            @Override
            public FilterTableSnapshot read(@NonNull BufferedReader reader, @NonNull String sourceName) throws IOException {
                return delegate.read(reader, sourceName);
            }

            @Override
            public void write(@NonNull Writer writer, @NonNull FilterTableSnapshot snapshot) throws IOException {
                if (fail.get()) {
                    throw new IOException("Simulated write failure");
                }
                delegate.write(writer, snapshot);
            }
        };
        FileFilterTable table = new FileFilterTable(filePath(), format);
        FilterEntry first = FilterEntry.allow(FilterTarget.clientAddress(IpNetwork.parse("192.0.2.1")));
        FilterEntry second = FilterEntry.deny(FilterTarget.clientAddress(IpNetwork.parse("192.0.2.2")));
        table.put(first);
        String previousFile = Files.readString(table.getPath());
        fail.set(true);

        assertThrows(UncheckedIOException.class, () -> table.put(second));
        assertEquals(List.of(first), table.getEntries());
        assertEquals(previousFile, Files.readString(table.getPath()));
    }

    @Test
    void retriesAConcurrentReloadAfterAnUpdate() throws Exception {
        CountDownLatch read = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean blockFirstRead = new AtomicBoolean(true);
        FilterTableFileFormat delegate = new LineFilterTableFileFormat();
        FilterTableFileFormat format = new FilterTableFileFormat() {
            @Override
            public FilterTableSnapshot read(@NonNull BufferedReader reader, @NonNull String sourceName) throws IOException {
                FilterTableSnapshot snapshot = delegate.read(reader, sourceName);
                if (blockFirstRead.compareAndSet(true, false)) {
                    read.countDown();
                    try {
                        if (!release.await(5, TimeUnit.SECONDS)) {
                            throw new IOException("Timed out waiting for the update");
                        }
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted while reading a filter table", exception);
                    }
                }
                return snapshot;
            }

            @Override
            public void write(@NonNull Writer writer, @NonNull FilterTableSnapshot snapshot) throws IOException {
                delegate.write(writer, snapshot);
            }
        };
        FileFilterTable table = new FileFilterTable(filePath(), format);
        FilterEntry first = FilterEntry.allow(FilterTarget.clientAddress(IpNetwork.parse("192.0.2.1")));
        FilterEntry second = FilterEntry.deny(FilterTarget.clientAddress(IpNetwork.parse("192.0.2.2")));
        table.put(first);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> reload = executor.submit(table::reload);
            assertTrue(read.await(5, TimeUnit.SECONDS));
            table.put(second);
            release.countDown();
            reload.get(10, TimeUnit.SECONDS);

            assertEquals(first, table.get(first.target()));
            assertEquals(second, table.get(second.target()));
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    private Path filePath() {
        return temporaryDirectory.resolve("firewall.rules");
    }
}
