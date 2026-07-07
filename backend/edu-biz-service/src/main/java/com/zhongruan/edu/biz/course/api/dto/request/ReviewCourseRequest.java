package com.zhongruan.edu.biz.course.api.dto.request;

import jakarta.validation.constraints.Size;

public record ReviewCourseRequest(@Size(max = 500) String remark) {}
