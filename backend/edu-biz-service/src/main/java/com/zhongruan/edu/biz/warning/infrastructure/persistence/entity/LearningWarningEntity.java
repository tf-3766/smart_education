package com.zhongruan.edu.biz.warning.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhongruan.edu.biz.shared.persistence.BaseAuditEntity;
import java.time.LocalDateTime;

@TableName("edu_learning_warning")
public class LearningWarningEntity extends BaseAuditEntity {
    private Long courseId;
    private Long studentId;
    private String warningType;
    private String warningLevel;
    private String warningStatus;
    private String summary;
    private String suggestion;
    private Long aiExplanationDraftId;
    private LocalDateTime generatedAt;
    private Long handledBy;
    private String handleRemark;
    private LocalDateTime handledAt;

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getWarningType() { return warningType; }
    public void setWarningType(String warningType) { this.warningType = warningType; }
    public String getWarningLevel() { return warningLevel; }
    public void setWarningLevel(String warningLevel) { this.warningLevel = warningLevel; }
    public String getWarningStatus() { return warningStatus; }
    public void setWarningStatus(String warningStatus) { this.warningStatus = warningStatus; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public Long getAiExplanationDraftId() { return aiExplanationDraftId; }
    public void setAiExplanationDraftId(Long aiExplanationDraftId) { this.aiExplanationDraftId = aiExplanationDraftId; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public Long getHandledBy() { return handledBy; }
    public void setHandledBy(Long handledBy) { this.handledBy = handledBy; }
    public String getHandleRemark() { return handleRemark; }
    public void setHandleRemark(String handleRemark) { this.handleRemark = handleRemark; }
    public LocalDateTime getHandledAt() { return handledAt; }
    public void setHandledAt(LocalDateTime handledAt) { this.handledAt = handledAt; }
}
