package com.zhongruan.edu.biz.course.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhongruan.edu.biz.course.api.dto.query.CourseMaterialListQuery;
import com.zhongruan.edu.biz.course.api.dto.request.CreateChapterRequest;
import com.zhongruan.edu.biz.course.api.dto.request.CreateCourseMaterialRequest;
import com.zhongruan.edu.biz.course.api.dto.request.CreateLessonRequest;
import com.zhongruan.edu.biz.course.api.dto.request.UpdateChapterRequest;
import com.zhongruan.edu.biz.course.api.dto.request.UpdateCourseMaterialRequest;
import com.zhongruan.edu.biz.course.api.dto.request.UpdateLessonRequest;
import com.zhongruan.edu.biz.course.api.vo.ChapterDetailVO;
import com.zhongruan.edu.biz.course.api.vo.CourseMaterialVO;
import com.zhongruan.edu.biz.course.api.vo.LessonDetailVO;
import com.zhongruan.edu.biz.course.application.assembler.CourseContentAssembler;
import com.zhongruan.edu.biz.course.domain.enums.ChapterStatus;
import com.zhongruan.edu.biz.course.domain.enums.CourseStatus;
import com.zhongruan.edu.biz.course.domain.enums.LessonStatus;
import com.zhongruan.edu.biz.course.domain.enums.LessonUnlockType;
import com.zhongruan.edu.biz.course.domain.enums.MaterialStatus;
import com.zhongruan.edu.biz.course.domain.enums.MaterialVisibility;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseChapterEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseLessonEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseMaterialEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseChapterMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseLessonMapper;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseMaterialMapper;
import com.zhongruan.edu.biz.storage.application.service.FileStorageService;
import com.zhongruan.edu.biz.storage.domain.FilePurpose;
import com.zhongruan.edu.biz.storage.infrastructure.persistence.entity.StoredFileEntity;
import com.zhongruan.edu.common.api.PageResponse;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseContentService {
    private final CourseChapterMapper chapterMapper;
    private final CourseLessonMapper lessonMapper;
    private final CourseMaterialMapper materialMapper;
    private final CourseManagementService courseService;
    private final CourseContentAssembler assembler;
    private final FileStorageService fileStorageService;

    public CourseContentService(
            CourseChapterMapper chapterMapper,
            CourseLessonMapper lessonMapper,
            CourseMaterialMapper materialMapper,
            CourseManagementService courseService,
            CourseContentAssembler assembler,
            FileStorageService fileStorageService) {
        this.chapterMapper = chapterMapper;
        this.lessonMapper = lessonMapper;
        this.materialMapper = materialMapper;
        this.courseService = courseService;
        this.assembler = assembler;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public List<ChapterDetailVO> listChapters(Long teacherId, Long courseId) {
        courseService.requireEditor(teacherId, courseId);
        return chapterMapper.selectList(Wrappers.<CourseChapterEntity>lambdaQuery()
                        .eq(CourseChapterEntity::getCourseId, courseId)
                        .orderByAsc(CourseChapterEntity::getSortOrder)
                        .orderByAsc(CourseChapterEntity::getId))
                .stream().map(assembler::toChapter).toList();
    }

    @Transactional
    public ChapterDetailVO createChapter(Long teacherId, Long courseId, CreateChapterRequest request) {
        courseService.requireEditor(teacherId, courseId);
        CourseChapterEntity chapter = new CourseChapterEntity();
        chapter.setCourseId(courseId);
        chapter.setTitle(request.title().trim());
        chapter.setDescription(trim(request.description()));
        chapter.setSortOrder(request.sortOrder());
        chapter.setStatus(ChapterStatus.DRAFT.name());
        chapterMapper.insert(chapter);
        return assembler.toChapter(chapter);
    }

    @Transactional
    public ChapterDetailVO updateChapter(Long teacherId, Long chapterId, UpdateChapterRequest request) {
        CourseChapterEntity chapter = requireChapter(chapterId);
        courseService.requireEditor(teacherId, chapter.getCourseId());
        chapter.setTitle(request.title().trim());
        chapter.setDescription(trim(request.description()));
        chapter.setSortOrder(request.sortOrder());
        chapter.setVersion(request.version());
        updateChapterOrConflict(chapter);
        return assembler.toChapter(chapter);
    }

    @Transactional
    public void deleteChapter(Long teacherId, Long chapterId) {
        CourseChapterEntity chapter = requireChapter(chapterId);
        courseService.requireEditor(teacherId, chapter.getCourseId());
        chapterMapper.deleteById(chapterId);
    }

    @Transactional
    public ChapterDetailVO publishChapter(Long teacherId, Long chapterId) {
        CourseChapterEntity chapter = requireChapter(chapterId);
        courseService.requireEditor(teacherId, chapter.getCourseId());
        requirePublishedCourse(chapter.getCourseId());
        ChapterStatus status = ChapterStatus.valueOf(chapter.getStatus());
        if (!status.canTransitionTo(ChapterStatus.PUBLISHED)) {
            throw stateConflict("当前章节状态不能发布");
        }
        chapter.setStatus(ChapterStatus.PUBLISHED.name());
        chapter.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));
        updateChapterOrConflict(chapter);
        return assembler.toChapter(chapter);
    }

    @Transactional
    public ChapterDetailVO offlineChapter(Long teacherId, Long chapterId) {
        CourseChapterEntity chapter = requireChapter(chapterId);
        courseService.requireEditor(teacherId, chapter.getCourseId());
        ChapterStatus status = ChapterStatus.valueOf(chapter.getStatus());
        if (!status.canTransitionTo(ChapterStatus.OFFLINE)) {
            throw stateConflict("当前章节状态不能下线");
        }
        chapter.setStatus(ChapterStatus.OFFLINE.name());
        updateChapterOrConflict(chapter);
        return assembler.toChapter(chapter);
    }

    @Transactional(readOnly = true)
    public List<LessonDetailVO> listLessons(Long teacherId, Long chapterId) {
        CourseChapterEntity chapter = requireChapter(chapterId);
        courseService.requireEditor(teacherId, chapter.getCourseId());
        return lessonMapper.selectList(Wrappers.<CourseLessonEntity>lambdaQuery()
                        .eq(CourseLessonEntity::getChapterId, chapterId)
                        .orderByAsc(CourseLessonEntity::getSortOrder)
                        .orderByAsc(CourseLessonEntity::getId))
                .stream().map(assembler::toLesson).toList();
    }

    @Transactional
    public LessonDetailVO createLesson(Long teacherId, Long chapterId, CreateLessonRequest request) {
        CourseChapterEntity chapter = requireChapter(chapterId);
        courseService.requireEditor(teacherId, chapter.getCourseId());
        validateCourseAssertion(request.courseId(), chapter.getCourseId());
        validateUnlock(request.unlockType(), request.unlockAt());
        CourseLessonEntity lesson = new CourseLessonEntity();
        lesson.setCourseId(chapter.getCourseId());
        lesson.setChapterId(chapterId);
        applyLesson(lesson, request.title(), request.contentType().name(), request.content(), request.videoUrl(),
                request.estimatedMinutes(), request.sortOrder(), request.unlockType(), request.unlockAt());
        lesson.setStatus(LessonStatus.DRAFT.name());
        lessonMapper.insert(lesson);
        return assembler.toLesson(lesson);
    }

    @Transactional(readOnly = true)
    public LessonDetailVO getLesson(Long teacherId, Long lessonId) {
        CourseLessonEntity lesson = requireLesson(lessonId);
        courseService.requireEditor(teacherId, lesson.getCourseId());
        return assembler.toLesson(lesson);
    }

    @Transactional
    public LessonDetailVO updateLesson(Long teacherId, Long lessonId, UpdateLessonRequest request) {
        CourseLessonEntity lesson = requireLesson(lessonId);
        courseService.requireEditor(teacherId, lesson.getCourseId());
        CourseChapterEntity chapter = requireChapter(lesson.getChapterId());
        if (!chapter.getCourseId().equals(lesson.getCourseId())) {
            throw hierarchyConflict("课时与章节的课程归属不一致");
        }
        validateCourseAssertion(request.courseId(), chapter.getCourseId());
        validateUnlock(request.unlockType(), request.unlockAt());
        applyLesson(lesson, request.title(), request.contentType().name(), request.content(), request.videoUrl(),
                request.estimatedMinutes(), request.sortOrder(), request.unlockType(), request.unlockAt());
        lesson.setVersion(request.version());
        updateLessonOrConflict(lesson);
        return assembler.toLesson(lesson);
    }

    @Transactional
    public void deleteLesson(Long teacherId, Long lessonId) {
        CourseLessonEntity lesson = requireLesson(lessonId);
        courseService.requireEditor(teacherId, lesson.getCourseId());
        lessonMapper.deleteById(lessonId);
    }

    @Transactional
    public LessonDetailVO publishLesson(Long teacherId, Long lessonId) {
        CourseLessonEntity lesson = requireLesson(lessonId);
        courseService.requireEditor(teacherId, lesson.getCourseId());
        requirePublishedCourse(lesson.getCourseId());
        CourseChapterEntity chapter = requireChapter(lesson.getChapterId());
        if (!chapter.getCourseId().equals(lesson.getCourseId())
                || !ChapterStatus.PUBLISHED.name().equals(chapter.getStatus())) {
            throw stateConflict("所属章节发布后才能发布课时");
        }
        LessonStatus status = LessonStatus.valueOf(lesson.getStatus());
        if (!status.canTransitionTo(LessonStatus.PUBLISHED)) {
            throw stateConflict("当前课时状态不能发布");
        }
        lesson.setStatus(LessonStatus.PUBLISHED.name());
        lesson.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));
        updateLessonOrConflict(lesson);
        return assembler.toLesson(lesson);
    }

    @Transactional
    public LessonDetailVO offlineLesson(Long teacherId, Long lessonId) {
        CourseLessonEntity lesson = requireLesson(lessonId);
        courseService.requireEditor(teacherId, lesson.getCourseId());
        LessonStatus status = LessonStatus.valueOf(lesson.getStatus());
        if (!status.canTransitionTo(LessonStatus.OFFLINE)) {
            throw stateConflict("当前课时状态不能下线");
        }
        lesson.setStatus(LessonStatus.OFFLINE.name());
        updateLessonOrConflict(lesson);
        return assembler.toLesson(lesson);
    }

    @Transactional(readOnly = true)
    public PageResponse<CourseMaterialVO> listMaterials(
            Long teacherId, Long courseId, CourseMaterialListQuery query) {
        courseService.requireEditor(teacherId, courseId);
        var wrapper = Wrappers.<CourseMaterialEntity>lambdaQuery()
                .eq(CourseMaterialEntity::getCourseId, courseId);
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.like(CourseMaterialEntity::getName, query.getKeyword().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(CourseMaterialEntity::getStatus, query.getStatus().name());
        }
        if (query.getVisibility() != null) {
            wrapper.eq(CourseMaterialEntity::getVisibility, query.getVisibility().name());
        }
        wrapper.orderByAsc(CourseMaterialEntity::getSortOrder).orderByAsc(CourseMaterialEntity::getId);
        IPage<CourseMaterialEntity> page = materialMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        return PageResponse.of(page.getRecords().stream().map(assembler::toMaterial).toList(),
                page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Transactional
    public CourseMaterialVO createMaterial(
            Long teacherId, Long courseId, CreateCourseMaterialRequest request) {
        courseService.requireEditor(teacherId, courseId);
        validateMaterialHierarchy(courseId, request.chapterId(), request.lessonId(), request.visibility());
        FileReference file = fileReference(teacherId, request.fileId(), request.fileKey(), request.fileUrl(),
                request.fileSize(), request.mimeType(), FilePurpose.COURSE_MATERIAL);
        CourseMaterialEntity material = new CourseMaterialEntity();
        material.setCourseId(courseId);
        applyMaterial(material, request.chapterId(), request.lessonId(), request.name(), request.materialType().name(),
                file, request.visibility(),
                request.status() == null ? MaterialStatus.DRAFT : request.status(), request.sortOrder());
        materialMapper.insert(material);
        return assembler.toMaterial(material);
    }

    @Transactional
    public CourseMaterialVO updateMaterial(
            Long teacherId, Long materialId, UpdateCourseMaterialRequest request) {
        CourseMaterialEntity material = requireMaterial(materialId);
        courseService.requireEditor(teacherId, material.getCourseId());
        validateMaterialHierarchy(material.getCourseId(), request.chapterId(), request.lessonId(), request.visibility());
        FileReference file = fileReference(teacherId, request.fileId(), request.fileKey(), request.fileUrl(),
                request.fileSize(), request.mimeType(), FilePurpose.COURSE_MATERIAL);
        applyMaterial(material, request.chapterId(), request.lessonId(), request.name(), request.materialType().name(),
                file, request.visibility(),
                request.status(), request.sortOrder());
        material.setVersion(request.version());
        if (materialMapper.updateById(material) != 1) {
            throw hierarchyConflict("资料已被其他请求修改，请刷新后重试");
        }
        return assembler.toMaterial(material);
    }

    @Transactional
    public void deleteMaterial(Long teacherId, Long materialId) {
        CourseMaterialEntity material = requireMaterial(materialId);
        courseService.requireEditor(teacherId, material.getCourseId());
        materialMapper.deleteById(materialId);
    }

    private void applyLesson(
            CourseLessonEntity lesson, String title, String contentType, String content, String videoUrl,
            Integer estimatedMinutes, Integer sortOrder, LessonUnlockType unlockType, OffsetDateTime unlockAt) {
        lesson.setTitle(title.trim());
        lesson.setContentType(contentType);
        lesson.setContent(trim(content));
        lesson.setVideoUrl(trim(videoUrl));
        lesson.setEstimatedMinutes(estimatedMinutes);
        lesson.setSortOrder(sortOrder);
        lesson.setUnlockType(unlockType.name());
        lesson.setUnlockAt(utc(unlockAt));
    }

    private void applyMaterial(
            CourseMaterialEntity material, Long chapterId, Long lessonId, String name, String materialType,
            FileReference file, MaterialVisibility visibility,
            MaterialStatus status, Integer sortOrder) {
        material.setChapterId(chapterId);
        material.setLessonId(lessonId);
        material.setName(name.trim());
        material.setMaterialType(materialType);
        material.setFileId(file.fileId());
        material.setFileKey(file.fileKey());
        material.setFileUrl(file.fileUrl());
        material.setFileSize(file.fileSize());
        material.setMimeType(file.mimeType());
        material.setVisibility(visibility.name());
        material.setStatus(status.name());
        material.setSortOrder(sortOrder);
    }

    private void validateMaterialHierarchy(
            Long courseId, Long chapterId, Long lessonId, MaterialVisibility visibility) {
        if (visibility == MaterialVisibility.COURSE) {
            if (chapterId != null || lessonId != null) {
                throw hierarchyConflict("课程级资料不能关联章节或课时");
            }
            return;
        }
        if (chapterId == null) {
            throw hierarchyConflict("章节级或课时级资料必须关联章节");
        }
        CourseChapterEntity chapter = requireChapter(chapterId);
        if (!courseId.equals(chapter.getCourseId())) {
            throw hierarchyConflict("资料与章节不属于同一课程");
        }
        if (visibility == MaterialVisibility.CHAPTER) {
            if (lessonId != null) {
                throw hierarchyConflict("章节级资料不能关联课时");
            }
            return;
        }
        if (lessonId == null) {
            throw hierarchyConflict("课时级资料必须关联课时");
        }
        CourseLessonEntity lesson = requireLesson(lessonId);
        if (!courseId.equals(lesson.getCourseId()) || !chapterId.equals(lesson.getChapterId())) {
            throw hierarchyConflict("资料、章节和课时不属于同一课程层级");
        }
    }

    private void validateUnlock(LessonUnlockType type, OffsetDateTime unlockAt) {
        if (type == LessonUnlockType.SCHEDULED && unlockAt == null) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "定时解锁课时必须填写 unlockAt");
        }
        if (type == LessonUnlockType.IMMEDIATE && unlockAt != null) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "立即解锁课时不能填写 unlockAt");
        }
    }

    private void validateCourseAssertion(Long requestedCourseId, Long actualCourseId) {
        if (requestedCourseId != null && !requestedCourseId.equals(actualCourseId)) {
            throw hierarchyConflict("请求中的 courseId 与章节所属课程不一致");
        }
    }

    private FileReference fileReference(
            Long ownerId,
            Long fileId,
            String fileKey,
            String fileUrl,
            Long fileSize,
            String mimeType,
            FilePurpose purpose) {
        if (fileId != null) {
            StoredFileEntity stored = fileStorageService.requireOwnedFile(ownerId, fileId, purpose);
            return new FileReference(
                    stored.getId(),
                    stored.getObjectKey(),
                    fileStorageService.accessUrl(stored.getId()),
                    stored.getFileSize(),
                    stored.getMimeType());
        }
        if ((fileKey == null || fileKey.isBlank()) && (fileUrl == null || fileUrl.isBlank())) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "fileKey 与 fileUrl 至少填写一个");
        }
        return new FileReference(null, trim(fileKey), trim(fileUrl), fileSize, trim(mimeType));
    }

    private record FileReference(Long fileId, String fileKey, String fileUrl, Long fileSize, String mimeType) {}

    private void requirePublishedCourse(Long courseId) {
        CourseEntity course = courseService.requireCourse(courseId);
        CourseStatus status = CourseStatus.valueOf(course.getStatus());
        if (status != CourseStatus.PUBLISHED && status != CourseStatus.ONGOING) {
            throw stateConflict("课程发布后才能发布章节或课时");
        }
    }

    private CourseChapterEntity requireChapter(Long chapterId) {
        CourseChapterEntity chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "章节不存在");
        }
        return chapter;
    }

    private CourseLessonEntity requireLesson(Long lessonId) {
        CourseLessonEntity lesson = lessonMapper.selectById(lessonId);
        if (lesson == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "课时不存在");
        }
        return lesson;
    }

    private CourseMaterialEntity requireMaterial(Long materialId) {
        CourseMaterialEntity material = materialMapper.selectById(materialId);
        if (material == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "资料不存在");
        }
        return material;
    }

    private void updateChapterOrConflict(CourseChapterEntity chapter) {
        if (chapterMapper.updateById(chapter) != 1) {
            throw hierarchyConflict("章节已被其他请求修改，请刷新后重试");
        }
    }

    private void updateLessonOrConflict(CourseLessonEntity lesson) {
        if (lessonMapper.updateById(lesson) != 1) {
            throw hierarchyConflict("课时已被其他请求修改，请刷新后重试");
        }
    }

    private BusinessException hierarchyConflict(String message) {
        return new BusinessException(CommonErrorCode.RESOURCE_CONFLICT, message);
    }

    private BusinessException stateConflict(String message) {
        return new BusinessException(CommonErrorCode.OPERATION_NOT_ALLOWED, message);
    }

    private LocalDateTime utc(OffsetDateTime value) {
        return value == null ? null : value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
