package com.lamprism.luxspec.database.autoconfigure;

import com.lamprism.luxspec.config.catalog.ConfigSpecContributor;
import com.lamprism.luxspec.config.runtime.ScopedLayeredConfigReader;
import com.lamprism.luxspec.config.source.ConfigSource;
import com.lamprism.luxspec.database.DatabaseConfig;
import com.lamprism.luxspec.database.DatabaseConfigBinder;
import com.lamprism.luxspec.database.DatabaseConfigSpec;
import com.lamprism.luxspec.database.hikari.HikariDataSourceFactory;
import com.lamprism.luxspec.database.jdbc.DataSourceFactory;
import com.lamprism.luxspec.database.jdbc.DatabaseUrlResolver;
import com.lamprism.luxspec.database.jdbc.DefaultSslMaterializer;
import com.lamprism.luxspec.database.jdbc.SslMaterializer;
import com.lamprism.luxspec.database.jdbc.StandardDatabaseUrlResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

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
            @Qualifier("bootstrap") ObjectProvider<ConfigSource> sources
    ) {
        return new DatabaseConfigBinder().bind(
                ScopedLayeredConfigReader.bootstrap(sources.orderedStream().toList())
        );
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
