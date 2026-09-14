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

package com.lamprism.luxspec.context;

import com.lamprism.luxspec.context.slf4j.Slf4jMdcScope;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Slf4jMdcScopeTest {
    @Test
    void rejectsOutOfOrderAndCrossThreadScopeClose() throws Exception {
        Slf4jMdcScope scope = Slf4jMdcScope.open(ExecutionContext.empty());
        Slf4jMdcScope nested = Slf4jMdcScope.open(ExecutionContext.empty());

        assertThrows(IllegalStateException.class, scope::close);
        nested.close();
        scope.close();

        Slf4jMdcScope crossThreadScope = Slf4jMdcScope.open(ExecutionContext.empty());
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try {
            Thread thread = new Thread(() -> {
                try {
                    crossThreadScope.close();
                } catch (Throwable exception) {
                    failure.set(exception);
                }
            });
            thread.start();
            thread.join();

            assertNotNull(failure.get());
            assertEquals(IllegalStateException.class, failure.get().getClass());
        } finally {
            crossThreadScope.close();
        }
    }
}
