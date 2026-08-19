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

package com.lamprism.luxspec.observability.runtime.health;

import com.lamprism.luxspec.observability.health.HealthContributor;
import com.lamprism.luxspec.observability.health.HealthGroup;
import com.lamprism.luxspec.observability.health.HealthRegistry;
import com.lamprism.luxspec.observability.runtime.ObservabilitySet;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Builds the default concurrent health registry.
 *
 * @author RollW
 */
public final class HealthRegistryBuilder {
    private final Executor executor;
    private final Map<String, HealthContributor> contributors = new LinkedHashMap<>();
    private final Map<HealthGroup, LinkedHashSet<String>> groups = new EnumMap<>(HealthGroup.class);
    private final List<ObservabilitySet> sets = new ArrayList<>();
    private Duration timeout;

    private HealthRegistryBuilder(Executor executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
        for (HealthGroup group : HealthGroup.values()) {
            groups.put(group, new LinkedHashSet<>());
        }
    }

    public static HealthRegistryBuilder builder(Executor executor) {
        return new HealthRegistryBuilder(executor);
    }

    public HealthRegistryBuilder register(String name, HealthContributor contributor) {
        String normalized = validateName(name);
        HealthContributor nonNullContributor = Objects.requireNonNull(contributor, "contributor");
        if (contributors.putIfAbsent(normalized, nonNullContributor) != null) {
            throw new IllegalArgumentException("Health contributor is registered more than once: " + normalized);
        }
        groups.get(HealthGroup.AGGREGATE).add(normalized);
        return this;
    }

    public HealthRegistryBuilder include(HealthGroup group, String contributorName) {
        HealthGroup nonNullGroup = Objects.requireNonNull(group, "group");
        String normalized = validateName(contributorName);
        groups.get(nonNullGroup).add(normalized);
        return this;
    }

    /**
     * Selects one domain contribution for this health registry.
     *
     * @param set the selected observability set
     * @return this builder
     */
    public HealthRegistryBuilder set(ObservabilitySet set) {
        sets.add(Objects.requireNonNull(set, "set"));
        return this;
    }

    public HealthRegistryBuilder timeout(Duration timeout) {
        Duration nonNullTimeout = Objects.requireNonNull(timeout, "timeout");
        if (nonNullTimeout.isZero() || nonNullTimeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        try {
            nonNullTimeout.toNanos();
        } catch (ArithmeticException failure) {
            throw new IllegalArgumentException("timeout is too large", failure);
        }
        this.timeout = nonNullTimeout;
        return this;
    }

    public HealthRegistry build() {
        for (ObservabilitySet set : sets) {
            set.registerHealth(this);
        }
        sets.clear();
        for (Map.Entry<HealthGroup, LinkedHashSet<String>> entry : groups.entrySet()) {
            for (String name : entry.getValue()) {
                if (!contributors.containsKey(name)) {
                    throw new IllegalArgumentException("Unknown health contributor: " + name);
                }
            }
        }
        return new DefaultHealthRegistry(executor, contributors, groups, timeout);
    }

    private static String validateName(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Health contributor name must not be empty");
        }
        for (int index = 0; index < name.length(); index++) {
            char character = name.charAt(index);
            if (Character.isWhitespace(character)
                    || Character.isSpaceChar(character)
                    || Character.isISOControl(character)) {
                throw new IllegalArgumentException("Health contributor name must not contain whitespace or control characters");
            }
        }
        return name;
    }
}
