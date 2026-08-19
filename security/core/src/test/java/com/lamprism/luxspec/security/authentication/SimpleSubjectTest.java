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

import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimpleSubjectTest {
    @Test
    void exposesItsDirectIdentityAndCopiesConcreteSubjects() {
        SimpleSubject subject = new SimpleSubject("service", "worker-1");

        assertEquals("service", subject.getType());
        assertEquals("worker-1", subject.getId());
        assertEquals(subject, SimpleSubject.from(new ServiceSubject("worker-1")));
    }

    @Test
    void rejectsBlankIdentityValues() {
        assertThrows(IllegalArgumentException.class, () -> new SimpleSubject("", "worker-1"));
        assertThrows(IllegalArgumentException.class, () -> new SimpleSubject("service", " "));
    }

    @Test
    void validatesEverySubjectImplementationAtTheAuthenticationBoundary() {
        Subject invalidSubject = new Subject() {
            @Override
            public String getType() {
                return "";
            }

            @Override
            public String getId() {
                return "worker-1";
            }
        };

        assertThrows(
                IllegalArgumentException.class,
                () -> new Authentication(invalidSubject, AuthorizationGrantSet.of(List.of()))
        );
    }
}
