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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Standard configurable plain-text Help renderer.
 *
 * @author RollW
 */
public final class PlainTextHelpRenderer implements HelpRenderer {
    private final HelpFormat format;

    /**
     * Creates a renderer with a formatting policy.
     *
     * @param format plain-text formatting policy
     */
    public PlainTextHelpRenderer(HelpFormat format) {
        this.format = Objects.requireNonNull(format, "format");
    }

    /**
     * @return the format used by this renderer
     */
    public HelpFormat getFormat() {
        return format;
    }

    @Override
    public String render(HelpDocument document) {
        Objects.requireNonNull(document, "document");
        List<String> lines = new ArrayList<>();
        appendHeader(lines, document.getApplicationHeader());
        appendHeader(lines, document.getCommandHeader());
        appendUsage(lines, document.getUsage());
        appendDescription(lines, document.getSummary(), document.getDescription());
        appendAliases(lines, document.getCommandAliases());
        appendCommands(lines, document.getCommands());
        appendArguments(lines, document.getArguments());
        appendOptions(lines, document.getOptions());
        appendExamples(lines, document.getExamples());
        appendNotes(lines, document.getNotes());
        appendFooter(lines, document.getFooter());
        return String.join("\n", trimTrailingBlankLines(lines));
    }

    private void appendHeader(List<String> lines, String header) {
        if (header == null) {
            return;
        }
        appendParagraph(lines, header, 0);
        appendBlankLine(lines);
    }

    private void appendUsage(List<String> lines, String usage) {
        appendWrappedLine(lines, format.getUsageHeading() + ": " + usage, 0);
        appendBlankLine(lines);
    }

    private void appendDescription(List<String> lines, String summary, String description) {
        if (summary != null) {
            appendParagraph(lines, summary, 0);
        }
        if (description != null && !description.equals(summary)) {
            if (summary != null) {
                appendBlankLine(lines);
            }
            appendParagraph(lines, description, 0);
        }
        if (summary != null || description != null) {
            appendBlankLine(lines);
        }
    }

    private void appendAliases(List<String> lines, List<String> aliases) {
        if (!format.isShowAliases() || aliases.isEmpty()) {
            return;
        }
        appendWrappedLine(lines, "Aliases: " + String.join(", ", aliases), 0);
        appendBlankLine(lines);
    }

    private void appendCommands(List<String> lines, List<HelpDocument.CommandEntry> commands) {
        Map<String, List<HelpDocument.CommandEntry>> grouped = new LinkedHashMap<>();
        for (HelpDocument.CommandEntry command : commands) {
            if (!format.isShowHidden() && command.isHidden()) {
                continue;
            }
            String group = command.getGroup() == null ? format.getCommandsHeading() : command.getGroup();
            grouped.computeIfAbsent(group, ignored -> new ArrayList<>()).add(command);
        }
        for (Map.Entry<String, List<HelpDocument.CommandEntry>> entry : grouped.entrySet()) {
            appendSectionHeading(lines, entry.getKey());
            List<Row> rows = new ArrayList<>();
            for (HelpDocument.CommandEntry command : entry.getValue()) {
                String label = command.getName();
                if (format.isShowAliases() && !command.getAliases().isEmpty()) {
                    label += " (" + String.join(", ", command.getAliases()) + ")";
                }
                rows.add(new Row(label, command.getSummary()));
            }
            appendRows(lines, rows);
            appendBlankLine(lines);
        }
    }

    private void appendArguments(List<String> lines, List<HelpDocument.ArgumentEntry> arguments) {
        List<Row> rows = new ArrayList<>();
        for (HelpDocument.ArgumentEntry argument : arguments) {
            if (!format.isShowHidden() && argument.isHidden()) {
                continue;
            }
            String label = argumentLabel(argument);
            String details = argument.getDescription();
            details = appendMetadata(details, argument.getMinimumValues() > 0,
                    argument.getDefaultValues(), argument.getMaximumValues() > 1);
            rows.add(new Row(label, details));
        }
        appendSection(lines, format.getArgumentsHeading(), rows);
    }

    private void appendOptions(List<String> lines, List<HelpDocument.OptionEntry> options) {
        List<Row> rows = new ArrayList<>();
        for (HelpDocument.OptionEntry option : options) {
            if (!format.isShowHidden() && option.isHidden()) {
                continue;
            }
            String label = String.join(", ", option.getNames());
            if (!option.isFlag()) {
                String valueLabel = option.getValueLabel() == null ? "VALUE" : option.getValueLabel();
                label += " <" + valueLabel + ">";
            }
            String details = option.getDescription();
            details = appendMetadata(details, option.isRequired(), option.getDefaultValues(), option.isRepeatable());
            rows.add(new Row(label, details));
        }
        appendSection(lines, format.getOptionsHeading(), rows);
    }

    private void appendExamples(List<String> lines, List<CommandDocumentation.Example> examples) {
        if (examples.isEmpty()) {
            return;
        }
        appendSectionHeading(lines, format.getExamplesHeading());
        for (CommandDocumentation.Example example : examples) {
            appendWrappedLine(lines, example.getInvocation(), format.getItemIndent());
            if (example.getDescription() != null) {
                appendParagraph(lines, example.getDescription(), format.getItemIndent() + format.getDescriptionGap());
            }
        }
        appendBlankLine(lines);
    }

    private void appendNotes(List<String> lines, List<String> notes) {
        if (notes.isEmpty()) {
            return;
        }
        appendSectionHeading(lines, format.getNotesHeading());
        for (String note : notes) {
            appendParagraph(lines, note, format.getItemIndent());
            appendBlankLine(lines);
        }
    }

