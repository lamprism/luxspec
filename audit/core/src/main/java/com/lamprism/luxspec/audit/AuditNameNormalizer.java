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

package com.lamprism.luxspec.audit;

import com.lamprism.luxspec.validation.Normalizer;
import com.lamprism.luxspec.validation.Validator;

/**
 * Normalizes and validates audit event names.
 *
 * <p>Surrounding characters recognized by {@link String#trim()} are removed. Empty names and ISO
 * control characters are rejected. Case and all other characters are preserved so this boundary
 * does not impose a provider-specific naming syntax.</p>
 *
 * @author RollW
 */
public final class AuditNameNormalizer implements Normalizer<String> {
    private static final AuditNameNormalizer INSTANCE = new AuditNameNormalizer();
    private static final Normalizer<String> NORMALIZER = Normalizer.of(String::trim)
            .validatedBy(
                    Validator.nonBlank("Audit event name")
                            .and(Validator.noControlCharacters("Audit event name"))
            );

    private AuditNameNormalizer() {
    }

    /**
     * Returns the shared stateless audit-name normalizer.
     *
     * @return the shared normalizer
     */
    public static AuditNameNormalizer instance() {
        return INSTANCE;
    }

    @Override
    public String normalize(String name) {
        return NORMALIZER.normalize(name);
    }
}
