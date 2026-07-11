package com.zhongruan.edu.biz.exam.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ExamAnswerSubmitRequest(
        @NotNull @Positive Long questionId,
        @NotBlank @Size(max = 10000) String answerContent) {}
