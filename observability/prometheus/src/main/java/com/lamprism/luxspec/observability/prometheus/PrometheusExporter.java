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

package com.lamprism.luxspec.observability.prometheus;

import com.lamprism.luxspec.observability.metric.MetricKind;
import com.lamprism.luxspec.observability.metric.MetricReading;
import com.lamprism.luxspec.observability.metric.MetricRegistry;
import com.lamprism.luxspec.observability.metric.MetricSnapshot;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Renders one on-demand Luxspec metric snapshot as Prometheus text exposition.
 *
 * <p>Semantic metric names are normalized by replacing characters outside
 * {@code [A-Za-z0-9_:]} with underscores and prefixing a leading digit with an
 * underscore. Label names use {@code [A-Za-z0-9_]} and must start with a letter
 * or underscore after normalization. Normalization collisions and the reserved
 * histogram label {@code le} are rejected instead of merged silently.
 *
 * @author RollW
 */
public class PrometheusExporter {
    private final MetricRegistry registry;

    public PrometheusExporter(MetricRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    /**
     * Collects one snapshot and renders it without rereading value sources.
     *
     * @return Prometheus text exposition
     */
    public String scrape() {
        MetricSnapshot snapshot = registry.snapshot();
        List<MetricFamily> families = plan(snapshot);
        StringBuilder output = new StringBuilder();
        for (MetricFamily family : families) {
            render(family, output);
        }
        return output.toString();
    }

    public String contentType() {
        return "text/plain; version=0.0.4; charset=utf-8";
    }

    private static List<MetricFamily> plan(MetricSnapshot snapshot) {
        Map<String, MetricFamily> families = new HashMap<>();
        for (MetricReading reading : snapshot.readings()) {
            String semanticName = reading.binding().getSpec().getName().value();
            String name = normalize(semanticName);
            MetricFamily family = families.get(name);
            if (family == null) {
                family = new MetricFamily(name, semanticName, reading.kind());
                families.put(name, family);
            } else if (!family.semanticName.equals(semanticName) || !family.kind.equals(reading.kind())) {
                throw new PrometheusExportException("Prometheus metric name collision: " + name);
            }
            family.readings.add(reading);
        }
        List<MetricFamily> result = new ArrayList<>(families.values());
        result.sort(Comparator.comparing(family -> family.name));
        for (MetricFamily family : result) {
            family.readings.sort(Comparator.comparing(PrometheusExporter::dimensionOrder));
        }
        return result;
    }

    private static void render(MetricFamily family, StringBuilder output) {
        MetricReading first = family.readings.get(0);
        output.append("# HELP ").append(family.name).append(' ')
                .append(escapeHelp(first.binding().getSpec().getDescription()))
                .append('\n');
        if (family.readings.stream().anyMatch(PrometheusExporter::isHistogram)) {
            output.append("# TYPE ").append(family.name).append(" histogram\n");
            for (MetricReading reading : family.readings) {
                renderHistogram(family.name, reading, output);
            }
            return;
        }
        output.append("# TYPE ").append(family.name).append(' ')
                .append(typeOf(first)).append('\n');
        for (MetricReading reading : family.readings) {
            renderSamples(family.name, reading, output);
        }
    }

    private static void renderSamples(String name, MetricReading reading, StringBuilder output) {
        Double value = reading.value();
        if (value != null) {
            sample(output, name, reading.binding().getDimensions(), value);
        }
        Long count = reading.count();
        if (count != null && isCountFamily(reading)) {
            sample(output, name + "_count", reading.binding().getDimensions(), count);
        }
        Duration totalTime = reading.totalTime();
        if (totalTime != null) {
            String sampleName = reading.kind().equals(MetricKind.TIME_GAUGE) ? name : name + "_sum";
            sample(output, sampleName, reading.binding().getDimensions(), seconds(totalTime));
        }
        Double total = reading.total();
        if (total != null) {
            sample(output, name + "_sum", reading.binding().getDimensions(), total);
        }
        Double max = reading.max();
        if (max != null) {
            sample(output, name + "_max", reading.binding().getDimensions(), max);
        }
        Long activeTasks = reading.activeTasks();
        if (activeTasks != null) {
            sample(output, name + "_active_tasks", reading.binding().getDimensions(), activeTasks);
        }
        Duration activeDuration = reading.activeDuration();
        if (activeDuration != null) {
            sample(
                    output,
                    name + "_duration_seconds",
                    reading.binding().getDimensions(),
                    seconds(activeDuration)
            );
        }
    }

    private static void renderHistogram(String name, MetricReading reading, StringBuilder output) {
        Map<String, String> baseLabels = reading.binding().getDimensions();
        if (containsNormalizedLabel(baseLabels, "le")) {
            throw new PrometheusExportException("Prometheus histogram dimensions must not contain le");
        }
        List<Map.Entry<Double, Long>> buckets = new ArrayList<>(reading.histogram().entrySet());
        buckets.sort(Map.Entry.comparingByKey());
        for (Map.Entry<Double, Long> bucket : buckets) {
            Map<String, String> labels = new LinkedHashMap<>(baseLabels);
            labels.put("le", Double.toString(bucket.getKey()));
            sample(output, name + "_bucket", labels, bucket.getValue());
        }
        Long count = reading.count();
        if (count != null) {
            Map<String, String> infiniteBucket = new LinkedHashMap<>(baseLabels);
            infiniteBucket.put("le", "+Inf");
            sample(output, name + "_bucket", infiniteBucket, count);
            sample(output, name + "_count", baseLabels, count);
        }
        Duration totalTime = reading.totalTime();
        if (totalTime != null) {
            sample(output, name + "_sum", baseLabels, seconds(totalTime));
        }
        Double total = reading.total();
        if (total != null) {
            sample(output, name + "_sum", baseLabels, total);
        }
    }

    private static boolean isHistogram(MetricReading reading) {
        return !reading.histogram().isEmpty();
    }

    private static boolean isCountFamily(MetricReading reading) {
        return reading.kind().equals(MetricKind.TIMER)
                || reading.kind().equals(MetricKind.DISTRIBUTION_SUMMARY)
                || reading.kind().equals(MetricKind.FUNCTION_TIMER);
    }

    private static String typeOf(MetricReading reading) {
        if (reading.kind().equals(MetricKind.COUNTER) || reading.kind().equals(MetricKind.FUNCTION_COUNTER)) {
            return "counter";
        }
        if (isCountFamily(reading)) {
            return "summary";
        }
        return "gauge";
    }

    private static void sample(StringBuilder output, String name, Map<String, String> labels, Object value) {
        output.append(name);
        appendLabels(output, labels);
        output.append(' ').append(value).append('\n');
    }

    private static void appendLabels(StringBuilder output, Map<String, String> labels) {
        if (labels.isEmpty()) {
            return;
        }
        List<String> names = new ArrayList<>(labels.keySet());
        names.sort(String::compareTo);
        Set<String> normalizedNames = new HashSet<>();
        output.append('{');
        for (int index = 0; index < names.size(); index++) {
            if (index > 0) {
                output.append(',');
            }
            String name = names.get(index);
            String normalizedName = normalizeLabel(name);
            if (!normalizedNames.add(normalizedName)) {
                throw new PrometheusExportException("Prometheus label name collision: " + normalizedName);
            }
            String value = Objects.requireNonNull(labels.get(name), "label value");
            output.append(normalizedName).append("=\"")
                    .append(escapeLabel(value)).append('"');
        }
        output.append('}');
    }

    private static String dimensionOrder(MetricReading reading) {
        List<String> dimensions = new ArrayList<>();
        for (Map.Entry<String, String> dimension : reading.binding().getDimensions().entrySet()) {
            dimensions.add(dimension.getKey() + "=" + dimension.getValue());
        }
        dimensions.sort(String::compareTo);
        return String.join("\u0000", dimensions);
    }

    private static String normalize(String name) {
        StringBuilder normalized = new StringBuilder(name.length());
        for (int index = 0; index < name.length(); index++) {
            char character = name.charAt(index);
            if (isPrometheusNameCharacter(character)) {
                normalized.append(character);
            } else {
                normalized.append('_');
            }
        }
        if (normalized.isEmpty()) {
            throw new PrometheusExportException("Prometheus metric name must not be empty");
        }
        if (Character.isDigit(normalized.charAt(0))) {
            normalized.insert(0, '_');
        }
        return normalized.toString();
    }

    private static String normalizeLabel(String name) {
        if (name.isEmpty()) {
            throw new PrometheusExportException("Prometheus label name must not be empty");
        }
        StringBuilder normalized = new StringBuilder(name.length());
        for (int index = 0; index < name.length(); index++) {
            char character = name.charAt(index);
            if (character == '_' || character >= 'a' && character <= 'z'
                    || character >= 'A' && character <= 'Z'
                    || character >= '0' && character <= '9') {
                normalized.append(character);
            } else {
                normalized.append('_');
            }
        }
        if (normalized.isEmpty() || Character.isDigit(normalized.charAt(0))) {
            throw new PrometheusExportException("Prometheus label name must start with a letter or underscore: " + name);
        }
        return normalized.toString();
    }

    private static boolean containsNormalizedLabel(Map<String, String> labels, String expected) {
        for (String label : labels.keySet()) {
            if (normalizeLabel(label).equals(expected)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPrometheusNameCharacter(char character) {
        return character == '_' || character == ':' || character >= 'a' && character <= 'z'
                || character >= 'A' && character <= 'Z' || character >= '0' && character <= '9';
    }

    private static String escapeLabel(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '\\') {
                escaped.append("\\\\");
            } else if (character == '"') {
                escaped.append("\\\"");
            } else if (character == '\n') {
                escaped.append("\\n");
            } else {
                escaped.append(character);
            }
        }
        return escaped.toString();
    }

    private static String escapeHelp(@Nullable Object description) {
        if (description == null) {
            return "Luxspec metric";
        }
        return escapeLabel(description.toString());
    }

    private static double seconds(Duration duration) {
        return duration.getSeconds() + duration.getNano() / 1_000_000_000.0d;
    }

    private static final class MetricFamily {
        private final String name;
        private final String semanticName;
        private final MetricKind<?> kind;
        private final List<MetricReading> readings = new ArrayList<>();

        private MetricFamily(String name, String semanticName, MetricKind<?> kind) {
            this.name = name;
            this.semanticName = semanticName;
            this.kind = kind;
        }
    }
}
