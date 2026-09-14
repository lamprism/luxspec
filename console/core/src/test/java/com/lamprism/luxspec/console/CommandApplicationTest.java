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

import com.lamprism.luxspec.console.help.HelpDocument;
import com.lamprism.luxspec.console.session.PromptResult;
import com.lamprism.luxspec.console.session.PromptSpec;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies command application parsing, execution, sessions, and control operations.
 *
 * @author RollW
 */
class CommandApplicationTest {
    @Test
    void executesTheSameHandlerFromTokensAndShellLine() {
        OptionSpec<String> message = OptionSpec.builder("--message", ValueParser.string())
                .alias("-m")
                .valueLabel("TEXT")
                .required()
                .build();
        OptionSpec<Boolean> verbose = OptionSpec.flag("--verbose")
                .alias("-v")
                .build();
        ArgumentSpec<String> target = ArgumentSpec.builder("target", ValueParser.string())
                .optional()
                .defaultValue("console")
                .build();
        CommandSpec echo = CommandSpec.builder("tools", "echo")
                .alias("say")
                .description("Echo a message")
                .options(List.of(message, verbose))
                .argument(target)
                .build();
        RecordingHandler handler = new RecordingHandler(message, verbose, target);
        CommandApplication application = CommandApplication.builder("demo")
                .version("1.2.3")
                .command(echo, handler)
                .build();

        TestSession tokenSession = new TestSession();
        CommandResult tokenResult = application.execute(
                List.of("tools", "say", "--message", "hello", "--verbose"), tokenSession
        );
        assertEquals(0, tokenResult.getExitCode());
        assertEquals("hello", handler.message());
        assertTrue(handler.verbose());
        assertEquals("console", handler.target());

        TestSession lineSession = new TestSession();
        CommandResult lineResult = application.executeLine(
                "tools echo --message \"hello world\"", lineSession
        );
        assertEquals(0, lineResult.getExitCode());
        assertEquals("hello world", handler.message());
        assertFalse(handler.verbose());
        assertEquals("console", handler.target());
    }

    @Test
    void parsesGlobalOptionsShortClustersAndPositionals() {
        OptionSpec<String> config = OptionSpec.builder("--config", ValueParser.string())
                .alias("-c")
                .build();
        OptionSpec<Boolean> quiet = OptionSpec.flag("--quiet")
                .alias("-q")
                .build();
        OptionSpec<String> message = OptionSpec.builder("--message", ValueParser.string())
                .alias("-m")
                .required()
                .build();
        OptionSpec<Boolean> verbose = OptionSpec.flag("--verbose")
                .alias("-v")
                .build();
        CommandSpec command = CommandSpec.builder("echo")
                .options(List.of(message, verbose))
                .build();
        CapturingInvocationHandler handler = new CapturingInvocationHandler();
        CommandApplication application = CommandApplication.builder("demo")
                .rootOption(config)
                .rootOption(quiet)
                .command(command, handler)
                .build();

        TestSession session = new TestSession();
        CommandResult result = application.execute(
                List.of("-c", "app.properties", "echo", "-qm", "hello"), session
        );

        assertEquals(0, result.getExitCode());
        assertEquals(List.of("app.properties"), handler.optionValues(config));
        assertEquals(List.of(Boolean.TRUE), handler.optionValues(quiet));
        assertEquals(List.of("hello"), handler.optionValues(message));
        assertEquals(List.of(Boolean.FALSE), handler.optionValues(verbose));
    }

    @Test
    void returnsStructuredUsageFailuresOnDiagnostics() {
        OptionSpec<String> message = OptionSpec.builder("--message", ValueParser.string())
                .required()
                .build();
        CommandSpec command = CommandSpec.builder("echo")
                .option(message)
                .build();
        CommandApplication application = CommandApplication.builder("demo")
                .command(command, context -> CommandResult.success())
                .build();
        TestSession session = new TestSession();

        CommandResult result = application.execute(List.of("echo"), session);

        assertEquals(2, result.getExitCode());
        assertEquals(CommandFailureKind.USAGE, result.getFailure().getKind());
        assertTrue(session.diagnosticsText().contains("Missing required option"));
    }

