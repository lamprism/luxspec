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

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable presentation metadata owned by a command specification.
 *
 * @author RollW
 */
public final class CommandDocumentation {
    private static final CommandDocumentation EMPTY = new CommandDocumentation(
            null, null, null, null, List.of(), List.of(), null
    );

    private final @Nullable String header;
    private final @Nullable String summary;
    private final @Nullable String description;
    private final @Nullable String group;
    private final List<Example> examples;
    private final List<String> notes;
    private final @Nullable String footer;

    private CommandDocumentation(@Nullable String header,
                                 @Nullable String summary,
                                 @Nullable String description,
                                 @Nullable String group,
                                 List<Example> examples,
                                 List<String> notes,
                                 @Nullable String footer) {
        this.header = header;
        this.summary = summary;
        this.description = description;
        this.group = group;
        this.examples = List.copyOf(examples);
        this.notes = List.copyOf(notes);
        this.footer = footer;
    }

    /**
     * Returns empty documentation.
     *
     * @return empty command documentation
     */
    public static CommandDocumentation empty() {
        return EMPTY;
    }

    /**
     * Creates a documentation builder.
     *
     * @return a documentation builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return the optional header
     */
    public @Nullable String getHeader() {
        return header;
    }

    /**
     * @return the optional one-line summary
     */
    public @Nullable String getSummary() {
        return summary;
    }

    /**
     * @return the optional long description
     */
    public @Nullable String getDescription() {
        return description;
    }

    /**
     * @return the optional Help group
     */
    public @Nullable String getGroup() {
        return group;
    }

    /**
     * @return immutable Help examples
     */
    public List<Example> getExamples() {
        return examples;
    }

    /**
     * @return immutable Help notes
     */
    public List<String> getNotes() {
        return notes;
    }

    /**
     * @return the optional footer
     */
    public @Nullable String getFooter() {
        return footer;
    }

    /**
     * @return a builder initialized from this documentation
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * One Help example and its optional explanation.
     *
     * @author RollW
     */
    public static final class Example {
        private final String invocation;
        private final @Nullable String description;

        /**
         * Creates a Help example.
         *
         * @param invocation  example command invocation
         * @param description optional example explanation
         */
        public Example(String invocation, @Nullable String description) {
            this.invocation = requireText(invocation, "Example invocation");
            this.description = normalizeOptional(description);
        }

        /**
         * @return the example command invocation
         */
        public String getInvocation() {
            return invocation;
        }

        /**
         * @return the optional example explanation
         */
        public @Nullable String getDescription() {
            return description;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Example that)) {
                return false;
            }
            return invocation.equals(that.invocation)
                    && Objects.equals(description, that.description);
        }

        @Override
        public int hashCode() {
            return Objects.hash(invocation, description);
        }

        @Override
        public String toString() {
            return "Example[invocation=" + invocation + ", description=" + description + "]";
        }
    }

    /**
     * Builder for immutable command documentation.
     */
    public static final class Builder {
        private @Nullable String header;
        private @Nullable String summary;
        private @Nullable String description;
        private @Nullable String group;
        private List<Example> examples = new ArrayList<>();
        private List<String> notes = new ArrayList<>();
        private @Nullable String footer;

        private Builder() {
        }

        private Builder(CommandDocumentation documentation) {
            this.header = documentation.header;
            this.summary = documentation.summary;
            this.description = documentation.description;
            this.group = documentation.group;
            this.examples = new ArrayList<>(documentation.examples);
            this.notes = new ArrayList<>(documentation.notes);
            this.footer = documentation.footer;
        }

        /**
         * Sets the Help header.
         *
         * @param header optional Help header
         * @return this builder
         */
        public Builder header(@Nullable String header) {
            this.header = normalizeOptional(header);
            return this;
        }

        /**
         * Sets the one-line Help summary.
         *
         * @param summary optional Help summary
         * @return this builder
         */
        public Builder summary(@Nullable String summary) {
            this.summary = normalizeOptional(summary);
            return this;
        }

        /**
         * Sets the long Help description.
         *
         * @param description optional Help description
         * @return this builder
         */
        public Builder description(@Nullable String description) {
            this.description = normalizeOptional(description);
            return this;
        }

        /**
         * Sets the Help group name.
         *
         * @param group optional Help group name
         * @return this builder
         */
        public Builder group(@Nullable String group) {
            this.group = normalizeOptional(group);
            return this;
        }

        /**
         * Replaces the Help examples.
         *
         * @param examples Help examples
         * @return this builder
         */
        public Builder examples(List<Example> examples) {
            Objects.requireNonNull(examples, "examples");
            this.examples = new ArrayList<>(examples.size());
            for (Example example : examples) {
                this.examples.add(Objects.requireNonNull(example, "examples cannot contain null"));
            }
            return this;
        }

        /**
         * Adds a Help example without an explanation.
         *
         * @param invocation example command invocation
         * @return this builder
         */
        public Builder example(String invocation) {
            this.examples.add(new Example(invocation, null));
            return this;
        }

        /**
         * Adds a Help example with an explanation.
         *
         * @param invocation  example command invocation
         * @param description optional example explanation
         * @return this builder
         */
        public Builder example(String invocation, @Nullable String description) {
            this.examples.add(new Example(invocation, description));
            return this;
        }

        /**
         * Replaces the Help notes.
         *
         * @param notes Help notes
         * @return this builder
         */
        public Builder notes(List<String> notes) {
            Objects.requireNonNull(notes, "notes");
            this.notes = new ArrayList<>(notes.size());
            for (String note : notes) {
                this.notes.add(requireText(note, "Help note"));
            }
            return this;
        }

        /**
         * Adds one Help note.
         *
         * @param note Help note
         * @return this builder
         */
        public Builder note(String note) {
            this.notes.add(requireText(note, "Help note"));
            return this;
        }

        /**
         * Sets the Help footer.
         *
         * @param footer optional Help footer
         * @return this builder
         */
        public Builder footer(@Nullable String footer) {
            this.footer = normalizeOptional(footer);
            return this;
        }

        /**
         * Builds immutable documentation.
         *
         * @return the immutable command documentation
         */
        public CommandDocumentation build() {
            if (header == null && summary == null && description == null && group == null
                    && examples.isEmpty() && notes.isEmpty() && footer == null) {
                return EMPTY;
            }
            return new CommandDocumentation(header, summary, description, group,
                    examples, notes, footer);
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = value.strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return normalized;
    }

    private static @Nullable String normalizeOptional(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
    }
}
