package com.lamprism.luxspec.config.autoconfigure;

import com.lamprism.luxspec.cache.Cache;
import com.lamprism.luxspec.cache.CacheInvalidator;
import com.lamprism.luxspec.config.ConfigProvider;
import com.lamprism.luxspec.config.ConfigReader;
import com.lamprism.luxspec.config.ConfigValue;
import com.lamprism.luxspec.config.ConfigWriter;
import com.lamprism.luxspec.config.cache.ConfigCacheKey;
import com.lamprism.luxspec.config.provider.CachingConfigProvider;
import com.lamprism.luxspec.config.runtime.LayeredConfigReader;
import com.lamprism.luxspec.config.runtime.SourceConfigWriter;
import com.lamprism.luxspec.config.source.ConfigSource;
import com.lamprism.luxspec.event.EventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * Assembles configuration roles from application-defined source layers.
 *
 * @author RollW
 */
@AutoConfiguration
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
    @Primary
    @ConditionalOnMissingBean(ConfigReader.class)
    public LayeredConfigReader configReader(List<ConfigSource> sources) {
        return new LayeredConfigReader(sources);
    }

    /**
     * Creates a writer over the unique sources participating in the configured layers.
     *
     * @param sources                the application-defined sources
     * @param reader                 the effective reader used to detect effective changes
     * @param eventPublisherProvider the optional provider-independent event publisher
     * @return a writer without an implicit default target
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(ConfigWriter.class)
    public SourceConfigWriter configWriter(
            List<ConfigSource> sources,
            ConfigReader reader,
            ObjectProvider<EventPublisher> eventPublisherProvider
    ) {
        EventPublisher eventPublisher = eventPublisherProvider.getIfAvailable(() -> event -> {
        });
        CacheInvalidator<ConfigCacheKey> invalidator = cacheInvalidator(reader);
        return new SourceConfigWriter(sources, null, eventPublisher, reader, invalidator);
    }

    /**
     * Creates the assembled configuration provider and decorates it with the available cache.
     *
     * @param reader the configured effective reader
     * @param writer the configured mutation writer
     * @param caches the optional generic cache
     * @return the assembled configuration provider
     */
    @Bean
    @ConditionalOnBean({LayeredConfigReader.class, SourceConfigWriter.class})
    @ConditionalOnMissingBean(ConfigProvider.class)
    public ConfigProvider configProvider(
            @Qualifier("configReader") LayeredConfigReader reader,
            @Qualifier("configWriter") SourceConfigWriter writer,
            ObjectProvider<Cache<?, ?>> caches
    ) {
        ConfigProvider delegate = ConfigProvider.of(reader, writer);
        Cache<?, ?> cache = caches.getIfAvailable();
        if (cache == null) {
            return delegate;
        }
        return new CachingConfigProvider(delegate, castCache(cache));
    }

    private static CacheInvalidator<ConfigCacheKey> cacheInvalidator(ConfigReader reader) {
        if (reader instanceof CachingConfigProvider cachingProvider) {
            return cachingProvider;
        }
        return CacheInvalidator.noOp();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Cache<ConfigCacheKey, ConfigValue<?>> castCache(Cache<?, ?> cache) {
        return (Cache) cache;
    }
}
