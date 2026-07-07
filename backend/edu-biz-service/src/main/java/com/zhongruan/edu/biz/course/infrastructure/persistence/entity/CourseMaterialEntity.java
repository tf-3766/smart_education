package com.zhongruan.edu.biz.course.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhongruan.edu.biz.shared.persistence.BaseAuditEntity;

@TableName("edu_course_material")
public class CourseMaterialEntity extends BaseAuditEntity {
    private Long courseId;
    private Long chapterId;
    private Long lessonId;
    private String name;
    private String materialType;
    private String fileKey;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
    private String visibility;
    private String status;
    private Integer sortOrder;

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getChapterId() { return chapterId; }
    public void setChapterId(Long chapterId) { this.chapterId = chapterId; }
    public Long getLessonId() { return lessonId; }
    public void setLessonId(Long lessonId) { this.lessonId = lessonId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMaterialType() { return materialType; }
    public void setMaterialType(String materialType) { this.materialType = materialType; }
    public String getFileKey() { return fileKey; }
    public void setFileKey(String fileKey) { this.fileKey = fileKey; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
