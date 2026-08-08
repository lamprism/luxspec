package com.lamprism.luxspec.config.catalog;

/**
 * Adds configuration definitions to an application catalog during assembly.
 *
 * @author RollW
 */
@FunctionalInterface
public interface ConfigSpecContributor {
    /**
     * Registers the definitions owned by this contributor.
     *
     * @param registry the application configuration registry
     */
    void contribute(ConfigCatalogRegistry registry);
}
