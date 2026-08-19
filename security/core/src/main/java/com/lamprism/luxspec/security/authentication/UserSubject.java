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
 * Identifies one authenticated user by its stable ID.
 *
 * @author RollW
 */
public final class UserSubject implements Subject {
    public static final String TYPE = "user";

    private final long userId;
    private final String id;

    /**
     * Creates a user subject with a positive stable user identifier.
     *
     * @param userId the stable user identifier
     */
    public UserSubject(long userId) {
        if (userId < 1L) {
            throw new IllegalArgumentException("userId must be positive");
        }
        this.userId = userId;
        this.id = Long.toString(userId);
    }

    /**
     * Returns the stable numeric user identifier.
     *
     * @return the user identifier
     */
    public long userId() {
        return userId;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getId() {
        return id;
    }
}
