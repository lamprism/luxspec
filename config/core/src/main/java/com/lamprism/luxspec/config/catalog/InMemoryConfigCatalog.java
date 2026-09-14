package com.lamprism.luxspec.config.catalog;

import com.lamprism.luxspec.config.ConfigBinding;
import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.ConfigSpec;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Mutable in-memory configuration catalog for application assembly.
 *
 * <p>Definitions should be registered during application assembly. Lookup operations can then use
 * the catalog as a read-only definition directory.</p>
 *
 * @author RollW
 */
public class InMemoryConfigCatalog implements ConfigCatalogRegistry {
    private final Map<ConfigKey, ConfigSpec<?>> fixedSpecs = new HashMap<>();
    private final Map<Integer, List<ConfigSpec<?>>> parameterizedSpecs = new HashMap<>();
    private final List<ConfigSpec<?>> definitions = new ArrayList<>();

    /**
     * Creates an empty in-memory configuration catalog.
     */
    public InMemoryConfigCatalog() {
    }

    @Override
    public void register(ConfigSpec<?> spec) {
        ConfigSpec<?> nonNullSpec = Objects.requireNonNull(spec, "spec");
        if (!nonNullSpec.getKey().isParameterized()) {
            ConfigKey key = nonNullSpec.bind().getKey();
            if (fixedSpecs.putIfAbsent(key, nonNullSpec) != null) {
                throw new IllegalArgumentException("Duplicate fixed configuration key");
            }
            definitions.add(nonNullSpec);
            return;
        }
        List<ConfigSpec<?>> sameLength = parameterizedSpecs.computeIfAbsent(
                nonNullSpec.getKey().getSegments().size(),
                ignored -> new ArrayList<>()
        );
        for (ConfigSpec<?> existing : sameLength) {
            if (overlapsAtEqualSpecificity(existing, nonNullSpec)) {
                throw new IllegalArgumentException("Ambiguous configuration key expressions");
            }
        }
        sameLength.add(nonNullSpec);
        definitions.add(nonNullSpec);
    }

    @Override
    public List<ConfigSpec<?>> getDefinitions() {
        return List.copyOf(definitions);
    }

    @Override
    public @Nullable ConfigBinding<?> resolve(ConfigKey key) {
        ConfigKey nonNullKey = Objects.requireNonNull(key, "key");
        ConfigSpec<?> fixed = fixedSpecs.get(nonNullKey);
        if (fixed != null) {
            return fixed.bind();
        }
        ConfigBinding<?> best = null;
        List<ConfigSpec<?>> candidates = parameterizedSpecs.get(segmentCount(nonNullKey));
        if (candidates == null) {
            return null;
        }
        for (ConfigSpec<?> candidate : candidates) {
            ConfigBinding<?> binding = candidate.match(nonNullKey);
            if (binding != null && isMoreSpecific(binding, best)) {
                best = binding;
            }
        }
        return best;
    }

    private static boolean overlapsAtEqualSpecificity(ConfigSpec<?> left, ConfigSpec<?> right) {
        if (left.getKey().fixedSegmentCount() != right.getKey().fixedSegmentCount()
                || left.getKey().getSegments().size() != right.getKey().getSegments().size()) {
            return false;
        }
        List<String> leftSegments = left.getKey().getSegments();
        List<String> rightSegments = right.getKey().getSegments();
        for (int index = 0; index < leftSegments.size(); index++) {
            String leftSegment = leftSegments.get(index);
            String rightSegment = rightSegments.get(index);
            if (!isParameter(leftSegment) && !isParameter(rightSegment) && !leftSegment.equals(rightSegment)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isMoreSpecific(ConfigBinding<?> candidate, @Nullable ConfigBinding<?> current) {
        return current == null
                || candidate.getSpec().getKey().fixedSegmentCount()
                > current.getSpec().getKey().fixedSegmentCount();
    }

    private static boolean isParameter(String segment) {
        return segment.length() > 2
                && segment.charAt(0) == '{'
                && segment.charAt(segment.length() - 1) == '}';
    }

    private static int segmentCount(ConfigKey key) {
        int segments = 1;
        String value = key.getValue();
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) == '.') {
                segments++;
            }
        }
        return segments;
    }
}
