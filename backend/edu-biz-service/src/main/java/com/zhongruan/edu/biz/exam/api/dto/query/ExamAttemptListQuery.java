package com.zhongruan.edu.biz.exam.api.dto.query;

import com.zhongruan.edu.biz.exam.domain.enums.ExamAttemptStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class ExamAttemptListQuery {
    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(100)
    private int size = 20;

    private ExamAttemptStatus status;

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public ExamAttemptStatus getStatus() { return status; }
    public void setStatus(ExamAttemptStatus status) { this.status = status; }
}
