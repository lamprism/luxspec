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

package com.lamprism.luxspec.data.query;

/**
 * Bounds the size and nesting of one structured query.
 *
 * @author RollW
 */
public final class QueryComplexityLimits {
    private static final QueryComplexityLimits DEFAULTS = new QueryComplexityLimits(8, 100, 20, 100, 256);

    private final int maximumDepth;
    private final int maximumNodes;
    private final int maximumOrBranches;
    private final int maximumMembershipValues;
    private final int maximumLikePatternLength;

    /**
     * Creates validated complexity limits.
     *
     * @param maximumDepth             the maximum expression nesting depth
     * @param maximumNodes             the maximum count of non-trivial expression nodes
     * @param maximumOrBranches        the maximum direct branches in one OR group
     * @param maximumMembershipValues  the maximum values in one IN or NOT IN condition
     * @param maximumLikePatternLength the maximum character length of one LIKE pattern
     */
    public QueryComplexityLimits(
            int maximumDepth,
            int maximumNodes,
            int maximumOrBranches,
            int maximumMembershipValues,
            int maximumLikePatternLength
    ) {
        requirePositive(maximumDepth, "maximumDepth");
        requirePositive(maximumNodes, "maximumNodes");
        requirePositive(maximumOrBranches, "maximumOrBranches");
        requirePositive(maximumMembershipValues, "maximumMembershipValues");
        requirePositive(maximumLikePatternLength, "maximumLikePatternLength");
        this.maximumDepth = maximumDepth;
        this.maximumNodes = maximumNodes;
        this.maximumOrBranches = maximumOrBranches;
        this.maximumMembershipValues = maximumMembershipValues;
        this.maximumLikePatternLength = maximumLikePatternLength;
    }

    /**
     * Returns the conservative default limits.
     *
     * @return the shared default limits
     */
    public static QueryComplexityLimits defaults() {
        return DEFAULTS;
    }

    /**
     * Returns the maximum expression nesting depth.
     *
     * @return the maximum depth
     */
    public int getMaximumDepth() {
        return maximumDepth;
    }

    /**
     * Returns the maximum count of non-trivial expression nodes.
     *
     * @return the maximum node count
     */
    public int getMaximumNodes() {
        return maximumNodes;
    }

    /**
     * Returns the maximum direct branches in one OR group.
     *
     * @return the maximum OR branch count
     */
    public int getMaximumOrBranches() {
        return maximumOrBranches;
    }

    /**
     * Returns the maximum values in one membership condition.
     *
     * @return the maximum membership value count
     */
    public int getMaximumMembershipValues() {
        return maximumMembershipValues;
    }

    /**
     * Returns the maximum character length of one LIKE pattern.
     *
     * @return the maximum pattern length
     */
    public int getMaximumLikePatternLength() {
        return maximumLikePatternLength;
    }

    private static void requirePositive(int value, String name) {
        if (value < 1) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
