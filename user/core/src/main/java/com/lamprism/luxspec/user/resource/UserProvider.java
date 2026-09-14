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

package com.lamprism.luxspec.user.resource;

import com.lamprism.luxspec.resource.ResourceProvider;
import com.lamprism.luxspec.resource.ResourceReference;
import com.lamprism.luxspec.user.User;

import java.util.Collection;
import java.util.List;

/**
 * Resolves users by their resource references and unique usernames.
 *
 * @author RollW
 */
public interface UserProvider extends ResourceProvider<Long> {
    /**
     * Resolves one required user reference.
     *
     * @param reference the user reference
     * @return the resolved user
     */
    @Override
    User provide(ResourceReference<Long> reference);

    /**
     * Resolves all required user references while preserving input order.
     *
     * @param references the user references
     * @return the resolved users in input order
     */
    @Override
    List<User> provide(Collection<ResourceReference<Long>> references);

    /**
     * Resolves one required user by its unique username.
     *
     * @param username the username
     * @return the resolved user
     */
    User provideByUsername(String username);
}
