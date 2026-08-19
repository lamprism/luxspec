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

package com.lamprism.luxspec.observability.autoconfigure;

import com.lamprism.luxspec.observability.health.HealthGroup;
import com.lamprism.luxspec.observability.health.HealthRegistry;
import com.lamprism.luxspec.observability.health.HealthResult;
import com.lamprism.luxspec.observability.health.HealthStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Native direct-status health endpoint without a dependency on Spring Actuator.
 *
 * <p>The endpoint exposes aggregate, liveness, and readiness checks at
 * {@code /health}, {@code /health/liveness}, and {@code /health/readiness}.
 * {@code UP} maps to HTTP 200; every other status maps to HTTP 503.
 *
 * @author RollW
 */
@RestController
@RequestMapping("/health")
public class LuxspecHealthEndpoint {
    private final HealthRegistry registry;

    public LuxspecHealthEndpoint(HealthRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> aggregate() {
        return project(HealthGroup.AGGREGATE);
    }

    @GetMapping("/liveness")
    public ResponseEntity<Map<String, String>> liveness() {
        return project(HealthGroup.LIVENESS);
    }

    @GetMapping("/readiness")
    public ResponseEntity<Map<String, String>> readiness() {
        return project(HealthGroup.READINESS);
    }

    private ResponseEntity<Map<String, String>> project(HealthGroup group) {
        HealthResult result = registry.evaluate(group);
        HttpStatus status = result.status() == HealthStatus.UP ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(Map.of("status", result.status().name()));
    }
}
