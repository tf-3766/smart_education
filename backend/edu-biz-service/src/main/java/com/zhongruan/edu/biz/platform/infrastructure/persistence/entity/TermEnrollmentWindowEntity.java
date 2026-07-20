package com.zhongruan.edu.biz.platform.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhongruan.edu.biz.shared.persistence.BaseAuditEntity;
import java.time.LocalDateTime;

@TableName("edu_term_enrollment_window")
public class TermEnrollmentWindowEntity extends BaseAuditEntity {
    private String term;
    private LocalDateTime enrollmentOpenAt;
    private LocalDateTime enrollmentCloseAt;

    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }
    public LocalDateTime getEnrollmentOpenAt() { return enrollmentOpenAt; }
    public void setEnrollmentOpenAt(LocalDateTime enrollmentOpenAt) { this.enrollmentOpenAt = enrollmentOpenAt; }
    public LocalDateTime getEnrollmentCloseAt() { return enrollmentCloseAt; }
    public void setEnrollmentCloseAt(LocalDateTime enrollmentCloseAt) { this.enrollmentCloseAt = enrollmentCloseAt; }
}
