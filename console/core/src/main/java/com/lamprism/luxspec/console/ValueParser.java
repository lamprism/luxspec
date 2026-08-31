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

/**
 * Converts one command token into one typed value.
 *
 * @author RollW
 */
@FunctionalInterface
public interface ValueParser<T> {
    /**
     * Parses one token.
     *
     * @param token the non-null token
     * @return the parsed non-null value
     * @throws ValueParseException when the token is invalid for this parser
     */
    T parse(String token) throws ValueParseException;
}
