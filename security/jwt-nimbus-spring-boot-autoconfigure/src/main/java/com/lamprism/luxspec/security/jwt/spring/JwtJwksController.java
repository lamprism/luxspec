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

package com.lamprism.luxspec.security.jwt.spring;

import com.lamprism.luxspec.config.ConfigReader;
import com.lamprism.luxspec.security.crypto.KeySetProvider;
import com.lamprism.luxspec.security.jwt.ConfigJwtAccessTokenSettingsSource;
import com.lamprism.luxspec.security.jwt.NimbusJwkSetProjection;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Publishes the current public verification keys for the configured JWT key set.
 *
 * <p>The provider is resolved for every request, so key rotation becomes visible without a
 * process restart. Symmetric verification material is filtered by
 * {@link NimbusJwkSetProjection}.</p>
 *
 * @author RollW
 */
@RestController
public final class JwtJwksController {
    private final ConfigReader configReader;
    private final KeySetProvider keySetProvider;
    private final NimbusJwkSetProjection jwkSetProjection;

    /**
     * Creates the configurable JWKS endpoint controller.
     *
     * @param configReader   the JWT configuration reader
     * @param keySetProvider the current key-set provider
     */
    public JwtJwksController(ConfigReader configReader, KeySetProvider keySetProvider) {
        this(configReader, keySetProvider, new NimbusJwkSetProjection());
    }

    /**
     * Creates the controller with an explicit Nimbus projection.
     *
     * @param configReader     the JWT configuration reader
     * @param keySetProvider   the current key-set provider
     * @param jwkSetProjection the Nimbus JWK-set projection
     */
    public JwtJwksController(
            ConfigReader configReader,
            KeySetProvider keySetProvider,
            NimbusJwkSetProjection jwkSetProjection
    ) {
        this.configReader = Objects.requireNonNull(configReader, "configReader");
        this.keySetProvider = Objects.requireNonNull(keySetProvider, "keySetProvider");
        this.jwkSetProjection = Objects.requireNonNull(jwkSetProjection, "jwkSetProjection");
    }

    /**
     * Returns the current public JWKS document.
     *
     * @return the public key document with conservative revalidation caching
     */
    @GetMapping(
            value = "${luxspec.security.jwt.jwks.path:/oauth2/jwks}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> keys() {
        ConfigJwtAccessTokenSettingsSource settings = new ConfigJwtAccessTokenSettingsSource(configReader);
        Map<String, Object> document = jwkSetProjection.project(
                keySetProvider.get(settings.getKeySetName())
        );
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(0L, TimeUnit.SECONDS).mustRevalidate())
                .body(document);
    }
}
