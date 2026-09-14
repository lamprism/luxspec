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

package com.lamprism.luxspec.security.authentication;

/**
 * Identifies a trusted internal system actor.
 *
 * <p>The singleton factory is package-private so an external token or application caller cannot
 * construct a system identity through the public API.</p>
 *
 * @author RollW
 */
public final class SystemSubject implements Subject {
    static final String TYPE = "system";
    private static final SystemSubject INSTANCE = new SystemSubject();

    private SystemSubject() {
    }

    static SystemSubject trustedInstance() {
        return INSTANCE;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getId() {
        return TYPE;
    }
}
