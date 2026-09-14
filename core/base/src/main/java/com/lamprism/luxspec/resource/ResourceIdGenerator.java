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

package com.lamprism.luxspec.resource;

/**
 * Generates identifiers for one typed resource ID family.
 *
 * @param <ID> the generated resource ID type
 * @author RollW
 */
public interface ResourceIdGenerator<ID> {
    /**
     * Generates the next identifier for a resource type.
     *
     * @param resourceType the resource type that will own the identifier
     * @return the generated identifier
     */
    ID nextId(ResourceType<ID> resourceType);
}