    private void appendFooter(List<String> lines, String footer) {
        if (footer == null) {
            return;
        }
        appendParagraph(lines, footer, 0);
    }

    private void appendSection(List<String> lines, String heading, List<Row> rows) {
        if (rows.isEmpty()) {
            return;
        }
        appendSectionHeading(lines, heading);
        appendRows(lines, rows);
        appendBlankLine(lines);
    }

    private void appendSectionHeading(List<String> lines, String heading) {
        lines.add(heading + ":");
    }

    private void appendRows(List<String> lines, List<Row> rows) {
        int labelWidth = 0;
        for (Row row : rows) {
            labelWidth = Math.max(labelWidth, row.label().length());
        }
        int indent = format.getItemIndent();
        int availableLabelWidth = Math.max(1, format.getWidth() / 2 - indent);
        boolean align = labelWidth <= availableLabelWidth;
        int descriptionIndent = indent + (align ? labelWidth + format.getDescriptionGap() : 0);
        int descriptionWidth = Math.max(10, format.getWidth() - descriptionIndent);
        for (Row row : rows) {
            String prefix = " ".repeat(indent);
            if (!align) {
                lines.add(prefix + row.label());
                if (row.description() != null) {
                    appendParagraph(lines, row.description(), descriptionIndent);
                }
                continue;
            }
            List<String> descriptionLines = row.description() == null
                    ? List.of()
                    : wrap(row.description(), descriptionWidth);
            if (descriptionLines.isEmpty()) {
                lines.add(prefix + row.label());
                continue;
            }
            lines.add(prefix + padRight(row.label(), labelWidth)
                    + " ".repeat(format.getDescriptionGap()) + descriptionLines.get(0));
            String continuationPrefix = " ".repeat(descriptionIndent);
            for (int index = 1; index < descriptionLines.size(); index++) {
                lines.add(continuationPrefix + descriptionLines.get(index));
            }
        }
    }

    private String appendMetadata(String description,
                                  boolean required,
                                  List<String> defaults,
                                  boolean repeatable) {
        StringBuilder metadata = new StringBuilder(description == null ? "" : description);
        if (repeatable) {
            appendMetadataPart(metadata, "repeatable");
        }
        if (format.isShowRequired() && required) {
            appendMetadataPart(metadata, "required");
        }
        if (format.isShowDefaults() && !defaults.isEmpty()) {
            appendMetadataPart(metadata, "default: " + String.join(", ", defaults));
        }
        return metadata.isEmpty() ? null : metadata.toString();
    }

    private void appendMetadataPart(StringBuilder description, String value) {
        if (!description.isEmpty()) {
            description.append(" (").append(value).append(')');
        } else {
            description.append('(').append(value).append(')');
        }
    }

    private String argumentLabel(HelpDocument.ArgumentEntry argument) {
        String label = argument.getValueLabel() == null
                ? argument.getName().toUpperCase(Locale.ROOT)
                : argument.getValueLabel();
        String token = "<" + label + ">";
        if (argument.getMaximumValues() > 1) {
            token += "...";
        }
        if (argument.getMinimumValues() == 0) {
            token = "[" + token + "]";
        }
        return token;
    }

    private void appendParagraph(List<String> lines, String text, int indent) {
        String prefix = " ".repeat(indent);
        for (String sourceLine : text.lines().toList()) {
            if (sourceLine.isBlank()) {
                lines.add("");
                continue;
            }
            for (String wrappedLine : wrap(sourceLine.strip(), Math.max(10, format.getWidth() - indent))) {
                lines.add(prefix + wrappedLine);
            }
        }
    }

    private void appendWrappedLine(List<String> lines, String text, int indent) {
        appendParagraph(lines, text, indent);
    }

    private void appendBlankLine(List<String> lines) {
        if (!lines.isEmpty() && !lines.get(lines.size() - 1).isEmpty()) {
            lines.add("");
        }
    }

    private List<String> wrap(String text, int width) {
        List<String> result = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        StringBuilder word = new StringBuilder();
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (Character.isWhitespace(character)) {
                if (word.length() > 0) {
                    appendWord(result, line, word.toString(), width);
                    word.setLength(0);
                }
            } else {
                word.append(character);
            }
        }
        if (word.length() > 0) {
            appendWord(result, line, word.toString(), width);
        }
        if (line.length() > 0) {
            result.add(line.toString());
        }
        if (result.isEmpty()) {
            result.add("");
        }
        return result;
    }

    private void appendWord(List<String> result, StringBuilder line, String word, int width) {
        if (word.length() > width) {
            if (line.length() > 0) {
                result.add(line.toString());
                line.setLength(0);
            }
            int offset = 0;
            while (word.length() - offset > width) {
                result.add(word.substring(offset, offset + width));
                offset += width;
            }
            if (offset < word.length()) {
                line.append(word.substring(offset));
            }
            return;
        }
        if (line.isEmpty()) {
            line.append(word);
            return;
        }
        if (line.length() + 1 + word.length() <= width) {
            line.append(' ').append(word);
            return;
        }
        result.add(line.toString());
        line.setLength(0);
        line.append(word);
    }

    private String padRight(String value, int width) {
        if (value.length() >= width) {
            return value;
        }
        return value + " ".repeat(width - value.length());
    }

    private List<String> trimTrailingBlankLines(List<String> lines) {
        int end = lines.size();
        while (end > 0 && lines.get(end - 1).isEmpty()) {
            end--;
        }
        return lines.subList(0, end);
    }

    private record Row(String label, String description) {
    }
}
