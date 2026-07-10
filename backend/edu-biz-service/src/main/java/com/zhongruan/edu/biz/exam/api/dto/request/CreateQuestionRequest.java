package com.zhongruan.edu.biz.exam.api.dto.request;

import com.zhongruan.edu.biz.exam.domain.enums.QuestionDifficulty;
import com.zhongruan.edu.biz.exam.domain.enums.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record CreateQuestionRequest(
        @NotNull QuestionType questionType,
        @NotBlank @Size(max = 10000) String stem,
        @Size(max = 10000) String analysis,
        @NotNull QuestionDifficulty difficulty,
        @NotNull @DecimalMin("0.01") @Digits(integer = 5, fraction = 2) BigDecimal score,
        List<@Valid QuestionOptionRequest> options) {}
