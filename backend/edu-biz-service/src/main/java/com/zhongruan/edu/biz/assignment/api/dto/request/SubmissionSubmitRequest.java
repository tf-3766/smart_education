package com.zhongruan.edu.biz.assignment.api.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;

public record SubmissionSubmitRequest(
        @Size(max = 20000) String content,
        @Positive Long fileId,
        @Size(max = 512) String fileKey,
        @Size(max = 1024) String fileUrl,
        Integer version) {}
