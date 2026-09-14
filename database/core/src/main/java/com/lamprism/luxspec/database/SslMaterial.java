package com.lamprism.luxspec.database;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * Identifies the source of one database SSL material value.
 *
 * <p>File material references an existing path. Value material contains PEM or driver-supported
 * text and is materialized by the selected JDBC adapter.</p>
 *
 * @author RollW
 */
public final class SslMaterial {
    public enum Source {
        FILE,
        VALUE
    }

    private final Source source;
    private final String value;

    private SslMaterial(Source source, String value) {
        this.source = Objects.requireNonNull(source, "source");
        this.value = requireValue(value);
    }

    /**
     * Creates material backed by an existing file.
     *
     * @param path the existing material path
     * @return the file material reference
     */
    public static SslMaterial file(Path path) {
        return new SslMaterial(Source.FILE, Objects.requireNonNull(path, "path").toString());
    }

    /**
     * Creates inline material.
     *
     * @param value the PEM or driver-supported material content
     * @return the inline material
     */
    public static SslMaterial value(String value) {
        return new SslMaterial(Source.VALUE, value);
    }

    /**
     * Parses a {@code file:} or {@code value:} material descriptor.
     *
     * @param serialized the material descriptor
     * @return the parsed material
     */
    public static SslMaterial parse(String serialized) {
        String input = Objects.requireNonNull(serialized, "serialized");
        int separator = input.indexOf(':');
        if (separator <= 0) {
            throw new IllegalArgumentException("SSL material must use file: or value: format");
        }
        String sourceName = input.substring(0, separator).trim().toLowerCase(Locale.ROOT);
        String sourceValue = input.substring(separator + 1);
        if (sourceName.equals("file")) {
            return file(Path.of(requireValue(sourceValue.trim())));
        }
        if (sourceName.equals("value")) {
            return value(requireValue(sourceValue));
        }
        throw new IllegalArgumentException("Unsupported SSL material source: " + sourceName);
    }

    public Source getSource() {
        return source;
    }

    public String getValue() {
        return value;
    }

    public @Nullable Path getPath() {
        return source == Source.FILE ? Path.of(value) : null;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SslMaterial material)) {
            return false;
        }
        return source == material.source && value.equals(material.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, value);
    }

    @Override
    public String toString() {
        return "SslMaterial[source=" + source + ", configured=true]";
    }

    private static String requireValue(String value) {
        String nonNullValue = Objects.requireNonNull(value, "value");
        if (nonNullValue.isBlank()) {
            throw new IllegalArgumentException("SSL material value must not be blank");
        }
        if (nonNullValue.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("SSL material contains a null character");
        }
        return nonNullValue;
    }
}
