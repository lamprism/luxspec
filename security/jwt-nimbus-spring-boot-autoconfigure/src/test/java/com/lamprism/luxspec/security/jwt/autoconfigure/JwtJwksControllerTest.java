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
import com.lamprism.luxspec.config.ConfigValue;
import com.lamprism.luxspec.security.crypto.KeyEntry;
import com.lamprism.luxspec.security.crypto.KeySet;
import com.lamprism.luxspec.security.jwt.ConfigJwtAccessTokenSettingsSource;
import com.lamprism.luxspec.security.jwt.spring.JwtJwksController;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author RollW
 */
class JwtJwksControllerTest {
    @Test
    void publishesTheCurrentPublicSnapshotWithConservativeCaching() throws GeneralSecurityException {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeySet keySet = KeySet.forVerification(List.of(
                new KeyEntry("current", generator.generateKeyPair().getPublic())
        ));
        JwtJwksController controller = new JwtJwksController(
                new AccessSettingsReader(),
                reference -> {
                    assertEquals("access", reference);
                    return keySet;
                }
        );

        ResponseEntity<Map<String, Object>> response = controller.keys();

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getHeaders().getCacheControl().contains("max-age=0"));
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        List<?> keys = (List<?>) body.get("keys");
        assertNotNull(keys);
        assertEquals(1, keys.size());
        assertEquals("current", ((Map<?, ?>) keys.get(0)).get("kid"));
    }

    private static final class AccessSettingsReader implements ConfigReader {
        @Override
        public <T> ConfigValue<T> get(ConfigBinding<T> binding) {
            if (ConfigJwtAccessTokenSettingsSource.KEY_SET_NAME.equals(binding.getSpec())) {
                return configured("access");
            }
            return ConfigValue.absent();
        }

        @SuppressWarnings("unchecked")
        private static <T> ConfigValue<T> configured(Object value) {
            return (ConfigValue<T>) ConfigValue.defaultValue(value);
        }
    }
}
