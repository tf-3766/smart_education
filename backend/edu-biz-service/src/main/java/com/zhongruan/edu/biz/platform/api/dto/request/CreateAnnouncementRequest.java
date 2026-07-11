package com.zhongruan.edu.biz.platform.api.dto.request;

import com.zhongruan.edu.biz.platform.domain.enums.AnnouncementAudience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAnnouncementRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 10000) String content,
        @NotNull AnnouncementAudience audience) {}
