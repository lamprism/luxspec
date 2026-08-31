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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable validated command tree and registration catalog.
 *
 * @author RollW
 */
public final class CommandCatalog {
    private final Node root;
    private final List<CommandSpec> specifications;

    private CommandCatalog(Node root, List<CommandSpec> specifications) {
        this.root = root;
        this.specifications = List.copyOf(specifications);
    }

    /**
     * Creates a catalog builder with one explicit root specification.
     *
     * @param root explicit root specification
     * @return the catalog builder
     */
    public static Builder builder(CommandSpec root) {
        CommandSpec nonNullRoot = Objects.requireNonNull(root, "root");
        if (!nonNullRoot.getPath().isRoot()) {
            throw new IllegalArgumentException("The catalog root specification must use the root path");
        }
        return new Builder(nonNullRoot);
    }

    /**
     * @return the explicit root specification
     */
    public CommandSpec getRoot() {
        return root.specification;
    }

    /**
     * @return immutable explicit and synthesized command specifications
     */
    public List<CommandSpec> getSpecifications() {
        return specifications;
    }

    /**
     * Finds a command by its canonical path.
     *
     * @param path canonical command path
     * @return the specification, or {@code null} when not found
     */
    public @Nullable CommandSpec find(CommandPath path) {
        Node node = findCanonical(Objects.requireNonNull(path, "path"));
        return node == null ? null : node.specification;
    }

    /**
     * Returns direct children of a canonical command path.
     *
     * @param path canonical parent path
     * @return immutable children in registration order
     */
    public List<CommandSpec> children(CommandPath path) {
        Node node = requireCanonical(Objects.requireNonNull(path, "path"));
        List<CommandSpec> children = new ArrayList<>(node.children.size());
        for (Node child : node.children) {
            children.add(child.specification);
        }
        return List.copyOf(children);
    }

    /**
     * Returns a handler for a canonical command path.
     *
     * @param path canonical command path
     * @return the registered handler when executable
     */
    public Optional<CommandHandler> handler(CommandPath path) {
        Node node = findCanonical(Objects.requireNonNull(path, "path"));
        if (node == null || node.handler == null) {
            return Optional.empty();
        }
        return Optional.of(node.handler);
    }

    Node rootNode() {
        return root;
    }

    @Nullable
    Node resolve(List<String> segments) {
        Objects.requireNonNull(segments, "segments");
        Node current = root;
        for (String segment : segments) {
            if (segment == null) {
                return null;
            }
            Node child = current.lookup.get(segment);
            if (child == null) {
                return null;
            }
            current = child;
        }
        return current;
    }

    List<OptionSpec<?>> effectiveOptions(Node node) {
        List<OptionSpec<?>> options = new ArrayList<>();
        options.addAll(root.specification.getOptions());
        if (node != root) {
            options.addAll(node.specification.getOptions());
        }
        return List.copyOf(options);
    }

    private @Nullable Node findCanonical(CommandPath path) {
        Node current = root;
        for (int index = 0; index < path.getSize(); index++) {
            Node child = current.canonicalChildren.get(path.getSegment(index));
            if (child == null) {
                return null;
            }
            current = child;
        }
        return current;
    }

    private Node requireCanonical(CommandPath path) {
        Node node = findCanonical(path);
        if (node == null) {
            throw new IllegalArgumentException("Unknown command path: " + path);
        }
        return node;
    }

    /**
     * Builder for a validated immutable catalog.
     */
    public static final class Builder {
        private final LinkedHashMap<CommandPath, CommandSpec> specifications = new LinkedHashMap<>();
        private final LinkedHashMap<CommandPath, CommandHandler> handlers = new LinkedHashMap<>();

        private Builder(CommandSpec root) {
            specifications.put(CommandPath.root(), root);
        }

        /**
         * Adds a specification without executable behavior.
         *
         * @param specification command specification
         * @return this builder
         */
        public Builder add(CommandSpec specification) {
            CommandSpec nonNullSpecification = Objects.requireNonNull(specification, "specification");
            CommandPath path = nonNullSpecification.getPath();
            if (path.isRoot()) {
                throw new IllegalArgumentException("The catalog root was already supplied");
            }
            if (specifications.putIfAbsent(path, nonNullSpecification) != null) {
                throw new IllegalArgumentException("Duplicate command path: " + path);
            }
            return this;
        }

