package com.zhongruan.edu.biz.course.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectCourseRequest(@NotBlank @Size(max = 500) String reason) {}
