package com.zhongruan.edu.biz.course.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhongruan.edu.biz.shared.persistence.BaseAuditEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("edu_course")
public class CourseEntity extends BaseAuditEntity {
    private String courseCode;
    private String name;
    private String summary;
    private String coverUrl;
    private Long categoryId;
    private String term;
    private String department;
    private BigDecimal credit;
    private Long ownerTeacherId;
    private String status;
    private String reviewStatus;
    private LocalDateTime enrollmentOpenAt;
    private LocalDateTime enrollmentCloseAt;
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public BigDecimal getCredit() { return credit; }
    public void setCredit(BigDecimal credit) { this.credit = credit; }
    public Long getOwnerTeacherId() { return ownerTeacherId; }
    public void setOwnerTeacherId(Long ownerTeacherId) { this.ownerTeacherId = ownerTeacherId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public LocalDateTime getEnrollmentOpenAt() { return enrollmentOpenAt; }
    public void setEnrollmentOpenAt(LocalDateTime enrollmentOpenAt) { this.enrollmentOpenAt = enrollmentOpenAt; }
    public LocalDateTime getEnrollmentCloseAt() { return enrollmentCloseAt; }
    public void setEnrollmentCloseAt(LocalDateTime enrollmentCloseAt) { this.enrollmentCloseAt = enrollmentCloseAt; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
}
