package com.zhongruan.edu.biz.storage.api.controller;

import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.biz.shared.web.RequestContextFactory;
import com.zhongruan.edu.biz.storage.api.vo.StoredFileVO;
import com.zhongruan.edu.biz.storage.api.vo.FileTextPreviewVO;
import com.zhongruan.edu.biz.storage.application.service.FileStorageService;
import com.zhongruan.edu.biz.storage.application.service.FileStorageService.StoredFileContent;
import com.zhongruan.edu.biz.storage.application.service.FilePreviewService;
import com.zhongruan.edu.biz.storage.application.service.MaterialTextExtractionService;
import com.zhongruan.edu.biz.storage.application.service.MaterialTextExtractionService.ExtractedText;
import com.zhongruan.edu.biz.storage.application.service.FilePreviewService.PreviewImage;
import com.zhongruan.edu.biz.storage.domain.FilePurpose;
import com.zhongruan.edu.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {
    private final FileStorageService service;
    private final FilePreviewService previewService;
    private final MaterialTextExtractionService extractionService;
    private final RequestContextFactory contextFactory;

    public FileController(FileStorageService service, FilePreviewService previewService, MaterialTextExtractionService extractionService, RequestContextFactory contextFactory) {
        this.service = service;
        this.previewService = previewService;
        this.extractionService = extractionService;
        this.contextFactory = contextFactory;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<StoredFileVO>> upload(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "GENERAL") FilePurpose purpose,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.upload(user.userId(), file, purpose), trace(request)));
    }

    @GetMapping("/{fileId}")
    public ApiResponse<StoredFileVO> metadata(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long fileId,
            HttpServletRequest request) {
        return ApiResponse.success(service.metadata(user, fileId), trace(request));
    }

    @GetMapping("/{fileId}/content")
    public ResponseEntity<Resource> content(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long fileId) {
        StoredFileContent content = service.content(user, fileId);
        MediaType mediaType = parseMediaType(content.metadata().mimeType());
        ContentDisposition disposition = mediaType.getType().equals("image")
                ? ContentDisposition.inline()
                        .filename(content.metadata().originalName(), StandardCharsets.UTF_8)
                        .build()
                : ContentDisposition.attachment()
                        .filename(content.metadata().originalName(), StandardCharsets.UTF_8)
                        .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(content.metadata().fileSize())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(content.resource());
    }

    @GetMapping("/{fileId}/preview")
    public ResponseEntity<Resource> preview(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long fileId,
            @RequestParam(defaultValue = "0") int page) {
        PreviewImage preview = previewService.renderPptx(user, fileId, page);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .contentLength(preview.content().length)
                .header("X-Preview-Page", String.valueOf(preview.page()))
                .header("X-Preview-Page-Count", String.valueOf(preview.pageCount()))
                .header("Cache-Control", "private, max-age=300")
                .header("X-Content-Type-Options", "nosniff")
                .body(new ByteArrayResource(preview.content()));
    }
    @GetMapping("/{fileId}/text-preview")
    public ApiResponse<FileTextPreviewVO> textPreview(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long fileId,
            HttpServletRequest request) {
        ExtractedText extracted = extractionService.extract(user, fileId);
        return ApiResponse.success(new FileTextPreviewVO(
                extracted.text(), extracted.status(), extracted.message(), extracted.truncated()), trace(request));
    }

    @DeleteMapping("/{fileId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long fileId,
            HttpServletRequest request) {
        service.delete(user, fileId);
        return ApiResponse.success(trace(request));
    }

    private MediaType parseMediaType(String value) {
        try {
            return MediaType.parseMediaType(value);
        } catch (IllegalArgumentException ignored) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private String trace(HttpServletRequest request) {
        return contextFactory.current(request).traceId();
    }
}
