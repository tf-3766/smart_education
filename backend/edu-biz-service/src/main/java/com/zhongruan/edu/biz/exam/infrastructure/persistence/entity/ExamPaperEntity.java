package com.zhongruan.edu.biz.exam.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhongruan.edu.biz.shared.persistence.BaseAuditEntity;
import java.math.BigDecimal;

@TableName("edu_exam_paper")
public class ExamPaperEntity extends BaseAuditEntity {
    private Long examId;
    private Long courseId;
    private String title;
    private BigDecimal totalScore;
    private String status;
    private Long aiGenerationRecordId;

    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getAiGenerationRecordId() { return aiGenerationRecordId; }
    public void setAiGenerationRecordId(Long aiGenerationRecordId) { this.aiGenerationRecordId = aiGenerationRecordId; }
}
