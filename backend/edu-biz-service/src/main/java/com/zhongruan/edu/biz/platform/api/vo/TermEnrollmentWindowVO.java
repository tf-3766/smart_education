package com.zhongruan.edu.biz.platform.api.vo;

import java.time.OffsetDateTime;

public record TermEnrollmentWindowVO(
        String windowId,
        String term,
        OffsetDateTime enrollmentOpenAt,
        OffsetDateTime enrollmentCloseAt,
        Integer version) {}
