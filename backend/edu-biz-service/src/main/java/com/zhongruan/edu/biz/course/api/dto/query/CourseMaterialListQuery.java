package com.zhongruan.edu.biz.course.api.dto.query;

import com.zhongruan.edu.biz.course.domain.enums.MaterialStatus;
import com.zhongruan.edu.biz.course.domain.enums.MaterialVisibility;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class CourseMaterialListQuery {
    @Min(1)
    private long page = 1;

    @Min(1)
    @Max(100)
    private long size = 20;

    private String keyword;
    private MaterialStatus status;
    private MaterialVisibility visibility;

    public long getPage() { return page; }
    public void setPage(long page) { this.page = page; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public MaterialStatus getStatus() { return status; }
    public void setStatus(MaterialStatus status) { this.status = status; }
    public MaterialVisibility getVisibility() { return visibility; }
    public void setVisibility(MaterialVisibility visibility) { this.visibility = visibility; }
}
