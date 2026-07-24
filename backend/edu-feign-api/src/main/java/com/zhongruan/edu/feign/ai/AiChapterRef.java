package com.zhongruan.edu.feign.ai;

public record AiChapterRef(
        Long chapterId,
        String title,
        String description,
        Integer sortOrder,
        String status) {}
