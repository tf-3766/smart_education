package com.zhongruan.edu.biz.grade.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PublishGradeRequest(@NotNull @Min(0) Integer version) {}
