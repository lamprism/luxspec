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

package com.lamprism.luxspec.console;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable canonical path of a command in a command catalog.
 *
 * @author RollW
 */
public final class CommandPath {
    private static final CommandPath ROOT = new CommandPath(List.of());

    private final List<String> segments;

    private CommandPath(List<String> segments) {
        this.segments = List.copyOf(segments);
    }

    /**
     * Returns the root command path.
     *
     * @return the empty command path
     */
    public static CommandPath root() {
        return ROOT;
    }

    /**
     * Creates a command path from validated segments.
     *
     * @param segments command name segments, or no segments for the root
     * @return the command path
     */
    public static CommandPath of(String... segments) {
        Objects.requireNonNull(segments, "segments");
        List<String> copiedSegments = new ArrayList<>(segments.length);
        for (String segment : segments) {
            copiedSegments.add(validateSegment(segment));
        }
        if (copiedSegments.isEmpty()) {
            return ROOT;
        }
        return new CommandPath(copiedSegments);
    }

    /**
     * Creates a command path from validated segments.
     *
     * @param segments command name segments, or an empty list for the root
     * @return the command path
     */
    public static CommandPath of(List<String> segments) {
        Objects.requireNonNull(segments, "segments");
        return of(segments.toArray(String[]::new));
    }

    /**
     * Returns the path segments in canonical order.
     *
     * @return immutable path segments
     */
    public List<String> getSegments() {
        return segments;
    }

    /**
     * Returns the number of path segments.
     *
     * @return the segment count
     */
    public int getSize() {
        return segments.size();
    }

    /**
     * Returns the segment at an index.
     *
     * @param index the segment index
     * @return the segment
     */
    public String getSegment(int index) {
        return segments.get(index);
    }

    /**
     * Returns whether this path is the root path.
     *
     * @return {@code true} for the root path
     */
    public boolean isRoot() {
        return segments.isEmpty();
    }

    /**
     * Returns a child path with one segment appended.
     *
     * @param segment the child segment
     * @return the child path
     */
    public CommandPath child(String segment) {
        List<String> childSegments = new ArrayList<>(segments.size() + 1);
        childSegments.addAll(segments);
        childSegments.add(validateSegment(segment));
        return new CommandPath(childSegments);
    }

    /**
     * Returns this path's parent. The root path is its own parent.
     *
     * @return the parent path
     */
    public CommandPath getParent() {
        if (isRoot()) {
            return this;
        }
        return new CommandPath(segments.subList(0, segments.size() - 1));
    }

    /**
     * Returns whether this path starts with another path.
     *
     * @param prefix the possible prefix
     * @return {@code true} when the prefix is an ancestor of this path
     */
    public boolean startsWith(CommandPath prefix) {
        Objects.requireNonNull(prefix, "prefix");
        if (prefix.getSize() > getSize()) {
            return false;
        }
        for (int index = 0; index < prefix.getSize(); index++) {
            if (!segments.get(index).equals(prefix.segments.get(index))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return String.join(" ", segments);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CommandPath that)) {
            return false;
        }
        return segments.equals(that.segments);
    }

    @Override
    public int hashCode() {
        return segments.hashCode();
    }

    private static String validateSegment(String segment) {
        Objects.requireNonNull(segment, "segment");
        if (segment.isBlank()) {
            throw new IllegalArgumentException("Command path segments cannot be blank");
        }
        for (int index = 0; index < segment.length(); index++) {
            char character = segment.charAt(index);
            if (Character.isWhitespace(character) || Character.isISOControl(character)) {
                throw new IllegalArgumentException(
                        "Command path segments cannot contain whitespace or control characters: " + segment
                );
            }
        }
        if (segment.indexOf('=') >= 0) {
            throw new IllegalArgumentException("Command path segments cannot contain '=': " + segment);
        }
        if (segment.charAt(0) == '-') {
            throw new IllegalArgumentException("Command path segments cannot start with '-': " + segment);
        }
        return segment;
    }
}