    @Test
    void requiresExplicitPresenceForRequiredFlags() {
        OptionSpec<Boolean> force = OptionSpec.flag("--force")
                .required()
                .build();
        CommandInvocation[] invocation = new CommandInvocation[1];
        CommandApplication application = CommandApplication.builder("demo")
                .command(CommandSpec.builder("run").option(force).build(), context -> {
                    invocation[0] = context.getInvocation();
                    return CommandResult.success();
                })
                .build();

        CommandResult missing = application.execute(List.of("run"), new TestSession());
        assertEquals(2, missing.getExitCode());
        assertEquals(CommandFailureKind.USAGE, missing.getFailure().getKind());

        CommandResult present = application.execute(List.of("run", "--force"), new TestSession());
        assertTrue(present.isSuccess());
        assertNotNull(invocation[0]);
        assertTrue(invocation[0].wasProvided(force));
        assertEquals(List.of(Boolean.TRUE), invocation[0].getOptionValues(force));
    }

    @Test
    void stopsResolvingChildCommandsAfterTheFirstPositionalToken() {
        ArgumentSpec<String> values = ArgumentSpec.builder("value", ValueParser.string())
                .repeatable()
                .build();
        CommandInvocation[] invocation = new CommandInvocation[1];
        boolean[] childInvoked = new boolean[1];
        CommandApplication application = CommandApplication.builder("demo")
                .command(CommandSpec.builder("run").argument(values).build(), context -> {
                    invocation[0] = context.getInvocation();
                    return CommandResult.success();
                })
                .command(CommandSpec.builder("run", "nested").build(), context -> {
                    childInvoked[0] = true;
                    return CommandResult.success();
                })
                .build();

        CommandResult result = application.execute(List.of("run", "first", "nested"), new TestSession());

        assertTrue(result.isSuccess());
        assertNotNull(invocation[0]);
        assertFalse(childInvoked[0]);
        assertEquals(List.of("first", "nested"), invocation[0].getArgumentValues(values));
    }

    @Test
    void treatsTokensAfterDoubleDashAsPositionals() {
        ArgumentSpec<String> value = ArgumentSpec.builder("value", ValueParser.string())
                .build();
        CommandInvocation[] invocation = new CommandInvocation[1];
        CommandApplication application = CommandApplication.builder("demo")
                .command(CommandSpec.builder("run").argument(value).build(), context -> {
                    invocation[0] = context.getInvocation();
                    return CommandResult.success();
                })
                .build();

        CommandResult result = application.execute(List.of("run", "--", "--value"), new TestSession());

        assertTrue(result.isSuccess());
        assertNotNull(invocation[0]);
        assertEquals(List.of("--value"), invocation[0].getArgumentValues(value));
    }

    @Test
    void exposesSemanticHelpAndAllowsReplacingTheRenderer() {
        CommandSpec command = CommandSpec.builder("tools", "echo")
                .alias("say")
                .header("Echo command")
                .description("Echo values")
                .option(OptionSpec.flag("--verbose").description("Enable verbose output").build())
                .build();
        CommandApplication application = CommandApplication.builder("demo")
                .header("Demo application")
                .description("Application description")
                .helpRenderer(document -> "CUSTOM\n" + document.getUsage())
                .command(command, context -> CommandResult.success())
                .build();

        HelpDocument document = application.helpDocument(CommandPath.of("tools", "echo"));
        assertEquals("Echo command", document.getCommandHeader());
        assertEquals(List.of("say"), document.getCommandAliases());
        assertTrue(document.getUsage().contains("demo tools echo"));

        TestSession session = new TestSession();
        CommandResult result = application.execute(List.of("tools", "echo", "--help"), session);
        assertEquals(0, result.getExitCode());
        assertEquals("CUSTOM\ndemo tools echo [OPTIONS]" + System.lineSeparator(), session.outputText());
    }

