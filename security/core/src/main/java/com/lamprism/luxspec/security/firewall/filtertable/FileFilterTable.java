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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Objects;

/**
 * File-backed filter table with atomic replacement and immutable runtime snapshots.
 *
 * <p>The file is loaded at construction. External edits are applied through {@link #reload()}.
 * Mutations write a complete replacement before publishing it to readers, so a failed write does
 * not change the active in-memory table.</p>
 *
 * @author RollW
 */
public final class FileFilterTable implements FilterTable {
    private final Path path;
    private volatile FilterTableSnapshot currentSnapshot;

    /**
     * Loads a filter table from a file, treating a missing file as empty.
     *
     * @param path the filter-table file path
     */
    public FileFilterTable(Path path) {
        this.path = normalizePath(path);
        this.currentSnapshot = readSnapshot(this.path);
    }

    /**
     * Returns the normalized file path.
     *
     * @return the file path
     */
    public Path getPath() {
        return path;
    }

    @Override
    public FilterTableSnapshot snapshot() {
        return currentSnapshot;
    }

    @Override
    public synchronized void put(FilterEntry entry) {
        FilterEntry nonNullEntry = Objects.requireNonNull(entry, "entry");
        FilterTableSnapshot replacement = currentSnapshot.withEntry(nonNullEntry);
        writeSnapshot(path, replacement);
        currentSnapshot = replacement;
    }

    @Override
    public synchronized boolean remove(FilterTarget target) {
        FilterTarget nonNullTarget = Objects.requireNonNull(target, "target");
        FilterTableSnapshot replacement = currentSnapshot.withoutTarget(nonNullTarget);
        if (replacement == currentSnapshot) {
            return false;
        }
        writeSnapshot(path, replacement);
        currentSnapshot = replacement;
        return true;
    }

    @Override
    public synchronized void replaceAll(Collection<? extends FilterEntry> entries) {
        FilterTableSnapshot replacement = new FilterTableSnapshot(entries);
        writeSnapshot(path, replacement);
        currentSnapshot = replacement;
    }

    @Override
    public synchronized void clear() {
        FilterTableSnapshot replacement = FilterTableSnapshot.empty();
        writeSnapshot(path, replacement);
        currentSnapshot = replacement;
    }

    /**
     * Reloads the current contents of the file and publishes them after a successful parse.
     *
     * <p>This method is the explicit boundary for administrator edits made outside the process.
     * A malformed or unreadable file leaves the current in-memory snapshot unchanged.</p>
     */
    public synchronized void reload() {
        FilterTableSnapshot replacement = readSnapshot(path);
        currentSnapshot = replacement;
    }

    private static FilterTableSnapshot readSnapshot(Path path) {
        if (Files.notExists(path)) {
            return FilterTableSnapshot.empty();
        }
        if (Files.isDirectory(path)) {
            throw new IllegalStateException("Filter table path is a directory: " + path);
        }
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return FilterTableFileFormat.read(reader, path.toString());
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read filter table: " + path, exception);
        }
    }

    private static void writeSnapshot(Path path, FilterTableSnapshot snapshot) {
        Path parent = path.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("Filter table path must have a parent directory");
        }
        Path temporaryPath = null;
        try {
            Files.createDirectories(parent);
            temporaryPath = Files.createTempFile(parent, path.getFileName().toString(), ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(
                    temporaryPath,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
            )) {
                FilterTableFileFormat.write(writer, snapshot);
            }
            moveIntoPlace(temporaryPath, path);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not write filter table: " + path, exception);
        } finally {
            deleteTemporaryFile(temporaryPath);
        }
    }

    private static void moveIntoPlace(Path temporaryPath, Path targetPath) throws IOException {
        try {
            Files.move(
                    temporaryPath,
                    targetPath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteTemporaryFile(Path temporaryPath) {
        if (temporaryPath == null) {
            return;
        }
        try {
            Files.deleteIfExists(temporaryPath);
        } catch (IOException ignored) {
            // The target file has already been replaced or the original write failure is primary.
        }
    }

    private static Path normalizePath(Path path) {
        Path nonNullPath = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        if (Files.exists(nonNullPath) && Files.isDirectory(nonNullPath)) {
            throw new IllegalArgumentException("Filter table path is a directory: " + nonNullPath);
        }
        return nonNullPath;
    }
}
