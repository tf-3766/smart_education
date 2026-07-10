package com.zhongruan.edu.biz.exam.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateExamPaperRequest(
        @NotBlank @Size(max = 160) String title,
        @NotEmpty List<@Valid ExamPaperQuestionRequest> questions) {}
