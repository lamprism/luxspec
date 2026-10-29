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

import com.lamprism.luxspec.security.firewall.filtertable.format.FilterTableFileFormat;
import com.lamprism.luxspec.security.firewall.filtertable.format.LineFilterTableFileFormat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.UnaryOperator;

/**
 * File-backed filter table with immutable runtime snapshots.
 *
 * <p>The file is loaded at construction. External edits are applied through {@link #reload()}.
 * Mutations prepare complete replacement files before publishing them, so a failed write does
 * not change the active in-memory table. Concurrent mutations of this instance are coordinated;
 * separate instances and external writers require application-level coordination. Replacement
 * uses an atomic move when supported and falls back to a regular move otherwise. File rewrites use
 * the selected format and do not preserve comments from an externally edited file.</p>
 *
 * @author RollW
 */
public final class FileFilterTable implements FilterTable {
    private final Path path;
    private final FilterTableFileFormat format;
    private final ReentrantReadWriteLock fileLock = new ReentrantReadWriteLock();
    private volatile FilterTableSnapshot currentSnapshot;

    /**
     * Loads a filter table from a file, treating a missing file as empty.
     *
     * @param path the filter-table file path
     */
    public FileFilterTable(Path path) {
        this(path, new LineFilterTableFileFormat());
    }

    /**
     * Loads a filter table using a caller-supplied file format.
     *
     * @param path   the filter-table file path
     * @param format the thread-safe file format
     */
    public FileFilterTable(Path path, FilterTableFileFormat format) {
        this.path = normalizePath(path);
        this.format = Objects.requireNonNull(format, "format");
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
    public void put(FilterEntry entry) {
        FilterEntry nonNullEntry = Objects.requireNonNull(entry, "entry");
        update(snapshot -> snapshot.withEntry(nonNullEntry));
    }

    @Override
    public boolean remove(FilterTarget target) {
        FilterTarget nonNullTarget = Objects.requireNonNull(target, "target");
        return update(snapshot -> snapshot.withoutTarget(nonNullTarget));
    }

    @Override
    public void replaceAll(Collection<? extends FilterEntry> entries) {
        FilterTableSnapshot replacement = new FilterTableSnapshot(entries);
        update(snapshot -> replacement);
    }

    @Override
    public void clear() {
        FilterTableSnapshot replacement = FilterTableSnapshot.empty();
        update(snapshot -> replacement);
    }

    /**
     * Reloads the current contents of the file and publishes them after a successful parse.
     *
     * <p>This method is the explicit boundary for administrator edits made outside the process.
     * A malformed or unreadable file leaves the current in-memory snapshot unchanged.</p>
     */
    public void reload() {
        while (true) {
            FilterTableSnapshot previous = currentSnapshot;
            FilterTableSnapshot replacement = readSnapshot(path);
            fileLock.writeLock().lock();
            try {
                if (currentSnapshot == previous) {
                    currentSnapshot = replacement;
                    return;
                }
            } finally {
                fileLock.writeLock().unlock();
            }
        }
    }

    private boolean update(UnaryOperator<FilterTableSnapshot> operation) {
        while (true) {
            FilterTableSnapshot previous = currentSnapshot;
            FilterTableSnapshot replacement = operation.apply(previous);
            if (replacement == previous) {
                return false;
            }
            Path temporaryPath = prepareSnapshot(path, replacement);
            try {
                fileLock.writeLock().lock();
                try {
                    if (currentSnapshot != previous) {
                        continue;
                    }
                    moveIntoPlace(temporaryPath, path);
                    currentSnapshot = replacement;
                    return true;
                } catch (IOException exception) {
                    throw new UncheckedIOException("Could not write filter table: " + path, exception);
                } finally {
                    fileLock.writeLock().unlock();
                }
            } finally {
                deleteTemporaryFile(temporaryPath);
            }
        }
    }

    private FilterTableSnapshot readSnapshot(Path path) {
        String contents;
        fileLock.readLock().lock();
        try {
            if (Files.notExists(path)) {
                return FilterTableSnapshot.empty();
            }
            if (Files.isDirectory(path)) {
                throw new IllegalStateException("Filter table path is a directory: " + path);
            }
            contents = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read filter table: " + path, exception);
        } finally {
            fileLock.readLock().unlock();
        }
        try (BufferedReader reader = new BufferedReader(new StringReader(contents))) {
            return Objects.requireNonNull(format.read(reader, path.toString()), "format.read");
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read filter table: " + path, exception);
        }
    }

    private Path prepareSnapshot(Path path, FilterTableSnapshot snapshot) {
        Path parent = path.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("Filter table path must have a parent directory");
        }
        Path temporaryPath = null;
        boolean prepared = false;
        try {
            Files.createDirectories(parent);
            temporaryPath = Files.createTempFile(parent, path.getFileName().toString(), ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(
                    temporaryPath,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
            )) {
                format.write(writer, snapshot);
            }
            prepared = true;
            return temporaryPath;
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not write filter table: " + path, exception);
        } finally {
            if (!prepared) {
                deleteTemporaryFile(temporaryPath);
            }
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
            /*
             * The target file has already been replaced or the original write failure is primary.
             */
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
