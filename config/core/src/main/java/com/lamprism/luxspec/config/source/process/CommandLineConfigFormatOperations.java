package com.lamprism.luxspec.config.source.process;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

final class CommandLineConfigFormatOperations {
    private CommandLineConfigFormatOperations() {
    }

    static CommandLineConfigFormat inline() {
        return CommandLineConfigFormatOperations::parseInline;
    }

    static CommandLineConfigFormat separate() {
        return CommandLineConfigFormatOperations::parseSeparate;
    }

    static CommandLineConfigFormat named(
            CommandLineConfigFormat format,
            String optionName
    ) {
        String requiredName = ProcessConfigNames.requireName(optionName, "optionName");
        return filter(format, assignment -> assignment.name().equals(requiredName));
    }

    static CommandLineConfigFormat excluding(
            CommandLineConfigFormat format,
            String optionName
    ) {
        String excludedName = ProcessConfigNames.requireName(optionName, "optionName");
        return filter(format, assignment -> !assignment.name().equals(excludedName));
    }

    static CommandLineConfigFormat keyValueAssignments(CommandLineConfigFormat format) {
        CommandLineConfigFormat source = Objects.requireNonNull(format, "format");
        return arguments -> parseAssignments(source, arguments);
    }

    static CommandLineConfigFormat allOf(
            Iterable<? extends CommandLineConfigFormat> formats
    ) {
        List<CommandLineConfigFormat> values = new ArrayList<>();
        for (CommandLineConfigFormat format : Objects.requireNonNull(formats, "formats")) {
            values.add(Objects.requireNonNull(format, "format"));
        }
        return arguments -> parseAll(values, arguments);
    }

    static CommandLineConfigFormat allOf(
            CommandLineConfigFormat first,
            CommandLineConfigFormat second,
            CommandLineConfigFormat... additional
    ) {
        List<CommandLineConfigFormat> values = new ArrayList<>();
        values.add(Objects.requireNonNull(first, "first"));
        values.add(Objects.requireNonNull(second, "second"));
        for (CommandLineConfigFormat format : Objects.requireNonNull(additional, "additional")) {
            values.add(Objects.requireNonNull(format, "format"));
        }
        return allOf(values);
    }

    private static List<CommandLineConfigAssignment> parseInline(List<String> arguments) {
        List<String> values = requireArguments(arguments);
        List<CommandLineConfigAssignment> assignments = new ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            String argument = values.get(index);
            if (argument.equals("--")) {
                break;
            }
            if (!isLongOption(argument)) {
                continue;
            }
            String option = argument.substring(2);
            int separator = option.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            assignments.add(new CommandLineConfigAssignment(
                    index,
                    option.substring(0, separator),
                    option.substring(separator + 1),
                    true
            ));
        }
        return List.copyOf(assignments);
    }

    private static List<CommandLineConfigAssignment> parseSeparate(List<String> arguments) {
        List<String> values = requireArguments(arguments);
        List<CommandLineConfigAssignment> assignments = new ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            String argument = values.get(index);
            if (argument.equals("--")) {
                break;
            }
            if (!isLongOption(argument) || argument.indexOf('=') >= 0) {
                continue;
            }
            int argumentIndex = index;
            String name = argument.substring(2);
            String value = "true";
            boolean hasExplicitValue = false;
            if (index + 1 < values.size() && !values.get(index + 1).startsWith("--")) {
                value = values.get(++index);
                hasExplicitValue = true;
            }
            assignments.add(new CommandLineConfigAssignment(
                    argumentIndex,
                    name,
                    value,
                    hasExplicitValue
            ));
        }
        return List.copyOf(assignments);
    }

    private static List<CommandLineConfigAssignment> parseAll(
            List<CommandLineConfigFormat> formats,
            List<String> arguments
    ) {
        List<String> values = requireArguments(arguments);
        List<CommandLineConfigAssignment> assignments = new ArrayList<>();
        for (CommandLineConfigFormat format : formats) {
            assignments.addAll(parse(format, values));
        }
        assignments.sort(Comparator.comparingInt(CommandLineConfigAssignment::argumentIndex));
        return List.copyOf(assignments);
    }

    private static CommandLineConfigFormat filter(
            CommandLineConfigFormat format,
            Predicate<? super CommandLineConfigAssignment> predicate
    ) {
        CommandLineConfigFormat source = Objects.requireNonNull(format, "format");
        Predicate<? super CommandLineConfigAssignment> selector = Objects.requireNonNull(
                predicate,
                "predicate"
        );
        return arguments -> {
            List<CommandLineConfigAssignment> assignments = new ArrayList<>();
            for (CommandLineConfigAssignment assignment : parse(source, arguments)) {
                if (selector.test(assignment)) {
                    assignments.add(assignment);
                }
            }
            return List.copyOf(assignments);
        };
    }

    private static List<CommandLineConfigAssignment> parseAssignments(
            CommandLineConfigFormat format,
            List<String> arguments
    ) {
        List<CommandLineConfigAssignment> assignments = new ArrayList<>();
        for (CommandLineConfigAssignment assignment : parse(format, arguments)) {
            assignments.add(parseKeyValueAssignment(assignment));
        }
        return List.copyOf(assignments);
    }

    private static List<CommandLineConfigAssignment> parse(
            CommandLineConfigFormat format,
            List<String> arguments
    ) {
        List<CommandLineConfigAssignment> parsed = Objects.requireNonNull(
                Objects.requireNonNull(format, "format").parse(requireArguments(arguments)),
                "format assignments"
        );
        return List.copyOf(parsed);
    }

    private static CommandLineConfigAssignment parseKeyValueAssignment(
            CommandLineConfigAssignment outer
    ) {
        if (!outer.hasExplicitValue()) {
            throw new IllegalArgumentException("Option value requires a key=value assignment");
        }
        String assignment = outer.value();
        int separator = assignment.indexOf('=');
        if (separator <= 0 || assignment.substring(0, separator).isBlank()) {
            throw new IllegalArgumentException("Option value requires a key=value assignment");
        }
        return new CommandLineConfigAssignment(
                outer.argumentIndex(),
                assignment.substring(0, separator),
                assignment.substring(separator + 1),
                true
        );
    }

    private static List<String> requireArguments(List<String> arguments) {
        List<String> nonNullArguments = Objects.requireNonNull(arguments, "arguments");
        for (String argument : nonNullArguments) {
            Objects.requireNonNull(argument, "argument");
        }
        return nonNullArguments;
    }

    private static boolean isLongOption(String argument) {
        return argument.startsWith("--") && argument.length() > 2;
    }
}
