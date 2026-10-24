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

import com.lamprism.luxspec.data.pagination.CompleteResult;
import com.lamprism.luxspec.data.pagination.KeysetPosition;
import com.lamprism.luxspec.data.pagination.KeysetResult;
import com.lamprism.luxspec.data.pagination.PageResult;
import com.lamprism.luxspec.data.pagination.SliceResult;
import com.lamprism.luxspec.data.query.OrderBy;
import com.lamprism.luxspec.data.query.QueryField;
import com.lamprism.luxspec.web.pagination.DefaultKeysetCursorCodec;
import com.lamprism.luxspec.web.pagination.KeysetCursorCodec;
import com.lamprism.luxspec.web.query.QueryParameterParser;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CollectionResponseTest {
    @Test
    void allowsAnEmptyPageBeyondTheExactTotal() {
        PagePagination pagination = assertDoesNotThrow(() -> new PagePagination(100L, 20, 3L));

        assertEquals(100L, pagination.offset());
        assertEquals(3L, pagination.total());
        assertThrows(IllegalArgumentException.class, () -> new PagePagination(0L, 20, -1L));
    }

    @Test
    void mapsOrdinaryListsAndCompleteResultsToCompletePagination() {
        CollectionResponse<String> ordinary = CollectionResponse.from(List.of("first"));
        CollectionResponse<String> complete = CollectionResponse.from(new CompleteResult<>(List.of("second")));

        assertEquals(List.of("first"), ordinary.items());
        assertEquals("complete", ordinary.pagination().getMode());
        assertEquals(List.of("second"), complete.items());
        assertEquals("complete", complete.pagination().getMode());
    }

    @Test
    void preservesPageAndSliceMetadata() {
        CollectionResponse<String> page = CollectionResponse.from(
                new PageResult<>(List.of("first"), 10L, 5, 21L)
        );
        CollectionResponse<String> slice = CollectionResponse.from(
                new SliceResult<>(List.of("second"), 15L, 5, true)
        );

        PagePagination pagePagination = (PagePagination) page.pagination();
        SlicePagination slicePagination = (SlicePagination) slice.pagination();
        assertEquals("page", pagePagination.getMode());
        assertEquals(10L, pagePagination.offset());
        assertEquals(5, pagePagination.limit());
        assertEquals(21L, pagePagination.total());
        assertEquals("slice", slicePagination.getMode());
        assertEquals(15L, slicePagination.offset());
        assertEquals(5, slicePagination.limit());
        assertEquals(true, slicePagination.hasNext());
    }

    @Test
    void mapsKeysetResultsToOpaqueCursorMetadata() {
        QueryField<Integer> SCORE = QueryField.of("score", Integer.class);
        List<OrderBy<?>> orders = List.of(OrderBy.descending(SCORE));
        KeysetCursorCodec codec = DefaultKeysetCursorCodec.builder(
                        "items",
                        new SecretKeySpec(
                                "luxspec-test-signing-key".getBytes(StandardCharsets.UTF_8),
                                "HmacSHA256"
                        )
                )
                .field(SCORE, QueryParameterParser.integer())
                .build();
        CollectionResponse<String> response = CollectionResponse.from(
                new KeysetResult<>(List.of("item"), 1, new KeysetPosition(List.of(10))),
                orders,
                codec
        );

        KeysetPagination pagination = (KeysetPagination) response.pagination();
        assertEquals("keyset", pagination.getMode());
        assertEquals(1, pagination.limit());
        assertEquals(true, pagination.hasNext());
        assertEquals(
                new KeysetPosition(List.of(10)).values(),
                codec.decode(orders, pagination.nextCursor()).values()
        );
    }
}
