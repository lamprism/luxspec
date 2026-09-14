package com.lamprism.luxspec.database.autoconfigure;

import com.lamprism.luxspec.config.catalog.ConfigSpecContributor;
import com.lamprism.luxspec.config.runtime.ScopedLayeredConfigReader;
import com.lamprism.luxspec.config.source.ConfigSource;
import com.lamprism.luxspec.config.source.ConfigSourceScope;
import com.lamprism.luxspec.database.DatabaseConfig;
import com.lamprism.luxspec.database.DatabaseConfigBinder;
import com.lamprism.luxspec.database.DatabaseConfigSpec;
import com.lamprism.luxspec.database.hikari.HikariDataSourceFactory;
import com.lamprism.luxspec.database.jdbc.DataSourceFactory;
import com.lamprism.luxspec.database.jdbc.DatabaseUrlResolver;
import com.lamprism.luxspec.database.jdbc.DefaultSslMaterializer;
import com.lamprism.luxspec.database.jdbc.SslMaterializer;
import com.lamprism.luxspec.database.jdbc.StandardDatabaseUrlResolver;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates the default database assembly only when an application supplies database settings.
 */
@AutoConfiguration(beforeName = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
@ConditionalOnClass({DataSource.class, HikariDataSourceFactory.class})
public class LuxspecDatabaseAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(name = "databaseConfigSpecContributor")
    public ConfigSpecContributor databaseConfigSpecContributor() {
        return registry -> registry.registerAll(DatabaseConfigSpec.all());
    }

    @Bean
    @ConditionalOnMissingBean(DatabaseConfig.class)
    @ConditionalOnBean(ConfigSource.class)
    @ConditionalOnProperty(prefix = "luxspec.database", name = "enabled", havingValue = "true")
    public DatabaseConfig databaseConfig(
            ConfigurableListableBeanFactory beanFactory
    ) {
        return new DatabaseConfigBinder().bind(
                ScopedLayeredConfigReader.bootstrap(bootstrapSources(beanFactory))
        );
    }

    private static List<ConfigSource> bootstrapSources(
            ConfigurableListableBeanFactory beanFactory
    ) {
        List<ConfigSource> sources = new ArrayList<>();
        for (String beanName : beanFactory.getBeanNamesForType(ConfigSource.class, true, false)) {
            // A lazy source may depend on the database being assembled. Do not initialize it while
            // discovering the eager bootstrap sources.
            if (beanFactory.getBeanDefinition(beanName).isLazyInit()) {
                continue;
            }
            ConfigSource source = beanFactory.getBean(beanName, ConfigSource.class);
            if (source.getScope() == ConfigSourceScope.BOOTSTRAP) {
                sources.add(source);
            }
        }
        AnnotationAwareOrderComparator.sort(sources);
        return List.copyOf(sources);
    }

    @Bean
    @ConditionalOnBean(DatabaseConfig.class)
    @ConditionalOnMissingBean(SslMaterializer.class)
    public SslMaterializer sslMaterializer() {
        return new DefaultSslMaterializer();
    }

    @Bean
    @ConditionalOnBean(DatabaseConfig.class)
    @ConditionalOnMissingBean(DatabaseUrlResolver.class)
    public DatabaseUrlResolver databaseUrlResolver(SslMaterializer sslMaterializer) {
        return new StandardDatabaseUrlResolver(sslMaterializer);
    }

    @Bean
    @ConditionalOnBean(DatabaseConfig.class)
    @ConditionalOnMissingBean({DataSource.class, DataSourceFactory.class})
    public DataSourceFactory dataSourceFactory(DatabaseUrlResolver urlResolver) {
        return new HikariDataSourceFactory(urlResolver);
    }

    @Bean
    @ConditionalOnBean(DatabaseConfig.class)
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource dataSource(DatabaseConfig databaseConfig, DataSourceFactory dataSourceFactory) {
        return dataSourceFactory.create(databaseConfig);
    }
}
