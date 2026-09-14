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
    private final CommandSpec command;
    private final List<String> rawTokens;
    private final Map<OptionSpec<?>, List<?>> optionValues;
    private final Set<OptionSpec<?>> providedOptions;
    private final Map<ArgumentSpec<?>, List<?>> argumentValues;

    CommandInvocation(CommandSpec command,
                      List<String> rawTokens,
                      Map<OptionSpec<?>, ? extends List<?>> optionValues,
                      Set<OptionSpec<?>> providedOptions,
                      Map<ArgumentSpec<?>, List<?>> argumentValues) {
        this.command = Objects.requireNonNull(command, "command");
        this.rawTokens = List.copyOf(rawTokens);
        this.optionValues = copyIdentityMap(optionValues);
        Set<OptionSpec<?>> copiedProvidedOptions = Collections.newSetFromMap(new IdentityHashMap<>());
        copiedProvidedOptions.addAll(providedOptions);
        this.providedOptions = Collections.unmodifiableSet(copiedProvidedOptions);
        this.argumentValues = copyIdentityMap(argumentValues);
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
