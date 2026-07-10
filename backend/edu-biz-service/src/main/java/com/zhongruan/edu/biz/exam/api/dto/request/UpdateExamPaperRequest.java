package com.zhongruan.edu.biz.exam.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateExamPaperRequest(
        @NotBlank @Size(max = 160) String title,
        @NotEmpty List<@Valid ExamPaperQuestionRequest> questions,
        @NotNull @Min(0) Integer version) {}
