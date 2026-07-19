package com.lamprism.luxspec.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Registers fixed and template configuration definitions and resolves complete keys.
 *
 * @author RollW
 */
public final class ConfigCatalog {
    private final Map<ConfigKey, ConfigSpec<?>> fixedSpecs = new HashMap<>();
    private final Map<Integer, List<TemplateConfigSpec<?>>> templatesBySegmentCount = new HashMap<>();

    /**
     * Registers a fixed or already bound definition by its complete key.
     *
     * @param spec the complete configuration definition
     */
    public void register(ConfigSpec<?> spec) {
        ConfigSpec<?> nonNullSpec = Objects.requireNonNull(spec, "spec");
        if (fixedSpecs.putIfAbsent(nonNullSpec.getKey(), nonNullSpec) != null) {
            throw new IllegalArgumentException("Duplicate fixed configuration key");
        }
    }

    /**
     * Registers a reusable parameterized definition after ambiguity validation.
     *
     * @param template the parameterized definition
     */
    public void register(TemplateConfigSpec<?> template) {
        TemplateConfigSpec<?> nonNullTemplate = Objects.requireNonNull(template, "template");
        List<TemplateConfigSpec<?>> sameLength = templatesBySegmentCount.computeIfAbsent(
                nonNullTemplate.getSegments().size(),
                ignored -> new ArrayList<>()
        );
        for (TemplateConfigSpec<?> existing : sameLength) {
            if (overlapsAtEqualSpecificity(existing, nonNullTemplate)) {
                throw new IllegalArgumentException("Ambiguous configuration templates");
            }
        }
        sameLength.add(nonNullTemplate);
    }

    /**
     * Resolves one complete key to a fixed or validated template binding.
     *
     * @param key the complete configuration key
     * @return the matching definition, when registered
     */
    public Optional<ConfigSpec<?>> resolve(ConfigKey key) {
        ConfigSpec<?> fixed = fixedSpecs.get(Objects.requireNonNull(key, "key"));
        if (fixed != null) {
            return Optional.of(fixed);
        }
        BoundConfigSpec<?> best = null;
        List<TemplateConfigSpec<?>> candidates = templatesBySegmentCount.get(keySegmentCount(key));
        if (candidates == null) {
            return Optional.empty();
        }
        for (TemplateConfigSpec<?> candidate : candidates) {
            Optional<? extends BoundConfigSpec<?>> bound = candidate.bind(key);
            if (bound.isPresent() && isMoreSpecific(bound.get(), best)) {
                best = bound.get();
            }
        }
        return Optional.ofNullable(best);
    }

    private static boolean overlapsAtEqualSpecificity(TemplateConfigSpec<?> left, TemplateConfigSpec<?> right) {
        if (left.fixedSegmentCount() != right.fixedSegmentCount() || left.getSegments().size() != right.getSegments().size()) {
            return false;
        }
        for (int index = 0; index < left.getSegments().size(); index++) {
            String leftSegment = left.getSegments().get(index);
            String rightSegment = right.getSegments().get(index);
            if (!isParameter(leftSegment) && !isParameter(rightSegment) && !leftSegment.equals(rightSegment)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isParameter(String segment) {
        return segment.length() > 2 && segment.charAt(0) == '{' && segment.charAt(segment.length() - 1) == '}';
    }

    private static boolean isMoreSpecific(BoundConfigSpec<?> candidate, BoundConfigSpec<?> current) {
        return current == null || candidate.getTemplate().fixedSegmentCount() > current.getTemplate().fixedSegmentCount();
    }

    private static int keySegmentCount(ConfigKey key) {
        int count = 1;
        String value = key.getValue();
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) == '.') {
                count++;
            }
        }
        return count;
    }
}
