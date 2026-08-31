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

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Creates semantic Help documents from a resolved immutable command catalog.
 *
 * @author RollW
 */
final class HelpDocumentFactory {
    private final String applicationName;
    private final CommandCatalog catalog;
    private final List<String> helpOptionNames;
    private final List<String> versionOptionNames;

    HelpDocumentFactory(String applicationName,
                        CommandCatalog catalog,
                        List<String> helpOptionNames,
                        List<String> versionOptionNames) {
        this.applicationName = Objects.requireNonNull(applicationName, "applicationName");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.helpOptionNames = List.copyOf(helpOptionNames);
        this.versionOptionNames = List.copyOf(versionOptionNames);
    }

    HelpDocument create(CommandPath path) {
        CommandCatalog.Node node = catalog.resolve(path.getSegments());
        if (node == null || !node.path.equals(path)) {
            throw new IllegalArgumentException("Unknown command path: " + path);
        }
        String usage = buildUsage(node);
        CommandSpec specification = node.specification;
        CommandDocumentation documentation = specification.getDocumentation();
        HelpDocument.Builder builder = HelpDocument.builder(applicationName, path, usage)
                .commandAliases(specification.getAliases())
                .applicationHeader(catalog.getRoot().getDocumentation().getHeader())
                .commandHeader(path.isRoot() ? null : documentation.getHeader())
                .summary(documentation.getSummary())
                .description(documentation.getDescription())
                .footer(documentation.getFooter());

        for (CommandCatalog.Node child : node.children) {
            CommandDocumentation childDocumentation = child.specification.getDocumentation();
            builder.command(new HelpDocument.CommandEntry(
                    child.specification.getName(),
                    child.specification.getAliases(),
                    summaryFor(childDocumentation),
                    childDocumentation.getGroup(),
                    child.specification.isHidden()
            ));
        }
        for (OptionSpec<?> option : catalog.effectiveOptions(node)) {
            builder.option(toOptionEntry(option));
        }
        builder.option(new HelpDocument.OptionEntry(
                helpOptionNames, null, "Display help information for this command",
                true, false, false, false, List.of()
        ));
        if (path.isRoot()) {
            builder.option(new HelpDocument.OptionEntry(
                    versionOptionNames, null, "Display application version information and exit",
                    true, false, false, false, List.of()
            ));
        }
        for (ArgumentSpec<?> argument : specification.getArguments()) {
            builder.argument(toArgumentEntry(argument));
        }
        for (CommandDocumentation.Example example : documentation.getExamples()) {
            builder.example(example);
        }
        for (String note : documentation.getNotes()) {
            builder.note(note);
        }
        return builder.build();
    }

    private String buildUsage(CommandCatalog.Node node) {
        StringBuilder usage = new StringBuilder(applicationName);
        if (!node.path.isRoot()) {
            usage.append(' ').append(node.path);
        }
        usage.append(" [OPTIONS]");
        if (!node.children.isEmpty()) {
            if (node.handler == null) {
                usage.append(" <COMMAND>");
            } else {
                usage.append(" [<COMMAND>]");
            }
        }
        for (ArgumentSpec<?> argument : node.specification.getArguments()) {
            usage.append(' ').append(argumentToken(argument));
        }
        return usage.toString();
    }

    private String argumentToken(ArgumentSpec<?> argument) {
        String label = argument.getValueLabel() == null
                ? argument.getName().toUpperCase(Locale.ROOT)
                : argument.getValueLabel();
        String token = "<" + label + ">";
        if (argument.isRepeatable()) {
            token += "...";
        }
        if (!argument.isRequired()) {
            token = "[" + token + "]";
        }
        return token;
    }

    private @Nullable String summaryFor(CommandDocumentation documentation) {
        if (documentation.getSummary() != null) {
            return documentation.getSummary();
        }
        String description = documentation.getDescription();
        if (description == null) {
            return null;
        }
        int lineEnd = description.indexOf('\n');
        int paragraphEnd = description.indexOf("\n\n");
        int end = lineEnd < 0 ? description.length() : lineEnd;
        if (paragraphEnd >= 0) {
            end = Math.min(end, paragraphEnd);
        }
        String summary = description.substring(0, end).strip();
        return summary.isEmpty() ? null : summary;
    }

    private HelpDocument.OptionEntry toOptionEntry(OptionSpec<?> option) {
        List<String> defaults = new ArrayList<>();
        for (Object value : option.getDefaultValues()) {
            defaults.add(String.valueOf(value));
        }
        return new HelpDocument.OptionEntry(
                option.getNames(), option.getValueLabel(), option.getDescription(), option.isFlag(),
                option.isRequired(), option.isRepeatable(), option.isHidden(), defaults
        );
    }

    private HelpDocument.ArgumentEntry toArgumentEntry(ArgumentSpec<?> argument) {
        List<String> defaults = new ArrayList<>();
        for (Object value : argument.getDefaultValues()) {
            defaults.add(String.valueOf(value));
        }
        return new HelpDocument.ArgumentEntry(
                argument.getName(), argument.getValueLabel(), argument.getDescription(),
                argument.getMinimumValues(), argument.getMaximumValues(), argument.isHidden(), defaults
        );
    }
}
