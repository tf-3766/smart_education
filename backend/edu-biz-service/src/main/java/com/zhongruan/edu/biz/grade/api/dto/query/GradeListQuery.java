package com.zhongruan.edu.biz.grade.api.dto.query;

import com.zhongruan.edu.biz.grade.domain.enums.GradeStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class GradeListQuery {
    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(100)
    private int size = 20;

    private Long courseId;
    private Long assignmentId;

    @Size(max = 32)
    private String sourceType = "ASSIGNMENT";

    private GradeStatus status;

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public GradeStatus getStatus() { return status; }
    public void setStatus(GradeStatus status) { this.status = status; }
}
