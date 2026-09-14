package com.lamprism.luxspec.database;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Describes where a database is hosted.
 */
public final class DatabaseTarget {
    public enum Kind {
        MEMORY,
        FILE,
        NETWORK
    }

    private final Kind kind;
    private final @Nullable Path file;
    private final @Nullable String host;
    private final @Nullable Integer port;

    private DatabaseTarget(
            Kind kind,
            @Nullable Path file,
            @Nullable String host,
            @Nullable Integer port
    ) {
        this.kind = kind;
        this.file = file;
        this.host = host;
        this.port = port;
    }

    public static DatabaseTarget memory() {
        return new DatabaseTarget(Kind.MEMORY, null, null, null);
    }

    public static DatabaseTarget file(Path file) {
        Path input = Objects.requireNonNull(file, "file");
        if (input.toString().isBlank()) {
            throw new IllegalArgumentException("file must not be blank");
        }
        Path value = input.normalize();
        validatePath(value);
        return new DatabaseTarget(Kind.FILE, value, null, null);
    }

    public static DatabaseTarget network(String host) {
        return network(host, null);
    }

    public static DatabaseTarget network(String host, @Nullable Integer port) {
        String value = validateHost(host);
        validatePort(port);
        return new DatabaseTarget(Kind.NETWORK, null, value, port);
    }

    public static DatabaseTarget parse(String serialized) {
        String value = Objects.requireNonNull(serialized, "serialized").trim();
        if (value.equals("memory")) {
            return memory();
        }
        if (value.startsWith("file:")) {
            String path = value.substring("file:".length());
            if (path.isBlank()) {
                throw new IllegalArgumentException("File database target must contain a path");
            }
            return file(Path.of(path));
        }
        if (value.startsWith("network:")) {
            return parseNetwork(value.substring("network:".length()));
        }
        throw new IllegalArgumentException("Unsupported database target format");
    }

    public Kind getKind() {
        return kind;
    }

    public @Nullable Path getFile() {
        return file;
    }

    public @Nullable String getHost() {
        return host;
    }

    public @Nullable Integer getPort() {
        return port;
    }

    private static DatabaseTarget parseNetwork(String serialized) {
        if (serialized.isBlank()) {
            throw new IllegalArgumentException("Network database target must contain a host");
        }
        if (serialized.charAt(0) == '[') {
            int closingBracket = serialized.indexOf(']');
            if (closingBracket < 2) {
                throw new IllegalArgumentException("Network target contains an invalid IPv6 host");
            }
            String host = serialized.substring(1, closingBracket);
            String suffix = serialized.substring(closingBracket + 1);
            if (suffix.isEmpty()) {
                return network(host);
            }
            if (!suffix.startsWith(":")) {
                throw new IllegalArgumentException("Network target contains an invalid port");
            }
            return network(host, parsePort(suffix.substring(1)));
        }
        int firstColon = serialized.indexOf(':');
        int lastColon = serialized.lastIndexOf(':');
        if (firstColon != lastColon) {
            throw new IllegalArgumentException("IPv6 hosts must use brackets");
        }
        if (firstColon < 0) {
            return network(serialized);
        }
        String host = serialized.substring(0, firstColon);
        String port = serialized.substring(firstColon + 1);
        return network(host, parsePort(port));
    }

    private static int parsePort(String value) {
        if (value.isBlank()) {
            throw new IllegalArgumentException("Network target port must not be blank");
        }
        for (int index = 0; index < value.length(); index++) {
            if (!Character.isDigit(value.charAt(index))) {
                throw new IllegalArgumentException("Network target port must be numeric");
            }
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Network target port is out of range", exception);
        }
    }

    private static String validateHost(String host) {
        String value = Objects.requireNonNull(host, "host").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isISOControl(character) || character == '/' || character == '?'
                    || character == '#' || character == '@' || character == ';'
                    || character == '=' || character == '\\') {
                throw new IllegalArgumentException("host contains an unsupported character");
            }
        }
        return value;
    }

    private static void validatePort(@Nullable Integer port) {
        if (port != null && (port < 1 || port > 65535)) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
    }

    private static void validatePath(Path path) {
        String value = path.toString();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isISOControl(character)) {
                throw new IllegalArgumentException("file contains an unsupported character");
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof DatabaseTarget target) || kind != target.kind) {
            return false;
        }
        return Objects.equals(file, target.file)
                && Objects.equals(host, target.host)
                && Objects.equals(port, target.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, file, host, port);
    }

    @Override
    public String toString() {
        return switch (kind) {
            case MEMORY -> "memory";
            case FILE -> "file:" + file;
            case NETWORK -> "network:" + formatHost(host) + formatPort(port);
        };
    }

    private static String formatHost(@Nullable String host) {
        Objects.requireNonNull(host, "host");
        return host.indexOf(':') >= 0 && !host.startsWith("[")
                ? "[" + host + "]"
                : host;
    }

    private static String formatPort(@Nullable Integer port) {
        return port == null ? "" : ":" + port;
    }
}
