package com.zhongruan.edu.biz.course.application.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhongruan.edu.biz.course.api.vo.ChapterOutlineVO;
import com.zhongruan.edu.biz.course.api.vo.CodeLabelVO;
import com.zhongruan.edu.biz.course.api.vo.CourseOutlineVO;
import com.zhongruan.edu.biz.course.api.vo.CourseProgressVO;
import com.zhongruan.edu.biz.course.api.vo.LearningRecordVO;
import com.zhongruan.edu.biz.course.api.vo.LessonOutlineVO;
import com.zhongruan.edu.biz.course.api.vo.MaterialAccessVO;
import com.zhongruan.edu.biz.course.api.vo.StudentLessonDetailVO;
import com.zhongruan.edu.biz.course.application.assembler.CourseContentAssembler;
import com.zhongruan.edu.biz.course.domain.enums.ChapterStatus;
import com.zhongruan.edu.biz.course.domain.enums.CourseStatus;
import com.zhongruan.edu.biz.course.domain.enums.LearningStatus;
import com.zhongruan.edu.biz.course.domain.enums.LessonContentType;
import com.zhongruan.edu.biz.course.domain.enums.LessonStatus;
import com.zhongruan.edu.biz.course.domain.enums.MaterialType;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseChapterEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseLessonEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseMaterialEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.LessonLearningRecordEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseChapterMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseLessonMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseMaterialMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.LessonLearningRecordMapper;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentLearningService {
    private final CourseChapterMapper chapterMapper;
    private final CourseLessonMapper lessonMapper;
    private final CourseMaterialMapper materialMapper;
    private final LessonLearningRecordMapper learningRecordMapper;
    private final CourseManagementService courseService;
    private final CoursePermissionService permissionService;
    private final CourseContentAssembler assembler;

    public StudentLearningService(
            CourseChapterMapper chapterMapper,
            CourseLessonMapper lessonMapper,
            CourseMaterialMapper materialMapper,
            LessonLearningRecordMapper learningRecordMapper,
            CourseManagementService courseService,
            CoursePermissionService permissionService,
            CourseContentAssembler assembler) {
        this.chapterMapper = chapterMapper;
        this.lessonMapper = lessonMapper;
        this.materialMapper = materialMapper;
        this.learningRecordMapper = learningRecordMapper;
        this.courseService = courseService;
        this.permissionService = permissionService;
        this.assembler = assembler;
    }

    @Transactional(readOnly = true)
    public CourseOutlineVO outline(Long studentId, Long courseId) {
        CourseEntity course = requireStudentCourse(studentId, courseId);
        List<CourseChapterEntity> chapters = publishedChapters(courseId);
        List<CourseLessonEntity> lessons = publishedLessons(courseId).stream()
                .filter(permissionService::isUnlocked)
                .toList();
        Map<Long, LessonLearningRecordEntity> records = learningRecords(studentId, courseId);
        Map<Long, List<CourseLessonEntity>> byChapter = new HashMap<>();
        lessons.forEach(lesson -> byChapter.computeIfAbsent(lesson.getChapterId(), ignored -> new java.util.ArrayList<>())
                .add(lesson));

        List<ChapterOutlineVO> chapterVOs = chapters.stream()
                .map(chapter -> new ChapterOutlineVO(
                        assembler.id(chapter.getId()), chapter.getTitle(), chapter.getSortOrder(),
                        byChapter.getOrDefault(chapter.getId(), List.of()).stream()
                                .sorted(Comparator.comparing(CourseLessonEntity::getSortOrder)
                                        .thenComparing(CourseLessonEntity::getId))
                                .map(lesson -> toOutlineLesson(lesson, records.get(lesson.getId())))
                                .toList()))
                .toList();
        return new CourseOutlineVO(assembler.id(course.getId()), course.getName(),
                CodeLabelVO.of(CourseStatus.valueOf(course.getStatus())), chapterVOs);
    }

    @Transactional(readOnly = true)
    public StudentLessonDetailVO lesson(Long studentId, Long lessonId) {
        CourseLessonEntity lesson = requireAccessibleLesson(studentId, lessonId);
        LessonLearningRecordEntity record = findRecord(studentId, lessonId);
        return toStudentLesson(lesson, record);
    }

    @Transactional
    public LearningRecordVO start(Long studentId, Long lessonId) {
        CourseLessonEntity lesson = requireAccessibleLesson(studentId, lessonId);
        LessonLearningRecordEntity record = findRecord(studentId, lessonId);
        LocalDateTime now = now();
        if (record == null) {
            record = newRecord(studentId, lesson, LearningStatus.IN_PROGRESS, now);
            insertIdempotently(record, studentId, lessonId);
        } else if (!LearningStatus.COMPLETED.name().equals(record.getStatus())) {
            record.setStatus(LearningStatus.IN_PROGRESS.name());
            if (record.getStartedAt() == null) {
                record.setStartedAt(now);
            }
            record.setLastStudiedAt(now);
            updateRecordOrConflict(record);
        }
        return assembler.toLearningRecord(record);
    }

    @Transactional
    public LearningRecordVO complete(Long studentId, Long lessonId) {
        CourseLessonEntity lesson = requireAccessibleLesson(studentId, lessonId);
        LessonLearningRecordEntity record = findRecord(studentId, lessonId);
        if (record != null && LearningStatus.COMPLETED.name().equals(record.getStatus())) {
            return assembler.toLearningRecord(record);
        }
        LocalDateTime now = now();
        if (record == null) {
            record = newRecord(studentId, lesson, LearningStatus.COMPLETED, now);
            record.setCompletedAt(now);
            insertIdempotently(record, studentId, lessonId);
            if (!LearningStatus.COMPLETED.name().equals(record.getStatus())) {
                return completeExisting(record, now);
            }
        } else {
            return completeExisting(record, now);
        }
        return assembler.toLearningRecord(record);
    }

    @Transactional(readOnly = true)
    public CourseProgressVO progress(Long studentId, Long courseId) {
        requireStudentCourse(studentId, courseId);
        List<CourseLessonEntity> allLessons = lessonMapper.selectList(Wrappers.<CourseLessonEntity>lambdaQuery()
                .eq(CourseLessonEntity::getCourseId, courseId));
        Set<Long> publishedChapterIds = publishedChapters(courseId).stream()
                .map(CourseChapterEntity::getId).collect(java.util.stream.Collectors.toSet());
        List<CourseLessonEntity> available = allLessons.stream()
                .filter(lesson -> LessonStatus.PUBLISHED.name().equals(lesson.getStatus()))
                .filter(lesson -> publishedChapterIds.contains(lesson.getChapterId()))
                .filter(permissionService::isUnlocked)
                .sorted(Comparator.comparing(CourseLessonEntity::getChapterId)
                        .thenComparing(CourseLessonEntity::getSortOrder)
                .thenComparing(CourseLessonEntity::getId))
                .toList();
        Set<Long> availableIds = available.stream()
                .map(CourseLessonEntity::getId)
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, LessonLearningRecordEntity> records = learningRecords(studentId, courseId);
        Set<Long> completedIds = new HashSet<>();
        records.forEach((lessonId, record) -> {
            if (LearningStatus.COMPLETED.name().equals(record.getStatus())) {
                completedIds.add(lessonId);
            }
        });
        long completed = available.stream().filter(lesson -> completedIds.contains(lesson.getId())).count();
        BigDecimal percent = available.isEmpty()
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(completed * 100L)
                        .divide(BigDecimal.valueOf(available.size()), 2, RoundingMode.HALF_UP);
        String lastLessonId = records.values().stream()
                .filter(record -> availableIds.contains(record.getLessonId()))
                .filter(record -> record.getLastStudiedAt() != null)
                .max(Comparator.comparing(LessonLearningRecordEntity::getLastStudiedAt))
                .map(record -> assembler.id(record.getLessonId()))
                .orElse(null);
        String nextLessonId = available.stream()
                .filter(lesson -> !completedIds.contains(lesson.getId()))
                .findFirst().map(lesson -> assembler.id(lesson.getId())).orElse(null);
        return new CourseProgressVO(assembler.id(courseId), allLessons.size(), available.size(), completed,
                percent, lastLessonId, nextLessonId);
    }

    @Transactional(readOnly = true)
    public MaterialAccessVO material(Long studentId, Long materialId) {
        CourseMaterialEntity material = materialMapper.selectById(materialId);
        if (material == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "课程资料不存在");
        }
        if (!permissionService.canAccessMaterial(studentId, materialId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "你无权访问该课程资料");
        }
        MaterialType type = MaterialType.valueOf(material.getMaterialType());
        boolean externalLink = type == MaterialType.LINK && material.getFileUrl() != null;
        boolean managedFile = material.getFileId() != null;
        return new MaterialAccessVO(assembler.id(material.getId()), material.getName(), CodeLabelVO.of(type),
                material.getFileSize(), material.getMimeType(),
                managedFile ? "MANAGED_FILE" : externalLink ? "EXTERNAL_LINK" : "MOCK_METADATA_ONLY",
                managedFile || externalLink ? material.getFileUrl() : null);
    }

    private LearningRecordVO completeExisting(LessonLearningRecordEntity record, LocalDateTime now) {
        record.setStatus(LearningStatus.COMPLETED.name());
        if (record.getStartedAt() == null) {
            record.setStartedAt(now);
        }
        record.setCompletedAt(now);
        record.setLastStudiedAt(now);
        updateRecordOrConflict(record);
        return assembler.toLearningRecord(record);
    }

    private void insertIdempotently(
            LessonLearningRecordEntity record, Long studentId, Long lessonId) {
        try {
            learningRecordMapper.insert(record);
        } catch (DuplicateKeyException exception) {
            LessonLearningRecordEntity existing = findRecord(studentId, lessonId);
            if (existing == null) {
                throw exception;
            }
            copyRecord(record, existing);
        }
    }

    private LessonLearningRecordEntity newRecord(
            Long studentId, CourseLessonEntity lesson, LearningStatus status, LocalDateTime now) {
        LessonLearningRecordEntity record = new LessonLearningRecordEntity();
        record.setCourseId(lesson.getCourseId());
        record.setChapterId(lesson.getChapterId());
        record.setLessonId(lesson.getId());
        record.setStudentId(studentId);
        record.setStatus(status.name());
        record.setStartedAt(now);
        record.setLastStudiedAt(now);
        record.setStudySeconds(0L);
        return record;
    }

    private StudentLessonDetailVO toStudentLesson(
            CourseLessonEntity lesson, LessonLearningRecordEntity record) {
        return new StudentLessonDetailVO(assembler.id(lesson.getId()), assembler.id(lesson.getCourseId()),
                assembler.id(lesson.getChapterId()), lesson.getTitle(),
                CodeLabelVO.of(LessonContentType.valueOf(lesson.getContentType())), lesson.getContent(),
                lesson.getVideoUrl(), lesson.getEstimatedMinutes(),
                CodeLabelVO.of(LessonStatus.valueOf(lesson.getStatus())), assembler.time(lesson.getUnlockAt()),
                record == null ? null : assembler.toLearningRecord(record));
    }

    private LessonOutlineVO toOutlineLesson(
            CourseLessonEntity lesson, LessonLearningRecordEntity record) {
        LearningStatus status = record == null ? LearningStatus.NOT_STARTED : LearningStatus.valueOf(record.getStatus());
        return new LessonOutlineVO(assembler.id(lesson.getId()), lesson.getTitle(), lesson.getSortOrder(),
                CodeLabelVO.of(LessonContentType.valueOf(lesson.getContentType())), lesson.getEstimatedMinutes(),
                true, status == LearningStatus.COMPLETED, CodeLabelVO.of(status));
    }

    private CourseEntity requireStudentCourse(Long studentId, Long courseId) {
        CourseEntity course = courseService.requireCourse(courseId);
        if (!permissionService.canViewCourseAsStudent(studentId, courseId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "你尚未选修该课程或课程不可学习");
        }
        return course;
    }

    private CourseLessonEntity requireAccessibleLesson(Long studentId, Long lessonId) {
        CourseLessonEntity lesson = lessonMapper.selectById(lessonId);
        if (lesson == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "课时不存在");
        }
        if (!permissionService.canViewCourseAsStudent(studentId, lesson.getCourseId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "你尚未选修该课时所属课程");
        }
        CourseChapterEntity chapter = chapterMapper.selectById(lesson.getChapterId());
        if (chapter == null
                || !lesson.getCourseId().equals(chapter.getCourseId())
                || !ChapterStatus.PUBLISHED.name().equals(chapter.getStatus())
                || !LessonStatus.PUBLISHED.name().equals(lesson.getStatus())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "章节或课时尚未发布");
        }
        if (!permissionService.isUnlocked(lesson)) {
            throw new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, "课时尚未解锁");
        }
        return lesson;
    }

    private List<CourseChapterEntity> publishedChapters(Long courseId) {
        return chapterMapper.selectList(Wrappers.<CourseChapterEntity>lambdaQuery()
                .eq(CourseChapterEntity::getCourseId, courseId)
                .eq(CourseChapterEntity::getStatus, ChapterStatus.PUBLISHED.name())
                .orderByAsc(CourseChapterEntity::getSortOrder)
                .orderByAsc(CourseChapterEntity::getId));
    }

    private List<CourseLessonEntity> publishedLessons(Long courseId) {
        return lessonMapper.selectList(Wrappers.<CourseLessonEntity>lambdaQuery()
                .eq(CourseLessonEntity::getCourseId, courseId)
                .eq(CourseLessonEntity::getStatus, LessonStatus.PUBLISHED.name())
                .orderByAsc(CourseLessonEntity::getSortOrder)
                .orderByAsc(CourseLessonEntity::getId));
    }

    private Map<Long, LessonLearningRecordEntity> learningRecords(Long studentId, Long courseId) {
        Map<Long, LessonLearningRecordEntity> result = new HashMap<>();
        learningRecordMapper.selectList(Wrappers.<LessonLearningRecordEntity>lambdaQuery()
                        .eq(LessonLearningRecordEntity::getStudentId, studentId)
                        .eq(LessonLearningRecordEntity::getCourseId, courseId))
                .forEach(record -> result.put(record.getLessonId(), record));
        return result;
    }

    private LessonLearningRecordEntity findRecord(Long studentId, Long lessonId) {
        return learningRecordMapper.selectOne(Wrappers.<LessonLearningRecordEntity>lambdaQuery()
                .eq(LessonLearningRecordEntity::getStudentId, studentId)
                .eq(LessonLearningRecordEntity::getLessonId, lessonId));
    }

    private void updateRecordOrConflict(LessonLearningRecordEntity record) {
        if (learningRecordMapper.updateById(record) != 1) {
            throw new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, "学习记录已变化，请重试");
        }
    }

    private void copyRecord(LessonLearningRecordEntity target, LessonLearningRecordEntity source) {
        target.setId(source.getId());
        target.setCourseId(source.getCourseId());
        target.setChapterId(source.getChapterId());
        target.setLessonId(source.getLessonId());
        target.setStudentId(source.getStudentId());
        target.setStatus(source.getStatus());
        target.setStartedAt(source.getStartedAt());
        target.setCompletedAt(source.getCompletedAt());
        target.setLastStudiedAt(source.getLastStudiedAt());
        target.setStudySeconds(source.getStudySeconds());
        target.setVersion(source.getVersion());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
