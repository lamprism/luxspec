package com.lamprism.luxspec.data;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class QueryResultTest {
    @Test
    void permitsAnEmptyPageBeyondTheExactTotal() {
        assertDoesNotThrow(() -> new PageResult<>(List.of(), 100L, 10, 3L));
    }

    @Test
    void rejectsResultItemsBeyondTheRequestedWindowLimit() {
        assertThrows(IllegalArgumentException.class, () -> new PageResult<>(List.of("first", "second"), 0L, 1, 2L));
        assertThrows(IllegalArgumentException.class, () -> new SliceResult<>(List.of("first", "second"), 0L, 1, true));
    }
}
