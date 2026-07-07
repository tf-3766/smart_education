package com.zhongruan.edu.biz.course.api.vo;

import java.math.BigDecimal;

public record CourseProgressVO(
        String courseId,
        long totalLessons,
        long availableLessons,
        long completedLessons,
        BigDecimal progressPercent,
        String lastLessonId,
        String nextLessonId) {}
