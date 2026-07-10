package com.zhongruan.edu.biz.forum.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForumTopicCreateRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 5000) String content) {}
