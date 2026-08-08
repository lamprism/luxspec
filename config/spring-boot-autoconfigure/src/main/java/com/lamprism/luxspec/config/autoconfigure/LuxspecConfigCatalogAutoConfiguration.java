package com.lamprism.luxspec.config.autoconfigure;

import com.lamprism.luxspec.config.catalog.ConfigCatalog;
import com.lamprism.luxspec.config.catalog.ConfigSpecContributor;
import com.lamprism.luxspec.config.catalog.InMemoryConfigCatalog;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Assembles a read-only configuration catalog from application contributors.
 *
 * @author RollW
 */
@AutoConfiguration
@ConditionalOnBean(ConfigSpecContributor.class)
public class LuxspecConfigCatalogAutoConfiguration {
    /**
     * Creates the default catalog when the application has not supplied one.
     *
     * @param contributors the ordered application configuration contributors
     * @return the assembled configuration catalog
     */
    @Bean
    @ConditionalOnMissingBean(ConfigCatalog.class)
    public ConfigCatalog configCatalog(ObjectProvider<ConfigSpecContributor> contributors) {
        InMemoryConfigCatalog catalog = new InMemoryConfigCatalog();
        contributors.orderedStream().forEach(contributor -> contributor.contribute(catalog));
        return catalog;
    }
}
