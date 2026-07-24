package com.zhongruan.edu.biz.warning.api.dto.request;

import com.zhongruan.edu.biz.warning.domain.enums.WarningStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WarningHandleRequest(
        @NotNull WarningStatus action,
        @Size(max = 4000) String remark,
        @NotNull @Min(0) Integer version) {}
