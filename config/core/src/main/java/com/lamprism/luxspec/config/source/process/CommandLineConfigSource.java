package com.lamprism.luxspec.config.source.process;

import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.source.ConfigEntry;
import com.lamprism.luxspec.config.source.ConfigSource;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.ConfigSourceScope;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Reads long-form configuration options from a command-line argument snapshot.
 *
 * <p>By default, options use either {@code --key=value} or {@code --key value}. A flag without a
 * following value is represented as the string {@code true}. Repeated options are retained as a
 * raw list. Positional arguments are ignored. A {@link CommandLineConfigFormat} owns alternative
 * representations, while the source builder may map a configuration key to a different complete
 * external name.</p>
 *
 * <p>Alternative representations can be assembled from the selection, exclusion, value
 * transformation, and additive composition operations in {@link CommandLineConfigFormat}.</p>
 *
 * @author RollW
 */
public final class CommandLineConfigSource implements ConfigSource {
    private static final ConfigSourceId DEFAULT_ID = ConfigSourceId.of("command-line");
    private final ConfigSourceId id;
    private final Map<String, ConfigEntry> entries;
    private final Function<? super ConfigKey, String> nameMapper;

    /**
     * Creates a source with the default ID and parsing settings.
     *
     * @param arguments raw process arguments
     * @return the command-line source
     */
    public static CommandLineConfigSource from(Iterable<String> arguments) {
        return builder(arguments).build();
    }

    /**
     * Creates a source with the default ID and parsing settings.
     *
     * @param arguments raw process arguments
     * @return the command-line source
     */
    public static CommandLineConfigSource from(String... arguments) {
        return from(List.of(Objects.requireNonNull(arguments, "arguments")));
    }

    /**
     * Creates a builder from an immutable snapshot of the supplied arguments.
     *
     * @param arguments raw process arguments
     * @return the source builder
     */
    public static Builder builder(Iterable<String> arguments) {
        return new Builder(arguments);
    }

    /**
     * Creates a builder from an immutable snapshot of the supplied arguments.
     *
     * @param arguments raw process arguments
     * @return the source builder
     */
    public static Builder builder(String... arguments) {
        return builder(List.of(Objects.requireNonNull(arguments, "arguments")));
    }

    private CommandLineConfigSource(
            ConfigSourceId id,
            List<String> arguments,
            CommandLineConfigFormat format,
            Function<? super ConfigKey, String> nameMapper
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.entries = parse(arguments, format);
        this.nameMapper = Objects.requireNonNull(nameMapper, "nameMapper");
    }

    @Override
    public ConfigSourceId getId() {
        return id;
    }

    @Override
    public ConfigSourceScope getScope() {
        return ConfigSourceScope.BOOTSTRAP;
    }

    @Override
    public ConfigEntry get(ConfigKey key) {
        return entries.getOrDefault(
                ProcessConfigName.map(nameMapper, key),
                ConfigEntry.absent()
        );
    }

    private static Map<String, ConfigEntry> parse(
            List<String> arguments,
            CommandLineConfigFormat format
    ) {
        List<CommandLineConfigAssignment> assignments = new ArrayList<>(Objects.requireNonNull(
                Objects.requireNonNull(format, "format").parse(arguments),
                "format assignments"
        ));
        assignments.sort(Comparator.comparingInt(CommandLineConfigAssignment::argumentIndex));
        Map<String, List<String>> parsed = new LinkedHashMap<>();
        for (CommandLineConfigAssignment assignment : assignments) {
            CommandLineConfigAssignment nonNullAssignment = Objects.requireNonNull(
                    assignment,
                    "assignment"
            );
            if (nonNullAssignment.argumentIndex() >= arguments.size()) {
                throw new IllegalArgumentException("Assignment argument index is outside the snapshot");
            }
            addValue(parsed, nonNullAssignment.name(), nonNullAssignment.value());
        }
        Map<String, ConfigEntry> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : parsed.entrySet()) {
            List<String> optionValues = entry.getValue();
            ConfigEntry configEntry = optionValues.size() == 1
                    ? ConfigEntry.present(optionValues.get(0))
                    : ConfigEntry.present(optionValues);
            result.put(entry.getKey(), configEntry);
        }
        return Map.copyOf(result);
    }

    private static void addValue(Map<String, List<String>> parsed, String name, String value) {
        parsed.computeIfAbsent(name, ignored -> new ArrayList<>()).add(value);
    }

    /**
     * Builds one command-line source from a fixed argument snapshot.
     */
    public static final class Builder {
        private final List<String> arguments;
        private ConfigSourceId id = DEFAULT_ID;
        private CommandLineConfigFormat format = CommandLineConfigFormat.inlineOrSeparate();
        private Function<? super ConfigKey, String> nameMapper = ConfigKey::getValue;

        private Builder(Iterable<String> arguments) {
            List<String> values = new ArrayList<>();
            for (String argument : Objects.requireNonNull(arguments, "arguments")) {
                values.add(Objects.requireNonNull(argument, "argument"));
            }
            this.arguments = List.copyOf(values);
        }

        /**
         * Sets the source instance identifier.
         *
         * @param id the source instance identifier
         * @return this builder
         */
        public Builder id(ConfigSourceId id) {
            this.id = Objects.requireNonNull(id, "id");
            return this;
        }

        /**
         * Sets the complete command-line representation format.
         *
         * @param format the representation format
         * @return this builder
         */
        public Builder format(CommandLineConfigFormat format) {
            this.format = Objects.requireNonNull(format, "format");
            return this;
        }

        /**
         * Sets the complete external key-name mapping used to retrieve parsed assignments.
         *
         * @param nameMapper the external key-name mapping
         * @return this builder
         */
        public Builder nameMapper(Function<? super ConfigKey, String> nameMapper) {
            this.nameMapper = Objects.requireNonNull(nameMapper, "nameMapper");
            return this;
        }

        /**
         * Adds literal affixes around the default configuration key name.
         *
         * @param prefix the literal prefix
         * @param suffix the literal suffix
         * @return this builder
         */
        public Builder nameAffixes(String prefix, String suffix) {
            this.nameMapper = ProcessConfigName.affixed(ConfigKey::getValue, prefix, suffix);
            return this;
        }

        /**
         * Creates the command-line source.
         *
         * @return the command-line source
         */
        public CommandLineConfigSource build() {
            return new CommandLineConfigSource(id, arguments, format, nameMapper);
        }
    }
}
