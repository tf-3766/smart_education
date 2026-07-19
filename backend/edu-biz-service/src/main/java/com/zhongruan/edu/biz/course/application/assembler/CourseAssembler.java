package com.zhongruan.edu.biz.course.application.assembler;

import com.zhongruan.edu.biz.course.api.vo.CodeLabelVO;
import com.zhongruan.edu.biz.course.api.vo.CourseDetailVO;
import com.zhongruan.edu.biz.course.api.vo.CourseReviewListItemVO;
import com.zhongruan.edu.biz.course.api.vo.CourseReviewVO;
import com.zhongruan.edu.biz.course.api.vo.CourseTeacherVO;
import com.zhongruan.edu.biz.course.api.vo.EnrollmentVO;
import com.zhongruan.edu.biz.course.api.vo.StudentCourseListItemVO;
import com.zhongruan.edu.biz.course.api.vo.TeacherCourseListItemVO;
import com.zhongruan.edu.biz.course.domain.enums.CourseReviewStatus;
import com.zhongruan.edu.biz.course.domain.enums.CourseStatus;
import com.zhongruan.edu.biz.course.domain.enums.CourseTeacherRole;
import com.zhongruan.edu.biz.course.domain.enums.CourseTeacherStatus;
import com.zhongruan.edu.biz.course.domain.enums.EnrollmentStatus;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseEnrollmentEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseReviewEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseTeacherEntity;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class CourseAssembler {
    public CourseDetailVO toDetail(CourseEntity course, String ownerName, String reviewReason, boolean internalView) {
        return new CourseDetailVO(
                id(course.getId()),
                course.getCourseCode(),
                course.getName(),
                course.getSummary(),
                course.getCoverUrl(),
                id(course.getCategoryId()),
                course.getTerm(),
                course.getDepartment(),
                course.getCredit(),
                id(course.getOwnerTeacherId()),
                ownerName,
                CodeLabelVO.of(CourseStatus.valueOf(course.getStatus())),
                CodeLabelVO.of(CourseReviewStatus.valueOf(course.getReviewStatus())),
                time(course.getEnrollmentOpenAt()),
                time(course.getEnrollmentCloseAt()),
                time(course.getStartAt()),
                time(course.getEndAt()),
                internalView ? reviewReason : null,
                internalView ? course.getVersion() : null);
    }

    public TeacherCourseListItemVO toTeacherListItem(CourseEntity course, String ownerName) {
        return new TeacherCourseListItemVO(
                id(course.getId()),
                course.getCourseCode(),
                course.getName(),
                course.getTerm(),
                id(course.getOwnerTeacherId()),
                ownerName,
                CodeLabelVO.of(CourseStatus.valueOf(course.getStatus())),
                CodeLabelVO.of(CourseReviewStatus.valueOf(course.getReviewStatus())),
                time(course.getStartAt()),
                time(course.getEndAt()),
                time(course.getUpdatedAt()));
    }

    public StudentCourseListItemVO toStudentListItem(
            CourseEntity course, String ownerName, CourseEnrollmentEntity enrollment, boolean enrollable) {
        return new StudentCourseListItemVO(
                id(course.getId()),
                course.getCourseCode(),
                course.getName(),
                course.getSummary(),
                course.getCoverUrl(),
                course.getTerm(),
                course.getCredit(),
                ownerName,
                CodeLabelVO.of(CourseStatus.valueOf(course.getStatus())),
                enrollment == null ? null : CodeLabelVO.of(EnrollmentStatus.valueOf(enrollment.getStatus())),
                enrollable,
                time(course.getStartAt()),
                time(course.getEndAt()));
    }

    public CourseTeacherVO toTeacher(CourseTeacherEntity relation, String teacherName) {
        return new CourseTeacherVO(
                id(relation.getId()),
                id(relation.getCourseId()),
                id(relation.getTeacherId()),
                teacherName,
                CodeLabelVO.of(CourseTeacherRole.valueOf(relation.getRole())),
                CodeLabelVO.of(CourseTeacherStatus.valueOf(relation.getStatus())),
                relation.getVersion());
    }

    public EnrollmentVO toEnrollment(CourseEnrollmentEntity enrollment) {
        return new EnrollmentVO(
                id(enrollment.getId()),
                id(enrollment.getCourseId()),
                id(enrollment.getStudentId()),
                CodeLabelVO.of(EnrollmentStatus.valueOf(enrollment.getStatus())),
                time(enrollment.getEnrolledAt()),
                time(enrollment.getWithdrawnAt()),
                enrollment.getVersion());
    }

    public CourseReviewVO toReview(CourseReviewEntity review, String reviewerName) {
        return new CourseReviewVO(
                id(review.getId()),
                id(review.getCourseId()),
                CodeLabelVO.of(CourseReviewStatus.valueOf(review.getReviewStatus())),
                id(review.getReviewerId()),
                reviewerName,
                review.getReason(),
                review.getRemark(),
                time(review.getReviewedAt()));
    }

    public CourseReviewListItemVO toReviewListItem(CourseEntity course, String ownerName) {
        return new CourseReviewListItemVO(
                id(course.getId()),
                course.getCourseCode(),
                course.getName(),
                id(course.getOwnerTeacherId()),
                ownerName,
                course.getTerm(),
                CodeLabelVO.of(CourseStatus.valueOf(course.getStatus())),
                CodeLabelVO.of(CourseReviewStatus.valueOf(course.getReviewStatus())),
                time(course.getUpdatedAt()));
    }

    public OffsetDateTime time(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private String id(Long value) {
        return value == null ? null : String.valueOf(value);
    }
}
