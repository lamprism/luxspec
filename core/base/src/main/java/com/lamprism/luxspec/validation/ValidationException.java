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

package com.lamprism.luxspec.validation;

import java.util.Objects;

/**
 * Signals that a value violates an explicit validation rule.
 *
 * <p>The detail is value-free so validation failures do not accidentally
 * disclose sensitive input. Domain-specific validation exceptions may extend
 * this type when callers need a more precise failure category.
 *
 * @author RollW
 */
public class ValidationException extends IllegalArgumentException {
    public ValidationException(String detail) {
        super(requireDetail(detail));
    }

    public ValidationException(String detail, Throwable cause) {
        super(requireDetail(detail), Objects.requireNonNull(cause, "cause"));
    }

    private static String requireDetail(String detail) {
        String nonNullDetail = Objects.requireNonNull(detail, "detail");
        if (nonNullDetail.isBlank()) {
            throw new IllegalArgumentException("detail must not be blank");
        }
        return nonNullDetail;
    }
}
