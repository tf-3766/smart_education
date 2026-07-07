package com.zhongruan.edu.common.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class PageResponseTest {
    @Test
    void calculatesTotalPagesFromKnownPageSize() {
        PageResponse<String> page = PageResponse.of(List.of("one", "two"), 2, 20, 41);

        assertEquals(3, page.totalPages());
        assertEquals(2, page.records().size());
    }
}

