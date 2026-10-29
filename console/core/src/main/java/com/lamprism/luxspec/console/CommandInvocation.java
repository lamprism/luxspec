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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable typed values produced by parsing one command invocation.
 *
 * @author RollW
 */
public final class CommandInvocation {
    private final CommandCatalog catalog;
    private final CommandSpec command;
    private final List<String> rawTokens;
    private final Map<OptionSpec<?>, List<?>> optionValues;
    private final Set<OptionSpec<?>> providedOptions;
    private final Map<ArgumentSpec<?>, List<?>> argumentValues;

    CommandInvocation(CommandCatalog catalog,
                      CommandSpec command,
                      List<String> rawTokens,
                      Map<OptionSpec<?>, ? extends List<?>> optionValues,
                      Set<OptionSpec<?>> providedOptions,
                      Map<ArgumentSpec<?>, List<?>> argumentValues) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.command = Objects.requireNonNull(command, "command");
        this.rawTokens = List.copyOf(rawTokens);
        this.optionValues = copyIdentityMap(optionValues);
        Set<OptionSpec<?>> copiedProvidedOptions = Collections.newSetFromMap(new IdentityHashMap<>());
        copiedProvidedOptions.addAll(providedOptions);
        this.providedOptions = Collections.unmodifiableSet(copiedProvidedOptions);
        this.argumentValues = copyIdentityMap(argumentValues);
    }

    /**
     * Creates a typed invocation builder for an executable catalog command.
     *
     * @param catalog   the application's command catalog
     * @param path      the canonical command path
     * @param rawTokens the original input tokens
     * @return a builder that enforces the declared option and argument cardinalities
     */
    public static Builder builder(CommandCatalog catalog, CommandPath path, List<String> rawTokens) {
        return new Builder(catalog, path, rawTokens);
    }

    /**
     * Builds a parsed invocation for a custom command parser.
     */
    public static final class Builder {
        private final CommandCatalog catalog;
        private final CommandSpec command;
        private final List<String> rawTokens;
        private final List<OptionSpec<?>> options;
        private final Map<OptionSpec<?>, List<?>> optionValues = new IdentityHashMap<>();
        private final Map<ArgumentSpec<?>, List<?>> argumentValues = new IdentityHashMap<>();

        private Builder(CommandCatalog catalog, CommandPath path, List<String> rawTokens) {
            CommandCatalog nonNullCatalog = Objects.requireNonNull(catalog, "catalog");
            this.catalog = nonNullCatalog;
            CommandPath nonNullPath = Objects.requireNonNull(path, "path");
            CommandSpec found = nonNullCatalog.find(nonNullPath);
            if (found == null) {
                throw new IllegalArgumentException("Unknown command path: " + nonNullPath);
            }
            this.command = found;
            if (nonNullCatalog.runner(nonNullPath).isEmpty()) {
                throw new IllegalArgumentException("Command is not executable: " + nonNullPath);
            }
            this.rawTokens = List.copyOf(rawTokens);
            List<OptionSpec<?>> available = new ArrayList<>(nonNullCatalog.getRoot().getOptions());
            if (!nonNullPath.isRoot()) {
                available.addAll(command.getOptions());
            }
            this.options = List.copyOf(available);
        }

        /**
         * Adds all explicitly supplied values for one declared option.
         *
         * @param option the option from the current command catalog
         * @param values the nonempty typed values; provided flags contain only true values
         * @param <T>    the option value type
         * @return this builder
         */
        public <T> Builder option(OptionSpec<T> option, List<T> values) {
            OptionSpec<T> key = Objects.requireNonNull(option, "option");
            if (!containsIdentity(options, key)) {
                throw new IllegalArgumentException("Option does not belong to this command: " + key.getName());
            }
            List<T> copied = List.copyOf(values);
            if (copied.isEmpty() || (!key.isRepeatable() && copied.size() > 1)) {
                throw new IllegalArgumentException("Invalid option value count: " + key.getName());
            }
            if (key.isFlag() && copied.stream().anyMatch(value -> !Boolean.TRUE.equals(value))) {
                throw new IllegalArgumentException("Provided flag values must be true: " + key.getName());
            }
            if (optionValues.putIfAbsent(key, copied) != null) {
                throw new IllegalArgumentException("Option is already supplied: " + key.getName());
            }
            return this;
        }

        /**
         * Adds the supplied values for one declared positional argument.
         *
         * @param argument the declared positional argument
         * @param values   the typed supplied values
         * @param <T>      the argument value type
         * @return this builder
         */
        public <T> Builder argument(ArgumentSpec<T> argument, List<T> values) {
            ArgumentSpec<T> key = Objects.requireNonNull(argument, "argument");
            if (!containsIdentity(command.getArguments(), key)) {
                throw new IllegalArgumentException("Argument does not belong to this command: " + key.getName());
            }
            List<T> copied = List.copyOf(values);
            if (copied.size() < key.getMinimumValues() || copied.size() > key.getMaximumValues()) {
                throw new IllegalArgumentException("Invalid argument value count: " + key.getName());
            }
            if (argumentValues.putIfAbsent(key, copied) != null) {
                throw new IllegalArgumentException("Argument is already supplied: " + key.getName());
            }
            return this;
        }

        /**
         * Completes defaults and validates required declarations.
         *
         * @return the immutable invocation
         */
        public CommandInvocation build() {
            Map<OptionSpec<?>, List<?>> completeOptions = new IdentityHashMap<>(optionValues);
            for (OptionSpec<?> option : options) {
                if (!completeOptions.containsKey(option)) {
                    if (option.isRequired()) {
                        throw new IllegalArgumentException("Missing required option: " + option.getName());
                    }
                    completeOptions.put(option, option.isFlag() ? List.of(Boolean.FALSE) : option.getDefaultValues());
                }
            }
            Map<ArgumentSpec<?>, List<?>> completeArguments = new IdentityHashMap<>(argumentValues);
            for (ArgumentSpec<?> argument : command.getArguments()) {
                List<?> supplied = completeArguments.getOrDefault(argument, List.of());
                if (supplied.size() < argument.getMinimumValues()) {
                    throw new IllegalArgumentException("Missing required argument: " + argument.getName());
                }
                if (supplied.isEmpty()) {
                    completeArguments.put(argument, argument.getDefaultValues());
                }
            }
            return new CommandInvocation(catalog, command, rawTokens, completeOptions, optionValues.keySet(), completeArguments);
        }

        private static boolean containsIdentity(List<?> declarations, Object value) {
            return declarations.stream().anyMatch(declaration -> declaration == value);
        }
    }

    boolean belongsTo(CommandCatalog catalog) {
        return this.catalog == catalog;
    }

    /**
     * @return the resolved canonical command specification
     */
    public CommandSpec getCommand() {
        return command;
    }

    /**
     * @return the canonical command path
     */
    public CommandPath getPath() {
        return command.getPath();
    }

    /**
     * @return immutable raw tokens supplied to the parser
     */
    public List<String> getRawTokens() {
        return rawTokens;
    }

    /**
     * Returns typed values for a known option. An absent optional value has an
     * empty list. A flag always has one Boolean value, either true or false.
     *
     * @param option option declaration belonging to this invocation
     * @param <T>    option value type
     * @return immutable typed values
     */
    public <T> List<T> getOptionValues(OptionSpec<T> option) {
        Objects.requireNonNull(option, "option");
        List<?> values = optionValues.get(option);
        if (values == null) {
            throw new IllegalArgumentException("Option does not belong to this invocation: " + option.getName());
        }
        return castValues(values);
    }

    /**
     * Returns one typed value for a non-repeatable option when present.
     *
     * @param option non-repeatable option declaration
     * @param <T>    option value type
     * @return the value when present
     */
    public <T> Optional<T> getOptionValue(OptionSpec<T> option) {
        if (option.isRepeatable()) {
            throw new IllegalArgumentException("Repeatable options require getOptionValues: " + option.getName());
        }
        List<T> values = getOptionValues(option);
        if (values.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(values.get(0));
    }

    /**
     * @param option option declaration belonging to this invocation
     * @return whether the option was explicitly supplied by the caller
     */
    public boolean wasProvided(OptionSpec<?> option) {
        Objects.requireNonNull(option, "option");
        if (!optionValues.containsKey(option)) {
            throw new IllegalArgumentException("Option does not belong to this invocation: " + option.getName());
        }
        return providedOptions.contains(option);
    }

    /**
     * Returns typed values for a known positional argument.
     *
     * @param argument argument declaration belonging to this invocation
     * @param <T>      argument value type
     * @return immutable typed values
     */
    public <T> List<T> getArgumentValues(ArgumentSpec<T> argument) {
        Objects.requireNonNull(argument, "argument");
        List<?> values = argumentValues.get(argument);
        if (values == null) {
            throw new IllegalArgumentException("Argument does not belong to this invocation: " + argument.getName());
        }
        return castValues(values);
    }

    /**
     * Returns one typed value for a non-repeatable positional argument.
     *
     * @param argument non-repeatable argument declaration
     * @param <T>      argument value type
     * @return the value when present
     */
    public <T> Optional<T> getArgumentValue(ArgumentSpec<T> argument) {
        if (argument.isRepeatable()) {
            throw new IllegalArgumentException("Repeatable arguments require getArgumentValues: " + argument.getName());
        }
        List<T> values = getArgumentValues(argument);
        if (values.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(values.get(0));
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> castValues(List<?> values) {
        return (List<T>) values;
    }

    private static <K> Map<K, List<?>> copyIdentityMap(Map<K, ? extends List<?>> values) {
        Objects.requireNonNull(values, "values");
        Map<K, List<?>> copiedValues = new IdentityHashMap<>();
        for (Map.Entry<K, ? extends List<?>> entry : values.entrySet()) {
            copiedValues.put(Objects.requireNonNull(entry.getKey(), "value key"),
                    List.copyOf(Objects.requireNonNull(entry.getValue(), "value list")));
        }
        return Collections.unmodifiableMap(copiedValues);
    }

}
