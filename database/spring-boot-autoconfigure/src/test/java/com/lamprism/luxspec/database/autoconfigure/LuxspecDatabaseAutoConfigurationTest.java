package com.lamprism.luxspec.database.autoconfigure;

import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.source.ConfigEntry;
import com.lamprism.luxspec.config.source.ConfigSource;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.ConfigSourceScope;
import com.lamprism.luxspec.config.source.InMemoryConfigSource;
import com.lamprism.luxspec.database.DatabaseConfig;
import com.lamprism.luxspec.database.DatabaseConfigSpec;
import com.lamprism.luxspec.database.DatabaseTarget;
import com.lamprism.luxspec.database.DatabaseType;
import com.lamprism.luxspec.database.jdbc.DataSourceFactory;
import com.lamprism.luxspec.database.jdbc.SslMaterializer;
import com.zaxxer.hikari.HikariDataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.Ordered;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class LuxspecDatabaseAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LuxspecDatabaseAutoConfiguration.class));
    private final ApplicationContextRunner bootContextRunner = contextRunner
            .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class));

    @Test
    void createsTheDefaultDataSourceFromApplicationDatabaseConfig() {
        bootContextRunner
                .withBean(DatabaseConfig.class, LuxspecDatabaseAutoConfigurationTest::h2Config)
                .run(context -> {
                    assertThat(context).hasSingleBean(DatabaseConfig.class);
                    assertThat(context).hasSingleBean(SslMaterializer.class);
                    assertThat(context).hasSingleBean(DataSourceFactory.class);
                    assertThat(context).hasSingleBean(DataSource.class);
                    assertThat(context.getBean(DataSource.class)).isInstanceOf(HikariDataSource.class);
                });
    }

    @Test
    void readsDatabaseConfigFromConfigReaderWhenExplicitlyEnabled() {
        ConfigSource source = bootstrapSource(Map.of(
                DatabaseConfigSpec.TYPE.bind().getKey(), ConfigEntry.present("h2"),
                DatabaseConfigSpec.TARGET.bind().getKey(), ConfigEntry.present("memory"),
                DatabaseConfigSpec.NAME.bind().getKey(), ConfigEntry.present("config-reader-test"),
                DatabaseConfigSpec.OPTIONS.bind().getKey(), ConfigEntry.present(List.of())
        ));
        bootContextRunner
                .withPropertyValues("luxspec.database.enabled=true")
                .withBean("bootstrapSource", ConfigSource.class, () -> source)
                .run(context -> {
                    assertThat(context).hasSingleBean(DatabaseConfig.class);
                    assertThat(context).hasSingleBean(DataSource.class);
                });
    }

    @Test
    void backsOffWithoutDatabaseConfig() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(DataSource.class);
            assertThat(context).doesNotHaveBean(DataSourceFactory.class);
        });
    }

    @Test
    void assemblesTheBootstrapReaderFromApplicationSources() {
        ConfigSource source = bootstrapSource(Map.of(
                DatabaseConfigSpec.TYPE.bind().getKey(), ConfigEntry.present("h2"),
                DatabaseConfigSpec.TARGET.bind().getKey(), ConfigEntry.present("memory"),
                DatabaseConfigSpec.NAME.bind().getKey(), ConfigEntry.present("source-assembly-test"),
                DatabaseConfigSpec.OPTIONS.bind().getKey(), ConfigEntry.present(List.of())
        ));

        bootContextRunner
                .withPropertyValues("luxspec.database.enabled=true")
                .withBean("bootstrapSource", ConfigSource.class, () -> source)
                .run(context -> {
                    assertThat(context).hasSingleBean(DatabaseConfig.class);
                    assertThat(context.getBean(DatabaseConfig.class).getType()).isEqualTo(DatabaseType.H2);
                    assertThat(context).hasSingleBean(DataSource.class);
                });
    }

    @Test
    void excludesRuntimeSourcesFromBootstrapAssembly() {
        ConfigSource bootstrap = bootstrapSource(Map.of(
                DatabaseConfigSpec.TYPE.bind().getKey(), ConfigEntry.present("h2"),
                DatabaseConfigSpec.TARGET.bind().getKey(), ConfigEntry.present("memory"),
                DatabaseConfigSpec.NAME.bind().getKey(), ConfigEntry.present("bootstrap-test"),
                DatabaseConfigSpec.OPTIONS.bind().getKey(), ConfigEntry.present(List.of())
        ));
        ConfigSource runtime = InMemoryConfigSource.fromEntries(
                ConfigSourceId.of("runtime"),
                ConfigSourceScope.RUNTIME,
                Map.of(
                        DatabaseConfigSpec.TYPE.bind().getKey(), ConfigEntry.present("unsupported-runtime-type"),
                        DatabaseConfigSpec.TARGET.bind().getKey(), ConfigEntry.present("memory"),
                        DatabaseConfigSpec.NAME.bind().getKey(), ConfigEntry.present("runtime-test"),
                        DatabaseConfigSpec.OPTIONS.bind().getKey(), ConfigEntry.present(List.of())
                )
        );

        bootContextRunner
                .withPropertyValues("luxspec.database.enabled=true")
                .withBean("bootstrapSource", ConfigSource.class, () -> bootstrap)
                .withBean("runtimeSource", ConfigSource.class, () -> runtime)
                .run(context -> {
                    assertThat(context).hasSingleBean(DatabaseConfig.class);
                    assertThat(context.getBean(DatabaseConfig.class).getType()).isEqualTo(DatabaseType.H2);
                    assertThat(context).hasSingleBean(DataSource.class);
                });
    }

    @Test
    void backsOffWhenApplicationOwnsTheDataSource() {
        JdbcDataSource applicationDataSource = new JdbcDataSource();
        applicationDataSource.setURL("jdbc:h2:mem:application-owned");

        bootContextRunner
                .withBean(DatabaseConfig.class, LuxspecDatabaseAutoConfigurationTest::h2Config)
                .withBean(DataSource.class, () -> applicationDataSource)
                .run(context -> {
                    assertThat(context).hasSingleBean(DataSource.class);
                    assertThat(context.getBean(DataSource.class)).isSameAs(applicationDataSource);
                    assertThat(context).doesNotHaveBean(DataSourceFactory.class);
                });
    }

    @Test
    void doesNotCreateRuntimeSourcesDuringBootstrapAssembly() {
        AtomicBoolean runtimeSourceCreated = new AtomicBoolean();
        ConfigSource bootstrap = bootstrapSource(Map.of(
                DatabaseConfigSpec.TYPE.bind().getKey(), ConfigEntry.present("h2"),
                DatabaseConfigSpec.TARGET.bind().getKey(), ConfigEntry.present("memory"),
                DatabaseConfigSpec.NAME.bind().getKey(), ConfigEntry.present("bootstrap-isolation-test"),
                DatabaseConfigSpec.OPTIONS.bind().getKey(), ConfigEntry.present(List.of())
        ));

        bootContextRunner
                .withPropertyValues("luxspec.database.enabled=true")
                .withBean("bootstrapSource", ConfigSource.class, () -> bootstrap)
                .withBean("runtimeSource", ConfigSource.class, () -> {
                    runtimeSourceCreated.set(true);
                    return InMemoryConfigSource.fromEntries(
                            ConfigSourceId.of("runtime"),
                            ConfigSourceScope.RUNTIME,
                            Map.of()
                    );
                }, lazyBean())
                .run(context -> {
                    assertThat(context).hasSingleBean(DataSource.class);
                    assertThat(runtimeSourceCreated).isFalse();
                });
    }

    @Test
    void ordersBootstrapSourcesAtTheAssemblyBoundary() {
        ConfigSource lower = new OrderedConfigSource(ConfigSourceId.of("lower"), Map.of(
                DatabaseConfigSpec.TYPE.bind().getKey(), ConfigEntry.present("h2"),
                DatabaseConfigSpec.TARGET.bind().getKey(), ConfigEntry.present("memory"),
                DatabaseConfigSpec.NAME.bind().getKey(), ConfigEntry.present("lower"),
                DatabaseConfigSpec.OPTIONS.bind().getKey(), ConfigEntry.present(List.of())
        ), 20);
        ConfigSource higher = new OrderedConfigSource(ConfigSourceId.of("higher"), Map.of(
                DatabaseConfigSpec.TYPE.bind().getKey(), ConfigEntry.present("h2"),
                DatabaseConfigSpec.TARGET.bind().getKey(), ConfigEntry.present("memory"),
                DatabaseConfigSpec.NAME.bind().getKey(), ConfigEntry.present("higher"),
                DatabaseConfigSpec.OPTIONS.bind().getKey(), ConfigEntry.present(List.of())
        ), 10);

        bootContextRunner
                .withPropertyValues("luxspec.database.enabled=true")
                .withBean("lowerSource", ConfigSource.class, () -> lower)
                .withBean("higherSource", ConfigSource.class, () -> higher)
                .run(context -> assertThat(context.getBean(DatabaseConfig.class).getDatabaseName())
                        .isEqualTo("higher"));
    }

    private static ConfigSource bootstrapSource(Map<ConfigKey, ConfigEntry> entries) {
        return InMemoryConfigSource.fromEntries(
                ConfigSourceId.of("test-config"),
                ConfigSourceScope.BOOTSTRAP,
                entries
        );
    }

    private static BeanDefinitionCustomizer lazyBean() {
        return definition -> definition.setLazyInit(true);
    }

    private static DatabaseConfig h2Config() {
        return DatabaseConfig.builder(DatabaseType.H2, DatabaseTarget.memory())
                .databaseName("auto-config-test")
                .build();
    }

    private static final class OrderedConfigSource extends InMemoryConfigSource implements Ordered {
        private final int order;

        private OrderedConfigSource(
                ConfigSourceId id,
                Map<ConfigKey, ConfigEntry> entries,
                int order
        ) {
            super(id, ConfigSourceScope.BOOTSTRAP, entries, Map.of());
            this.order = order;
        }

        @Override
        public int getOrder() {
            return order;
        }
    }
}
