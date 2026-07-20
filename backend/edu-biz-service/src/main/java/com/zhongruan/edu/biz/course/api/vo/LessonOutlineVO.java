package com.zhongruan.edu.biz.course.api.vo;

import java.util.List;

public record LessonOutlineVO(
        String lessonId,
        String title,
        Integer sortOrder,
        CodeLabelVO contentType,
        Integer estimatedMinutes,
        boolean unlocked,
        boolean completed,
        CodeLabelVO learningStatus,
        List<MaterialAccessVO> materials) {}
