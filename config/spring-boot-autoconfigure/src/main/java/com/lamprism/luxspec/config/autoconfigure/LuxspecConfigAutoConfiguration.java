package com.lamprism.luxspec.config.autoconfigure;

import com.lamprism.luxspec.config.ConfigCacheInvalidator;
import com.lamprism.luxspec.config.ConfigLayer;
import com.lamprism.luxspec.config.ConfigReader;
import com.lamprism.luxspec.config.ConfigSource;
import com.lamprism.luxspec.config.ConfigWriter;
import com.lamprism.luxspec.config.LayeredConfigReader;
import com.lamprism.luxspec.config.SourceConfigWriter;
import com.lamprism.luxspec.event.EventPublisher;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Assembles configuration roles from application-defined source layers.
 *
 * @author RollW
 */
@AutoConfiguration
@ConditionalOnBean(ConfigLayer.class)
public class LuxspecConfigAutoConfiguration {
    /**
     * Creates the effective reader from explicitly ordered layers.
     *
     * @param layers the application-defined source layers
     * @return the layered configuration reader
     */
    @Bean
    @ConditionalOnMissingBean(ConfigReader.class)
    public ConfigReader configReader(List<ConfigLayer> layers) {
        return new LayeredConfigReader(layers);
    }

    /**
     * Creates a writer over the unique sources participating in the configured layers.
     *
     * @param layers the application-defined source layers
     * @param reader the effective reader used to detect effective changes
     * @param eventPublisherProvider the optional provider-independent event publisher
     * @return a writer without an implicit default target
     */
    @Bean
    @ConditionalOnMissingBean(ConfigWriter.class)
    public ConfigWriter configWriter(
            List<ConfigLayer> layers,
            ConfigReader reader,
            ObjectProvider<EventPublisher> eventPublisherProvider
    ) {
        EventPublisher eventPublisher = eventPublisherProvider.getIfAvailable(() -> event -> { });
        ConfigCacheInvalidator invalidator = cacheInvalidator(reader);
        return new SourceConfigWriter(sources(layers), null, eventPublisher, reader, invalidator);
    }

    private static List<ConfigSource> sources(List<ConfigLayer> layers) {
        LinkedHashSet<ConfigSource> sources = new LinkedHashSet<>();
        for (ConfigLayer layer : layers) {
            sources.add(layer.getSource());
        }
        return new ArrayList<>(sources);
    }

    private static ConfigCacheInvalidator cacheInvalidator(ConfigReader reader) {
        if (reader instanceof ConfigCacheInvalidator invalidator) {
            return invalidator;
        }
        return key -> { };
    }
}
