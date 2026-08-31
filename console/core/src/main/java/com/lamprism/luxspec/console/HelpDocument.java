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
 * Immutable semantic Help document independent of terminal formatting.
 *
 * @author RollW
 */
public final class HelpDocument {
    private final String applicationName;
    private final CommandPath commandPath;
    private final List<String> commandAliases;
    private final String usage;
    private final @Nullable String applicationHeader;
    private final @Nullable String commandHeader;
    private final @Nullable String summary;
    private final @Nullable String description;
    private final List<CommandEntry> commands;
    private final List<OptionEntry> options;
    private final List<ArgumentEntry> arguments;
    private final List<CommandDocumentation.Example> examples;
    private final List<String> notes;
    private final @Nullable String footer;

    private HelpDocument(Builder builder) {
        this.applicationName = builder.applicationName;
        this.commandPath = builder.commandPath;
        this.commandAliases = List.copyOf(builder.commandAliases);
        this.usage = builder.usage;
        this.applicationHeader = builder.applicationHeader;
        this.commandHeader = builder.commandHeader;
        this.summary = builder.summary;
        this.description = builder.description;
        this.commands = List.copyOf(builder.commands);
        this.options = List.copyOf(builder.options);
        this.arguments = List.copyOf(builder.arguments);
        this.examples = List.copyOf(builder.examples);
        this.notes = List.copyOf(builder.notes);
        this.footer = builder.footer;
    }

    /**
     * Creates a Help document builder.
     *
     * @param applicationName application display name
     * @param commandPath     canonical command path
     * @param usage           semantic usage text
     * @return the Help document builder
     */
    public static Builder builder(String applicationName,
                                  CommandPath commandPath,
                                  String usage) {
        return new Builder(applicationName, commandPath, usage);
    }

    /**
     * @return the application display name
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * @return the canonical command path being documented
     */
    public CommandPath getCommandPath() {
        return commandPath;
    }

    /**
     * @return aliases of the documented command
     */
    public List<String> getCommandAliases() {
        return commandAliases;
    }

    /**
     * @return semantic usage text
     */
    public String getUsage() {
        return usage;
    }

    /**
     * @return optional application header
     */
    public @Nullable String getApplicationHeader() {
        return applicationHeader;
    }

    /**
     * @return optional command header
     */
    public @Nullable String getCommandHeader() {
        return commandHeader;
    }

    /**
     * @return optional short summary
     */
    public @Nullable String getSummary() {
        return summary;
    }

    /**
     * @return optional long description
     */
    public @Nullable String getDescription() {
        return description;
    }

    /**
     * @return immutable child command entries
     */
    public List<CommandEntry> getCommands() {
        return commands;
    }

    /**
     * @return immutable option entries
     */
    public List<OptionEntry> getOptions() {
        return options;
    }

    /**
     * @return immutable positional argument entries
     */
    public List<ArgumentEntry> getArguments() {
        return arguments;
    }

    /**
     * @return immutable examples
     */
    public List<CommandDocumentation.Example> getExamples() {
        return examples;
    }

    /**
     * @return immutable notes
     */
    public List<String> getNotes() {
        return notes;
    }

    /**
     * @return optional footer
     */
    public @Nullable String getFooter() {
        return footer;
    }

    /**
     * Child command entry in a Help document.
     *
     * @author RollW
     */
    public static final class CommandEntry {
        private final String name;
        private final List<String> aliases;
        private final @Nullable String summary;
        private final @Nullable String group;
        private final boolean hidden;

        /**
         * Creates a child command entry.
         *
         * @param name    canonical command name
         * @param aliases command aliases
         * @param summary optional command summary
         * @param group   optional Help group
         * @param hidden  whether the command is hidden by default
         */
        public CommandEntry(String name,
                            List<String> aliases,
                            @Nullable String summary,
                            @Nullable String group,
                            boolean hidden) {
            this.name = requireText(name, "Command name");
            this.aliases = copyTextList(aliases, "Command aliases");
            this.summary = normalizeOptional(summary);
            this.group = normalizeOptional(group);
            this.hidden = hidden;
        }

        /**
         * @return the canonical command name
         */
        public String getName() {
            return name;
        }

        /**
         * @return immutable command aliases
         */
        public List<String> getAliases() {
            return aliases;
        }

        /**
         * @return the optional command summary
         */
        public @Nullable String getSummary() {
            return summary;
        }

        /**
         * @return the optional Help group
         */
        public @Nullable String getGroup() {
            return group;
        }

