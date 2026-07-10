package com.zhongruan.edu.biz.grade.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record GradeSubmissionRequest(
        @NotNull @DecimalMin("0.00") BigDecimal score,
        @NotNull @DecimalMin("0.01") BigDecimal maxScore,
        @Size(max = 1000) String teacherComment,
        Long aiCommentDraftId,
        Boolean publishNow,
        @NotNull @Min(0) Integer version) {}
