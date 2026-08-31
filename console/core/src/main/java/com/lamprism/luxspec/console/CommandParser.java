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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Parses tokenized commands against one immutable catalog.
 *
 * @author RollW
 */
final class CommandParser {
    private final CommandCatalog catalog;
    private final Controls controls;

    CommandParser(CommandCatalog catalog, Controls controls) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.controls = Objects.requireNonNull(controls, "controls");
    }

    ParseOutcome parse(List<String> rawTokens) {
        List<String> tokens = copyTokens(rawTokens);
        if (!tokens.isEmpty() && tokens.get(0).equals(controls.helpCommand())) {
            return parseHelpCommand(tokens);
        }

        CommandCatalog.Node current = catalog.rootNode();
        Map<OptionSpec<?>, List<Object>> optionValues = new IdentityHashMap<>();
        Set<OptionSpec<?>> providedOptions = Collections.newSetFromMap(new IdentityHashMap<>());
        List<String> positionalTokens = new ArrayList<>();
        boolean optionsEnabled = true;
        boolean commandPathEnabled = true;
        int index = 0;
        try {
            while (index < tokens.size()) {
                String token = tokens.get(index);
                if (optionsEnabled && token.equals("--")) {
                    optionsEnabled = false;
                    commandPathEnabled = false;
                    index++;
                    continue;
                }
                if (optionsEnabled && isOptionToken(token)) {
                    if (controls.helpOptions().contains(token)) {
                        return new HelpRequest(current.path);
                    }
                    if (controls.versionOptions().contains(token)) {
                        return new VersionRequest();
                    }
                    OptionIndex optionIndex = optionIndex(current);
                    OptionSpec<?> option = optionIndex.byName().get(optionName(token));
                    if (option == null) {
                        throw usage("Unknown option '" + token + "'", current.path, null);
                    }
                    index = parseOption(tokens, index, current, option, optionValues, providedOptions);
                    if (current != catalog.rootNode()
                            && current.specification.getOptions().contains(option)) {
                        commandPathEnabled = false;
                    }
                    continue;
                }
                CommandCatalog.Node child = commandPathEnabled ? current.lookup.get(token) : null;
                if (child != null) {
                    current = child;
                    index++;
                    continue;
                }
                if (current.handler == null && !current.children.isEmpty()) {
                    throw usage("Unknown command '" + token + "' under '" + displayPath(current.path) + "'",
                            current.path, null);
                }
                positionalTokens.add(token);
                commandPathEnabled = false;
                index++;
            }
            completeOptionValues(current, optionValues, providedOptions);
            Map<ArgumentSpec<?>, List<?>> argumentValues = parseArguments(current, positionalTokens);
            if (current.handler == null) {
                if (!current.children.isEmpty()) {
                    throw usage("A subcommand is required", current.path, null);
                }
                throw usage("Command '" + displayPath(current.path) + "' is not executable",
                        current.path, null);
            }
            return new InvocationRequest(new CommandInvocation(
                    current.specification,
                    tokens,
                    optionValues,
                    providedOptions,
                    argumentValues
            ), current.handler);
        } catch (ParseException exception) {
            return new ParseFailure(exception.failure());
        }
    }

    private ParseOutcome parseHelpCommand(List<String> tokens) {
        if (tokens.size() == 1) {
            return new HelpRequest(CommandPath.root());
        }
        List<String> pathTokens = tokens.subList(1, tokens.size());
        for (String token : pathTokens) {
            if (isOptionToken(token)) {
                return new ParseFailure(usage("The help command accepts only a command path",
                        CommandPath.root(), null).failure());
            }
        }
        CommandCatalog.Node node = catalog.resolve(pathTokens);
        if (node == null) {
            return new ParseFailure(usage("Unknown command path '" + String.join(" ", pathTokens) + "'",
                    CommandPath.root(), null).failure());
        }
        return new HelpRequest(node.path);
    }

    private int parseOption(List<String> tokens,
                            int index,
                            CommandCatalog.Node current,
                            OptionSpec<?> option,
                            Map<OptionSpec<?>, List<Object>> optionValues,
                            Set<OptionSpec<?>> providedOptions) throws ParseException {
        String token = tokens.get(index);
        if (!token.startsWith("--")) {
            return parseShortOptions(tokens, index, current, optionValues, providedOptions);
        }
        String name = optionName(token);
        int equalsIndex = token.indexOf('=');
        String inlineValue = equalsIndex < 0 ? null : token.substring(equalsIndex + 1);
        if (option.isFlag()) {
            if (equalsIndex >= 0) {
                throw usage("Flag '" + name + "' does not accept a value", current.path, name);
            }
            addValue(option, Boolean.TRUE, current.path, optionValues, providedOptions, name);
            return index + 1;
        }

        String value;
        if (inlineValue == null) {
            if (index + 1 >= tokens.size()) {
                throw usage("Missing value for option '" + name + "'", current.path, name);
            }
            value = tokens.get(index + 1);
            index++;
        } else {
            value = inlineValue;
        }
        Object parsedValue;
        try {
            parsedValue = parseValue(option.getParser(), value);
        } catch (ValueParseException exception) {
            throw usage("Invalid value for option '" + name + "': " + exception.getMessage(),
                    current.path, name);
        }
        addValue(option, parsedValue, current.path, optionValues, providedOptions, name);
        return index + 1;
    }

    private int parseShortOptions(List<String> tokens,
                                  int index,
                                  CommandCatalog.Node current,
                                  Map<OptionSpec<?>, List<Object>> optionValues,
                                  Set<OptionSpec<?>> providedOptions) throws ParseException {
        String token = tokens.get(index);
        Map<String, OptionSpec<?>> options = optionIndex(current).byName();
        int cursor = 1;
        while (cursor < token.length()) {
            String name = "-" + token.charAt(cursor);
            OptionSpec<?> option = options.get(name);
            if (option == null) {
                throw usage("Unknown option '" + name + "'", current.path, name);
            }
            if (option.isFlag()) {
                if (cursor + 1 < token.length() && token.charAt(cursor + 1) == '=') {
                    throw usage("Flag '" + name + "' does not accept a value", current.path, name);
                }
                addValue(option, Boolean.TRUE, current.path, optionValues, providedOptions, name);
                cursor++;
                continue;
            }
            String remainder = token.substring(cursor + 1);
            if (remainder.startsWith("=")) {
                remainder = remainder.substring(1);
            }
            String value;
            int nextIndex = index + 1;
            if (remainder.isEmpty()) {
                if (nextIndex >= tokens.size()) {
                    throw usage("Missing value for option '" + name + "'", current.path, name);
                }
                value = tokens.get(nextIndex);
                nextIndex++;
            } else {
                value = remainder;
            }
            Object parsedValue;
            try {
                parsedValue = parseValue(option.getParser(), value);
            } catch (ValueParseException exception) {
                throw usage("Invalid value for option '" + name + "': " + exception.getMessage(),
                        current.path, name);
            }
            addValue(option, parsedValue, current.path, optionValues, providedOptions, name);
            return nextIndex;
        }
        return index + 1;
    }

    private void addValue(OptionSpec<?> option,
                          Object value,
                          CommandPath path,
                          Map<OptionSpec<?>, List<Object>> optionValues,
                          Set<OptionSpec<?>> providedOptions,
                          String name) throws ParseException {
        List<Object> values = optionValues.computeIfAbsent(option, ignored -> new ArrayList<>());
        if (!option.isRepeatable() && !values.isEmpty()) {
            throw usage("Option '" + name + "' cannot be supplied more than once", path, name);
        }
        values.add(value);
        providedOptions.add(option);
    }

    private void completeOptionValues(CommandCatalog.Node current,
                                      Map<OptionSpec<?>, List<Object>> optionValues,
                                      Set<OptionSpec<?>> providedOptions) throws ParseException {
        for (OptionSpec<?> option : catalog.effectiveOptions(current)) {
            List<Object> values = optionValues.get(option);
            if (values == null) {
                values = new ArrayList<>();
                if (option.isFlag()) {
                    values.add(Boolean.FALSE);
                } else if (option.defaultValuesOrNull() != null) {
                    values.addAll(option.defaultValuesOrNull());
                }
                optionValues.put(option, values);
            }
            if (option.isRequired() && !providedOptions.contains(option)) {
                throw usage("Missing required option '" + option.getName() + "'", current.path, option.getName());
            }
        }
    }

    private Map<ArgumentSpec<?>, List<?>> parseArguments(CommandCatalog.Node current,
                                                         List<String> positionalTokens)
            throws ParseException {
        Map<ArgumentSpec<?>, List<?>> parsedArguments = new IdentityHashMap<>();
        int tokenIndex = 0;
        List<ArgumentSpec<?>> arguments = current.specification.getArguments();
        for (int argumentIndex = 0; argumentIndex < arguments.size(); argumentIndex++) {
            ArgumentSpec<?> argument = arguments.get(argumentIndex);
            int remainingMinimum = minimumValues(arguments, argumentIndex + 1);
            int remainingTokens = positionalTokens.size() - tokenIndex;
            int maximumTake = Math.min(argument.getMaximumValues(), remainingTokens - remainingMinimum);
            if (maximumTake < argument.getMinimumValues()) {
                throw usage("Missing required argument '" + argument.getName() + "'",
                        current.path, argument.getName());
            }
            int take = maximumTake;
            List<Object> values = new ArrayList<>();
            for (int valueIndex = 0; valueIndex < take; valueIndex++) {
                String token = positionalTokens.get(tokenIndex++);
                try {
                    values.add(parseValue(argument.getParser(), token));
                } catch (ValueParseException exception) {
                    throw usage("Invalid value for argument '" + argument.getName() + "': "
                                    + exception.getMessage(),
                            current.path, argument.getName());
                }
            }
            if (values.isEmpty() && argument.defaultValuesOrNull() != null) {
                values.addAll(argument.defaultValuesOrNull());
            }
            parsedArguments.put(argument, values);
        }
        if (tokenIndex < positionalTokens.size()) {
            String token = positionalTokens.get(tokenIndex);
            throw usage("Unexpected argument '" + token + "'", current.path, null);
        }
        return parsedArguments;
    }

    private int minimumValues(List<ArgumentSpec<?>> arguments, int startIndex) {
        int minimum = 0;
        for (int index = startIndex; index < arguments.size(); index++) {
            minimum += arguments.get(index).getMinimumValues();
        }
        return minimum;
    }

    private OptionIndex optionIndex(CommandCatalog.Node node) {
        Map<String, OptionSpec<?>> options = new LinkedHashMap<>();
        for (OptionSpec<?> option : catalog.effectiveOptions(node)) {
            for (String name : option.getNames()) {
                options.put(name, option);
            }
        }
        return new OptionIndex(options);
    }

    private String optionName(String token) {
        int equalsIndex = token.startsWith("--") ? token.indexOf('=') : -1;
        if (equalsIndex >= 0) {
            return token.substring(0, equalsIndex);
        }
        if (token.startsWith("--")) {
            return token;
        }
        String name = "-" + token.charAt(1);
        return name;
    }

    private boolean isOptionToken(String token) {
        return token.length() > 1 && token.charAt(0) == '-';
    }

    @SuppressWarnings("unchecked")
    private static <T> T parseValue(ValueParser<?> parser, String value) throws ValueParseException {
        return ((ValueParser<T>) parser).parse(value);
    }

    private static List<String> copyTokens(List<String> tokens) {
        Objects.requireNonNull(tokens, "tokens");
        List<String> copiedTokens = new ArrayList<>(tokens.size());
        for (String token : tokens) {
            copiedTokens.add(Objects.requireNonNull(token, "tokens cannot contain null"));
        }
        return List.copyOf(copiedTokens);
    }

    private static String displayPath(CommandPath path) {
        return path.isRoot() ? "<root>" : path.toString();
    }

    private static ParseException usage(String message,
                                        CommandPath path,
                                        @Nullable String argumentName) {
        return new ParseException(CommandFailure.usage(message, path, argumentName));
    }

    record Controls(String helpCommand, List<String> helpOptions, List<String> versionOptions) {
        Controls {
            Objects.requireNonNull(helpCommand, "helpCommand");
            helpOptions = List.copyOf(helpOptions);
            versionOptions = List.copyOf(versionOptions);
        }
    }

    sealed interface ParseOutcome permits InvocationRequest, HelpRequest, VersionRequest, ParseFailure {
    }

    record InvocationRequest(CommandInvocation invocation, CommandHandler handler) implements ParseOutcome {
    }

    record HelpRequest(CommandPath path) implements ParseOutcome {
    }

    record VersionRequest() implements ParseOutcome {
    }

    record ParseFailure(CommandFailure failure) implements ParseOutcome {
    }

    private record OptionIndex(Map<String, OptionSpec<?>> byName) {
    }

    private static final class ParseException extends Exception {
        private final CommandFailure failure;

        private ParseException(CommandFailure failure) {
            this.failure = failure;
        }

        private CommandFailure failure() {
            return failure;
        }
    }
}
