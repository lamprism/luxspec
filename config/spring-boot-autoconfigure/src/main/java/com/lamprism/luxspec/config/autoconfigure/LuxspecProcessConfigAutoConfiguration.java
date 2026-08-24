package com.lamprism.luxspec.config.autoconfigure;

import com.lamprism.luxspec.config.source.ConfigSource;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.ConfigSourceScope;
import com.lamprism.luxspec.config.source.process.CommandLineConfigSource;
import com.lamprism.luxspec.config.source.process.EnvironmentConfigSource;
import com.lamprism.luxspec.config.source.process.SystemPropertyConfigSource;
import com.lamprism.luxspec.config.source.toml.TomlConfigSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

import java.nio.file.Path;

/**
 * Provides bootstrap-safe process configuration sources for the generic Config system.
 *
 * <p>The source precedence is explicit: command line, environment, system properties, and TOML.
 * This assembly does not read the Config provider because the provider may include a runtime
 * database source.</p>
 *
 * @author RollW
 */
@AutoConfiguration(beforeName = {
        "com.lamprism.luxspec.config.autoconfigure.LuxspecConfigAutoConfiguration",
        "com.lamprism.luxspec.database.autoconfigure.LuxspecDatabaseAutoConfiguration"
})
public class LuxspecProcessConfigAutoConfiguration {
    private static final int COMMAND_LINE_ORDER = 0;
    private static final int ENVIRONMENT_ORDER = 100;
    private static final int SYSTEM_PROPERTY_ORDER = 200;
    private static final int TOML_ORDER = 300;

    /**
     * Creates the environment variable source.
     *
     * @return the bootstrap environment source
     */
    @Bean
    @Order(ENVIRONMENT_ORDER)
    @ConditionalOnMissingBean(name = "environmentConfigSource")
    public ConfigSource environmentConfigSource() {
        return new EnvironmentConfigSource();
    }

    /**
     * Creates the JVM system-property source.
     *
     * @return the bootstrap system-property source
     */
    @Bean
    @Order(SYSTEM_PROPERTY_ORDER)
    @ConditionalOnMissingBean(name = "systemPropertyConfigSource")
    public ConfigSource systemPropertyConfigSource() {
        return new SystemPropertyConfigSource();
    }

    /**
     * Creates the command-line source from the application argument snapshot.
     *
     * @param arguments the optional Spring Boot application arguments
     * @return the bootstrap command-line source
     */
    @Bean
    @Order(COMMAND_LINE_ORDER)
    @ConditionalOnMissingBean(name = "commandLineConfigSource")
    public ConfigSource commandLineConfigSource(
            ObjectProvider<ApplicationArguments> arguments
    ) {
        ApplicationArguments applicationArguments = arguments.getIfAvailable();
        String[] sourceArguments = applicationArguments == null
                ? new String[0]
                : applicationArguments.getSourceArgs();
        return new CommandLineConfigSource(
                ConfigSourceId.of("command-line"),
                sourceArguments
        );
    }

    /**
     * Creates the optional external TOML bootstrap source.
     *
     * @param environment the Spring environment used only for source location selection
     * @return the bootstrap TOML source
     */
    @Bean
    @Order(TOML_ORDER)
    @ConditionalOnProperty(prefix = "luxspec.config.toml", name = "path")
    @ConditionalOnMissingBean(name = "tomlConfigSource")
    public ConfigSource tomlConfigSource(Environment environment) {
        String path = environment.getProperty("luxspec.config.toml.path");
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("luxspec.config.toml.path must not be blank");
        }
        return TomlConfigSource.fromPath(
                ConfigSourceId.of("toml"),
                ConfigSourceScope.BOOTSTRAP,
                Path.of(path)
        );
    }
}
