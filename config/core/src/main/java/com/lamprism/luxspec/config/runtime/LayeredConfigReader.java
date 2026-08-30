package com.lamprism.luxspec.config.runtime;

import com.lamprism.luxspec.config.ConfigBinding;
import com.lamprism.luxspec.config.ConfigReader;
import com.lamprism.luxspec.config.ConfigSpec;
import com.lamprism.luxspec.config.ConfigValue;
import com.lamprism.luxspec.config.policy.ConfigPolicy;
import com.lamprism.luxspec.config.policy.ConfigPolicyContext;
import com.lamprism.luxspec.config.policy.ConfigPolicyDecision;
import com.lamprism.luxspec.config.policy.ConfigPolicyException;
import com.lamprism.luxspec.config.policy.ConfigPolicyOperation;
import com.lamprism.luxspec.config.resolution.ConfigResolutionException;
import com.lamprism.luxspec.config.resolution.ConfigValueOrigin;
import com.lamprism.luxspec.config.resolution.LayeredConfigValue;
import com.lamprism.luxspec.config.source.ConfigEntry;
import com.lamprism.luxspec.config.source.ConfigSource;
import com.lamprism.luxspec.config.source.ConfigSourceId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Reads eligible Source observations in configured precedence order and returns them as layers.
 *
 * <p>This is a layer-observing Reader. It evaluates every eligible Source until a policy returns
 * {@link ConfigPolicyDecision#STOP} or a Source returns a tombstone. Therefore an invalid entry in
 * any participating layer, including a lower layer, fails the read instead of being hidden by a
 * higher present value. A short-circuit Reader may implement the same {@link ConfigReader} contract
 * separately when layer observations are not required.</p>
 *
 * @author RollW
 */
public class LayeredConfigReader implements ConfigReader {
    private final List<ConfigSource> sources;

    /**
     * Creates a reader from sources ordered from highest to lowest priority.
     *
     * @param sources the ordered source instances
     */
    public LayeredConfigReader(List<? extends ConfigSource> sources) {
        List<ConfigSource> values = new ArrayList<>();
        Set<ConfigSourceId> sourceIds = new HashSet<>();
        for (ConfigSource source : Objects.requireNonNull(sources, "sources")) {
            ConfigSource nonNullSource = Objects.requireNonNull(source, "source");
            ConfigSourceId sourceId = Objects.requireNonNull(nonNullSource.getId(), "source ID");
            if (!sourceIds.add(sourceId)) {
                throw new IllegalArgumentException("Duplicate configuration source ID");
            }
            values.add(nonNullSource);
        }
        this.sources = List.copyOf(values);
    }

    @Override
    public <T> LayeredConfigValue<T> get(ConfigBinding<T> binding) {
        ConfigBinding<T> nonNullBinding = Objects.requireNonNull(binding, "binding");
        ConfigSpec<T> spec = nonNullBinding.getSpec();
        List<ConfigValue<T>> layers = new ArrayList<>();
        for (ConfigSource source : sources) {
            ConfigPolicyDecision beforeRead = evaluate(
                    spec.getPolicy(),
                    ConfigPolicyContext.forSource(
                            ConfigPolicyOperation.RESOLVE_SOURCE,
                            nonNullBinding,
                            source
                    )
            );
            if (beforeRead == ConfigPolicyDecision.SKIP) {
                continue;
            }
            if (beforeRead == ConfigPolicyDecision.STOP) {
                return withoutFallback(layers);
            }
            requireAllowed(nonNullBinding, ConfigPolicyOperation.RESOLVE_SOURCE, beforeRead);
            ConfigEntry entry = Objects.requireNonNull(source.get(nonNullBinding.getKey()), "source entry");
            ConfigPolicyDecision afterRead = evaluate(
                    spec.getPolicy(),
                    ConfigPolicyContext.forSourceState(
                            ConfigPolicyOperation.RESOLVE_SOURCE,
                            nonNullBinding,
                            source,
                            entry.getState()
                    )
            );
            if (afterRead == ConfigPolicyDecision.SKIP) {
                continue;
            }
            if (afterRead == ConfigPolicyDecision.STOP) {
                return withoutFallback(layers);
            }
            requireAllowed(nonNullBinding, ConfigPolicyOperation.RESOLVE_SOURCE, afterRead);
            ConfigValue<T> value = toValue(nonNullBinding, source, entry);
            layers.add(value);
            if (value.getOrigin() instanceof ConfigValueOrigin.TombstoneOrigin) {
                return LayeredConfigValue.of(layers);
            }
        }
        addFallback(layers, nonNullBinding, spec);
        return LayeredConfigValue.of(layers);
    }

    @Override
    public <T> LayeredConfigValue<T> get(ConfigSpec<T> spec) {
        return get(Objects.requireNonNull(spec, "spec").bind());
    }

    @Override
    public <T> LayeredConfigValue<T> get(ConfigSpec<T> spec, Map<String, String> arguments) {
        return get(Objects.requireNonNull(spec, "spec").bind(arguments));
    }

    private static <T> ConfigValue<T> toValue(
            ConfigBinding<T> binding,
            ConfigSource source,
            ConfigEntry entry
    ) {
        ConfigSourceId sourceId = Objects.requireNonNull(source.getId(), "source ID");
        return switch (entry.getState()) {
            case ABSENT -> ConfigValue.absent(sourceId);
            case TOMBSTONE -> ConfigValue.absent(new ConfigValueOrigin.TombstoneOrigin(sourceId));
            case INVALID -> throw new ConfigResolutionException();
            case PRESENT -> decode(binding, source, entry);
        };
    }

    private static <T> ConfigValue<T> decode(ConfigBinding<T> binding, ConfigSource source, ConfigEntry entry) {
        try {
            T value = Objects.requireNonNull(
                    binding.getSpec().getCodec().decode(entry.requireRawValue()),
                    "codec value"
            );
            binding.getSpec().validate(value);
            return ConfigValue.source(value, source.getId());
        } catch (RuntimeException ignored) {
            // Codec and validator failures may contain a raw or decoded configuration value.
            throw new ConfigResolutionException();
        }
    }

    private static <T> void addFallback(
            List<ConfigValue<T>> layers,
            ConfigBinding<T> binding,
            ConfigSpec<T> spec
    ) {
        ConfigPolicyDecision decision = evaluate(
                spec.getPolicy(),
                ConfigPolicyContext.forBinding(ConfigPolicyOperation.RESOLVE_FALLBACK, binding)
        );
        if (decision == ConfigPolicyDecision.STOP) {
            layers.add(ConfigValue.absent());
            return;
        }
        if (decision == ConfigPolicyDecision.SKIP) {
            throw new ConfigPolicyException(binding.getKey(), ConfigPolicyOperation.RESOLVE_FALLBACK, decision);
        }
        requireAllowed(binding, ConfigPolicyOperation.RESOLVE_FALLBACK, decision);
        T defaultValue = spec.getDefaultValue();
        if (defaultValue == null) {
            layers.add(ConfigValue.absent());
            return;
        }
        layers.add(ConfigValue.defaultValue(defaultValue));
    }

    private static <T> LayeredConfigValue<T> withoutFallback(List<ConfigValue<T>> layers) {
        layers.add(ConfigValue.absent());
        return LayeredConfigValue.of(layers);
    }

    private static ConfigPolicyDecision evaluate(
            ConfigPolicy policy,
            ConfigPolicyContext context
    ) {
        return Objects.requireNonNull(policy, "policy")
                .evaluate(Objects.requireNonNull(context, "context"));
    }

    private static void requireAllowed(
            ConfigBinding<?> binding,
            ConfigPolicyOperation operation,
            ConfigPolicyDecision decision
    ) {
        if (decision != ConfigPolicyDecision.ALLOW) {
            throw new ConfigPolicyException(binding.getKey(), operation, decision);
        }
    }

}
