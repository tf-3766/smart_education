package com.zhongruan.edu.biz.exam.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

public record SubmitExamAttemptRequest(
        @NotEmpty List<@Valid ExamAnswerSubmitRequest> answers,
        @NotNull @PositiveOrZero Integer version) {}
