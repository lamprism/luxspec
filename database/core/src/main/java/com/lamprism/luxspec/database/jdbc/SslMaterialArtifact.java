package com.lamprism.luxspec.database.jdbc;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Driver-readable SSL file and the resources needed to release it.
 *
 * @author RollW
 */
public class SslMaterialArtifact implements AutoCloseable {
    private final Path path;
    private final List<AutoCloseable> resources;
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * Creates an SSL artifact.
     *
     * @param path      the driver-readable path
     * @param resources resources to close in reverse order
     */
    public SslMaterialArtifact(Path path, List<? extends AutoCloseable> resources) {
        this.path = Objects.requireNonNull(path, "path");
        this.resources = List.copyOf(Objects.requireNonNull(resources, "resources"));
    }

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
