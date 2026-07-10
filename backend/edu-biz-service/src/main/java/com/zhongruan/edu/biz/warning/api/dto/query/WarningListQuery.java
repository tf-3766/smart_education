package com.zhongruan.edu.biz.warning.api.dto.query;

import com.zhongruan.edu.biz.warning.domain.enums.WarningLevel;
import com.zhongruan.edu.biz.warning.domain.enums.WarningStatus;
import com.zhongruan.edu.biz.warning.domain.enums.WarningType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class WarningListQuery {
    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(100)
    private int size = 20;

    private Long courseId;
    private Long studentId;
    private WarningType warningType;
    private WarningLevel warningLevel;
    private WarningStatus warningStatus;

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public WarningType getWarningType() { return warningType; }
    public void setWarningType(WarningType warningType) { this.warningType = warningType; }
    public WarningLevel getWarningLevel() { return warningLevel; }
    public void setWarningLevel(WarningLevel warningLevel) { this.warningLevel = warningLevel; }
    public WarningStatus getWarningStatus() { return warningStatus; }
    public void setWarningStatus(WarningStatus warningStatus) { this.warningStatus = warningStatus; }
}
