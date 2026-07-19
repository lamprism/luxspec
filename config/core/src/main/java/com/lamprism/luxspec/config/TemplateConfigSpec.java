package com.lamprism.luxspec.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Defines a parameterized configuration path and binds it to complete keys.
 *
 * @param <T> the typed value
 * @author RollW
 */
public final class TemplateConfigSpec<T> {
    private final List<String> segments;
    private final Map<String, ConfigParameter> parameters;
    private final ConfigCodec<T> codec;
    private final Optional<T> defaultValue;
    private final boolean sensitive;
    private final Set<ConfigSourceCapability> requiredSourceCapabilities;
    private final Map<String, String> requiredSourceAttributes;
    private final Optional<ConfigSourceId> requiredSourceId;

    private TemplateConfigSpec(
            List<String> segments,
            Map<String, ConfigParameter> parameters,
            ConfigCodec<T> codec,
            Optional<T> defaultValue,
            boolean sensitive,
            Set<ConfigSourceCapability> requiredSourceCapabilities,
            Map<String, String> requiredSourceAttributes,
            @Nullable ConfigSourceId requiredSourceId
    ) {
        this.segments = segments;
        this.parameters = parameters;
        this.codec = codec;
        this.defaultValue = defaultValue;
        this.sensitive = sensitive;
        this.requiredSourceCapabilities = requiredSourceCapabilities;
        this.requiredSourceAttributes = requiredSourceAttributes;
        this.requiredSourceId = Optional.ofNullable(requiredSourceId);
    }

    /**
     * Creates a parameterized definition with default usable-source requirements.
     *
     * @param path the parameterized path
     * @param parameters declarations for every path parameter
     * @param codec the typed codec
     * @param defaultValue the optional fallback value
     * @param sensitive whether values must be hidden from ordinary diagnostics
     * @param <T> the typed value
     * @return the template definition
     */
    public static <T> TemplateConfigSpec<T> of(
            String path,
            List<ConfigParameter> parameters,
            ConfigCodec<T> codec,
            @Nullable T defaultValue,
            boolean sensitive
    ) {
        return of(path, parameters, codec, defaultValue, sensitive, defaultCapabilities(sensitive), Map.of(), null);
    }

    /**
     * Creates a parameterized definition with explicit usable-source requirements.
     *
     * @param path the parameterized path
     * @param parameters declarations for every path parameter
     * @param codec the typed codec
     * @param defaultValue the optional fallback value
     * @param sensitive whether values must be hidden from ordinary diagnostics
     * @param requiredSourceCapabilities required source capabilities
     * @param <T> the typed value
     * @return the template definition
     */
    public static <T> TemplateConfigSpec<T> of(
            String path,
            List<ConfigParameter> parameters,
            ConfigCodec<T> codec,
            @Nullable T defaultValue,
            boolean sensitive,
            Set<ConfigSourceCapability> requiredSourceCapabilities
    ) {
        return of(path, parameters, codec, defaultValue, sensitive, requiredSourceCapabilities, Map.of(), null);
    }

    /**
     * Creates a parameterized definition with complete source requirements.
     *
     * @param path the parameterized path
     * @param parameters declarations for every path parameter
     * @param codec the typed codec
     * @param defaultValue the optional fallback value
     * @param sensitive whether values must be hidden from ordinary diagnostics
     * @param requiredSourceCapabilities required source capabilities
     * @param requiredSourceAttributes required source attributes and values
     * @param requiredSourceId the optional exact source ID
     * @param <T> the typed value
     * @return the template definition
     */
    public static <T> TemplateConfigSpec<T> of(
            String path,
            List<ConfigParameter> parameters,
            ConfigCodec<T> codec,
            @Nullable T defaultValue,
            boolean sensitive,
            Set<ConfigSourceCapability> requiredSourceCapabilities,
            Map<String, String> requiredSourceAttributes,
            @Nullable ConfigSourceId requiredSourceId
    ) {
        List<String> segments = split(path);
        Map<String, ConfigParameter> indexed = index(parameters);
        validateSegments(segments, indexed);
        return new TemplateConfigSpec<>(
                List.copyOf(segments),
                Map.copyOf(indexed),
                Objects.requireNonNull(codec, "codec"),
                Optional.ofNullable(defaultValue),
                sensitive,
                validateCapabilities(sensitive, requiredSourceCapabilities),
                ConfigSourceConstraints.validateAttributes(requiredSourceAttributes),
                requiredSourceId
        );
    }

    /**
     * Binds all declared parameters into one complete configuration definition.
     *
     * @param arguments values for exactly the declared parameters
     * @return the validated complete binding
     */
    public BoundConfigSpec<T> bind(Map<String, String> arguments) {
        Map<String, String> values = Map.copyOf(arguments);
        if (!values.keySet().equals(parameters.keySet())) {
            throw new IllegalArgumentException("Template arguments do not match parameters");
        }
        List<String> boundSegments = new ArrayList<>();
        for (String segment : segments) {
            ConfigParameter parameter = parameter(segment);
            if (parameter == null) {
                boundSegments.add(segment);
            } else {
                String value = values.get(parameter.getName());
                parameter.validate(value);
                boundSegments.add(value);
            }
        }
        return new BoundConfigSpec<>(this, ConfigKey.of(String.join(".", boundSegments)), values);
    }

