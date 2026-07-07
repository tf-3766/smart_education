package com.zhongruan.edu.biz.course.api.vo;

import java.time.OffsetDateTime;

public record StudentLessonDetailVO(
        String lessonId,
        String courseId,
        String chapterId,
        String title,
        CodeLabelVO contentType,
        String content,
        String videoUrl,
        Integer estimatedMinutes,
        CodeLabelVO status,
        OffsetDateTime unlockAt,
        LearningRecordVO learningRecord) {}
