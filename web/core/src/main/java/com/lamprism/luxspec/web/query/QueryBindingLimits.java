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

package com.lamprism.luxspec.web.query;

/**
 * Bounds HTTP query parameter and offset-window materialization.
 *
 * @author RollW
 */
public final class QueryBindingLimits {
    private static final QueryBindingLimits DEFAULTS = new QueryBindingLimits(50, 100, 100_000L, 64, 256);

    private final int defaultLimit;
    private final int maximumLimit;
    private final long maximumOffset;
    private final int maximumParameters;
    private final int maximumValues;

    /**
     * Creates HTTP query binding limits.
     *
     * @param defaultLimit      the limit used when no limit is supplied
     * @param maximumLimit      the largest accepted limit
     * @param maximumOffset     the largest accepted zero-based offset
     * @param maximumParameters the largest number of distinct parameters
     * @param maximumValues     the largest total number of parameter values
     */
    public QueryBindingLimits(
            int defaultLimit,
            int maximumLimit,
            long maximumOffset,
            int maximumParameters,
            int maximumValues
    ) {
        if (defaultLimit < 1) {
            throw new IllegalArgumentException("defaultLimit must be positive");
        }
        if (maximumLimit < defaultLimit) {
            throw new IllegalArgumentException("maximumLimit must not be smaller than defaultLimit");
        }
        if (maximumOffset < 0L) {
            throw new IllegalArgumentException("maximumOffset must not be negative");
        }
        if (maximumParameters < 1) {
            throw new IllegalArgumentException("maximumParameters must be positive");
        }
        if (maximumValues < 1) {
            throw new IllegalArgumentException("maximumValues must be positive");
        }
        this.defaultLimit = defaultLimit;
        this.maximumLimit = maximumLimit;
        this.maximumOffset = maximumOffset;
        this.maximumParameters = maximumParameters;
        this.maximumValues = maximumValues;
    }

    /**
     * Returns conservative default limits.
     *
     * @return the shared defaults
     */
    public static QueryBindingLimits defaults() {
        return DEFAULTS;
    }

    /**
     * Returns the default limit used when the request omits one.
     *
     * @return the default limit
     */
    public int getDefaultLimit() {
        return defaultLimit;
    }

    /**
     * Returns the largest accepted limit.
     *
     * @return the maximum limit
     */
    public int getMaximumLimit() {
        return maximumLimit;
    }

    /**
     * Returns the largest accepted offset.
     *
     * @return the maximum offset
     */
    public long getMaximumOffset() {
        return maximumOffset;
    }

    /**
     * Returns the largest number of distinct parameters.
     *
     * @return the maximum parameter count
     */
    public int getMaximumParameters() {
        return maximumParameters;
    }

    /**
     * Returns the largest total number of parameter values.
     *
     * @return the maximum value count
     */
    public int getMaximumValues() {
        return maximumValues;
    }
}
