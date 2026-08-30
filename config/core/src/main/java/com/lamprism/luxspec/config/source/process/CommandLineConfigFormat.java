package com.lamprism.luxspec.config.source.process;

import java.util.List;

/**
 * Parses one immutable command-line argument snapshot into ordered configuration assignments.
 *
 * <p>Built-in formats and transformations are exposed by this interface so custom and built-in
 * formats use the same composition model. Configuration key mapping remains a source setting
 * because it translates internal keys to the external names emitted by a format.</p>
 *
 * @author RollW
 */
@FunctionalInterface
public interface CommandLineConfigFormat {
    /**
     * Parses the supplied argument snapshot.
     *
     * @param arguments the immutable raw argument snapshot
     * @return assignments carrying their original argument positions
     */
    List<CommandLineConfigAssignment> parse(List<String> arguments);

    /**
     * Creates a format accepting only {@code --key=value} options.
     *
     * @return the inline option format
     */
    static CommandLineConfigFormat inline() {
        return CommandLineConfigFormatOperations.inline();
    }

    /**
     * Creates a format accepting {@code --key value} options and bare boolean flags.
     *
     * @return the separate value format
     */
    static CommandLineConfigFormat separate() {
        return CommandLineConfigFormatOperations.separate();
    }

    /**
     * Creates a format accepting inline and separate option values.
     *
     * @return the combined value format
     */
    static CommandLineConfigFormat inlineOrSeparate() {
        return allOf(inline(), separate());
    }

    /**
     * Selects assignments with one complete option name.
     *
     * @param optionName the complete option name without leading dashes
     * @return the name-selecting format
     */
    default CommandLineConfigFormat named(String optionName) {
        return CommandLineConfigFormatOperations.named(this, optionName);
    }

    /**
     * Excludes assignments with one complete option name.
     *
     * @param optionName the complete option name without leading dashes
     * @return the name-excluding format
     */
    default CommandLineConfigFormat excluding(String optionName) {
        return CommandLineConfigFormatOperations.excluding(this, optionName);
    }

    /**
     * Parses each explicit option value as a {@code key=value} assignment.
     *
     * <p>The first equals sign separates the inner name and value. An implicit outer value or an
     * inner assignment without a non-blank name is rejected.</p>
     *
     * @return the assignment-value format
     */
    default CommandLineConfigFormat keyValueAssignments() {
        return CommandLineConfigFormatOperations.keyValueAssignments(this);
    }

    /**
     * Composes this format with another additive format.
     *
     * <p>Assignments are merged by source argument position. The formats should recognize
     * non-overlapping representations unless duplicate assignments are intentional.</p>
     *
     * @param other the additional format
     * @return the composed format
     */
    default CommandLineConfigFormat and(CommandLineConfigFormat other) {
        return allOf(this, other);
    }

    /**
     * Additively composes formats and preserves source argument order.
     *
     * @param formats the formats in declaration order
     * @return the composed format
     */
    static CommandLineConfigFormat allOf(
            Iterable<? extends CommandLineConfigFormat> formats
    ) {
        return CommandLineConfigFormatOperations.allOf(formats);
    }

    /**
     * Additively composes formats and preserves source argument order.
     *
     * @param first      the first format
     * @param second     the second format
     * @param additional additional formats
     * @return the composed format
     */
    static CommandLineConfigFormat allOf(
            CommandLineConfigFormat first,
            CommandLineConfigFormat second,
            CommandLineConfigFormat... additional
    ) {
        return CommandLineConfigFormatOperations.allOf(first, second, additional);
    }
}
