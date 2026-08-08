package com.lamprism.luxspec.config.catalog;

import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.ConfigParameter;
import com.lamprism.luxspec.config.ConfigSpec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * Browses fixed definitions and definitions whose every parameter has a finite allowed domain.
 *
 * <p>Definitions with an unconstrained parameter produce no keys because this browser cannot
 * infer that parameter's values. Another browser may provide those values when the browsers are
 * composed.</p>
 *
 * @author RollW
 */
public class FiniteConfigKeyBrowser implements ConfigKeyBrowser {

    @Override
    public boolean supports(ConfigSpec<?> spec) {
        ConfigSpec<?> nonNullSpec = Objects.requireNonNull(spec, "spec");
        ConfigKey key = nonNullSpec.getKey();
        if (!key.isParameterized()) {
            return true;
        }
        return hasFiniteDomain(parameterOrder(key));
    }

    @Override
    public Iterable<ConfigKey> browse(ConfigSpec<?> spec) {
        ConfigSpec<?> nonNullSpec = Objects.requireNonNull(spec, "spec");
        ConfigKey key = nonNullSpec.getKey();
        if (!key.isParameterized()) {
            return List.of(key);
        }
        List<ConfigParameter> parameters = parameterOrder(key);
        if (!hasFiniteDomain(parameters)) {
            return List.of();
        }
        return new CartesianKeyIterable(nonNullSpec, parameters);
    }

    private static boolean hasFiniteDomain(List<ConfigParameter> parameters) {
        for (ConfigParameter parameter : parameters) {
            if (parameter.getAllowedValues().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static List<ConfigParameter> parameterOrder(ConfigKey key) {
        Map<String, ConfigParameter> declarations = key.getParameters();
        List<ConfigParameter> parameters = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (String segment : key.getSegments()) {
            if (!isParameterSegment(segment)) {
                continue;
            }
            String name = segment.substring(1, segment.length() - 1);
            if (names.add(name)) {
                parameters.add(Objects.requireNonNull(declarations.get(name), "parameter"));
            }
        }
        return List.copyOf(parameters);
    }

    private static boolean isParameterSegment(String segment) {
        return segment.length() > 2
                && segment.charAt(0) == '{'
                && segment.charAt(segment.length() - 1) == '}';
    }

    private static final class CartesianKeyIterable implements Iterable<ConfigKey> {
        private final ConfigSpec<?> spec;
        private final List<ConfigParameter> parameters;
        private final List<List<String>> values;

        private CartesianKeyIterable(ConfigSpec<?> spec, List<ConfigParameter> parameters) {
            this.spec = spec;
            this.parameters = parameters;
            List<List<String>> parameterValues = new ArrayList<>();
            for (ConfigParameter parameter : parameters) {
                List<String> sortedValues = new ArrayList<>(parameter.getAllowedValues());
                sortedValues.sort(String::compareTo);
                parameterValues.add(List.copyOf(sortedValues));
            }
            this.values = List.copyOf(parameterValues);
        }

        @Override
        public Iterator<ConfigKey> iterator() {
            return new CartesianKeyIterator(spec, parameters, values);
        }
    }

    private static final class CartesianKeyIterator implements Iterator<ConfigKey> {
        private final ConfigSpec<?> spec;
        private final List<ConfigParameter> parameters;
        private final List<List<String>> values;
        private final int[] indexes;
        private boolean hasNext = true;

        private CartesianKeyIterator(
                ConfigSpec<?> spec,
                List<ConfigParameter> parameters,
                List<List<String>> values
        ) {
            this.spec = spec;
            this.parameters = parameters;
            this.values = values;
            this.indexes = new int[parameters.size()];
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public ConfigKey next() {
            if (!hasNext) {
                throw new NoSuchElementException();
            }
            ConfigKey key = spec.bind(arguments()).getKey();
            advance();
            return key;
        }

        private Map<String, String> arguments() {
            Map<String, String> arguments = new LinkedHashMap<>();
            for (int index = 0; index < parameters.size(); index++) {
                arguments.put(
                        parameters.get(index).getName(),
                        values.get(index).get(indexes[index])
                );
            }
            return Map.copyOf(arguments);
        }

        private void advance() {
            for (int index = indexes.length - 1; index >= 0; index--) {
                indexes[index]++;
                if (indexes[index] < values.get(index).size()) {
                    return;
                }
                indexes[index] = 0;
            }
            hasNext = false;
        }
    }
}
