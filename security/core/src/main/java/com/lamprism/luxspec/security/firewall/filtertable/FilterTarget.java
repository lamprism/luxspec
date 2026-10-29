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

import com.lamprism.luxspec.network.ip.IpAddress;
import com.lamprism.luxspec.network.ip.IpNetwork;
import com.lamprism.luxspec.security.authentication.SimpleSubject;
import com.lamprism.luxspec.security.authentication.Subject;

import java.util.Objects;

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
    static ClientAddressTarget clientAddress(IpNetwork address) {
        return new ClientAddressTarget(address);
    }

    /**
     * Creates an exact-address target.
     *
     * @param address the exact IP address
     * @return the client-address target
     */
    static ClientAddressTarget clientAddress(IpAddress address) {
        IpAddress nonNullAddress = Objects.requireNonNull(address, "address");
        return new ClientAddressTarget(IpNetwork.of(nonNullAddress, nonNullAddress.version().bitCount()));
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
     */
    final class ClientAddressTarget implements FilterTarget {
        private final IpNetwork address;

        /**
         * Creates a normalized client-address target.
         *
         * @param address the exact IP address or CIDR network
         */
        public ClientAddressTarget(IpNetwork address) {
            this.address = Objects.requireNonNull(address, "address");
        }

        /**
         * Returns the normalized address network.
         *
         * @return the address network
         */
        public IpNetwork address() {
            return address;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ClientAddressTarget target && address.equals(target.address);
        }

        @Override
        public int hashCode() {
            return address.hashCode();
        }

        @Override
        public String toString() {
            return "ClientAddressTarget[address=" + address + "]";
        }
    }

    /**
     * Matches an authenticated subject.
     *
     */
    final class SubjectTarget implements FilterTarget {
        private final SimpleSubject subject;

        /**
         * Creates a subject target.
         *
         * @param subject the generic immutable subject identity
         */
        public SubjectTarget(SimpleSubject subject) {
            this.subject = Objects.requireNonNull(subject, "subject");
        }

        /**
         * Returns the subject identity.
         *
         * @return the subject identity
         */
        public SimpleSubject subject() {
            return subject;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof SubjectTarget target && subject.equals(target.subject);
        }

        @Override
        public int hashCode() {
            return subject.hashCode();
        }

        @Override
        public String toString() {
            return "SubjectTarget[subject=" + subject + "]";
        }
    }
}
