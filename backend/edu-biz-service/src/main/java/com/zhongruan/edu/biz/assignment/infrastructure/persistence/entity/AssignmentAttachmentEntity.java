package com.zhongruan.edu.biz.assignment.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhongruan.edu.biz.shared.persistence.BaseAuditEntity;

@TableName("edu_assignment_attachment")
public class AssignmentAttachmentEntity extends BaseAuditEntity {
    private Long assignmentId;
    private String name;
    private Long fileId;
    private String fileKey;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
    private Integer sortOrder;

    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }
    public String getFileKey() { return fileKey; }
    public void setFileKey(String fileKey) { this.fileKey = fileKey; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
