package com.lamprism.luxspec.config.policy;

import com.lamprism.luxspec.config.ConfigBinding;
import com.lamprism.luxspec.config.source.ConfigEntry;
import com.lamprism.luxspec.config.source.ConfigSource;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable inputs available to a ConfigPolicy evaluation.
 *
 * <p>Policies receive metadata only. They must not read or mutate a Source and must not perform
 * side effects.</p>
 *
 * @author RollW
 */
public final class ConfigPolicyContext {
    private final ConfigPolicyOperation operation;
    private final ConfigPolicyPhase phase;
    private final ConfigBinding<?> binding;
    private final @Nullable ConfigSourceId sourceId;
    private final @Nullable Map<String, String> sourceAttributes;
    private final ConfigEntry.@Nullable State sourceState;

    private ConfigPolicyContext(
            ConfigPolicyOperation operation,
            ConfigPolicyPhase phase,
            ConfigBinding<?> binding,
            @Nullable ConfigSourceId sourceId,
            @Nullable Map<String, String> sourceAttributes,
            ConfigEntry.@Nullable State sourceState
    ) {
        this.operation = Objects.requireNonNull(operation, "operation");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.binding = Objects.requireNonNull(binding, "binding");
        this.sourceId = sourceId;
        this.sourceAttributes = sourceAttributes;
        this.sourceState = sourceState;
        boolean hasSource = sourceId != null;
        if (hasSource != (sourceAttributes != null)) {
            throw new IllegalArgumentException("Source metadata must be complete");
        }
        if (phase == ConfigPolicyPhase.AFTER_SOURCE_READ && sourceState == null) {
            throw new IllegalArgumentException("After-source-read contexts require a Source entry state");
        }
        if (phase != ConfigPolicyPhase.AFTER_SOURCE_READ && sourceState != null) {
            throw new IllegalArgumentException("Only after-source-read contexts may contain a Source state");
        }
        if (phase == ConfigPolicyPhase.OPERATION && operation == ConfigPolicyOperation.RESOLVE_SOURCE) {
            throw new IllegalArgumentException("Source resolution requires an explicit Source read phase");
        }
        if (phase != ConfigPolicyPhase.OPERATION && operation != ConfigPolicyOperation.RESOLVE_SOURCE) {
            throw new IllegalArgumentException("Only Source resolution uses Source read phases");
        }
    }

    /**
     * Creates context for a definition-level operation.
     *
     * @param operation the operation
     * @param binding   the affected binding
     * @return the immutable context
     */
    public static ConfigPolicyContext forBinding(
            ConfigPolicyOperation operation,
            ConfigBinding<?> binding
    ) {
        return new ConfigPolicyContext(
                operation,
                ConfigPolicyPhase.OPERATION,
                binding,
                null,
                null,
                null
        );
    }

    /**
     * Creates context for a Source candidate before reading its entry.
     *
     * @param operation the operation
     * @param binding   the affected binding
     * @param source    the candidate Source
     * @return the immutable context
     */
    public static ConfigPolicyContext forSource(
            ConfigPolicyOperation operation,
            ConfigBinding<?> binding,
            ConfigSource source
    ) {
        return fromSource(
                operation,
                ConfigPolicyPhase.BEFORE_SOURCE_READ,
                binding,
                source,
                null
        );
    }

    /**
     * Creates context for a Source mutation operation.
     *
     * @param operation the mutation operation
     * @param binding   the affected binding
     * @param source    the target Source
     * @return the immutable mutation context
     */
    public static ConfigPolicyContext forSourceOperation(
            ConfigPolicyOperation operation,
            ConfigBinding<?> binding,
            ConfigSource source
    ) {
        return fromSource(
                operation,
                ConfigPolicyPhase.OPERATION,
                binding,
                source,
                null
        );
    }

    /**
     * Creates context for a Source candidate after reading its raw state.
     *
     * @param operation   the operation
     * @param binding     the affected binding
     * @param source      the Source
     * @param sourceState the raw entry state
     * @return the immutable context
     */
    public static ConfigPolicyContext forSourceState(
            ConfigPolicyOperation operation,
            ConfigBinding<?> binding,
            ConfigSource source,
            ConfigEntry.State sourceState
    ) {
        return fromSource(
                operation,
                ConfigPolicyPhase.AFTER_SOURCE_READ,
                binding,
                source,
                Objects.requireNonNull(sourceState, "sourceState")
        );
    }

    /**
     * Returns the policy operation.
     *
     * @return the operation
     */
    public ConfigPolicyOperation getOperation() {
        return operation;
    }

    /**
     * Returns the pipeline phase represented by this context.
     *
     * @return the policy phase
     */
    public ConfigPolicyPhase getPhase() {
        return phase;
    }

    /**
     * Returns the concrete binding.
     *
     * @return the binding
     */
    public ConfigBinding<?> getBinding() {
        return binding;
    }

    /**
     * Returns the candidate Source ID.
     *
     * @return the Source ID, or {@code null} for definition-level operations
     */
    public @Nullable ConfigSourceId getSourceId() {
        return sourceId;
    }

    /**
     * Returns an immutable snapshot of Source attributes.
     *
     * @return Source attributes, or {@code null} for definition-level operations
     */
    public @Nullable Map<String, String> getSourceAttributes() {
        return sourceAttributes;
    }

    /**
     * Returns the observed raw Source state.
     *
     * @return the state, or {@code null} outside the after-source-read phase
     */
    public ConfigEntry.@Nullable State getSourceState() {
        return sourceState;
    }

    private static ConfigPolicyContext fromSource(
            ConfigPolicyOperation operation,
            ConfigPolicyPhase phase,
            ConfigBinding<?> binding,
            ConfigSource source,
            ConfigEntry.@Nullable State sourceState
    ) {
        ConfigSource nonNullSource = Objects.requireNonNull(source, "source");
        return new ConfigPolicyContext(
                operation,
                phase,
                binding,
                Objects.requireNonNull(nonNullSource.getId(), "source ID"),
                Map.copyOf(Objects.requireNonNull(nonNullSource.getAttributes(), "source attributes")),
                sourceState
        );
    }
}
