package com.zhongruan.edu.biz.exam.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateQuestionBankRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 10000) String description) {}
