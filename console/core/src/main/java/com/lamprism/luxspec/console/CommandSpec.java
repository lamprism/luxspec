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
 * Immutable declarative command contract.
 *
 * @author RollW
 */
public final class CommandSpec {
    private final CommandPath path;
    private final List<String> aliases;
    private final CommandDocumentation documentation;
    private final List<OptionSpec<?>> options;
    private final List<ArgumentSpec<?>> arguments;
    private final boolean hidden;
    private final boolean implicit;

    private CommandSpec(CommandPath path,
                        List<String> aliases,
                        CommandDocumentation documentation,
                        List<OptionSpec<?>> options,
                        List<ArgumentSpec<?>> arguments,
                        boolean hidden,
                        boolean implicit) {
        this.path = Objects.requireNonNull(path, "path");
        this.aliases = copyAliases(path, aliases);
        this.documentation = Objects.requireNonNull(documentation, "documentation");
        this.options = copyOptions(options);
        this.arguments = copyArguments(arguments);
        this.hidden = hidden;
        this.implicit = implicit;
    }

    /**
     * Creates a builder for a canonical command path.
     *
     * @param path command path segments
     * @return the command specification builder
     */
    public static Builder builder(String... path) {
        return builder(CommandPath.of(path));
    }

    /**
     * Creates a builder for a canonical command path.
     *
     * @param path canonical command path
     * @return the command specification builder
     */
    public static Builder builder(CommandPath path) {
        return new Builder(Objects.requireNonNull(path, "path"));
    }

    /**
     * Creates a root command builder.
     *
     * @return the root command specification builder
     */
    public static Builder root() {
        return builder(CommandPath.root());
    }

    static CommandSpec implicit(CommandPath path) {
        return new CommandSpec(path, List.of(), CommandDocumentation.empty(), List.of(), List.of(),
                false, true);
    }

    /**
     * @return the canonical command path
     */
    public CommandPath getPath() {
        return path;
    }

    /**
     * @return the final canonical command name, or an empty string for root
     */
    public String getName() {
        if (path.isRoot()) {
            return "";
        }
        return path.getSegment(path.getSize() - 1);
    }

    /**
     * @return immutable local command aliases
     */
    public List<String> getAliases() {
        return aliases;
    }

    /**
     * @return immutable command documentation
     */
    public CommandDocumentation getDocumentation() {
        return documentation;
    }

    /**
     * @return immutable local options
     */
    public List<OptionSpec<?>> getOptions() {
        return options;
    }

    /**
     * @return immutable ordered positional arguments
     */
    public List<ArgumentSpec<?>> getArguments() {
        return arguments;
    }

    /**
     * @return whether this command is hidden from default Help
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * @return whether this node was synthesized as an implicit parent group
     */
    public boolean isImplicit() {
        return implicit;
    }

    /**
     * @return a builder initialized from this specification
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Builder for immutable command specifications.
     */
    public static final class Builder {
        private final CommandPath path;
        private List<String> aliases = new ArrayList<>();
        private CommandDocumentation.Builder documentation = CommandDocumentation.builder();
        private List<OptionSpec<?>> options = new ArrayList<>();
        private List<ArgumentSpec<?>> arguments = new ArrayList<>();
        private boolean hidden;

        private Builder(CommandPath path) {
            this.path = path;
        }

        private Builder(CommandSpec specification) {
            this.path = specification.path;
            this.aliases = new ArrayList<>(specification.aliases);
            this.documentation = specification.documentation.toBuilder();
            this.options = new ArrayList<>(specification.options);
            this.arguments = new ArrayList<>(specification.arguments);
            this.hidden = specification.hidden;
        }

        /**
         * Adds one local command alias.
         *
         * @param alias command alias
         * @return this builder
         */
        public Builder alias(String alias) {
            aliases.add(Objects.requireNonNull(alias, "alias"));
            return this;
        }

        /**
         * Adds ordered local aliases.
         *
         * @param aliases command aliases
         * @return this builder
         */
        public Builder aliases(String... aliases) {
            Objects.requireNonNull(aliases, "aliases");
            for (String alias : aliases) {
                alias(alias);
            }
            return this;
        }

        /**
         * Sets the command Help header.
         *
         * @param header command Help header
         * @return this builder
         */
        public Builder header(String header) {
            documentation.header(header);
            return this;
        }

        /**
         * Sets the one-line Help summary.
         *
         * @param summary command Help summary
         * @return this builder
         */
        public Builder summary(String summary) {
            documentation.summary(summary);
            return this;
        }

        /**
         * Sets the long Help description.
         *
         * @param description command Help description
         * @return this builder
         */
        public Builder description(String description) {
            documentation.description(description);
            return this;
        }

        /**
         * Sets the Help group used for child command entries.
         *
         * @param group Help group name
         * @return this builder
         */
        public Builder group(String group) {
            documentation.group(group);
            return this;
        }

