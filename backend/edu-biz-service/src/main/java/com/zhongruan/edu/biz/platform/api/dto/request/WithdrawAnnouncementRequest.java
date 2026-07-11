package com.zhongruan.edu.biz.platform.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record WithdrawAnnouncementRequest(@NotNull @PositiveOrZero Integer version) {}
