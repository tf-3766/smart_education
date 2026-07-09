package com.zhongruan.edu.feign.ai;

public record AiLessonRef(
        Long lessonId,
        Long chapterId,
        String title,
        String status,
        String contentType,
        Integer estimatedMinutes) {}
