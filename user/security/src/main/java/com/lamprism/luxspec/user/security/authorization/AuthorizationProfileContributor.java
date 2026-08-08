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

package com.lamprism.luxspec.user.security.authorization;

/**
 * Contributes one cohesive group of profiles, inheritance, or scopes during security assembly.
 *
 * @author RollW
 */
@FunctionalInterface
public interface AuthorizationProfileContributor {
    /**
     * Contributes profile definitions to the mutable assembly registry.
     *
     * @param registry the profile assembly registry
     */
    void contribute(AuthorizationProfileRegistry registry);
}
