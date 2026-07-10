package com.zhongruan.edu.biz.forum.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForumReplyCreateRequest(
        Long parentReplyId,
        @NotBlank @Size(max = 3000) String content) {}
