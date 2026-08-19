package com.lamprism.luxspec.config.source.process;

import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.source.ConfigEntry;
import com.lamprism.luxspec.config.source.ConfigSource;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.ConfigSourceScope;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reads long-form configuration options from a command-line argument snapshot.
 *
 * <p>Options use either {@code --key=value} or {@code --key value}. A flag without a following
 * value is represented as the string {@code true}. Repeated options are retained as a raw list.
 * Positional arguments and options with an unsupported configuration-key shape are ignored.</p>
 *
 * @author RollW
 */
public class CommandLineConfigSource implements ConfigSource {
    private static final ConfigSourceId DEFAULT_ID = ConfigSourceId.of("command-line");
    private final ConfigSourceId id;
    private final Map<String, ConfigEntry> entries;

    /**
     * Creates an empty command-line source.
     */
    public CommandLineConfigSource() {
        this(DEFAULT_ID, List.of());
    }

    /**
     * Creates a command-line source from the supplied argument snapshot.
     *
     * @param id        the source instance identifier
     * @param arguments raw process arguments
     */
    public CommandLineConfigSource(
            ConfigSourceId id,
            Iterable<String> arguments
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.entries = parse(Objects.requireNonNull(arguments, "arguments"));
    }

    /**
     * Creates a command-line source from a raw argument array.
     *
     * @param id        the source instance identifier
     * @param arguments raw process arguments
     */
    public CommandLineConfigSource(
            ConfigSourceId id,
            String... arguments
    ) {
        this(id, List.of(Objects.requireNonNull(arguments, "arguments")));
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
                Objects.requireNonNull(key, "key").getValue(),
                ConfigEntry.absent()
        );
    }

    private static Map<String, ConfigEntry> parse(Iterable<String> arguments) {
        List<String> values = new ArrayList<>();
        for (String argument : arguments) {
            values.add(Objects.requireNonNull(argument, "argument"));
        }

        Map<String, List<String>> parsed = new LinkedHashMap<>();
        for (int index = 0; index < values.size(); index++) {
            String argument = values.get(index);
            if (argument.equals("--")) {
                break;
            }
            if (!argument.startsWith("--") || argument.length() == 2) {
                continue;
            }
            String option = argument.substring(2);
            int separator = option.indexOf('=');
            String name;
            String value;
            if (separator >= 0) {
                name = option.substring(0, separator);
                value = option.substring(separator + 1);
            } else {
                name = option;
                if (index + 1 < values.size() && !values.get(index + 1).startsWith("--")) {
                    value = values.get(++index);
                } else {
                    value = "true";
                }
            }
            if (name.isBlank()) {
                continue;
            }
            if (!isCompleteKey(name)) {
                continue;
            }
            parsed.computeIfAbsent(name, ignored -> new ArrayList<>()).add(value);
        }

        Map<String, ConfigEntry> entries = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : parsed.entrySet()) {
            List<String> optionValues = entry.getValue();
            ConfigEntry configEntry = optionValues.size() == 1
                    ? ConfigEntry.present(optionValues.get(0))
                    : ConfigEntry.present(optionValues);
            entries.put(entry.getKey(), configEntry);
        }
        return Map.copyOf(entries);
    }

    private static boolean isCompleteKey(String value) {
        try {
            ConfigKey.of(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
