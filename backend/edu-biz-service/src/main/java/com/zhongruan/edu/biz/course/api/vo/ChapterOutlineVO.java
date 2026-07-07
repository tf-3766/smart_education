package com.zhongruan.edu.biz.course.api.vo;

import java.util.List;

public record ChapterOutlineVO(
        String chapterId,
        String title,
        Integer sortOrder,
        List<LessonOutlineVO> lessons) {
    public ChapterOutlineVO {
        lessons = lessons == null ? List.of() : List.copyOf(lessons);
    }
}
