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

package com.lamprism.luxspec.security.firewall.rule;

import com.lamprism.luxspec.ErrorCode;
import com.lamprism.luxspec.security.firewall.FirewallDecision;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

final class FirewallRuleSupport {
    private FirewallRuleSupport() {
    }

    static Set<String> copyTextValues(Collection<String> values, String name) {
        Objects.requireNonNull(values, name);
        Set<String> result = new LinkedHashSet<>();
        for (String value : values) {
            String nonNullValue = Objects.requireNonNull(value, name + " value");
            if (nonNullValue.isBlank()) {
                throw new IllegalArgumentException(name + " must not contain blank values");
            }
            result.add(nonNullValue);
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        if (result.size() != values.size()) {
            throw new IllegalArgumentException(name + " must not contain duplicates");
        }
        return Set.copyOf(result);
    }

    static FirewallDecision decide(boolean matched, boolean allowMatch, ErrorCode reasonCode) {
        if (matched == allowMatch) {
            return FirewallDecision.pass();
        }
        return FirewallDecision.deny(reasonCode);
    }
}
