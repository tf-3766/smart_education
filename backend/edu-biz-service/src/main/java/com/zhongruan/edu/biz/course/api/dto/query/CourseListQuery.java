package com.zhongruan.edu.biz.course.api.dto.query;

import com.zhongruan.edu.biz.course.domain.enums.CourseReviewStatus;
import com.zhongruan.edu.biz.course.domain.enums.CourseStatus;
import com.zhongruan.edu.biz.course.domain.enums.EnrollmentStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class CourseListQuery {
    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(100)
    private int size = 20;

    @Size(max = 100)
    private String keyword;

    private CourseStatus status;
    private CourseReviewStatus reviewStatus;
    private EnrollmentStatus enrollmentStatus;
    private Boolean formalOnly;

    @Size(max = 32)
    private String term;

    private Long categoryId;

    @Size(max = 32)
    private String sort = "createdAt,desc";

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public CourseStatus getStatus() { return status; }
    public void setStatus(CourseStatus status) { this.status = status; }
    public CourseReviewStatus getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(CourseReviewStatus reviewStatus) { this.reviewStatus = reviewStatus; }
    public EnrollmentStatus getEnrollmentStatus() { return enrollmentStatus; }
    public void setEnrollmentStatus(EnrollmentStatus enrollmentStatus) { this.enrollmentStatus = enrollmentStatus; }
    public Boolean getFormalOnly() { return formalOnly; }
    public void setFormalOnly(Boolean formalOnly) { this.formalOnly = formalOnly; }
    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
}
