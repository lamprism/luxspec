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

package com.lamprism.luxspec.security.authorization;

import com.lamprism.luxspec.AuthErrorCode;
import com.lamprism.luxspec.data.pagination.PageResult;
import com.lamprism.luxspec.data.pagination.PageWindow;
import com.lamprism.luxspec.data.pagination.QueryResult;
import com.lamprism.luxspec.data.pagination.SliceResult;
import com.lamprism.luxspec.data.pagination.SliceWindow;
import com.lamprism.luxspec.data.pagination.UnboundedWindow;
import com.lamprism.luxspec.data.query.OrderBy;
import com.lamprism.luxspec.data.query.QueryCondition;
import com.lamprism.luxspec.data.query.QueryCriteria;
import com.lamprism.luxspec.data.query.QueryExpression;
import com.lamprism.luxspec.data.query.QueryField;
import com.lamprism.luxspec.resource.InMemoryResourceBrowser;
import com.lamprism.luxspec.resource.Resource;
import com.lamprism.luxspec.resource.ResourceReference;
import com.lamprism.luxspec.resource.ResourceType;
import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authentication.SimpleSubject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryAuthorizedResourceBrowserTest {
    private static final ResourceType<Long> DOCUMENT = ResourceType.of("document", Long.class);
    private static final QueryField<Integer> RANK = QueryField.of("rank", Integer.class);

    @Test
    void appliesVisibilityBeforeOrderingCountingAndSliceContinuation() {
        TrackingAuthorizer authorizer = new TrackingAuthorizer(Set.of(2L, 3L));
        InMemoryAuthorizedResourceBrowser<Long, Document> browser = new InMemoryAuthorizedResourceBrowser<>(
                resourceBrowser(),
                authorizer
        );
        ResourceAction<Long> action = ResourceAction.of(
                DOCUMENT,
                "read",
                AuthorizationRequirement.none()
        );
        QueryCriteria criteria = new QueryCriteria(
                QueryExpression.all(),
                List.of(OrderBy.descending(RANK))
        );

        PageResult<Document> page = page(browser.browse(
                authentication(),
                action,
                criteria,
                new PageWindow(0L, 1)
        ));
        SliceResult<Document> slice = slice(browser.browse(
                authentication(),
                action,
                criteria,
                new SliceWindow(0L, 1)
        ));

        assertEquals(List.of(2L), ids(page));
        assertEquals(2L, page.total());
        assertEquals(List.of(2L), ids(slice));
        assertTrue(slice.hasNext());
        assertEquals(8, authorizer.invocations.get());
    }

    @Test
    void rejectsAnUnsatisfiedBaselineRequirementBeforeScanningResources() {
        TrackingAuthorizer authorizer = new TrackingAuthorizer(Set.of(2L, 3L));
        InMemoryAuthorizedResourceBrowser<Long, Document> browser = new InMemoryAuthorizedResourceBrowser<>(
                resourceBrowser(),
                authorizer
        );
        ResourceAction<Long> action = ResourceAction.of(
                DOCUMENT,
                "read",
                AuthorizationRequirement.requires(AuthorizationScope.of("document:read"))
        );

        ResourceAccessDeniedException exception = assertThrows(
                ResourceAccessDeniedException.class,
                () -> browser.browse(authentication(), action, QueryCriteria.empty(), new PageWindow(0L, 1))
        );

        assertEquals(AuthErrorCode.PERMISSION_DENIED, exception.getErrorCode());
        assertEquals(0, authorizer.invocations.get());
    }

    @Test
    void authorizesOnlyCandidatesMatchingStructuredCriteria() {
        TrackingAuthorizer authorizer = new TrackingAuthorizer(Set.of(2L));
        InMemoryAuthorizedResourceBrowser<Long, Document> browser = new InMemoryAuthorizedResourceBrowser<>(
                resourceBrowser(),
                authorizer
        );
        ResourceAction<Long> action = ResourceAction.of(
                DOCUMENT,
                "read",
                AuthorizationRequirement.none()
        );
        QueryCriteria criteria = new QueryCriteria(
                QueryCondition.equal(RANK, 30),
                List.of()
        );

        QueryResult<Document> result = browser.browse(
                authentication(),
                action,
                criteria,
                UnboundedWindow.getInstance()
        );

        assertEquals(List.of(2L), ids(result));
        assertEquals(1, authorizer.invocations.get());
    }

    @SuppressWarnings("unchecked")
    private static PageResult<Document> page(QueryResult<Document> result) {
        return (PageResult<Document>) result;
    }

    @SuppressWarnings("unchecked")
    private static SliceResult<Document> slice(QueryResult<Document> result) {
        return (SliceResult<Document>) result;
    }

    private static List<Long> ids(QueryResult<Document> result) {
        return result.getItems().stream().map(document -> document.getReference().id()).toList();
    }

    private static Authentication authentication() {
        return new Authentication(new SimpleSubject("user", "7"), AuthorizationGrantSet.of(List.of()));
    }

    private static InMemoryResourceBrowser<Long, Document> resourceBrowser() {
        return InMemoryResourceBrowser.builder(
                        DOCUMENT,
                        () -> List.of(
                                new Document(1L, 100),
                                new Document(2L, 30),
                                new Document(3L, 20),
                                new Document(4L, 10)
                        )
                )
                .field(RANK, Document::rank)
                .build();
    }

    private static final class TrackingAuthorizer implements ResourceAuthorizer<Long> {
        private final Set<Long> visibleIds;
        private final AtomicInteger invocations = new AtomicInteger();

        private TrackingAuthorizer(Set<Long> visibleIds) {
            this.visibleIds = Set.copyOf(visibleIds);
        }

        @Override
        public ResourceType<Long> getResourceType() {
            return DOCUMENT;
        }

        @Override
        public AuthorizationDecision authorize(
                Authentication authentication,
                ResourceAction<Long> action,
                ResourceReference<Long> reference
        ) {
            invocations.incrementAndGet();
            if (visibleIds.contains(reference.id())) {
                return AuthorizationDecision.allowed();
            }
            return AuthorizationDecision.denied(AuthErrorCode.PERMISSION_DENIED);
        }
    }

    private static final class Document implements Resource<Long> {
        private final long id;
        private final int rank;

        private Document(long id, int rank) {
            this.id = id;
            this.rank = rank;
        }

        @Override
        public ResourceReference<Long> getReference() {
            return new ResourceReference<>(DOCUMENT, id);
        }

        private Integer rank() {
            return rank;
        }
    }
}
