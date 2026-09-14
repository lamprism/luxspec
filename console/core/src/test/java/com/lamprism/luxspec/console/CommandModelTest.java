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

import com.lamprism.luxspec.console.session.PromptSpec;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies command model invariants and catalog validation.
 *
 * @author RollW
 */
class CommandModelTest {
    @Test
    void createsTypedImmutableSpecifications() {
        OptionSpec<Integer> limit = OptionSpec.builder("--limit", ValueParser.integer())
                .alias("-l")
                .valueLabel("NUMBER")
                .defaultValue(10)
                .description("Maximum number of entries")
                .build();
        List<OptionSpec<?>> options = new ArrayList<>();
        options.add(limit);
        CommandSpec specification = CommandSpec.builder("resource", "list")
                .aliases("ls")
                .description("List resources")
                .options(options)
                .build();

        options.clear();

        assertEquals(CommandPath.of("resource", "list"), specification.getPath());
        assertEquals(List.of("ls"), specification.getAliases());
        assertEquals(List.of(limit), specification.getOptions());
        assertEquals(List.of(10), limit.getDefaultValues());
        assertTrue(limit.hasDefaultValue());
    }

    @Test
    void rejectsRequiredValuesWithDefaults() {
        assertThrows(IllegalArgumentException.class, () -> OptionSpec
                .builder("--name", ValueParser.string())
                .required()
                .defaultValue("demo")
                .build());

        assertThrows(IllegalArgumentException.class, () -> ArgumentSpec
                .builder("name", ValueParser.string())
                .defaultValue("demo")
                .build());

        assertThrows(IllegalArgumentException.class, () -> PromptSpec
                .secret("Password")
                .withDefault(new char[]{'x'}));
    }

    @Test
    void rejectsInvalidArgumentNamesDuringConstruction() {
        assertThrows(IllegalArgumentException.class, () -> ArgumentSpec
                .builder("-name", ValueParser.string())
                .build());
        assertThrows(IllegalArgumentException.class, () -> ArgumentSpec
                .builder("name=value", ValueParser.string())
                .build());
    }

    @Test
    void resolvesCanonicalCommandsAndAliasesThroughTheCatalog() {
        CommandSpec echo = CommandSpec.builder("tools", "echo")
                .alias("say")
                .build();
        CommandCatalog catalog = CommandCatalog.builder(CommandSpec.root().build())
                .register(echo, context -> CommandResult.success())
                .build();

        assertEquals(echo, catalog.find(CommandPath.of("tools", "echo")));
        assertEquals(List.of(echo), catalog.children(CommandPath.of("tools")));
        assertEquals(echo,
                catalog.find(CommandPath.of("tools", "echo")));
    }

    @Test
    void rejectsConflictingCommandAliasesAndOptions() {
        CommandSpec first = CommandSpec.builder("tools", "first")
                .alias("run")
                .build();
        CommandSpec second = CommandSpec.builder("tools", "run")
                .build();
        assertThrows(IllegalArgumentException.class, () -> CommandCatalog.builder(CommandSpec.root().build())
                .add(first)
                .add(second)
                .build());

        OptionSpec<String> firstOption = OptionSpec.builder("--name", ValueParser.string()).build();
        OptionSpec<String> secondOption = OptionSpec.builder("--name", ValueParser.string()).build();
        CommandSpec command = CommandSpec.builder("run")
                .options(List.of(firstOption, secondOption))
                .build();
        assertThrows(IllegalArgumentException.class, () -> CommandCatalog.builder(CommandSpec.root().build())
                .add(command)
                .build());

        OptionSpec<String> sharedOption = OptionSpec.builder("--shared", ValueParser.string()).build();
        CommandSpec repeatedOptionCommand = CommandSpec.builder("repeat")
                .options(List.of(sharedOption, sharedOption))
                .build();
        assertThrows(IllegalArgumentException.class, () -> CommandCatalog.builder(CommandSpec.root().build())
                .add(repeatedOptionCommand)
                .build());
    }

    @Test
    void requiresRepeatableArgumentsToBeLast() {
        CommandSpec command = CommandSpec.builder("copy")
                .arguments(List.of(
                        ArgumentSpec.builder("source", ValueParser.string()).repeatable().build(),
                        ArgumentSpec.builder("target", ValueParser.string()).build()
                ))
                .build();

        assertThrows(IllegalArgumentException.class, () -> CommandCatalog.builder(CommandSpec.root().build())
                .add(command)
                .build());
    }
}
