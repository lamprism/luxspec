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

package com.lamprism.luxspec.security.firewall.filtertable;

import com.lamprism.luxspec.security.authentication.SimpleSubject;
import com.lamprism.luxspec.security.authentication.Subject;

/**
 * Identifies one request fact stored by a filter table.
 *
 * @author RollW
 */
public sealed interface FilterTarget permits FilterTarget.ClientAddressTarget, FilterTarget.SubjectTarget {
    /**
     * Creates a normalized exact-address or CIDR target.
     *
     * @param address the exact IP address or CIDR network
     * @return the client-address target
     */
    static ClientAddressTarget clientAddress(String address) {
        return new ClientAddressTarget(address);
    }

    /**
     * Creates a generic subject target.
     *
     * @param subject the subject identity
     * @return the subject target
     */
    static SubjectTarget subject(Subject subject) {
        return new SubjectTarget(SimpleSubject.from(subject));
    }

    /**
     * Matches a request client address.
     *
     * @param address the normalized exact IP address or CIDR network
     */
    record ClientAddressTarget(String address) implements FilterTarget {
        /**
         * Creates a normalized client-address target.
         *
         * @param address the exact IP address or CIDR network
         */
        public ClientAddressTarget {
            address = FilterTableAddress.canonicalize(address);
        }
    }

    /**
     * Matches an authenticated subject.
     *
     * @param subject the generic immutable subject identity
     */
    record SubjectTarget(SimpleSubject subject) implements FilterTarget {
        /**
         * Creates a subject target.
         *
         * @param subject the subject identity
         */
        public SubjectTarget {
            if (subject == null) {
                throw new NullPointerException("subject");
            }
        }
    }
}
