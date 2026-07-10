package com.zhongruan.edu.biz.exam.api.dto.query;

import com.zhongruan.edu.biz.exam.domain.enums.QuestionDifficulty;
import com.zhongruan.edu.biz.exam.domain.enums.QuestionStatus;
import com.zhongruan.edu.biz.exam.domain.enums.QuestionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class QuestionListQuery {
    @Min(1)
    private long page = 1;

    @Min(1)
    @Max(100)
    private long size = 20;

    @Size(max = 100)
    private String keyword;

    private QuestionType questionType;
    private QuestionDifficulty difficulty;
    private QuestionStatus status;

    public long getPage() { return page; }
    public void setPage(long page) { this.page = page; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public QuestionType getQuestionType() { return questionType; }
    public void setQuestionType(QuestionType questionType) { this.questionType = questionType; }
    public QuestionDifficulty getDifficulty() { return difficulty; }
    public void setDifficulty(QuestionDifficulty difficulty) { this.difficulty = difficulty; }
    public QuestionStatus getStatus() { return status; }
    public void setStatus(QuestionStatus status) { this.status = status; }
}
