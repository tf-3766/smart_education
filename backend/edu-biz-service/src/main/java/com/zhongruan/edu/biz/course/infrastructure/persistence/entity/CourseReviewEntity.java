package com.zhongruan.edu.biz.course.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhongruan.edu.biz.shared.persistence.BaseAuditEntity;
import java.time.LocalDateTime;

@TableName("edu_course_review")
public class CourseReviewEntity extends BaseAuditEntity {
    private Long courseId;
    private String reviewStatus;
    private Long reviewerId;
    private String reason;
    private String remark;
    private LocalDateTime reviewedAt;

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
