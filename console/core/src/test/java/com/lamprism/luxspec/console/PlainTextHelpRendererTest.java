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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the semantic coverage and formatting policy of the standard Help renderer.
 *
 * @author RollW
 */
class PlainTextHelpRendererTest {
    @Test
    void rendersConfiguredSectionsAndWrapsLongContent() {
        HelpFormat format = HelpFormat.builder()
                .width(60)
                .descriptionGap(2)
                .usageHeading("USAGE")
                .commandsHeading("COMMANDS")
                .argumentsHeading("ARGUMENTS")
                .optionsHeading("OPTIONS")
                .examplesHeading("EXAMPLES")
                .notesHeading("NOTES")
                .build();
        OptionSpec<String> include = OptionSpec.builder("--include", ValueParsers.string())
                .repeatable()
                .defaultValues(List.of("one", "two"))
                .description("Include matching values from the selected location")
                .build();
        OptionSpec<String> required = OptionSpec.builder("--required", ValueParsers.string())
                .required()
                .description("A required value")
                .build();
        OptionSpec<String> hidden = OptionSpec.builder("--internal", ValueParsers.string())
                .hidden()
                .build();
        ArgumentSpec<String> location = ArgumentSpec.builder("location", ValueParsers.string())
                .optional()
                .repeatable()
                .defaultValue("current")
                .description("One or more locations to inspect")
                .build();
        CommandSpec command = CommandSpec.builder("run")
                .alias("r")
                .header("Run command")
                .summary("Run the selected operation")
                .description("A deliberately long command description that must be wrapped within the configured width.")
                .options(List.of(include, required, hidden))
                .argument(location)
                .example("demo run --required value")
                .note("The operation does not modify the selected locations.")
                .build();
        CommandApplication application = CommandApplication.builder("demo")
                .header("Demo command line")
                .command(command, context -> CommandResult.success())
                .build();
        PlainTextHelpRenderer renderer = new PlainTextHelpRenderer(format);

        String rendered = renderer.render(application.helpDocument(CommandPath.of("run")));

        assertSame(format, renderer.getFormat());
        assertTrue(rendered.contains("Demo command line"));
        assertTrue(rendered.contains("Run command"));
        assertTrue(rendered.contains("USAGE:"));
        assertTrue(rendered.contains("ARGUMENTS:"));
        assertTrue(rendered.contains("OPTIONS:"));
        assertTrue(rendered.contains("EXAMPLES:"));
        assertTrue(rendered.contains("NOTES:"));
        assertTrue(rendered.contains("Aliases: r"));
        assertTrue(rendered.contains("[<LOCATION>...]"));
        assertTrue(rendered.contains("default: current"));
        assertTrue(rendered.contains("default: one, two"));
        assertTrue(rendered.contains("required"));
        assertTrue(rendered.contains("repeatable"));
        assertFalse(rendered.contains("--internal"));
        assertTrue(rendered.lines().allMatch(line -> line.length() <= 60));
    }

    @Test
    void canRenderHiddenEntriesWhenPolicyAllowsIt() {
        OptionSpec<String> hidden = OptionSpec.builder("--internal", ValueParsers.string())
                .hidden()
                .build();
        CommandSpec command = CommandSpec.builder("run")
                .option(hidden)
                .build();
        CommandApplication application = CommandApplication.builder("demo")
                .command(command, context -> CommandResult.success())
                .build();
        HelpDocument document = application.helpDocument(CommandPath.of("run"));
        HelpFormat format = HelpFormat.defaults().toBuilder()
                .showHidden(true)
                .build();

        String rendered = new PlainTextHelpRenderer(format).render(document);

        assertTrue(rendered.contains("--internal"));
    }
}