        /**
         * Adds multiple specifications without executable behavior.
         *
         * @param specifications command specifications
         * @return this builder
         */
        public Builder addAll(Iterable<CommandSpec> specifications) {
            Objects.requireNonNull(specifications, "specifications");
            for (CommandSpec specification : specifications) {
                add(specification);
            }
            return this;
        }

        /**
         * Registers one command and its handler.
         *
         * @param specification executable command specification
         * @param handler       command handler
         * @return this builder
         */
        public Builder register(CommandSpec specification, CommandHandler handler) {
            CommandSpec nonNullSpecification = Objects.requireNonNull(specification, "specification");
            CommandHandler nonNullHandler = Objects.requireNonNull(handler, "handler");
            CommandPath path = nonNullSpecification.getPath();
            CommandSpec previous = specifications.putIfAbsent(path, nonNullSpecification);
            if (previous != null && previous != nonNullSpecification) {
                throw new IllegalArgumentException("Duplicate command path: " + path);
            }
            if (handlers.putIfAbsent(path, nonNullHandler) != null) {
                throw new IllegalArgumentException("Duplicate command registration: " + path);
            }
            return this;
        }

        /**
         * Registers one command registration.
         *
         * @param registration command registration
         * @return this builder
         */
        public Builder register(CommandRegistration registration) {
            CommandRegistration nonNullRegistration = Objects.requireNonNull(registration, "registration");
            return register(nonNullRegistration.getSpecification(), nonNullRegistration.getHandler());
        }

        /**
         * Builds and validates the immutable catalog.
         *
         * @return the immutable command catalog
         */
        public CommandCatalog build() {
            LinkedHashMap<CommandPath, MutableNode> nodes = createNodes();
            attachChildren(nodes);
            validateNodes(nodes);
            Node immutableRoot = freeze(nodes.get(CommandPath.root()));
            List<CommandSpec> resolvedSpecifications = new ArrayList<>();
            for (MutableNode node : nodes.values()) {
                resolvedSpecifications.add(node.specification);
            }
            return new CommandCatalog(immutableRoot, resolvedSpecifications);
        }

        private LinkedHashMap<CommandPath, MutableNode> createNodes() {
            LinkedHashMap<CommandPath, MutableNode> nodes = new LinkedHashMap<>();
            MutableNode root = new MutableNode(CommandPath.root(), specifications.get(CommandPath.root()));
            nodes.put(CommandPath.root(), root);
            for (CommandSpec specification : specifications.values()) {
                if (specification.getPath().isRoot()) {
                    continue;
                }
                CommandPath currentPath = CommandPath.root();
                for (String segment : specification.getPath().getSegments()) {
                    currentPath = currentPath.child(segment);
                    MutableNode node = nodes.get(currentPath);
                    if (node == null) {
                        node = new MutableNode(currentPath, CommandSpec.implicit(currentPath));
                        nodes.put(currentPath, node);
                    }
                }
                MutableNode node = nodes.get(specification.getPath());
                node.specification = specification;
            }
            for (Map.Entry<CommandPath, CommandHandler> entry : handlers.entrySet()) {
                MutableNode node = nodes.get(entry.getKey());
                if (node == null) {
                    throw new IllegalStateException("Registration has no command node: " + entry.getKey());
                }
                node.handler = entry.getValue();
            }
            return nodes;
        }

        private void attachChildren(LinkedHashMap<CommandPath, MutableNode> nodes) {
            for (MutableNode node : nodes.values()) {
                if (node.path.isRoot()) {
                    continue;
                }
                MutableNode parent = nodes.get(node.path.getParent());
                if (parent == null) {
                    throw new IllegalStateException("Missing command parent: " + node.path.getParent());
                }
                parent.children.add(node);
            }
            for (MutableNode node : nodes.values()) {
                LinkedHashMap<String, MutableNode> lookup = new LinkedHashMap<>();
                for (MutableNode child : node.children) {
                    addLookup(lookup, child.specification.getName(), child);
                    for (String alias : child.specification.getAliases()) {
                        addLookup(lookup, alias, child);
                    }
                }
                node.lookup = lookup;
            }
        }

