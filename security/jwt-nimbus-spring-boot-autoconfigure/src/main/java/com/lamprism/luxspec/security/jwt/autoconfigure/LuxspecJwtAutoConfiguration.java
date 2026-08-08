/*
 * Copyright (C) Lamprism
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lamprism.luxspec.security.jwt.autoconfigure;

import com.lamprism.luxspec.config.ConfigReader;
import com.lamprism.luxspec.security.authentication.AccessTokenAuthenticator;
import com.lamprism.luxspec.security.authentication.SubjectResolver;
import com.lamprism.luxspec.security.crypto.DefaultPublicKeyDeriver;
import com.lamprism.luxspec.security.crypto.KeySetProvider;
import com.lamprism.luxspec.security.crypto.PublicKeyDeriver;
import com.lamprism.luxspec.security.crypto.config.ConfigKeySetProvider;
import com.lamprism.luxspec.security.jwt.ConfigJwtAccessTokenSettingsSource;
import com.lamprism.luxspec.security.jwt.JwtAccessTokenAdapter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

/**
 * Assembles the optional Config-backed Nimbus JWT access-token adapter.
 *
 * <p>The adapter is enabled explicitly because creating it requires trusted signing settings and
 * key material. The aggregate starter supplies the module, while the application controls whether
 * JWT authentication is active with {@code luxspec.security.jwt.enabled=true}.</p>
 *
 * @author RollW
 */
@AutoConfiguration
@ConditionalOnClass(JwtAccessTokenAdapter.class)
@ConditionalOnProperty(prefix = "luxspec.security.jwt", name = "enabled", havingValue = "true")
public class LuxspecJwtAutoConfiguration {
    /**
     * Creates the Config-backed JWT settings source when one has not been supplied.
     *
     * @param reader the configured Luxspec Config reader
     * @return the JWT settings source
     */
    @Bean
    @ConditionalOnBean(ConfigReader.class)
    @ConditionalOnMissingBean(ConfigJwtAccessTokenSettingsSource.class)
    public ConfigJwtAccessTokenSettingsSource jwtSettingsSource(ConfigReader reader) {
        return new ConfigJwtAccessTokenSettingsSource(reader);
    }

    /**
     * Creates the default private-key public-key deriver when one has not been supplied.
     *
     * @return the default public-key deriver
     */
    @Bean
    @ConditionalOnBean(ConfigReader.class)
    @ConditionalOnMissingBean(PublicKeyDeriver.class)
    public PublicKeyDeriver publicKeyDeriver() {
        return new DefaultPublicKeyDeriver();
    }

    /**
     * Creates the Config-backed key-set provider when one has not been supplied.
     *
     * @param reader           the configured Luxspec Config reader
     * @param publicKeyDeriver the public-key deriver used for key pairs without public material
     * @return the named key-set provider
     */
    @Bean
    @ConditionalOnBean(ConfigReader.class)
    @ConditionalOnMissingBean(KeySetProvider.class)
    public KeySetProvider keySetProvider(ConfigReader reader, PublicKeyDeriver publicKeyDeriver) {
        return new ConfigKeySetProvider(reader, publicKeyDeriver);
    }

    /**
     * Creates the Nimbus JWT issuer and verifier from the configured trusted settings.
     *
     * @param settingsSource the Config-backed JWT settings source
     * @param keySetProvider the configured named key-set provider
     * @param clocks         the optional application clock
     * @return the Nimbus JWT access-token adapter
     */
    @Bean
    @ConditionalOnBean({ConfigJwtAccessTokenSettingsSource.class, KeySetProvider.class})
    @ConditionalOnMissingBean(JwtAccessTokenAdapter.class)
    public JwtAccessTokenAdapter jwtAccessTokenAdapter(
            ConfigJwtAccessTokenSettingsSource settingsSource,
            KeySetProvider keySetProvider,
            ObjectProvider<Clock> clocks
    ) {
        return new JwtAccessTokenAdapter(
                keySetProvider,
                settingsSource.getKeySetName(),
                clocks.getIfAvailable(Clock::systemUTC),
                settingsSource.getOptions()
        );
    }

    /**
     * Creates an access-token authenticator when a subject resolver is available.
     *
     * @param adapter         the configured JWT verifier
     * @param subjectResolver the current-subject resolver
     * @return the access-token authenticator
     */
    @Bean
    @ConditionalOnBean({JwtAccessTokenAdapter.class, SubjectResolver.class})
    @ConditionalOnMissingBean(AccessTokenAuthenticator.class)
    public AccessTokenAuthenticator accessTokenAuthenticator(
            JwtAccessTokenAdapter adapter,
            SubjectResolver subjectResolver
    ) {
        return new AccessTokenAuthenticator(adapter, subjectResolver);
    }
}