    @Test
    void rendersVersionAndHelpCommandThroughTheSameEntryPoint() {
        CommandSpec command = CommandSpec.builder("tools", "echo").build();
        CommandApplication application = CommandApplication.builder("demo")
                .version("1.2.3")
                .command(command, context -> CommandResult.success())
                .build();
        TestSession session = new TestSession();

        assertEquals(0, application.execute(List.of("help", "tools", "echo"), session).getExitCode());
        assertTrue(session.outputText().contains("demo tools echo"));
        session.reset();
        assertEquals(0, application.execute(List.of("--version"), session).getExitCode());
        assertEquals("1.2.3" + System.lineSeparator(), session.outputText());
    }

    @Test
    void makesPromptAvailabilityExplicitWithoutASeparateCliApi() {
        TestSession session = new TestSession();
        PromptResult<String> result = session.prompt(PromptSpec.text("Name"));
        assertEquals(PromptResult.Status.UNAVAILABLE, result.getStatus());

        CommandSession standard = CommandSession.nonInteractive(
                new StringReader(""), new PrintWriter(new StringWriter()), new PrintWriter(new StringWriter())
        );
        assertEquals(PromptResult.Status.UNAVAILABLE,
                standard.prompt(PromptSpec.confirm("Continue", false)).getStatus());
    }

    private static final class RecordingHandler implements CommandHandler {
        private final OptionSpec<String> message;
        private final OptionSpec<Boolean> verbose;
        private final ArgumentSpec<String> target;
        private String messageValue;
        private boolean verboseValue;
        private String targetValue;

        private RecordingHandler(OptionSpec<String> message,
                                 OptionSpec<Boolean> verbose,
                                 ArgumentSpec<String> target) {
            this.message = message;
            this.verbose = verbose;
            this.target = target;
        }

        @Override
        public CommandResult execute(CommandContext context) {
            messageValue = context.getInvocation().getOptionValue(message).orElseThrow();
            verboseValue = context.getInvocation().getOptionValue(verbose).orElseThrow();
            targetValue = context.getInvocation().getArgumentValue(target).orElseThrow();
            return CommandResult.success();
        }

        private String message() {
            return messageValue;
        }

        private boolean verbose() {
            return verboseValue;
        }

        private String target() {
            return targetValue;
        }
    }

    private static final class CapturingInvocationHandler implements CommandHandler {
        private CommandInvocation invocation;

        @Override
        public CommandResult execute(CommandContext context) {
            invocation = context.getInvocation();
            return CommandResult.success();
        }

        private <T> List<T> optionValues(OptionSpec<T> option) {
            return invocation.getOptionValues(option);
        }
    }

    private static final class TestSession implements CommandSession {
        private final StringReader input = new StringReader("");
        private final StringWriter output = new StringWriter();
        private final StringWriter diagnostics = new StringWriter();
        private final PrintWriter outputWriter = new PrintWriter(output);
        private final PrintWriter diagnosticsWriter = new PrintWriter(diagnostics);

        @Override
        public Reader getInput() {
            return input;
        }

        @Override
        public PrintWriter getOutput() {
            return outputWriter;
        }

        @Override
        public PrintWriter getDiagnostics() {
            return diagnosticsWriter;
        }

        @Override
        public <T> PromptResult<T> prompt(PromptSpec<T> prompt) {
            return PromptResult.unavailable();
        }

        private String outputText() {
            outputWriter.flush();
            return output.toString();
        }

        private String diagnosticsText() {
            diagnosticsWriter.flush();
            return diagnostics.toString();
        }

        private void reset() {
            output.getBuffer().setLength(0);
            diagnostics.getBuffer().setLength(0);
        }
    }
}
