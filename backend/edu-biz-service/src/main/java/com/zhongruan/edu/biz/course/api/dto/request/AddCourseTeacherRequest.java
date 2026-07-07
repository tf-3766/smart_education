package com.zhongruan.edu.biz.course.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddCourseTeacherRequest(@NotNull @Positive Long teacherId) {}
