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

package com.lamprism.luxspec.database.jdbc;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

final class ManagedSslMaterialArtifact implements SslMaterialArtifact {
    private final Path path;
    private final List<AutoCloseable> resources;
    private final AtomicBoolean closed = new AtomicBoolean();

    ManagedSslMaterialArtifact(Path path, List<? extends AutoCloseable> resources) {
        this.path = Objects.requireNonNull(path, "path");
        this.resources = List.copyOf(Objects.requireNonNull(resources, "resources"));
    }

    @Override
    public Path getPath() {
        return path;
    }

    @Override
    public void close() throws Exception {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        Exception failure = null;
        List<AutoCloseable> reversed = new ArrayList<>(resources);
        Collections.reverse(reversed);
        for (AutoCloseable resource : reversed) {
            try {
                resource.close();
            } catch (Exception exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
