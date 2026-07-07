package com.zhongruan.edu.biz.course.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhongruan.edu.biz.shared.persistence.BaseAuditEntity;
import java.time.LocalDateTime;

@TableName("edu_lesson_learning_record")
public class LessonLearningRecordEntity extends BaseAuditEntity {
    private Long courseId;
    private Long chapterId;
    private Long lessonId;
    private Long studentId;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime lastStudiedAt;
    private Long studySeconds;

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getChapterId() { return chapterId; }
    public void setChapterId(Long chapterId) { this.chapterId = chapterId; }
    public Long getLessonId() { return lessonId; }
    public void setLessonId(Long lessonId) { this.lessonId = lessonId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getLastStudiedAt() { return lastStudiedAt; }
    public void setLastStudiedAt(LocalDateTime lastStudiedAt) { this.lastStudiedAt = lastStudiedAt; }
    public Long getStudySeconds() { return studySeconds; }
    public void setStudySeconds(Long studySeconds) { this.studySeconds = studySeconds; }
}
