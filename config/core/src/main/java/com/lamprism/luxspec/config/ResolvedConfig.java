package com.lamprism.luxspec.config;

import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Reports a resolved value and whether it originated from a source or default.
 *
 * @param <T> the typed value
 * @author RollW
 */
public final class ResolvedConfig<T> {
    /**
     * Identifies how a configuration resolution completed.
     */
    public enum Origin {
        SOURCE,
        DEFAULT,
        MASKED,
        ABSENT
    }

    private final Optional<T> value;
    private final Origin origin;
    private final @Nullable ConfigSourceId sourceId;

    private ResolvedConfig(Optional<T> value, Origin origin, @Nullable ConfigSourceId sourceId) {
        this.value = value;
        this.origin = origin;
        this.sourceId = sourceId;
    }

    /**
     * Creates a resolution supplied by one source.
     *
     * @param value the decoded source value
     * @param sourceId the supplying source
     * @param <T> the decoded value type
     * @return the source resolution
     */
    public static <T> ResolvedConfig<T> source(T value, ConfigSourceId sourceId) {
        return new ResolvedConfig<>(Optional.of(Objects.requireNonNull(value, "value")), Origin.SOURCE, Objects.requireNonNull(sourceId, "sourceId"));
    }

    /**
     * Creates a resolution supplied by a definition default.
     *
     * @param value the default value
     * @param <T> the decoded value type
     * @return the default resolution
     */
    public static <T> ResolvedConfig<T> defaultValue(T value) {
        return new ResolvedConfig<>(Optional.of(Objects.requireNonNull(value, "value")), Origin.DEFAULT, null);
    }

    /**
     * Creates a resolution blocked by an explicit mask.
     *
     * @param <T> the decoded value type
     * @return the masked resolution
     */
    public static <T> ResolvedConfig<T> masked() {
        return new ResolvedConfig<>(Optional.empty(), Origin.MASKED, null);
    }

    /**
     * Creates a resolution without a value or default.
     *
     * @param <T> the decoded value type
     * @return the absent resolution
     */
    public static <T> ResolvedConfig<T> absent() {
        return new ResolvedConfig<>(Optional.empty(), Origin.ABSENT, null);
    }

    /**
     * Returns the optional effective value.
     *
     * @return the optional value
     */
    public Optional<T> getValue() {
        return value;
    }
    /**
     * Returns how resolution completed.
     *
     * @return the resolution origin
     */
    public Origin getOrigin() {
        return origin;
    }
    /**
     * Returns the supplying source when the origin is SOURCE.
     *
     * @return the optional source identifier
     */
    public Optional<ConfigSourceId> getSourceId() {
        return Optional.ofNullable(sourceId);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ResolvedConfig<?> resolved)) {
            return false;
        }
        return value.equals(resolved.value) && origin == resolved.origin && Objects.equals(sourceId, resolved.sourceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, origin, sourceId);
    }
}