        private void validateNodes(LinkedHashMap<CommandPath, MutableNode> nodes) {
            for (MutableNode node : nodes.values()) {
                validateArguments(node.specification);
                validateLocalOptions(node.specification);
            }
            MutableNode root = nodes.get(CommandPath.root());
            validateEffectiveOptions(root, root.specification.getOptions());
            for (MutableNode node : nodes.values()) {
                if (node == root) {
                    continue;
                }
                List<OptionSpec<?>> effectiveOptions = new ArrayList<>(root.specification.getOptions());
                effectiveOptions.addAll(node.specification.getOptions());
                validateEffectiveOptions(node, effectiveOptions);
            }
        }

        private void validateArguments(CommandSpec specification) {
            boolean repeatableSeen = false;
            List<ArgumentSpec<?>> arguments = specification.getArguments();
            for (int index = 0; index < arguments.size(); index++) {
                ArgumentSpec<?> argument = arguments.get(index);
                if (repeatableSeen) {
                    throw new IllegalArgumentException("No argument may follow a repeatable argument in "
                            + specification.getPath());
                }
                if (argument.isRepeatable()) {
                    repeatableSeen = true;
                    if (index != arguments.size() - 1) {
                        throw new IllegalArgumentException("A repeatable argument must be last in "
                                + specification.getPath());
                    }
                }
            }
        }

        private void validateLocalOptions(CommandSpec specification) {
            validateEffectiveOptions(null, specification.getOptions());
        }

        private void validateEffectiveOptions(@Nullable MutableNode node, List<OptionSpec<?>> options) {
            Map<String, OptionSpec<?>> names = new LinkedHashMap<>();
            for (OptionSpec<?> option : options) {
                for (String name : option.getNames()) {
                    OptionSpec<?> previous = names.putIfAbsent(name, option);
                    if (previous != null) {
                        String path = node == null ? "" : node.path.toString();
                        throw new IllegalArgumentException("Duplicate option name '" + name + "' in " + path);
                    }
                }
            }
        }

        private static void addLookup(Map<String, MutableNode> lookup,
                                      String name,
                                      MutableNode child) {
            MutableNode previous = lookup.putIfAbsent(name, child);
            if (previous != null && previous != child) {
                throw new IllegalArgumentException("Duplicate command name or alias '" + name
                        + "' under " + child.path.getParent());
            }
        }

        private static Node freeze(@Nullable MutableNode node) {
            if (node == null) {
                throw new IllegalStateException("The root command node is missing");
            }
            List<Node> children = new ArrayList<>(node.children.size());
            for (MutableNode child : node.children) {
                children.add(freeze(child));
            }
            Map<String, Node> lookup = new LinkedHashMap<>();
            Map<String, Node> canonicalChildren = new LinkedHashMap<>();
            for (Node child : children) {
                canonicalChildren.put(child.specification.getName(), child);
                lookup.put(child.specification.getName(), child);
                for (String alias : child.specification.getAliases()) {
                    lookup.put(alias, child);
                }
            }
            return new Node(node.path, node.specification, node.handler, children, lookup, canonicalChildren);
        }
    }

    private static final class MutableNode {
        private final CommandPath path;
        private CommandSpec specification;
        private @Nullable CommandHandler handler;
        private final List<MutableNode> children = new ArrayList<>();
        private Map<String, MutableNode> lookup = Collections.emptyMap();

        private MutableNode(CommandPath path, CommandSpec specification) {
            this.path = path;
            this.specification = specification;
        }
    }

    static final class Node {
        final CommandPath path;
        final CommandSpec specification;
        final @Nullable CommandHandler handler;
        final List<Node> children;
        final Map<String, Node> lookup;
        final Map<String, Node> canonicalChildren;

        private Node(CommandPath path,
                     CommandSpec specification,
                     @Nullable CommandHandler handler,
                     List<Node> children,
                     Map<String, Node> lookup,
                     Map<String, Node> canonicalChildren) {
            this.path = path;
            this.specification = specification;
            this.handler = handler;
            this.children = List.copyOf(children);
            this.lookup = Map.copyOf(lookup);
            this.canonicalChildren = Map.copyOf(canonicalChildren);
        }
    }
}
