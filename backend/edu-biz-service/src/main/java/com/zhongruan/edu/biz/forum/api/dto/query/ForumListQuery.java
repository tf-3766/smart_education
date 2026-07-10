package com.zhongruan.edu.biz.forum.api.dto.query;

import com.zhongruan.edu.biz.forum.domain.enums.ForumContentStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class ForumListQuery {
    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(100)
    private int size = 20;

    private ForumContentStatus status;

    @Size(max = 100)
    private String keyword;

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public ForumContentStatus getStatus() { return status; }
    public void setStatus(ForumContentStatus status) { this.status = status; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
}