        /**
         * Adds one Help example.
         *
         * @param invocation example command invocation
         * @return this builder
         */
        public Builder example(String invocation) {
            documentation.example(invocation);
            return this;
        }

        /**
         * Adds one Help example with a description.
         *
         * @param invocation  example command invocation
         * @param description example description
         * @return this builder
         */
        public Builder example(String invocation, String description) {
            documentation.example(invocation, description);
            return this;
        }

        /**
         * Adds one Help note.
         *
         * @param note Help note
         * @return this builder
         */
        public Builder note(String note) {
            documentation.note(note);
            return this;
        }

        /**
         * Sets Help footer content.
         *
         * @param footer Help footer
         * @return this builder
         */
        public Builder footer(String footer) {
            documentation.footer(footer);
            return this;
        }

        /**
         * Hides the command from default Help.
         *
         * @return this builder
         */
        public Builder hidden() {
            this.hidden = true;
            return this;
        }

        /**
         * Adds one local option.
         *
         * @param option local option
         * @return this builder
         */
        public Builder option(OptionSpec<?> option) {
            options.add(Objects.requireNonNull(option, "option"));
            return this;
        }

        /**
         * Replaces the local option list.
         *
         * @param options local options
         * @return this builder
         */
        public Builder options(List<? extends OptionSpec<?>> options) {
            Objects.requireNonNull(options, "options");
            this.options = new ArrayList<>(options.size());
            for (OptionSpec<?> option : options) {
                this.options.add(Objects.requireNonNull(option, "options cannot contain null"));
            }
            return this;
        }

        /**
         * Adds one positional argument.
         *
         * @param argument positional argument
         * @return this builder
         */
        public Builder argument(ArgumentSpec<?> argument) {
            arguments.add(Objects.requireNonNull(argument, "argument"));
            return this;
        }

        /**
         * Replaces the positional argument list.
         *
         * @param arguments positional arguments
         * @return this builder
         */
        public Builder arguments(List<? extends ArgumentSpec<?>> arguments) {
            Objects.requireNonNull(arguments, "arguments");
            this.arguments = new ArrayList<>(arguments.size());
            for (ArgumentSpec<?> argument : arguments) {
                this.arguments.add(Objects.requireNonNull(argument, "arguments cannot contain null"));
            }
            return this;
        }

        /**
         * Builds the immutable command specification.
         *
         * @return the immutable command specification
         */
        public CommandSpec build() {
            if (path.isRoot() && !aliases.isEmpty()) {
                throw new IllegalArgumentException("The root command cannot define aliases");
            }
            return new CommandSpec(path, aliases, documentation.build(), options, arguments,
                    hidden, false);
        }
    }

    private static List<String> copyAliases(CommandPath path, List<String> aliases) {
        Objects.requireNonNull(aliases, "aliases");
        if (path.isRoot() && !aliases.isEmpty()) {
            throw new IllegalArgumentException("The root command cannot define aliases");
        }
        List<String> copiedAliases = new ArrayList<>(aliases.size());
        String canonicalName = path.isRoot() ? "" : path.getSegment(path.getSize() - 1);
        for (String alias : aliases) {
            validateAlias(alias);
            if (canonicalName.equals(alias) || copiedAliases.contains(alias)) {
                throw new IllegalArgumentException("Duplicate command alias: " + alias);
            }
            copiedAliases.add(alias);
        }
        return List.copyOf(copiedAliases);
    }

    private static List<OptionSpec<?>> copyOptions(List<OptionSpec<?>> options) {
        Objects.requireNonNull(options, "options");
        List<OptionSpec<?>> copiedOptions = new ArrayList<>(options.size());
        for (OptionSpec<?> option : options) {
            copiedOptions.add(Objects.requireNonNull(option, "options cannot contain null"));
        }
        return List.copyOf(copiedOptions);
    }

    private static List<ArgumentSpec<?>> copyArguments(List<ArgumentSpec<?>> arguments) {
        Objects.requireNonNull(arguments, "arguments");
        List<ArgumentSpec<?>> copiedArguments = new ArrayList<>(arguments.size());
        for (ArgumentSpec<?> argument : arguments) {
            copiedArguments.add(Objects.requireNonNull(argument, "arguments cannot contain null"));
        }
        return List.copyOf(copiedArguments);
    }

    private static void validateAlias(String alias) {
        Objects.requireNonNull(alias, "alias");
        if (alias.isBlank()) {
            throw new IllegalArgumentException("Command aliases cannot be blank");
        }
        for (int index = 0; index < alias.length(); index++) {
            char character = alias.charAt(index);
            if (Character.isWhitespace(character) || Character.isISOControl(character)) {
                throw new IllegalArgumentException("Command aliases cannot contain whitespace: " + alias);
            }
        }
        if (alias.indexOf('=') >= 0 || alias.charAt(0) == '-') {
            throw new IllegalArgumentException("Invalid command alias: " + alias);
        }
    }
}
