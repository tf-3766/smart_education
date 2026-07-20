package com.zhongruan.edu.biz.course.api.vo;

import java.time.OffsetDateTime;
import java.util.List;

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
        List<MaterialAccessVO> materials,
        LearningRecordVO learningRecord) {}
