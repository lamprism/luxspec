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

package com.lamprism.luxspec.web.pagination;

import com.lamprism.luxspec.data.pagination.KeysetPosition;
import com.lamprism.luxspec.data.query.OrderBy;
import com.lamprism.luxspec.data.query.QueryField;
import com.lamprism.luxspec.web.query.QueryParameterParser;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultKeysetCursorCodecTest {
    private static final QueryField<String> NAME = QueryField.of("name", String.class);
    private static final QueryField<Integer> SCORE = QueryField.of("score", Integer.class);
    private static final List<OrderBy<?>> ORDERS = List.of(
            OrderBy.descending(SCORE),
            OrderBy.ascending(NAME)
    );

    private final KeysetCursorCodec codec = DefaultKeysetCursorCodec.builder(
                    "items",
                    new SecretKeySpec(
                            "luxspec-test-signing-key".getBytes(StandardCharsets.UTF_8),
                            "HmacSHA256"
                    )
            )
            .field(SCORE, QueryParameterParser.integer())
            .field(NAME, QueryParameterParser.string())
            .build();

    @Test
    void roundTripsConfiguredTypedValuesAsAnOpaqueToken() {
        KeysetPosition position = new KeysetPosition(List.of(20, "bravo"));

        String token = codec.encode(ORDERS, position);
        KeysetPosition decoded = codec.decode(ORDERS, token);

        assertNotEquals(position.values().toString(), token);
        assertEquals(position.values(), decoded.values());
    }

    @Test
    void rejectsTamperingAndOrderingReuse() {
        String token = codec.encode(ORDERS, new KeysetPosition(List.of(20, "bravo")));
        char replacement = token.charAt(token.length() - 1) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, token.length() - 1) + replacement;

        assertThrows(IllegalArgumentException.class, () -> codec.decode(ORDERS, tampered));
        assertThrows(
                IllegalArgumentException.class,
                () -> codec.decode(
                        List.of(OrderBy.ascending(SCORE), OrderBy.ascending(NAME)),
                        token
                )
        );
    }

    @Test
    void rejectsValuesThatDoNotMatchConfiguredCodecs() {
        assertThrows(
                IllegalArgumentException.class,
                () -> codec.encode(ORDERS, new KeysetPosition(List.of("20", "bravo")))
        );
    }

    @Test
    void rejectsOrderingFieldsThatDoNotMatchConfiguredCodecs() {
        QueryField<Long> IDENTIFIER = QueryField.of("identifier", Long.class);

        assertThrows(
                IllegalArgumentException.class,
                () -> codec.encode(
                        List.of(OrderBy.ascending(IDENTIFIER), OrderBy.ascending(NAME)),
                        new KeysetPosition(List.of(20L, "bravo"))
                )
        );
    }

    @Test
    void usesEnumNamesAsTheStableCursorRepresentation() {
        QueryField<CursorState> state = QueryField.of("state", CursorState.class);
        KeysetCursorCodec enumCodec = DefaultKeysetCursorCodec.builder(
                        "items",
                        new SecretKeySpec(
                                "luxspec-test-signing-key".getBytes(StandardCharsets.UTF_8),
                                "HmacSHA256"
                        )
                )
                .field(state, QueryParameterParser.enumValue(CursorState.class))
                .build();
        List<OrderBy<?>> orders = List.of(OrderBy.ascending(state));

        KeysetPosition decoded = enumCodec.decode(
                orders,
                enumCodec.encode(orders, new KeysetPosition(List.of(CursorState.ACTIVE)))
        );

        assertEquals(List.of(CursorState.ACTIVE), decoded.values());
    }

    private enum CursorState {
        ACTIVE;

        @Override
        public String toString() {
            return "custom-display-value";
        }
    }
}
