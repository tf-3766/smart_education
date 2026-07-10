package com.zhongruan.edu.biz.exam.api.dto.request;

import com.zhongruan.edu.biz.exam.domain.enums.QuestionBankStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateQuestionBankRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 10000) String description,
        @NotNull QuestionBankStatus status,
        @NotNull @Min(0) Integer version) {}
