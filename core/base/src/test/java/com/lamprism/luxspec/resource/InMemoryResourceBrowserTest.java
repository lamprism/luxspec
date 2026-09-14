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

import com.lamprism.luxspec.data.pagination.QueryResult;
import com.lamprism.luxspec.data.pagination.UnboundedWindow;
import com.lamprism.luxspec.data.query.QueryCondition;
import com.lamprism.luxspec.data.query.QueryCriteria;
import com.lamprism.luxspec.data.query.QueryField;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryResourceBrowserTest {
    private static final ResourceType<Long> DOCUMENT = ResourceType.of("document", Long.class);
    private static final QueryField<String> TITLE = QueryField.of("title", String.class);

    @Test
    void resolvesResourcesFromOneSnapshotAndPreservesDuplicatePositions() {
        TestResource first = new TestResource(1L, "first");
        TestResource second = new TestResource(2L, "second");
        AtomicInteger supplierCalls = new AtomicInteger();
        InMemoryResourceBrowser<Long, TestResource> browser = InMemoryResourceBrowser.builder(
                DOCUMENT,
                () -> {
                    supplierCalls.incrementAndGet();
                    return List.of(first, second);
                }
        ).field(TITLE, TestResource::title).build();

        TestResource resolved = browser.provide(second.getReference());
        List<TestResource> resolvedMany = browser.provide(List.of(
                second.getReference(),
                first.getReference(),
                second.getReference()
        ));

        assertEquals(second, resolved);
        assertEquals(List.of(second, first, second), resolvedMany);
        assertEquals(2, supplierCalls.get());
    }

    @Test
    void browsesUsingTheExplicitFieldSchema() {
        TestResource first = new TestResource(1L, "first");
        TestResource second = new TestResource(2L, "second");
        InMemoryResourceBrowser<Long, TestResource> browser = InMemoryResourceBrowser.builder(
                DOCUMENT,
                () -> List.of(first, second)
        ).field(TITLE, TestResource::title).build();

        QueryResult<TestResource> result = browser.browse(
                new QueryCriteria(QueryCondition.equal(TITLE, "second"), List.of()),
                UnboundedWindow.getInstance()
        );

        assertEquals(List.of(second), result.getItems());
    }

    @Test
    void rejectsReferencesForAnotherResourceType() {
        InMemoryResourceBrowser<Long, TestResource> browser = InMemoryResourceBrowser.<Long, TestResource>builder(
                DOCUMENT,
                () -> List.of()
        ).field(TITLE, TestResource::title).build();
        ResourceType<Long> otherType = ResourceType.of("otherDocument", Long.class);

        ResourceException exception = assertThrows(
                ResourceException.class,
                () -> browser.provide(new ResourceReference<>(otherType, 1L))
        );

        assertEquals(ResourceErrorCode.INVALID_REFERENCE, exception.getErrorCode());
    }

    private static final class TestResource implements Resource<Long> {
        private final long id;
        private final String title;

        private TestResource(long id, String title) {
            this.id = id;
            this.title = title;
        }

        @Override
        public ResourceReference<Long> getReference() {
            return new ResourceReference<>(DOCUMENT, id);
        }

        private String title() {
            return title;
        }
    }
}
