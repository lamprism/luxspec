package com.lamprism.luxspec.config.catalog;

import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.ConfigSpec;

import java.util.Objects;

/**
 * Discovers the complete keys that can be browsed for a configuration definition.
 *
 * <p>A browser does not create or retain {@code ConfigBinding} instances. Callers can create a
 * binding from a returned key through {@link ConfigSpec#match(ConfigKey)} when they need to read
 * or write a configuration value.
 *
 * @author RollW
 */
@FunctionalInterface
public interface ConfigKeyBrowser {

    /**
     * Combines browsers as additive key sources in iteration order.
     *
     * @param browsers the browsers to compose
     * @return a browser that unions the complete keys returned by all applicable browsers
     */
    static ConfigKeyBrowser compose(Iterable<? extends ConfigKeyBrowser> browsers) {
        return union(browsers);
    }

    /**
     * Combines browsers as additive key sources in iteration order.
     *
     * @param browsers the browsers to combine
     * @return a browser that unions the complete keys returned by all applicable browsers
     */
    static ConfigKeyBrowser union(Iterable<? extends ConfigKeyBrowser> browsers) {
        return new CompositeConfigKeyBrowser(browsers);
    }

    /**
     * Combines browsers as ordered fallbacks.
     *
     * <p>The first applicable browser owns the complete result, including an empty result.</p>
     *
     * @param browsers the fallback browsers in priority order
     * @return a browser that uses the first applicable browser
     */
    static ConfigKeyBrowser fallback(Iterable<? extends ConfigKeyBrowser> browsers) {
        return new FallbackConfigKeyBrowser(browsers);
    }

    /**
     * Reports whether this browser applies to the specified definition.
     *
     * <p>The default is applicable to every definition so existing lambda implementations remain
     * valid. A browser intended for a subset of definitions should override this method.</p>
     *
     * @param spec the definition to inspect
     * @return {@code true} when this browser can provide the definition's keys
     */
    default boolean supports(ConfigSpec<?> spec) {
        Objects.requireNonNull(spec, "spec");
        return true;
    }

    /**
     * Returns the complete keys available for the specified configuration definition.
     *
     * @param spec the definition whose concrete keys are being browsed
     * @return the finite browsable complete keys, never {@code null}; every key must match the spec
     */
    Iterable<ConfigKey> browse(ConfigSpec<?> spec);
}