    /**
     * Returns the codec shared by every binding.
     *
     * @return the typed codec
     */
    public ConfigCodec<T> getCodec() {
        return codec;
    }

    /**
     * Returns the optional fallback shared by every binding.
     *
     * @return the optional default value
     */
    public Optional<T> getDefaultValue() {
        return defaultValue;
    }

    /**
     * Reports whether every binding is sensitive.
     *
     * @return {@code true} when values must remain hidden
     */
    public boolean isSensitive() {
        return sensitive;
    }

    /**
     * Returns capabilities required from a source that may serve this definition.
     *
     * @return immutable required source capabilities
     */
    public Set<ConfigSourceCapability> getRequiredSourceCapabilities() {
        return requiredSourceCapabilities;
    }

    /**
     * Returns attributes required from a usable source.
     *
     * @return immutable required source attributes
     */
    public Map<String, String> getRequiredSourceAttributes() {
        return requiredSourceAttributes;
    }

    /**
     * Returns the only source ID that may serve this template when one is configured.
     *
     * @return the optional exact source ID
     */
    public Optional<ConfigSourceId> getRequiredSourceId() {
        return requiredSourceId;
    }

    /**
     * Returns immutable literal and parameter path segments.
     *
     * @return the template path segments
     */
    public List<String> getSegments() {
        return segments;
    }

    Optional<BoundConfigSpec<T>> bind(ConfigKey key) {
        List<String> keySegments = split(key.getValue());
        if (keySegments.size() != segments.size()) {
            return Optional.empty();
        }
        Map<String, String> arguments = new LinkedHashMap<>();
        for (int index = 0; index < segments.size(); index++) {
            String templateSegment = segments.get(index);
            ConfigParameter parameter = parameter(templateSegment);
            String keySegment = keySegments.get(index);
            if (parameter == null) {
                if (!templateSegment.equals(keySegment)) {
                    return Optional.empty();
                }
            } else {
                try {
                    parameter.validate(keySegment);
                } catch (IllegalArgumentException exception) {
                    return Optional.empty();
                }
                arguments.put(parameter.getName(), keySegment);
            }
        }
        return Optional.of(bind(arguments));
    }

    int fixedSegmentCount() {
        int count = 0;
        for (String segment : segments) {
            if (parameter(segment) == null) {
                count++;
            }
        }
        return count;
    }

    private ConfigParameter parameter(String segment) {
        if (segment.length() > 2 && segment.charAt(0) == '{' && segment.charAt(segment.length() - 1) == '}') {
            return parameters.get(segment.substring(1, segment.length() - 1));
        }
        return null;
    }

    private static List<String> split(String path) {
        ConfigKey.of(path.replace('{', 'x').replace('}', 'x'));
        List<String> segments = new ArrayList<>();
        int segmentStart = 0;
        for (int index = 0; index < path.length(); index++) {
            if (path.charAt(index) == '.') {
                segments.add(path.substring(segmentStart, index));
                segmentStart = index + 1;
            }
        }
        segments.add(path.substring(segmentStart));
        return List.copyOf(segments);
    }

    private static Map<String, ConfigParameter> index(List<ConfigParameter> parameters) {
        Map<String, ConfigParameter> result = new LinkedHashMap<>();
        for (ConfigParameter parameter : parameters) {
            ConfigParameter nonNullParameter = Objects.requireNonNull(parameter, "parameter");
            if (result.put(nonNullParameter.getName(), nonNullParameter) != null) {
                throw new IllegalArgumentException("Duplicate template parameter");
            }
        }
        return result;
    }

    private static void validateSegments(List<String> segments, Map<String, ConfigParameter> parameters) {
        Set<String> used = new HashSet<>();
        for (String segment : segments) {
            if (segment.startsWith("{") && segment.endsWith("}")) {
                String name = segment.substring(1, segment.length() - 1);
                if (!parameters.containsKey(name) || !used.add(name)) {
                    throw new IllegalArgumentException("Template parameter declaration is invalid");
                }
            } else if (segment.indexOf('{') >= 0 || segment.indexOf('}') >= 0) {
                throw new IllegalArgumentException("Template parameters must occupy complete segments");
            }
        }
        if (!used.equals(parameters.keySet())) {
            throw new IllegalArgumentException("Unused template parameter declaration");
        }
    }

    private static Set<ConfigSourceCapability> defaultCapabilities(boolean sensitive) {
        if (sensitive) {
            return Set.of(ConfigSourceCapability.READ, ConfigSourceCapability.SECURE);
        }
        return Set.of(ConfigSourceCapability.READ);
    }

    private static Set<ConfigSourceCapability> validateCapabilities(
            boolean sensitive,
            Set<ConfigSourceCapability> capabilities
    ) {
        Set<ConfigSourceCapability> values = Set.copyOf(Objects.requireNonNull(capabilities, "requiredSourceCapabilities"));
        if (!values.contains(ConfigSourceCapability.READ)) {
            throw new IllegalArgumentException("Configuration source requirements must include READ");
        }
        if (sensitive && !values.contains(ConfigSourceCapability.SECURE)) {
            throw new IllegalArgumentException("Sensitive configuration requires a secure source");
        }
        return values;
    }
}
