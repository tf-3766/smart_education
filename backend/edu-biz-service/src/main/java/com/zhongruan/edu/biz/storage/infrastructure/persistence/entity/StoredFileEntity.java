package com.zhongruan.edu.biz.storage.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhongruan.edu.biz.shared.persistence.BaseAuditEntity;

@TableName("sys_file")
public class StoredFileEntity extends BaseAuditEntity {
    private Long ownerUserId;
    private String originalName;
    private String objectKey;
    private String storageProvider;
    private Long fileSize;
    private String mimeType;
    private String sha256;
    private String purpose;
    private String fileStatus;

    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }
    public String getStorageProvider() { return storageProvider; }
    public void setStorageProvider(String storageProvider) { this.storageProvider = storageProvider; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getFileStatus() { return fileStatus; }
    public void setFileStatus(String fileStatus) { this.fileStatus = fileStatus; }
}
