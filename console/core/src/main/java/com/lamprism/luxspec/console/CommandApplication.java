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
import com.lamprism.luxspec.console.help.HelpRenderer;
import com.lamprism.luxspec.console.help.text.PlainTextHelpFormat;
import com.lamprism.luxspec.console.help.text.PlainTextHelpRenderer;
import com.lamprism.luxspec.console.syntax.CommandTokenizationException;
import com.lamprism.luxspec.console.syntax.CommandTokenizer;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Immutable command application facade shared by process CLI and application Shell.
 *
 * <p>The application consumes already separated tokens through
 * {@link #execute(List, CommandSession)}. Shell integrations may use
 * {@link #executeLine(String, CommandSession)} when they need the built-in
 * quote and escape tokenizer.</p>
 *
 * @author RollW
 */
public final class CommandApplication {
    private final String name;
    private final @Nullable String version;
    private final CommandCatalog catalog;
    private final HelpRenderer helpRenderer;
    private final String helpCommand;
    private final List<String> helpOptionNames;
    private final List<String> versionOptionNames;
    private final HelpDocumentFactory helpDocumentFactory;
    private final CommandParser parser;

    private CommandApplication(Builder builder, CommandCatalog catalog) {
        this.name = builder.name;
        this.version = builder.version;
        this.catalog = catalog;
        this.helpRenderer = builder.helpRenderer;
        this.helpCommand = builder.helpCommand;
        this.helpOptionNames = List.copyOf(builder.helpOptionNames);
        this.versionOptionNames = List.copyOf(builder.versionOptionNames);
        this.helpDocumentFactory = new HelpDocumentFactory(
                name, catalog, helpOptionNames, versionOptionNames
        );
        this.parser = new CommandParser(catalog,
                new CommandParser.CommandControl(helpCommand, helpOptionNames, versionOptionNames));
    }

    /**
     * Creates an application builder.
     *
     * @param name application display and usage name
     * @return the application builder
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * @return application display and usage name
     */
    public String getName() {
        return name;
    }

    /**
     * @return configured application version, or {@code null} when absent
     */
    public @Nullable String getVersion() {
        return version;
    }

    /**
     * @return immutable resolved command catalog
     */
    public CommandCatalog getCatalog() {
        return catalog;
    }

    /**
     * @return the configured Help renderer
     */
    public HelpRenderer getHelpRenderer() {
        return helpRenderer;
    }

    /**
     * @return the configured Help command name
     */
    public String getHelpCommand() {
        return helpCommand;
    }

    /**
     * Creates the semantic Help document for a canonical path.
     *
     * @param path canonical command path
     * @return immutable semantic Help document
     */
    public HelpDocument helpDocument(CommandPath path) {
        return helpDocumentFactory.create(Objects.requireNonNull(path, "path"));
    }

    /**
     * Parses and executes one tokenized command in a session.
     *
     * @param tokens  command tokens without the process name
     * @param session provider-neutral command session
     * @return command result and exit code
     */
    public CommandResult execute(List<String> tokens, CommandSession session) {
        Objects.requireNonNull(session, "session");
        CommandResult result;
        CommandParser.ParseOutcome outcome = parser.parse(tokens);
        if (outcome instanceof CommandParser.HelpRequest helpRequest) {
            result = renderHelp(helpRequest.path(), session);
        } else if (outcome instanceof CommandParser.VersionRequest) {
            result = renderVersion(session);
        } else if (outcome instanceof CommandParser.ParseFailure parseFailure) {
            result = completeFailure(parseFailure.failure(), session);
        } else if (outcome instanceof CommandParser.InvocationRequest invocationRequest) {
            result = executeHandler(invocationRequest, session);
        } else {
            result = completeFailure(CommandFailure.internal(
                    "Unsupported parser outcome", CommandPath.root(), null
            ), session);
        }
        flush(session);
        return result;
    }

    /**
     * Executes an array of already separated command tokens.
     *
     * @param tokens  command tokens without the process name
     * @param session provider-neutral command session
     * @return command result and exit code
     */
    public CommandResult execute(String[] tokens, CommandSession session) {
        Objects.requireNonNull(tokens, "tokens");
        return execute(Arrays.asList(tokens), session);
    }

    /**
     * Tokenizes and executes one Shell line.
     *
     * @param line    raw Shell line
     * @param session provider-neutral command session
     * @return command result and exit code
     */
    public CommandResult executeLine(String line, CommandSession session) {
        Objects.requireNonNull(session, "session");
        try {
            return execute(CommandTokenizer.tokenize(line), session);
        } catch (CommandTokenizationException exception) {
            CommandFailure failure = CommandFailure.usage(
                    "Invalid command line at character " + exception.getPosition() + ": " + exception.getMessage(),
                    CommandPath.root(), null
            );
            CommandResult result = completeFailure(failure, session);
            flush(session);
            return result;
        }
    }

    /**
     * Executes tokens against process standard streams.
     *
     * @param tokens command tokens without the process name
     * @return command result and exit code
     */
    public CommandResult execute(String[] tokens) {
        return execute(tokens, CommandSession.standard());
    }

    /**
     * Executes one Shell line against process standard streams.
     *
     * @param line raw Shell line
     * @return command result and exit code
     */
    public CommandResult executeLine(String line) {
        return executeLine(line, CommandSession.standard());
    }

    private CommandResult executeHandler(CommandParser.InvocationRequest request,
                                         CommandSession session) {
        try {
            CommandResult result = request.handler().execute(new CommandContext(request.invocation(), session));
            if (result == null) {
                return completeFailure(CommandFailure.internal(
                        "Command handler returned null", request.invocation().getPath(), null
                ), session);
            }
            if (result.getFailure() != null) {
                renderFailure(result.getFailure(), session);
            }
            return result;
        } catch (CommandFailureException exception) {
            return completeFailure(exception.getFailure(), session);
        } catch (Exception exception) {
            return completeFailure(CommandFailure.execution(
                    "Command execution failed", request.invocation().getPath(), exception
            ), session);
        }
    }

    private CommandResult renderHelp(CommandPath path, CommandSession session) {
        try {
            session.getOutput().print(helpRenderer.render(helpDocument(path)));
            session.getOutput().println();
            return CommandResult.success();
        } catch (RuntimeException exception) {
            return completeFailure(CommandFailure.internal(
                    "Help rendering failed", path, exception
            ), session);
        }
    }

    private CommandResult renderVersion(CommandSession session) {
        if (version == null) {
            return completeFailure(CommandFailure.execution(
                    "Version information is not configured", CommandPath.root(), null
            ), session);
        }
        session.getOutput().println(version);
        return CommandResult.success();
    }

    private CommandResult completeFailure(CommandFailure failure, CommandSession session) {
        renderFailure(failure, session);
        return CommandResult.failure(failure);
    }

    private void renderFailure(CommandFailure failure, CommandSession session) {
        session.getDiagnostics().println("Error: " + failure.getMessage());
    }

    private void flush(CommandSession session) {
        session.getOutput().flush();
        session.getDiagnostics().flush();
    }

    /**
     * Builder for an immutable command application.
     */
    public static final class Builder {
        private final String name;
        private @Nullable String version;
        private @Nullable String header;
        private @Nullable String summary;
        private @Nullable String description;
        private @Nullable String footer;
        private final List<CommandDocumentation.Example> examples = new ArrayList<>();
        private final List<String> notes = new ArrayList<>();
        private final List<OptionSpec<?>> rootOptions = new ArrayList<>();
        private final List<CommandSpec> specifications = new ArrayList<>();
        private final List<CommandRegistration> registrations = new ArrayList<>();
        private HelpRenderer helpRenderer = new PlainTextHelpRenderer(PlainTextHelpFormat.defaults());
        private String helpCommand = "help";
        private List<String> helpOptionNames = List.of("--help", "-h");
        private List<String> versionOptionNames = List.of("--version");

        private Builder(String name) {
            this.name = requireApplicationName(name);
        }

        /**
         * Sets the application version.
         *
         * @param version application version
         * @return this builder
         */
        public Builder version(String version) {
            this.version = requireText(version, "Version");
            return this;
        }

        /**
         * Sets the application Help header.
         *
         * @param header application Help header
         * @return this builder
         */
        public Builder header(String header) {
            this.header = requireText(header, "Application header");
            return this;
        }

        /**
         * Sets the application Help summary.
         *
         * @param summary application Help summary
         * @return this builder
         */
        public Builder summary(String summary) {
            this.summary = requireText(summary, "Application summary");
            return this;
        }

        /**
         * Sets the application Help description.
         *
         * @param description application Help description
         * @return this builder
         */
        public Builder description(String description) {
            this.description = requireText(description, "Application description");
            return this;
        }

        /**
         * Adds an application Help example.
         *
         * @param invocation example command invocation
         * @return this builder
         */
        public Builder example(String invocation) {
            examples.add(new CommandDocumentation.Example(invocation, null));
            return this;
        }

        /**
         * Adds an application Help example with a description.
         *
         * @param invocation  example command invocation
         * @param description example explanation
         * @return this builder
         */
        public Builder example(String invocation, String description) {
            examples.add(new CommandDocumentation.Example(invocation, description));
            return this;
        }

        /**
         * Adds an application Help note.
         *
         * @param note Help note
         * @return this builder
         */
        public Builder note(String note) {
            notes.add(requireText(note, "Help note"));
            return this;
        }

        /**
         * Sets application Help footer content.
         *
         * @param footer application Help footer
         * @return this builder
         */
        public Builder footer(String footer) {
            this.footer = requireText(footer, "Application footer");
            return this;
        }

        /**
         * Adds one application-wide option inherited by every command.
         *
         * @param option application-wide option
         * @return this builder
         */
        public Builder rootOption(OptionSpec<?> option) {
            rootOptions.add(Objects.requireNonNull(option, "option"));
            return this;
        }

        /**
         * Adds a non-executable command group specification.
         *
         * @param specification command specification
         * @return this builder
         */
        public Builder add(CommandSpec specification) {
            CommandSpec nonNullSpecification = Objects.requireNonNull(specification, "specification");
            if (nonNullSpecification.getPath().isRoot()) {
                throw new IllegalArgumentException("The root specification is owned by the application builder");
            }
            specifications.add(nonNullSpecification);
            return this;
        }

        /**
         * Adds an executable command registration.
         *
         * @param specification executable command specification
         * @param handler       command handler
         * @return this builder
         */
        public Builder command(CommandSpec specification, CommandHandler handler) {
            registrations.add(new CommandRegistration(specification, handler));
            return this;
        }

        /**
         * Adds an executable command registration.
         *
         * @param registration command registration
         * @return this builder
         */
        public Builder command(CommandRegistration registration) {
            registrations.add(Objects.requireNonNull(registration, "registration"));
            return this;
        }

        /**
         * Replaces the complete Help renderer.
         *
         * @param helpRenderer complete Help renderer
         * @return this builder
         */
        public Builder helpRenderer(HelpRenderer helpRenderer) {
            this.helpRenderer = Objects.requireNonNull(helpRenderer, "helpRenderer");
            return this;
        }

        /**
         * Sets the built-in Help command name.
         *
         * @param helpCommand Help command name
         * @return this builder
         */
        public Builder helpCommand(String helpCommand) {
            this.helpCommand = validateCommandName(helpCommand, "Help command");
            return this;
        }

        /**
         * Sets the built-in Help option names, canonical name first.
         *
         * @param canonicalName canonical Help option name
         * @param aliases       ordered Help option aliases
         * @return this builder
         */
        public Builder helpOptionNames(String canonicalName, String... aliases) {
            this.helpOptionNames = optionNames(canonicalName, aliases);
            return this;
        }

        /**
         * Sets the built-in version option names, canonical name first.
         *
         * @param canonicalName canonical version option name
         * @param aliases       ordered version option aliases
         * @return this builder
         */
        public Builder versionOptionNames(String canonicalName, String... aliases) {
            this.versionOptionNames = optionNames(canonicalName, aliases);
            return this;
        }

        /**
         * Builds and validates the immutable application.
         *
         * @return the immutable command application
         */
        public CommandApplication build() {
            CommandSpec.Builder rootBuilder = CommandSpec.root();
            if (header != null) {
                rootBuilder.header(header);
            }
            if (summary != null) {
                rootBuilder.summary(summary);
            }
            if (description != null) {
                rootBuilder.description(description);
            }
            if (footer != null) {
                rootBuilder.footer(footer);
            }
            for (CommandDocumentation.Example example : examples) {
                if (example.getDescription() == null) {
                    rootBuilder.example(example.getInvocation());
                } else {
                    rootBuilder.example(example.getInvocation(), example.getDescription());
                }
            }
            for (String note : notes) {
                rootBuilder.note(note);
            }
            for (OptionSpec<?> option : rootOptions) {
                rootBuilder.option(option);
            }
            CommandSpec root = rootBuilder.build();
            CommandCatalog.Builder catalogBuilder = CommandCatalog.builder(root);
            catalogBuilder.addAll(specifications);
            for (CommandRegistration registration : registrations) {
                catalogBuilder.register(registration);
            }
            CommandCatalog catalog = catalogBuilder.build();
            validateControls(catalog);
            return new CommandApplication(this, catalog);
        }

        private void validateControls(CommandCatalog catalog) {
            for (CommandSpec specification : catalog.getSpecifications()) {
                if (specification.getPath().getSize() == 1) {
                    if (specification.getName().equals(helpCommand)
                            || specification.getAliases().contains(helpCommand)) {
                        throw new IllegalArgumentException("Help command name conflicts with command: "
                                + helpCommand);
                    }
                }
                for (OptionSpec<?> option : specification.getOptions()) {
                    for (String name : option.getNames()) {
                        if (helpOptionNames.contains(name) || versionOptionNames.contains(name)) {
                            throw new IllegalArgumentException("Option name is reserved by a control option: " + name);
                        }
                    }
                }
            }
            if (hasOverlap(helpOptionNames, versionOptionNames)) {
                throw new IllegalArgumentException("Help and version option names must be different");
            }
        }

        private static boolean hasOverlap(List<String> first, List<String> second) {
            for (String value : first) {
                if (second.contains(value)) {
                    return true;
                }
            }
            return false;
        }

        private static List<String> optionNames(String canonicalName, String[] aliases) {
            List<String> names = new ArrayList<>();
            names.add(Objects.requireNonNull(canonicalName, "canonicalName"));
            Objects.requireNonNull(aliases, "aliases");
            for (String alias : aliases) {
                names.add(Objects.requireNonNull(alias, "alias"));
            }
            for (String name : names) {
                OptionSpec.validateOptionName(name);
            }
            if (names.size() != names.stream().distinct().count()) {
                throw new IllegalArgumentException("Control option names must be unique");
            }
            return List.copyOf(names);
        }

        private static String requireApplicationName(String name) {
            return requireText(name, "Application name");
        }

        private static String validateCommandName(String value, String name) {
            String commandName = requireText(value, name);
            for (int index = 0; index < commandName.length(); index++) {
                char character = commandName.charAt(index);
                if (Character.isWhitespace(character) || Character.isISOControl(character)
                        || character == '=') {
                    throw new IllegalArgumentException(name + " must be one command segment: " + value);
                }
            }
            if (commandName.charAt(0) == '-') {
                throw new IllegalArgumentException(name + " must be one command segment: " + value);
            }
            return commandName;
        }

        private static String requireText(String value, String name) {
            Objects.requireNonNull(value, name);
            String normalized = value.strip();
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException(name + " cannot be blank");
            }
            return normalized;
        }
    }
}