        /**
         * @return whether the command is hidden by default
         */
        public boolean isHidden() {
            return hidden;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof CommandEntry that)) {
                return false;
            }
            return hidden == that.hidden
                    && name.equals(that.name)
                    && aliases.equals(that.aliases)
                    && Objects.equals(summary, that.summary)
                    && Objects.equals(group, that.group);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, aliases, summary, group, hidden);
        }

        @Override
        public String toString() {
            return "CommandEntry[name=" + name + ", aliases=" + aliases
                    + ", summary=" + summary + ", group=" + group
                    + ", hidden=" + hidden + "]";
        }
    }

    /**
     * Option entry in a Help document.
     *
     * @author RollW
     */
    public static final class OptionEntry {
        private final List<String> names;
        private final @Nullable String valueLabel;
        private final @Nullable String description;
        private final boolean flag;
        private final boolean required;
        private final boolean repeatable;
        private final boolean hidden;
        private final List<String> defaultValues;

        /**
         * Creates an option entry.
         *
         * @param names         canonical option name followed by aliases
         * @param valueLabel    optional option value label
         * @param description   optional option description
         * @param flag          whether the option is a presence flag
         * @param required      whether the option is required
         * @param repeatable    whether the option can occur more than once
         * @param hidden        whether the option is hidden by default
         * @param defaultValues display values for the option defaults
         */
        public OptionEntry(List<String> names,
                           @Nullable String valueLabel,
                           @Nullable String description,
                           boolean flag,
                           boolean required,
                           boolean repeatable,
                           boolean hidden,
                           List<String> defaultValues) {
            this.names = copyTextList(names, "Option names");
            if (this.names.isEmpty()) {
                throw new IllegalArgumentException("At least one option name is required");
            }
            this.valueLabel = normalizeOptional(valueLabel);
            this.description = normalizeOptional(description);
            this.flag = flag;
            this.required = required;
            this.repeatable = repeatable;
            this.hidden = hidden;
            this.defaultValues = copyTextList(defaultValues, "Option defaults");
        }

        /**
         * @return immutable option names
         */
        public List<String> getNames() {
            return names;
        }

        /**
         * @return the optional option value label
         */
        public @Nullable String getValueLabel() {
            return valueLabel;
        }

        /**
         * @return the optional option description
         */
        public @Nullable String getDescription() {
            return description;
        }

        /**
         * @return whether the option is a presence flag
         */
        public boolean isFlag() {
            return flag;
        }

        /**
         * @return whether the option is required
         */
        public boolean isRequired() {
            return required;
        }

        /**
         * @return whether the option is repeatable
         */
        public boolean isRepeatable() {
            return repeatable;
        }

        /**
         * @return whether the option is hidden by default
         */
        public boolean isHidden() {
            return hidden;
        }

        /**
         * @return immutable display values for the option defaults
         */
        public List<String> getDefaultValues() {
            return defaultValues;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof OptionEntry that)) {
                return false;
            }
            return flag == that.flag
                    && required == that.required
                    && repeatable == that.repeatable
                    && hidden == that.hidden
                    && names.equals(that.names)
                    && Objects.equals(valueLabel, that.valueLabel)
                    && Objects.equals(description, that.description)
                    && defaultValues.equals(that.defaultValues);
        }

        @Override
        public int hashCode() {
            return Objects.hash(names, valueLabel, description, flag, required,
                    repeatable, hidden, defaultValues);
        }

        @Override
        public String toString() {
            return "OptionEntry[names=" + names + ", valueLabel=" + valueLabel
                    + ", description=" + description + ", flag=" + flag
                    + ", required=" + required + ", repeatable=" + repeatable
                    + ", hidden=" + hidden + ", defaultValues=" + defaultValues + "]";
        }
    }

    /**
     * Positional argument entry in a Help document.
     *
     * @author RollW
     */
    public static final class ArgumentEntry {
        private final String name;
        private final @Nullable String valueLabel;
        private final @Nullable String description;
        private final int minimumValues;
        private final int maximumValues;
        private final boolean hidden;
        private final List<String> defaultValues;

        /**
         * Creates a positional argument entry.
         *
         * @param name          argument name
         * @param valueLabel    optional argument value label
         * @param description   optional argument description
         * @param minimumValues minimum value count
         * @param maximumValues maximum value count
         * @param hidden        whether the argument is hidden by default
         * @param defaultValues display values for the argument defaults
         */
        public ArgumentEntry(String name,
                             @Nullable String valueLabel,
                             @Nullable String description,
                             int minimumValues,
                             int maximumValues,
                             boolean hidden,
                             List<String> defaultValues) {
            this.name = requireText(name, "Argument name");
            this.valueLabel = normalizeOptional(valueLabel);
            this.description = normalizeOptional(description);
            if (minimumValues < 0 || maximumValues < minimumValues || maximumValues == 0) {
                throw new IllegalArgumentException("Invalid argument arity: "
                        + minimumValues + ".." + maximumValues);
            }
            this.minimumValues = minimumValues;
            this.maximumValues = maximumValues;
            this.hidden = hidden;
            this.defaultValues = copyTextList(defaultValues, "Argument defaults");
        }

        /**
         * @return the argument name
         */
        public String getName() {
            return name;
        }

        /**
         * @return the optional argument value label
         */
        public @Nullable String getValueLabel() {
            return valueLabel;
        }

        /**
         * @return the optional argument description
         */
        public @Nullable String getDescription() {
            return description;
        }

        /**
         * @return the minimum value count
         */
        public int getMinimumValues() {
            return minimumValues;
        }

        /**
         * @return the maximum value count
         */
        public int getMaximumValues() {
            return maximumValues;
        }

        /**
         * @return whether the argument is hidden by default
         */
        public boolean isHidden() {
            return hidden;
        }

        /**
         * @return immutable display values for the argument defaults
         */
        public List<String> getDefaultValues() {
            return defaultValues;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ArgumentEntry that)) {
                return false;
            }
            return minimumValues == that.minimumValues
                    && maximumValues == that.maximumValues
                    && hidden == that.hidden
                    && name.equals(that.name)
                    && Objects.equals(valueLabel, that.valueLabel)
                    && Objects.equals(description, that.description)
                    && defaultValues.equals(that.defaultValues);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, valueLabel, description, minimumValues,
                    maximumValues, hidden, defaultValues);
        }

        @Override
        public String toString() {
            return "ArgumentEntry[name=" + name + ", valueLabel=" + valueLabel
                    + ", description=" + description + ", minimumValues=" + minimumValues
                    + ", maximumValues=" + maximumValues + ", hidden=" + hidden
                    + ", defaultValues=" + defaultValues + "]";
        }
    }

    /**
     * Builder for immutable semantic Help documents.
     */
    public static final class Builder {
        private final String applicationName;
        private final CommandPath commandPath;
        private final String usage;
        private List<String> commandAliases = new ArrayList<>();
        private @Nullable String applicationHeader;
        private @Nullable String commandHeader;
        private @Nullable String summary;
        private @Nullable String description;
        private final List<CommandEntry> commands = new ArrayList<>();
        private final List<OptionEntry> options = new ArrayList<>();
        private final List<ArgumentEntry> arguments = new ArrayList<>();
        private final List<CommandDocumentation.Example> examples = new ArrayList<>();
        private final List<String> notes = new ArrayList<>();
        private @Nullable String footer;

        private Builder(String applicationName, CommandPath commandPath, String usage) {
            this.applicationName = requireText(applicationName, "Application name");
            this.commandPath = Objects.requireNonNull(commandPath, "commandPath");
            this.usage = requireText(usage, "Usage");
        }

        /**
         * Replaces the documented command aliases.
         *
         * @param commandAliases command aliases
         * @return this builder
         */
        public Builder commandAliases(List<String> commandAliases) {
            this.commandAliases = new ArrayList<>(copyTextList(commandAliases, "Command aliases"));
            return this;
        }

        /**
         * Sets the application header.
         *
         * @param applicationHeader optional application header
         * @return this builder
         */
        public Builder applicationHeader(@Nullable String applicationHeader) {
            this.applicationHeader = normalizeOptional(applicationHeader);
            return this;
        }

        /**
         * Sets the command header.
         *
         * @param commandHeader optional command header
         * @return this builder
         */
        public Builder commandHeader(@Nullable String commandHeader) {
            this.commandHeader = normalizeOptional(commandHeader);
            return this;
        }

        /**
         * Sets the command summary.
         *
         * @param summary optional command summary
         * @return this builder
         */
        public Builder summary(@Nullable String summary) {
            this.summary = normalizeOptional(summary);
            return this;
        }

        /**
         * Sets the command description.
         *
         * @param description optional command description
         * @return this builder
         */
        public Builder description(@Nullable String description) {
            this.description = normalizeOptional(description);
            return this;
        }

        /**
         * Adds one child command entry.
         *
         * @param command child command entry
         * @return this builder
         */
        public Builder command(CommandEntry command) {
            commands.add(Objects.requireNonNull(command, "command"));
            return this;
        }

        /**
         * Adds one option entry.
         *
         * @param option option entry
         * @return this builder
         */
        public Builder option(OptionEntry option) {
            options.add(Objects.requireNonNull(option, "option"));
            return this;
        }

        /**
         * Adds one positional argument entry.
         *
         * @param argument positional argument entry
         * @return this builder
         */
        public Builder argument(ArgumentEntry argument) {
            arguments.add(Objects.requireNonNull(argument, "argument"));
            return this;
        }

        /**
         * Adds one Help example.
         *
         * @param example Help example
         * @return this builder
         */
        public Builder example(CommandDocumentation.Example example) {
            examples.add(Objects.requireNonNull(example, "example"));
            return this;
        }

        /**
         * Adds one Help note.
         *
         * @param note Help note
         * @return this builder
         */
        public Builder note(String note) {
            notes.add(requireText(note, "Help note"));
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
         * Builds the immutable Help document.
         *
         * @return the immutable Help document
         */
        public HelpDocument build() {
            return new HelpDocument(this);
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

    private static List<String> copyTextList(List<String> values, String name) {
        Objects.requireNonNull(values, name);
        List<String> copiedValues = new ArrayList<>(values.size());
        for (String value : values) {
            copiedValues.add(requireText(value, name + " value"));
        }
        return List.copyOf(copiedValues);
    }
}
