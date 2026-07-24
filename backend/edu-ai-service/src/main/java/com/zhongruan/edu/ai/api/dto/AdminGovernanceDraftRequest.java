package com.zhongruan.edu.ai.api.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

public record AdminGovernanceDraftRequest(
        @Size(max = 50) List<Long> teacherUserIds,
        @Size(max = 50) List<Long> courseIds,
        @Size(max = 2000) String criteria) {
    public AdminGovernanceDraftRequest {
        teacherUserIds = teacherUserIds == null ? List.of() : List.copyOf(teacherUserIds);
        courseIds = courseIds == null ? List.of() : List.copyOf(courseIds);
    }
}
