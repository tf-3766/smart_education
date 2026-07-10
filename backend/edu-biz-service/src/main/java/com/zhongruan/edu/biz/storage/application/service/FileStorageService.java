package com.zhongruan.edu.biz.storage.application.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhongruan.edu.biz.assignment.domain.enums.AssignmentStatus;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.entity.AssignmentAttachmentEntity;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.entity.AssignmentEntity;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.entity.AssignmentSubmissionEntity;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.mapper.AssignmentAttachmentMapper;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.mapper.AssignmentMapper;
import com.zhongruan.edu.biz.assignment.infrastructure.persistence.mapper.AssignmentSubmissionMapper;
import com.zhongruan.edu.biz.auth.domain.enums.RoleCode;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.entity.UserEntity;
import com.zhongruan.edu.biz.auth.infrastructure.persistence.mapper.UserMapper;
import com.zhongruan.edu.biz.course.application.service.CoursePermissionService;
import com.zhongruan.edu.biz.course.infrastructure.persistence.entity.CourseMaterialEntity;
import com.zhongruan.edu.biz.course.infrastructure.persistence.mapper.CourseMaterialMapper;
import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.biz.storage.api.vo.StoredFileVO;
import com.zhongruan.edu.biz.storage.config.FileStorageProperties;
import com.zhongruan.edu.biz.storage.domain.FilePurpose;
import com.zhongruan.edu.biz.storage.domain.FileStatus;
import com.zhongruan.edu.biz.storage.domain.StorageErrorCode;
import com.zhongruan.edu.biz.storage.infrastructure.persistence.entity.StoredFileEntity;
import com.zhongruan.edu.biz.storage.infrastructure.persistence.mapper.StoredFileMapper;
import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.exception.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.DigestInputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {
    private static final Set<String> AVATAR_MIME_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> BLOCKED_MIME_TYPES = Set.of(
            "image/svg+xml",
            "text/html",
            "application/javascript",
            "text/javascript",
            "application/x-msdownload",
            "application/x-sh");

    private final StoredFileMapper fileMapper;
    private final UserMapper userMapper;
    private final CourseMaterialMapper materialMapper;
    private final AssignmentAttachmentMapper attachmentMapper;
    private final AssignmentSubmissionMapper submissionMapper;
    private final AssignmentMapper assignmentMapper;
    private final CoursePermissionService coursePermissionService;
    private final Path storageRoot;
    private final long maxFileSize;

    public FileStorageService(
            StoredFileMapper fileMapper,
            UserMapper userMapper,
            CourseMaterialMapper materialMapper,
            AssignmentAttachmentMapper attachmentMapper,
            AssignmentSubmissionMapper submissionMapper,
            AssignmentMapper assignmentMapper,
            CoursePermissionService coursePermissionService,
            FileStorageProperties properties) {
        this.fileMapper = fileMapper;
        this.userMapper = userMapper;
        this.materialMapper = materialMapper;
        this.attachmentMapper = attachmentMapper;
        this.submissionMapper = submissionMapper;
        this.assignmentMapper = assignmentMapper;
        this.coursePermissionService = coursePermissionService;
        this.storageRoot = properties.root().toAbsolutePath().normalize();
        this.maxFileSize = properties.maxFileSize().toBytes();
    }

    @Transactional
    public StoredFileVO upload(Long userId, MultipartFile upload, FilePurpose purpose) {
        validateUpload(upload, purpose);
        String originalName = safeOriginalName(upload.getOriginalFilename());
        String mimeType = normalizedMimeType(upload.getContentType());
        String objectKey = objectKey(purpose, originalName);
        Path target = resolve(objectKey);
        String sha256 = write(upload, target);

        StoredFileEntity file = new StoredFileEntity();
        file.setOwnerUserId(userId);
        file.setOriginalName(originalName);
        file.setObjectKey(objectKey);
        file.setStorageProvider("LOCAL");
        file.setFileSize(upload.getSize());
        file.setMimeType(mimeType);
        file.setSha256(sha256);
        file.setPurpose(purpose.name());
        file.setFileStatus(FileStatus.ACTIVE.name());
        try {
            fileMapper.insert(file);
        } catch (RuntimeException exception) {
            deletePhysicalFile(target);
            throw exception;
        }
        return toVO(file);
    }

    @Transactional(readOnly = true)
    public StoredFileVO metadata(AuthenticatedUser user, Long fileId) {
        StoredFileEntity file = requireActive(fileId);
        requireAccess(user, file);
        return toVO(file);
    }

    @Transactional(readOnly = true)
    public StoredFileContent content(AuthenticatedUser user, Long fileId) {
        StoredFileEntity file = requireActive(fileId);
        requireAccess(user, file);
        Path path = resolve(file.getObjectKey());
        if (!Files.isRegularFile(path)) {
            throw new BusinessException(StorageErrorCode.FILE_NOT_FOUND);
        }
        return new StoredFileContent(toVO(file), new FileSystemResource(path));
    }

    @Transactional
    public void delete(AuthenticatedUser user, Long fileId) {
        StoredFileEntity file = requireActive(fileId);
        if (!user.userId().equals(file.getOwnerUserId()) && !user.roles().contains(RoleCode.ADMIN.name())) {
            throw new BusinessException(StorageErrorCode.FILE_ACCESS_DENIED);
        }
        if (isReferenced(fileId)) {
            throw new BusinessException(StorageErrorCode.FILE_IN_USE);
        }
        fileMapper.deleteById(fileId);
        deletePhysicalFileOrThrow(resolve(file.getObjectKey()));
    }

    @Transactional(readOnly = true)
    public StoredFileEntity requireOwnedFile(Long userId, Long fileId, FilePurpose purpose) {
        StoredFileEntity file = requireActive(fileId);
        if (!userId.equals(file.getOwnerUserId())) {
            throw new BusinessException(StorageErrorCode.FILE_ACCESS_DENIED);
        }
        if (FilePurpose.valueOf(file.getPurpose()) != purpose) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "文件用途与业务场景不匹配");
        }
        return file;
    }

    public String accessUrl(Long fileId) {
        return "/api/v1/files/" + fileId + "/content";
    }

    private void requireAccess(AuthenticatedUser user, StoredFileEntity file) {
        if (user.userId().equals(file.getOwnerUserId()) || user.roles().contains(RoleCode.ADMIN.name())) {
            return;
        }
        Long fileId = file.getId();
        if (userMapper.selectCount(Wrappers.<UserEntity>lambdaQuery()
                        .eq(UserEntity::getAvatarFileId, fileId))
                > 0) {
            return;
        }
        if (canAccessCourseMaterial(user.userId(), fileId)
                || canAccessAssignmentAttachment(user.userId(), fileId)
                || canAccessSubmission(user.userId(), fileId)) {
            return;
        }
        throw new BusinessException(StorageErrorCode.FILE_ACCESS_DENIED);
    }

    private boolean canAccessCourseMaterial(Long userId, Long fileId) {
        List<CourseMaterialEntity> materials = materialMapper.selectList(
                Wrappers.<CourseMaterialEntity>lambdaQuery().eq(CourseMaterialEntity::getFileId, fileId));
        return materials.stream().anyMatch(material -> coursePermissionService.canAccessMaterial(userId, material.getId()));
    }

    private boolean canAccessAssignmentAttachment(Long userId, Long fileId) {
        List<AssignmentAttachmentEntity> attachments = attachmentMapper.selectList(
                Wrappers.<AssignmentAttachmentEntity>lambdaQuery().eq(AssignmentAttachmentEntity::getFileId, fileId));
        for (AssignmentAttachmentEntity attachment : attachments) {
            AssignmentEntity assignment = assignmentMapper.selectById(attachment.getAssignmentId());
            if (assignment == null) {
                continue;
            }
            if (coursePermissionService.canEditCourseContent(userId, assignment.getCourseId())) {
                return true;
            }
            if (AssignmentStatus.PUBLISHED.name().equals(assignment.getStatus())
                    && coursePermissionService.canViewCourseAsStudent(userId, assignment.getCourseId())) {
                return true;
            }
        }
        return false;
    }

    private boolean canAccessSubmission(Long userId, Long fileId) {
        List<AssignmentSubmissionEntity> submissions = submissionMapper.selectList(
                Wrappers.<AssignmentSubmissionEntity>lambdaQuery().eq(AssignmentSubmissionEntity::getFileId, fileId));
        return submissions.stream().anyMatch(submission -> userId.equals(submission.getStudentId())
                || coursePermissionService.canEditCourseContent(userId, submission.getCourseId()));
    }

    private boolean isReferenced(Long fileId) {
        return userMapper.selectCount(Wrappers.<UserEntity>lambdaQuery().eq(UserEntity::getAvatarFileId, fileId)) > 0
                || materialMapper.selectCount(
                                Wrappers.<CourseMaterialEntity>lambdaQuery().eq(CourseMaterialEntity::getFileId, fileId))
                        > 0
                || attachmentMapper.selectCount(Wrappers.<AssignmentAttachmentEntity>lambdaQuery()
                                .eq(AssignmentAttachmentEntity::getFileId, fileId))
                        > 0
                || submissionMapper.selectCount(Wrappers.<AssignmentSubmissionEntity>lambdaQuery()
                                .eq(AssignmentSubmissionEntity::getFileId, fileId))
                        > 0;
    }

    private StoredFileEntity requireActive(Long fileId) {
        StoredFileEntity file = fileMapper.selectById(fileId);
        if (file == null || !FileStatus.ACTIVE.name().equals(file.getFileStatus())) {
            throw new BusinessException(StorageErrorCode.FILE_NOT_FOUND);
        }
        return file;
    }

    private void validateUpload(MultipartFile upload, FilePurpose purpose) {
        if (upload == null || upload.isEmpty()) {
            throw new BusinessException(CommonErrorCode.PARAM_VALIDATION_ERROR, "上传文件不能为空");
        }
        if (upload.getSize() > maxFileSize) {
            throw new BusinessException(StorageErrorCode.FILE_TOO_LARGE);
        }
        String mimeType = normalizedMimeType(upload.getContentType());
        if (BLOCKED_MIME_TYPES.contains(mimeType)) {
            throw new BusinessException(StorageErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
        if (purpose == FilePurpose.AVATAR && !AVATAR_MIME_TYPES.contains(mimeType)) {
            throw new BusinessException(StorageErrorCode.FILE_TYPE_NOT_ALLOWED, "头像仅支持 JPEG、PNG、WebP 或 GIF");
        }
        if (purpose == FilePurpose.AVATAR && !hasExpectedImageSignature(upload, mimeType)) {
            throw new BusinessException(StorageErrorCode.FILE_TYPE_NOT_ALLOWED, "头像文件内容与声明的图片类型不匹配");
        }
    }

    private String safeOriginalName(String value) {
        if (value == null || value.isBlank()) {
            return "unnamed-file";
        }
        String normalized = value.replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "")
                .trim();
        if (name.isBlank()) {
            return "unnamed-file";
        }
        return name.length() <= 255 ? name : name.substring(name.length() - 255);
    }

    private String objectKey(FilePurpose purpose, String originalName) {
        String extension = safeExtension(originalName);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return purpose.name().toLowerCase(Locale.ROOT) + "/" + today.getYear() + "/"
                + "%02d".formatted(today.getMonthValue()) + "/" + UUID.randomUUID() + extension;
    }

    private String safeExtension(String originalName) {
        int dot = originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) {
            return "";
        }
        String extension = originalName.substring(dot).toLowerCase(Locale.ROOT);
        return extension.matches("\\.[a-z0-9]{1,10}") ? extension : "";
    }

    private String normalizedMimeType(String value) {
        return value == null || value.isBlank() ? "application/octet-stream" : value.toLowerCase(Locale.ROOT);
    }

    private boolean hasExpectedImageSignature(MultipartFile upload, String mimeType) {
        byte[] header;
        try (InputStream input = upload.getInputStream()) {
            header = input.readNBytes(12);
        } catch (IOException exception) {
            throw new BusinessException(CommonErrorCode.FILE_UPLOAD_FAILED);
        }
        return switch (mimeType) {
            case "image/png" -> startsWith(header, new int[] {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            case "image/jpeg" -> startsWith(header, new int[] {0xFF, 0xD8, 0xFF});
            case "image/gif" -> startsWith(header, "GIF87a".getBytes(StandardCharsets.US_ASCII))
                    || startsWith(header, "GIF89a".getBytes(StandardCharsets.US_ASCII));
            case "image/webp" -> startsWith(header, "RIFF".getBytes(StandardCharsets.US_ASCII))
                    && header.length >= 12
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
            default -> false;
        };
    }

    private boolean startsWith(byte[] value, int[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (Byte.toUnsignedInt(value[index]) != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (value[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private Path resolve(String objectKey) {
        Path path = storageRoot.resolve(objectKey).normalize();
        if (!path.startsWith(storageRoot)) {
            throw new BusinessException(StorageErrorCode.FILE_NOT_FOUND);
        }
        return path;
    }

    private String write(MultipartFile upload, Path target) {
        try {
            Files.createDirectories(target.getParent());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = new DigestInputStream(upload.getInputStream(), digest)) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            deletePhysicalFile(target);
            throw new BusinessException(CommonErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private void deletePhysicalFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort cleanup for a failed upload.
        }
    }

    private void deletePhysicalFileOrThrow(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new BusinessException(CommonErrorCode.FILE_UPLOAD_FAILED, "文件删除失败，请稍后重试");
        }
    }

    private StoredFileVO toVO(StoredFileEntity file) {
        OffsetDateTime uploadedAt = file.getCreatedAt() == null ? null : file.getCreatedAt().atOffset(ZoneOffset.UTC);
        return new StoredFileVO(
                String.valueOf(file.getId()),
                file.getOriginalName(),
                file.getObjectKey(),
                accessUrl(file.getId()),
                file.getFileSize(),
                file.getMimeType(),
                file.getSha256(),
                file.getPurpose(),
                uploadedAt,
                file.getVersion());
    }

    public record StoredFileContent(StoredFileVO metadata, Resource resource) {}
}
