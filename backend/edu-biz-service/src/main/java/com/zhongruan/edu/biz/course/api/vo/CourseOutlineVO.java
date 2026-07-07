package com.zhongruan.edu.biz.course.api.vo;

import java.util.List;

public record CourseOutlineVO(
        String courseId,
        String courseName,
        CodeLabelVO status,
        List<ChapterOutlineVO> chapters) {
    public CourseOutlineVO {
        chapters = chapters == null ? List.of() : List.copyOf(chapters);
    }
}
