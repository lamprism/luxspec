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

package com.lamprism.luxspec;

/**
 * Exposes the stable error code carried by an object.
 *
 * @author RollW
 */
public interface ErrorCodeCarrier {
    /**
     * Returns the non-null stable business error carried by this object.
     *
     * @return the error code
     */
    ErrorCode getErrorCode();
}
