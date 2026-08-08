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

package com.lamprism.luxspec.web.collection;


/**
 * Indicates that a collection is complete.
 *
 * @author RollW
 */
public final class CompletePagination implements CollectionPagination {
    private static final CompletePagination INSTANCE = new CompletePagination();

    private CompletePagination() {
    }

    /**
     * Returns the shared complete-pagination value.
     *
     * @return the complete-pagination value
     */
    public static CompletePagination getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the complete pagination mode.
     *
     * @return {@code complete}
     */
    @Override
    public String getMode() {
        return "complete";
    }
}
