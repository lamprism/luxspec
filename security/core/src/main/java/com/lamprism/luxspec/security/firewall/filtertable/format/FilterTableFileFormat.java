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

package com.lamprism.luxspec.security.firewall.filtertable.format;

import com.lamprism.luxspec.security.firewall.filtertable.FileFilterTable;
import com.lamprism.luxspec.security.firewall.filtertable.FilterTableSnapshot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;

/**
 * Reads and writes filter-table snapshots without owning the supplied streams.
 *
 * <p>A format used by a {@link FileFilterTable} must support concurrent calls. The table may
 * prepare multiple candidate files and read an external edit at the same time.</p>
 *
 * @author RollW
 */
public interface FilterTableFileFormat {
    /**
     * Reads a complete snapshot.
     *
     * @param reader     the source text
     * @param sourceName a description used in error messages
     * @return the parsed snapshot
     * @throws IOException when reading fails
     */
    FilterTableSnapshot read(BufferedReader reader, String sourceName) throws IOException;

    /**
     * Writes a complete snapshot.
     *
     * @param writer   the destination text
     * @param snapshot the snapshot to write
     * @throws IOException when writing fails
     */
    void write(Writer writer, FilterTableSnapshot snapshot) throws IOException;
}
