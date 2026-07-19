package com.zhongruan.edu.biz.assignment.api.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;

public record SubmissionSubmitRequest(
        @Size(max = 20000) String content,
        Map<@Size(max = 64) String, List<@Size(max = 5000) String>> answers,
        @Positive Long fileId,
        @Size(max = 512) String fileKey,
        @Size(max = 1024) String fileUrl,
        Integer version) {}
