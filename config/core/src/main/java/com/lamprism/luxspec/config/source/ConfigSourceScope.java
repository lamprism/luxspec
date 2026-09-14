package com.lamprism.luxspec.config.source;

/**
 * Identifies the lifecycle boundary at which a configuration source is available.
 *
 * <p>A bootstrap source is available before runtime infrastructure is created. A runtime source
 * may depend on that infrastructure and must not be used to resolve bootstrap settings.</p>
 *
 * @author RollW
 */
public enum ConfigSourceScope {
    /**
     * Available before runtime infrastructure is created.
     */
    BOOTSTRAP,
    /**
     * Available after runtime infrastructure has been created.
     */
    RUNTIME
}
