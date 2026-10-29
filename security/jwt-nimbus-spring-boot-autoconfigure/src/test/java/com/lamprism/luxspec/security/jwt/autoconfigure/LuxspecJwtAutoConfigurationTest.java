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

import com.lamprism.luxspec.config.ConfigBinding;
import com.lamprism.luxspec.config.ConfigReader;
import com.lamprism.luxspec.config.ConfigSpec;
import com.lamprism.luxspec.config.ConfigValue;
import com.lamprism.luxspec.context.ExecutionContext;
import com.lamprism.luxspec.security.authentication.AccessTokenAuthenticator;
import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authentication.Subject;
import com.lamprism.luxspec.security.authentication.SubjectResolver;
import com.lamprism.luxspec.security.authentication.UserSubject;
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import com.lamprism.luxspec.security.crypto.DefaultPublicKeyDeriver;
import com.lamprism.luxspec.security.crypto.FixedKeySetProvider;
import com.lamprism.luxspec.security.crypto.KeyEntry;
import com.lamprism.luxspec.security.crypto.KeySet;
import com.lamprism.luxspec.security.crypto.KeySetProvider;
import com.lamprism.luxspec.security.crypto.PublicKeyDeriver;
import com.lamprism.luxspec.security.crypto.config.ConfigKeySetProvider;
import com.lamprism.luxspec.security.jwt.ConfigJwtAccessTokenSettingsSource;
import com.lamprism.luxspec.security.jwt.JwtAccessTokenAdapter;

import com.lamprism.luxspec.security.jwt.JwtAccessTokenClaimsPolicy;
import com.lamprism.luxspec.security.jwt.JwtAccessTokenConfiguration;
import com.lamprism.luxspec.security.jwt.JwtAccessTokenMissingScopesPolicy;
import com.lamprism.luxspec.security.jwt.JwtAlgorithmPolicy;

import com.lamprism.luxspec.security.token.access.AccessToken;
import com.lamprism.luxspec.security.token.access.AccessTokenRevocationStore;
import com.lamprism.luxspec.security.token.access.VerifiedAccessToken;
import com.nimbusds.jose.JWSAlgorithm;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author RollW
 */
class LuxspecJwtAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LuxspecJwtAutoConfiguration.class));

    @Test
    void remainsDisabledByDefault() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(JwtAccessTokenAdapter.class);
            assertThat(context).doesNotHaveBean(ConfigJwtAccessTokenSettingsSource.class);
        });
    }

    @Test
    void waitsForConfigReaderWhenExplicitlyEnabled() {
        contextRunner
                .withPropertyValues("luxspec.security.jwt.enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(JwtAccessTokenAdapter.class);
                    assertThat(context).doesNotHaveBean(ConfigJwtAccessTokenSettingsSource.class);
                });
    }

    @Test
    void createsDefaultKeyMaterialDeriverWhenConfigReaderIsAvailable() {
        contextRunner
                .withPropertyValues("luxspec.security.jwt.enabled=true")
                .withBean(ConfigReader.class, EmptyConfigReader::new)
                .withBean(JwtAccessTokenAdapter.class, () -> new JwtAccessTokenAdapter(
                        keySetName -> {
                            throw new AssertionError("Key-set provider must not be called");
                        },
                        "access",
                        Clock.systemUTC(),
                        new JwtAccessTokenConfiguration(
                                Duration.ofMinutes(1),
                                "issuer",
                                Set.of(),
                                Duration.ZERO
                        )
                ))
                .run(context -> {
                    assertThat(context).hasSingleBean(PublicKeyDeriver.class);
                    assertThat(context.getBean(PublicKeyDeriver.class))
                            .isInstanceOf(DefaultPublicKeyDeriver.class);
                    assertThat(context).hasSingleBean(ConfigKeySetProvider.class);
                    assertThat(context).doesNotHaveBean(ConfigJwtAccessTokenSettingsSource.class);
                });
    }

    @Test
    void createsTheDefaultAdapterWithoutRegisteringItsSettingsAdapter() {
        contextRunner
                .withPropertyValues("luxspec.security.jwt.enabled=true")
                .withBean(ConfigReader.class, ConfiguredJwtReader::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(JwtAccessTokenAdapter.class);
                    assertThat(context).doesNotHaveBean(ConfigJwtAccessTokenSettingsSource.class);
                });
    }

    @Test
    void usesAnApplicationProvidedKeySetProviderWithoutCreatingTheConfigProvider() {
        SecretKeySpec key = new SecretKeySpec(
                "application-provided-signing-key".getBytes(StandardCharsets.US_ASCII),
                "HmacSHA256"
        );
        FixedKeySetProvider keySetProvider = new FixedKeySetProvider(Map.of(
                "access",
                KeySet.withActiveKey("active", List.of(new KeyEntry("active", key, key)))
        ));

        contextRunner
                .withPropertyValues("luxspec.security.jwt.enabled=true")
                .withBean(ConfigReader.class, ConfiguredJwtReader::new)
                .withBean(KeySetProvider.class, () -> keySetProvider)
                .run(context -> {
                    assertThat(context).hasSingleBean(JwtAccessTokenAdapter.class);
                    assertThat(context.getBean(KeySetProvider.class)).isSameAs(keySetProvider);
                    assertThat(context).doesNotHaveBean(ConfigKeySetProvider.class);
                });
    }

    @Test
    void includesApplicationClaimPoliciesInTheDefaultAdapter() {
        SecretKeySpec key = new SecretKeySpec(
                "application-provided-signing-key".getBytes(StandardCharsets.US_ASCII), "HmacSHA256"
        );
        FixedKeySetProvider keys = new FixedKeySetProvider(Map.of(
                "access", KeySet.withActiveKey("active", List.of(new KeyEntry("active", key, key)))
        ));
        AtomicInteger validated = new AtomicInteger();
        JwtAccessTokenClaimsPolicy policy = new JwtAccessTokenClaimsPolicy() {
            @Override
            public Map<String, ?> contribute(Authentication authentication, ExecutionContext attributes,
                                             String tokenId, Instant issuedAt, Instant expiresAt, String issuer, Set<String> audiences) {
                return Map.of("application_version", 1);
            }

            @Override
            public void validate(VerifiedAccessToken token, Map<String, Object> claims) {
                assertThat(((Number) claims.get("application_version")).intValue()).isEqualTo(1);
                validated.incrementAndGet();
            }
        };

        contextRunner
                .withPropertyValues("luxspec.security.jwt.enabled=true")
                .withBean(ConfigReader.class, ConfiguredJwtReader::new)
                .withBean(KeySetProvider.class, () -> keys)
                .withBean(JwtAccessTokenClaimsPolicy.class, () -> policy)
                .run(context -> {
                    JwtAccessTokenAdapter adapter = context.getBean(JwtAccessTokenAdapter.class);
                    Authentication authentication = new Authentication(
                            new UserSubject(1), AuthorizationGrantSet.of(List.of())
                    );
                    AccessToken token = adapter.issue(authentication).getToken();
                    adapter.verify(token);
                    assertThat(validated.get()).isEqualTo(1);
                });
    }

    @Test
    void usesTheApplicationAlgorithmPolicy() {
        SecretKeySpec key = new SecretKeySpec(
                "application-provided-signing-key".getBytes(StandardCharsets.US_ASCII), "HmacSHA256"
        );
        FixedKeySetProvider keys = new FixedKeySetProvider(Map.of(
                "access", KeySet.withActiveKey("active", List.of(new KeyEntry("active", key, key)))
        ));
        AtomicInteger selections = new AtomicInteger();
        JwtAlgorithmPolicy policy = new JwtAlgorithmPolicy() {
            @Override
            public JWSAlgorithm signingAlgorithm(Key signingKey, JWSAlgorithm suggested) {
                selections.incrementAndGet();
                return suggested;
            }

            @Override
            public boolean permitsVerification(Key verificationKey, JWSAlgorithm algorithm) {
                return JWSAlgorithm.HS256.equals(algorithm);
            }
        };
        contextRunner
                .withPropertyValues("luxspec.security.jwt.enabled=true")
                .withBean(ConfigReader.class, ConfiguredJwtReader::new)
                .withBean(KeySetProvider.class, () -> keys)
                .withBean(JwtAlgorithmPolicy.class, () -> policy)
                .run(context -> {
                    JwtAccessTokenAdapter adapter = context.getBean(JwtAccessTokenAdapter.class);
                    AccessToken token = adapter.issue(new Authentication(
                            new UserSubject(1), AuthorizationGrantSet.of(List.of())
                    )).getToken();
                    adapter.verify(token);
                    assertThat(selections.get()).isEqualTo(1);
                });
    }

    @Test
    void usesTheApplicationMissingScopesPolicy() throws GeneralSecurityException {
        SecretKeySpec key = new SecretKeySpec(
                "application-provided-signing-key".getBytes(StandardCharsets.US_ASCII), "HmacSHA256"
        );
        FixedKeySetProvider keys = new FixedKeySetProvider(Map.of(
                "access", KeySet.withActiveKey("active", List.of(new KeyEntry("active", key, key)))
        ));
        Instant now = Instant.parse("2026-07-12T00:00:00Z");
        String header = "{\"alg\":\"HS256\",\"kid\":\"active\"}";
        String payload = "{\"iss\":\"issuer\",\"sub\":\"42\",\"subject_type\":\"user\","
                + "\"aud\":\"application\",\"token_purpose\":\"access\",\"jti\":\"token\","
                + "\"iat\":" + now.getEpochSecond() + ",\"exp\":" + now.plusSeconds(60).getEpochSecond() + "}";
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String input = encoder.encodeToString(header.getBytes(StandardCharsets.UTF_8)) + "."
                + encoder.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        AccessToken token = new AccessToken(input + "."
                + encoder.encodeToString(mac.doFinal(input.getBytes(StandardCharsets.US_ASCII))));
        AtomicInteger resolutions = new AtomicInteger();
        JwtAccessTokenMissingScopesPolicy policy = subject -> {
            resolutions.incrementAndGet();
            return AuthorizationGrantSet.of(List.of());
        };

        contextRunner
                .withPropertyValues("luxspec.security.jwt.enabled=true")
                .withBean(ConfigReader.class, ConfiguredJwtReader::new)
                .withBean(KeySetProvider.class, () -> keys)
                .withBean(Clock.class, () -> Clock.fixed(now, ZoneOffset.UTC))
                .withBean(JwtAccessTokenMissingScopesPolicy.class, () -> policy)
                .run(context -> {
                    VerifiedAccessToken verified = context.getBean(JwtAccessTokenAdapter.class).verify(token);
                    assertThat(verified.getSubjectId()).isEqualTo("42");
                    assertThat(verified.getGrants().getScopes()).isEmpty();
                    assertThat(resolutions.get()).isEqualTo(1);
                });
    }

    @Test
    void leavesAccessTokenAuthenticationAssemblyToSecurityAutoConfiguration() {
        contextRunner
                .withPropertyValues("luxspec.security.jwt.enabled=true")
                .withBean(ConfigReader.class, EmptyConfigReader::new)
                .withBean(JwtAccessTokenAdapter.class, LuxspecJwtAutoConfigurationTest::accessTokenAdapter)
                .withBean(SubjectResolver.class, UnsupportedSubjectResolver::new)
                .withBean(AccessTokenRevocationStore.class, NeverRevokedAccessTokenStore::new)
                .run(context -> assertThat(context).doesNotHaveBean(AccessTokenAuthenticator.class));
    }

    private static JwtAccessTokenAdapter accessTokenAdapter() {
        return new JwtAccessTokenAdapter(
                keySetName -> {
                    throw new AssertionError("Key-set provider must not be called");
                },
                "access",
                Clock.systemUTC(),
                new JwtAccessTokenConfiguration(
                        Duration.ofMinutes(1),
                        "issuer",
                        Set.of(),
                        Duration.ZERO
                )
        );
    }

    private static final class EmptyConfigReader implements ConfigReader {
        @Override
        public <T> ConfigValue<T> get(ConfigBinding<T> binding) {
            return ConfigValue.absent();
        }
    }

    private static final class ConfiguredJwtReader implements ConfigReader {
        @Override
        public <T> ConfigValue<T> get(ConfigBinding<T> binding) {
            ConfigSpec<T> spec = binding.getSpec();
            if (ConfigJwtAccessTokenSettingsSource.ACCESS_TTL.equals(spec)) {
                return configured(Duration.ofMinutes(1));
            }
            if (ConfigJwtAccessTokenSettingsSource.ACCESS_ISSUER.equals(spec)) {
                return configured("issuer");
            }
            if (ConfigJwtAccessTokenSettingsSource.ACCESS_AUDIENCES.equals(spec)) {
                return configured(List.of("application"));
            }
            if (ConfigJwtAccessTokenSettingsSource.ACCESS_CLOCK_SKEW.equals(spec)) {
                return configured(Duration.ZERO);
            }
            if (ConfigJwtAccessTokenSettingsSource.KEY_SET_NAME.equals(spec)) {
                return configured("access");
            }
            return ConfigValue.absent();
        }

        @SuppressWarnings("unchecked")
        private static <T> ConfigValue<T> configured(Object value) {
            return (ConfigValue<T>) ConfigValue.defaultValue(value);
        }
    }

    private static final class UnsupportedSubjectResolver implements SubjectResolver {
        @Override
        public Subject resolve(String type, String id) {
            throw new UnsupportedOperationException("Context tests do not resolve subjects");
        }
    }

    private static final class NeverRevokedAccessTokenStore implements AccessTokenRevocationStore {
        @Override
        public void revoke(VerifiedAccessToken accessToken, Instant acceptanceDeadline) {
        }

        @Override
        public boolean isRevoked(VerifiedAccessToken accessToken) {
            return false;
        }
    }
}
