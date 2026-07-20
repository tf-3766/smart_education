package com.zhongruan.edu.biz.course.application.assembler;

import com.zhongruan.edu.biz.course.api.vo.ChapterDetailVO;
import com.zhongruan.edu.biz.course.api.vo.CodeLabelVO;
import com.zhongruan.edu.biz.course.api.vo.CourseMaterialVO;
import com.zhongruan.edu.biz.course.api.vo.LearningRecordVO;
import com.zhongruan.edu.biz.course.api.vo.LessonDetailVO;
import com.zhongruan.edu.biz.course.domain.enums.ChapterStatus;
import com.zhongruan.edu.biz.course.domain.enums.LearningStatus;
import com.zhongruan.edu.biz.course.domain.enums.LessonContentType;
import com.zhongruan.edu.biz.course.domain.enums.LessonStatus;
import com.zhongruan.edu.biz.course.domain.enums.LessonUnlockType;
import com.zhongruan.edu.biz.course.domain.enums.MaterialStatus;
import com.zhongruan.edu.biz.course.domain.enums.MaterialType;
import com.zhongruan.edu.biz.course.domain.enums.MaterialVisibility;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseChapterEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseLessonEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseMaterialEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.LessonLearningRecordEntity;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class CourseContentAssembler {
    public ChapterDetailVO toChapter(CourseChapterEntity chapter) {
        return new ChapterDetailVO(id(chapter.getId()), id(chapter.getCourseId()), chapter.getTitle(),
                chapter.getDescription(), chapter.getSortOrder(),
                CodeLabelVO.of(ChapterStatus.valueOf(chapter.getStatus())),
                time(chapter.getPublishedAt()), chapter.getVersion());
    }

    public LessonDetailVO toLesson(CourseLessonEntity lesson) {
        return new LessonDetailVO(id(lesson.getId()), id(lesson.getCourseId()), id(lesson.getChapterId()),
                lesson.getTitle(), CodeLabelVO.of(LessonContentType.valueOf(lesson.getContentType())),
                lesson.getContent(), lesson.getVideoUrl(), lesson.getEstimatedMinutes(), lesson.getSortOrder(),
                CodeLabelVO.of(LessonStatus.valueOf(lesson.getStatus())),
                CodeLabelVO.of(LessonUnlockType.valueOf(lesson.getUnlockType())),
                time(lesson.getUnlockAt()), time(lesson.getPublishedAt()), lesson.getVersion());
    }

    public CourseMaterialVO toMaterial(CourseMaterialEntity material) {
        return new CourseMaterialVO(id(material.getId()), id(material.getCourseId()), id(material.getChapterId()),
                id(material.getLessonId()), material.getName(),
                CodeLabelVO.of(MaterialType.valueOf(material.getMaterialType())), id(material.getFileId()), material.getFileKey(),
                material.getFileUrl(), material.getFileSize(), material.getMimeType(),
                CodeLabelVO.of(MaterialVisibility.valueOf(material.getVisibility())),
                CodeLabelVO.of(MaterialStatus.valueOf(material.getStatus())), material.getSortOrder(),
                material.getVersion());
    }

    public LearningRecordVO toLearningRecord(LessonLearningRecordEntity record) {
        return new LearningRecordVO(id(record.getId()), id(record.getCourseId()), id(record.getChapterId()),
                id(record.getLessonId()), id(record.getStudentId()),
                CodeLabelVO.of(LearningStatus.valueOf(record.getStatus())), record.getStudySeconds(),
                time(record.getStartedAt()),
                time(record.getCompletedAt()), time(record.getLastStudiedAt()));
    }

    public OffsetDateTime time(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    public String id(Long value) {
        return value == null ? null : value.toString();
    }
}
