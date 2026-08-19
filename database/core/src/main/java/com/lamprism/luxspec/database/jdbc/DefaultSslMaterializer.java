package com.lamprism.luxspec.database.jdbc;

import com.lamprism.luxspec.database.SslMaterial;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Materializes file or inline SSL content as a PEM file.
 *
 * <p>Inline files are owner-readable and owner-writable where the host file system exposes those
 * permissions. Temporary files are deleted when the returned artifact is closed.</p>
 *
 * @author RollW
 */
public class DefaultSslMaterializer implements SslMaterializer {
    @Override
    public SslMaterialArtifact materialize(String name, SslMaterial material) {
        String prefix = temporaryPrefix(name);
        SslMaterial nonNullMaterial = Objects.requireNonNull(material, "material");
        if (nonNullMaterial.getSource() == SslMaterial.Source.FILE) {
            return new SslMaterialArtifact(
                    Objects.requireNonNull(nonNullMaterial.getPath(), "material path")
                            .toAbsolutePath()
                            .normalize(),
                    List.of()
            );
        }

        Path path = null;
        try {
            path = Files.createTempFile(prefix, ".pem");
            restrictPermissions(path);
            Files.writeString(path, nonNullMaterial.getValue(), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException exception) {
            deleteAfterFailure(path);
            throw new IllegalArgumentException("Unable to materialize database SSL material", exception);
        }
        Path materialPath = path;
        return new SslMaterialArtifact(materialPath, List.of(
                () -> Files.deleteIfExists(materialPath)
        ));
    }

    private static String temporaryPrefix(String name) {
        String value = Objects.requireNonNull(name, "name").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        StringBuilder prefix = new StringBuilder("luxspec-db-");
        for (int index = 0; index < value.length() && prefix.length() < 24; index++) {
            char character = value.charAt(index);
            if (Character.isLetterOrDigit(character) || character == '-') {
                prefix.append(Character.toLowerCase(character));
            }
        }
        while (prefix.length() < 3) {
            prefix.append('x');
        }
        return prefix.toString();
    }

    private static void restrictPermissions(Path path) throws IOException {
        try {
            Files.setPosixFilePermissions(path, Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
            ));
        } catch (UnsupportedOperationException exception) {
            path.toFile().setReadable(false, false);
            path.toFile().setWritable(false, false);
            path.toFile().setExecutable(false, false);
            boolean readable = path.toFile().setReadable(true, true);
            boolean writable = path.toFile().setWritable(true, true);
            if (!readable || !writable) {
                throw new IOException("Unable to restrict temporary SSL material permissions", exception);
            }
        }
    }

    private static void deleteAfterFailure(@Nullable Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException cleanupException) {
            // The original materialization failure is more useful to the caller.
        }
    }

}
