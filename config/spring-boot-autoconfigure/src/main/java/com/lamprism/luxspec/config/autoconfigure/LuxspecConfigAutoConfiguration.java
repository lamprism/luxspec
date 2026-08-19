package com.lamprism.luxspec.config.autoconfigure;

import com.lamprism.luxspec.cache.CacheFactory;
import com.lamprism.luxspec.cache.CacheInvalidator;
import com.lamprism.luxspec.cache.CacheProfile;
import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.ConfigProvider;
import com.lamprism.luxspec.config.ConfigReader;
import com.lamprism.luxspec.config.ConfigWriter;
import com.lamprism.luxspec.config.cache.ConfigValueCache;
import com.lamprism.luxspec.config.provider.CachingConfigProvider;
import com.lamprism.luxspec.config.runtime.LayeredConfigReader;
import com.lamprism.luxspec.config.runtime.SourceConfigWriter;
import com.lamprism.luxspec.config.source.ConfigSource;
import com.lamprism.luxspec.event.EventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Fallback;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * Assembles configuration roles from application-defined source layers.
 *
 * @author RollW
 */
@AutoConfiguration(afterName = "com.lamprism.luxspec.cache.autoconfigure.LuxspecCacheAutoConfiguration")
@ConditionalOnBean(ConfigSource.class)
public class LuxspecConfigAutoConfiguration {
    /**
     * Creates the configuration auto-configuration component.
     */
    public LuxspecConfigAutoConfiguration() {
    }

    /**
     * Creates the effective reader from explicitly ordered layers.
     *
     * @param sources the application-defined sources ordered by Spring
     * @return the layered configuration reader
     */
    @Bean
    @Lazy
    @Primary
    @Fallback
    @ConditionalOnMissingBean(ConfigReader.class)
    public LayeredConfigReader configReader(List<ConfigSource> sources) {
        return new LayeredConfigReader(sources);
    }

    /**
     * Creates the configuration-owned value cache through the application cache factory.
     *
     * @param factory  the application-provided cache factory
     * @param profiles the optional generic cache profile
     * @return the configuration value cache
     */
    @Bean
    @ConditionalOnBean(CacheFactory.class)
    @ConditionalOnMissingBean(ConfigValueCache.class)
    public ConfigValueCache configValueCache(
            CacheFactory factory,
            ObjectProvider<CacheProfile> profiles
    ) {
        CacheProfile profile = profiles.getIfAvailable();
        if (profile == null) {
            profile = CacheProfile.defaults();
        }
        return new ConfigValueCache(factory, profile);
    }

    /**
     * Creates a writer over the unique sources participating in the configured layers.
     *
     * @param sources                the application-defined sources
     * @param reader                 the effective reader used to detect effective changes
     * @param eventPublisherProvider the optional provider-independent event publisher
     * @param configValueCaches      the optional shared configuration value cache
     * @return a writer without an implicit default target
     */
    @Bean
    @Lazy
    @Primary
    @Fallback
    @ConditionalOnMissingBean(ConfigWriter.class)
    public SourceConfigWriter configWriter(
            List<ConfigSource> sources,
            ConfigReader reader,
            ObjectProvider<EventPublisher> eventPublisherProvider,
            ObjectProvider<ConfigValueCache> configValueCaches
    ) {
        EventPublisher eventPublisher = eventPublisherProvider.getIfAvailable(() -> event -> {
        });
        ConfigValueCache cache = configValueCaches.getIfAvailable();
        CacheInvalidator<ConfigKey> invalidator = cache == null ? CacheInvalidator.noOp() : cache;
        return new SourceConfigWriter(sources, null, eventPublisher, reader, invalidator);
    }

    /**
     * Creates the assembled configuration provider and decorates it with the available cache.
     *
     * @param reader the configured effective reader
     * @param writer the configured mutation writer
     * @param caches the optional shared configuration value cache
     * @return the assembled configuration provider
     */
    @Bean
    @Lazy
    @Fallback
    @ConditionalOnBean({ConfigReader.class, ConfigWriter.class})
    @ConditionalOnMissingBean(ConfigProvider.class)
    public ConfigProvider configProvider(
            ConfigReader reader,
            ConfigWriter writer,
            ObjectProvider<ConfigValueCache> caches
    ) {
        ConfigProvider delegate = ConfigProvider.of(reader, writer);
        ConfigValueCache cache = caches.getIfAvailable();
        if (cache == null) {
            return delegate;
        }
        return new CachingConfigProvider(delegate, cache);
    }

}
