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
import java.util.List;
import java.util.Objects;

/**
 * Dependency-free tokenizer for Shell lines with quote and escape support.
 *
 * @author RollW
 */
public final class CommandTokenizer {
    private CommandTokenizer() {
    }

    /**
     * Splits one line into command tokens.
     *
     * <p>This method handles whitespace, single quotes, double quotes, and
     * backslash escapes. It deliberately does not implement Shell operators,
     * variable expansion, globbing, or command substitution.</p>
     *
     * @param line raw Shell line
     * @return immutable tokens
     * @throws CommandTokenizationException when quoting or escaping is incomplete
     */
    public static List<String> tokenize(String line) throws CommandTokenizationException {
        Objects.requireNonNull(line, "line");
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        State state = State.OUTSIDE;
        boolean tokenStarted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (state == State.OUTSIDE) {
                if (Character.isWhitespace(character)) {
                    if (tokenStarted) {
                        tokens.add(token.toString());
                        token.setLength(0);
                        tokenStarted = false;
                    }
                    continue;
                }
                if (character == '\\') {
                    if (index + 1 >= line.length()) {
                        throw new CommandTokenizationException("Trailing escape character", index);
                    }
                    token.append(line.charAt(++index));
                    tokenStarted = true;
                    continue;
                }
                if (character == '\'') {
                    state = State.SINGLE_QUOTE;
                    tokenStarted = true;
                    continue;
                }
                if (character == '"') {
                    state = State.DOUBLE_QUOTE;
                    tokenStarted = true;
                    continue;
                }
                token.append(character);
                tokenStarted = true;
                continue;
            }
            if (state == State.SINGLE_QUOTE) {
                if (character == '\'') {
                    state = State.OUTSIDE;
                } else {
                    token.append(character);
                }
                continue;
            }
            if (character == '"') {
                state = State.OUTSIDE;
                continue;
            }
            if (character == '\\') {
                if (index + 1 >= line.length()) {
                    throw new CommandTokenizationException("Trailing escape character", index);
                }
                token.append(line.charAt(++index));
                continue;
            }
            token.append(character);
        }
        if (state == State.SINGLE_QUOTE) {
            throw new CommandTokenizationException("Unclosed single quote", line.length());
        }
        if (state == State.DOUBLE_QUOTE) {
            throw new CommandTokenizationException("Unclosed double quote", line.length());
        }
        if (tokenStarted) {
            tokens.add(token.toString());
        }
        return List.copyOf(tokens);
    }

    private enum State {
        OUTSIDE,
        SINGLE_QUOTE,
        DOUBLE_QUOTE
    }
}
