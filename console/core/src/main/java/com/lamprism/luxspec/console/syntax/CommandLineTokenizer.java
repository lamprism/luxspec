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

package com.lamprism.luxspec.console.syntax;

import java.util.List;

/**
 * Converts a command line into tokens for the command catalog parser.
 *
 * @author RollW
 */
@FunctionalInterface
public interface CommandLineTokenizer {
    /**
     * Splits one command line.
     *
     * @param line the command line
     * @return the command tokens
     * @throws CommandTokenizationException when the line is malformed
     */
    List<String> tokenize(String line) throws CommandTokenizationException;
}
