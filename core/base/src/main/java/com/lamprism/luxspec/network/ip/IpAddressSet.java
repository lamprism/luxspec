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

package com.lamprism.luxspec.network.ip;

/**
 * An IP address set with a single address family.
 *
 * @author RollW
 */
public interface IpAddressSet {
    /**
     * Returns the address family represented by this set.
     *
     * @return the IP version
     */
    IpVersion version();

    /**
     * Tests membership without resolving hostnames.
     *
     * @param address the candidate address
     * @return whether the address belongs to this set
     */
    boolean contains(IpAddress address);
}
