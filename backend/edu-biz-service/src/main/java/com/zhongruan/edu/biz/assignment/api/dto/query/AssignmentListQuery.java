package com.zhongruan.edu.biz.assignment.api.dto.query;

import com.zhongruan.edu.biz.assignment.domain.enums.AssignmentStatus;
import com.zhongruan.edu.biz.assignment.domain.enums.SubmissionStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class AssignmentListQuery {
    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(100)
    private int size = 20;

    @Size(max = 100)
    private String keyword;

    private AssignmentStatus status;
    private SubmissionStatus submissionStatus;

    @Size(max = 32)
    private String sort = "createdAt,desc";

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public AssignmentStatus getStatus() { return status; }
    public void setStatus(AssignmentStatus status) { this.status = status; }
    public SubmissionStatus getSubmissionStatus() { return submissionStatus; }
    public void setSubmissionStatus(SubmissionStatus submissionStatus) { this.submissionStatus = submissionStatus; }
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
}
