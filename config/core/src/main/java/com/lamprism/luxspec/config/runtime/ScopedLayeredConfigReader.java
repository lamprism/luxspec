package com.lamprism.luxspec.config.runtime;

import com.lamprism.luxspec.config.ConfigBinding;
import com.lamprism.luxspec.config.ConfigReader;
import com.lamprism.luxspec.config.ConfigValue;
import com.lamprism.luxspec.config.source.ConfigSource;
import com.lamprism.luxspec.config.source.ConfigSourceScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Layered reader that accepts only sources from one lifecycle scope.
 *
 * @author RollW
 */
public class ScopedLayeredConfigReader implements ConfigReader {
    private final ConfigSourceScope scope;
    private final LayeredConfigReader delegate;

    /**
     * Creates a scope-constrained reader from sources ordered by precedence.
     *
     * @param scope   the required source scope
     * @param sources the ordered source instances
     */
    public ScopedLayeredConfigReader(
            ConfigSourceScope scope,
            List<? extends ConfigSource> sources
    ) {
        this.scope = Objects.requireNonNull(scope, "scope");
        this.delegate = new LayeredConfigReader(validateSources(scope, sources));
    }

    /**
     * Creates a bootstrap reader from sources already ordered by the assembly boundary.
     *
     * @param sources bootstrap-safe sources in descending precedence order
     * @return a scope-constrained bootstrap reader
     */
    public static ScopedLayeredConfigReader bootstrap(
            List<? extends ConfigSource> sources
    ) {
        return new ScopedLayeredConfigReader(
                ConfigSourceScope.BOOTSTRAP,
                sources
        );
    }

    @Override
    public ConfigSourceScope getSourceScope() {
        return scope;
    }

    @Override
    public <T> ConfigValue<T> get(ConfigBinding<T> binding) {
        return delegate.get(Objects.requireNonNull(binding, "binding"));
    }

    private static List<ConfigSource> validateSources(
            ConfigSourceScope scope,
            List<? extends ConfigSource> sources
    ) {
        List<ConfigSource> values = new ArrayList<>();
        for (ConfigSource source : Objects.requireNonNull(sources, "sources")) {
            ConfigSource nonNullSource = Objects.requireNonNull(source, "source");
            if (scope != nonNullSource.getScope()) {
                throw new IllegalArgumentException("Configuration source scope does not match reader scope");
            }
            values.add(nonNullSource);
        }
        return List.copyOf(values);
    }

}
